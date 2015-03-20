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

import com.hubrick.client.id.flurry.thrift.Flurry;

/**
 * @author Emir Dizdarevic
 * @since 1.0.0
 */
class ClientHolder {

    private final ClientDetail clientDetail;
    private long waitUntil;
    private Flurry.Client client;

    public ClientHolder(ClientDetail clientDetail, Flurry.Client client) {
       this(clientDetail, client, -1);
    }

    public ClientHolder(ClientDetail clientDetail, Flurry.Client client, long waitUntil) {
        this.clientDetail = clientDetail;
        this.client = client;
        this.waitUntil = waitUntil;
    }

    public ClientDetail getClientDetail() {
        return clientDetail;
    }

    public Flurry.Client getClient() {
        return client;
    }

    public void setClient(Flurry.Client client) {
        this.client = client;
    }

    public long getWaitUntil() {
        return waitUntil;
    }

    public void setWaitUntil(long waitUntil) {
        this.waitUntil = waitUntil;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ClientHolder that = (ClientHolder) o;

        if (waitUntil != that.waitUntil) return false;
        if (client != null ? !client.equals(that.client) : that.client != null) return false;
        if (clientDetail != null ? !clientDetail.equals(that.clientDetail) : that.clientDetail != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = clientDetail != null ? clientDetail.hashCode() : 0;
        result = 31 * result + (int) (waitUntil ^ (waitUntil >>> 32));
        result = 31 * result + (client != null ? client.hashCode() : 0);
        return result;
    }
}
