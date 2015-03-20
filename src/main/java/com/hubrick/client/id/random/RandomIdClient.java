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
package com.hubrick.client.id.random;

import com.hubrick.client.id.IdClient;

import java.util.Random;

/**
 * @author Emir Dizdarevic
 * @since 1.0.0
 *
 * Returns simple random long numbers. Should not be used in production.
 */
public class RandomIdClient implements IdClient {

    private final Random random = new Random();

    @Override
    public Long getIdNonBlocking() {
        return random.nextLong();
    }

    @Override
    public Long getId() throws InterruptedException {
        return random.nextLong();
    }

    @Override
    public Long getId(long timeout) throws InterruptedException {
        return random.nextLong();
    }
}
