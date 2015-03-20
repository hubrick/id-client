/**
 * Copyright (C) 2015 Etaia AS (oss@hubrick.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.hubrick.client.id.flurry;

import com.google.common.collect.LinkedHashMultimap;
import com.google.common.collect.Multimap;
import com.hubrick.client.id.IdClient;
import com.hubrick.client.id.exception.ConnectionException;
import com.hubrick.client.id.flurry.thrift.Flurry;
import org.apache.thrift.TException;
import org.apache.thrift.protocol.TBinaryProtocol;
import org.apache.thrift.protocol.TProtocol;
import org.apache.thrift.transport.TSocket;
import org.apache.thrift.transport.TTransport;
import org.apache.thrift.transport.TTransportException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static com.google.common.base.Preconditions.checkArgument;

/**
 * @author Emir Dizdarevic
 * @since 1.0.0
 */
public class FlurryIdClient implements IdClient {

    private static final Logger log = LoggerFactory.getLogger(FlurryIdClient.class);
    private final AtomicInteger threadSequenceNumber = new AtomicInteger(0);

    private final BlockingQueue<Long> idQueue;
    private final long waitOnFailMillis;
    private final Executor clientExecutor;

    private FlurryIdClient(Multimap<String, Integer> hostPortMap, int queueSize, int threadPoolSize, long waitOnFailMillis) throws ConnectionException {
        checkArgument(queueSize >= 2, "queueSize must be greater then 2");
        checkArgument(threadPoolSize >= 2, "threadPoolSize must be greater then 2");

        this.idQueue = new ArrayBlockingQueue<Long>(queueSize);
        this.waitOnFailMillis = waitOnFailMillis;
        this.clientExecutor = Executors.newFixedThreadPool(threadPoolSize, new ThreadFactory() {
            @Override
            public Thread newThread(Runnable r) {
                final Thread thread = new Thread(r);
                thread.setDaemon(true);
                thread.setName("flurry-id-fetcher-" + threadSequenceNumber.getAndIncrement());
                return thread;
            }
        });
        initFetcher(threadPoolSize, hostPortMap);
    }

    @Override
    public Long getIdNonBlocking() {
        return idQueue.poll();
    }


    @Override
    public Long getId() throws InterruptedException {
        try {
            return idQueue.take();
        } catch (InterruptedException e) {
            log.error("Failed to get id from queue", e);
            throw e;
        }
    }

    @Override
    public Long getId(long timeout) throws InterruptedException {
        try {
            return idQueue.poll(timeout, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            log.error("Failed to get id from queue", e);
            throw e;
        }
    }

    private void initFetcher(int count, Multimap<String, Integer> hostPortMap) {
        for (int i = 0; i < count; i++) {
            final List<ClientHolder> clients = createClients(hostPortMap);
            checkCollisions(clients);
            clientExecutor.execute(new IdFetcher(clients));
        }
    }

    private void checkCollisions(List<ClientHolder> clientHolders) {
        try {
            final Map<Long, ClientDetail> workerIdClientDetailMap = new HashMap<Long, ClientDetail>();
            for (ClientHolder clientHolder : clientHolders) {
                final Flurry.Client client = clientHolder.getClient();
                final Long workerId = client.get_worker_id();
                if (workerIdClientDetailMap.containsKey(workerId)) {
                    final ClientDetail presentClientDetail = workerIdClientDetailMap.get(workerId);
                    final String currentHostPort = clientHolder.getClientDetail().getHost() + ":" + clientHolder.getClientDetail().getPort();
                    final String presentHostPort = presentClientDetail.getHost() + ":" + presentClientDetail.getPort();
                    throw new IllegalStateException("Failed to create client. Found worker id collision. Worker id: " + workerId + " on hosts [" + currentHostPort + "," + presentHostPort + "]");
                }

                workerIdClientDetailMap.put(workerId, clientHolder.getClientDetail());
            }
        } catch (TException e) {
            throw new ConnectionException("Failed to connect to flurry server", e);
        }
    }

    private List<ClientHolder> createClients(Multimap<String, Integer> hostPortMap) throws ConnectionException {
        try {
            final List<ClientHolder> clients = new ArrayList<ClientHolder>(hostPortMap.size());
            for (Map.Entry<String, Integer> hostPortEntry : hostPortMap.entries()) {
                final ClientDetail clientDetail = new ClientDetail(hostPortEntry.getKey(), hostPortEntry.getValue());
                clients.add(new ClientHolder(clientDetail, createClient(clientDetail)));
            }
            return clients;
        } catch (TTransportException e) {
            throw new ConnectionException("Failed to connect to Flurry server", e);
        }
    }

    private Flurry.Client createClient(ClientDetail clientDetail) throws TTransportException {
        final TTransport transport = new TSocket(clientDetail.getHost(), clientDetail.getPort());
        transport.open();

        TProtocol protocol = new TBinaryProtocol(transport);
        return new Flurry.Client(protocol);
    }

    private class IdFetcher implements Runnable {

        private long clientSequenceNumber = 0;
        private final List<ClientHolder> clients;

        private IdFetcher(List<ClientHolder> clients) {
            this.clients = clients;
        }

        @Override
        public void run() {
            while (true) {
                try {
                    final ClientHolder clientHolder = getClientHolder();
                    final Flurry.Client availableClient = clientHolder.getClient();
                    try {
                        idQueue.put(availableClient.get_id());
                    } catch (TException e) {
                        final String hostPort = clientHolder.getClientDetail().getHost() + ":" + clientHolder.getClientDetail().getPort();
                        log.error("Failed to fetch id from service {}. Suspending client for the next {}ms", hostPort, waitOnFailMillis, e);
                        clientHolder.setWaitUntil(System.currentTimeMillis() + waitOnFailMillis);
                    } catch (InterruptedException e) {
                        log.error("Failed to enqueue id", e);
                    }
                } catch (TTransportException e) {
                    log.error("Failed to connect to Flurry server", e);
                }
            }
        }

        private ClientHolder getClientHolder() throws TTransportException {
            while (true) {
                final int clientIndex = (int) (clientSequenceNumber  % clients.size());
                clientSequenceNumber += 1;
                final ClientHolder clientHolder = clients.get(clientIndex);
                if (clientHolder.getWaitUntil() == -1) {
                    return clientHolder;
                } else if (clientHolder.getWaitUntil() > System.currentTimeMillis()) {
                    clientHolder.setWaitUntil(-1);
                    clientHolder.setClient(createClient(clientHolder.getClientDetail()));
                    return clientHolder;
                }
            }
        }
    }

    public static class Builder {

        private final Multimap<String, Integer> hostPortMap = LinkedHashMultimap.create();
        private int queueSize = 75;
        private int threadPoolSize = 8;
        private long waitOnFailMillis = 1000;
        private boolean valid = false;

        public Builder addServiceEndpoint(String host, int port) {
            hostPortMap.put(host, port);
            valid = true;
            return this;
        }

        public Builder withQueueSize(int queueSize) {
            this.queueSize = queueSize;
            return this;
        }

        public Builder withThreadPoolSize(int threadPoolSize) {
            this.threadPoolSize = threadPoolSize;
            return this;
        }

        public Builder withWaitOnFailMillis(long waitOnFailMillis) {
            this.waitOnFailMillis = waitOnFailMillis;
            return this;
        }

        public FlurryIdClient build() {
            if (!valid) {
                throw new IllegalStateException("At least one service endpoint must be added.");
            }

            return new FlurryIdClient(hostPortMap, queueSize, threadPoolSize, waitOnFailMillis);
        }

    }
}
