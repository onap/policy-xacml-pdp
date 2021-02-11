/*-
 * ============LICENSE_START=======================================================
 * Copyright (C) 2019, 2021 AT&T Intellectual Property. All rights reserved.
 * ================================================================================
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * SPDX-License-Identifier: Apache-2.0
 * ============LICENSE_END=========================================================
 */

package org.onap.policy.pdpx.main.comm;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.onap.policy.common.endpoints.event.comm.client.TopicSinkClient;
import org.onap.policy.common.endpoints.event.comm.client.TopicSinkClientException;
import org.onap.policy.models.pdp.concepts.PdpStatus;

@RunWith(MockitoJUnitRunner.class)
public class XacmlPdpPapRegistrationTest {

    @Mock
    private TopicSinkClient client;

    @Mock
    private PdpStatus status;

    private XacmlPdpPapRegistration reg;

    /**
     * Initializes objects, including the registration object.
     */
    @Before
    public void setUp() {
        when(client.send(status)).thenReturn(true);

        reg = new XacmlPdpPapRegistration(client);
    }

    @Test
    public void testPdpRegistration_SendOk() throws TopicSinkClientException {
        assertThatCode(() ->
            reg.pdpRegistration(status)
        ).doesNotThrowAnyException();
    }

    @Test
    public void testPdpRegistration_SendFail() throws TopicSinkClientException {
        when(client.send(status)).thenReturn(false);
        assertThatCode(() ->
            reg.pdpRegistration(status)
        ).doesNotThrowAnyException();
    }

    @Test
    public void testPdpRegistration_SendEx() throws TopicSinkClientException {
        when(client.send(status)).thenThrow(new IllegalStateException());
        assertThatCode(() ->
            reg.pdpRegistration(status)
        ).doesNotThrowAnyException();
    }
}
