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
package com.hubrick.client.id;

/**
 * @author Emir Dizdarevic
 * @since 1.0.0
 *
 * The actual id client interface.
 */
public interface IdClient {

    /**
     * Get the id in a non blocking way.
     *
     * @return If id available returns id otherwise null.
     */
    Long getIdNonBlocking();

    /**
     * Get the id in a blocking way.
     *
     * @return The id when available.
     * @throws InterruptedException If the thread is interrupted.
     */
    Long getId() throws InterruptedException;

    /**
     * Get the id in a blocking way with timeout.
     *
     * @return The id when available.
     * @throws InterruptedException If the thread is interrupted or the time run out.
     */
    Long getId(long timeout) throws InterruptedException;
}
