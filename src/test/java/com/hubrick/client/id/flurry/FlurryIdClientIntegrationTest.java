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

import com.hubrick.client.id.IdClient;
import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.Matchers.greaterThan;
import static org.junit.Assert.assertThat;

public class FlurryIdClientIntegrationTest {

    private IdClient idClient;

    @Before
    public void setUp() throws Exception {
        idClient = new FlurryIdClient.Builder().addServiceEndpoint("localhost", 9090).build();
    }

    @Test
    public void testGetIdNonBlocking() throws Exception {
        Thread.sleep(1000);
        assertThat(idClient.getIdNonBlocking(), greaterThan(0l));
    }

    @Test
    public void testGetId() throws Exception {
        assertThat(idClient.getId(), greaterThan(0l));
    }

    @Test
    public void testGetIdWithTimeout() throws Exception {
        assertThat(idClient.getId(1000), greaterThan(0l));
    }
}
