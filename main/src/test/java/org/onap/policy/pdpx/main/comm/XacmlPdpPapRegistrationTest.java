/*-
 * ============LICENSE_START=======================================================
 * Copyright (C) 2019, 2021 AT&T Intellectual Property. All rights reserved.
 * Modifications Copyright (C) 2024 Nordix Foundation.
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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.onap.policy.common.message.bus.event.client.TopicSinkClient;
import org.onap.policy.models.pdp.concepts.PdpStatus;

@ExtendWith(MockitoExtension.class)
class XacmlPdpPapRegistrationTest {

    @Mock
    private TopicSinkClient client;

    @Mock
    private PdpStatus status;

    private XacmlPdpPapRegistration reg;

    /**
     * Initializes objects, including the registration object.
     */
    @BeforeEach
    void setUp() {
        when(client.send(status)).thenReturn(true);

        reg = new XacmlPdpPapRegistration(client);
    }

    @Test
    void testPdpRegistration_SendOk() {
        assertThatCode(() ->
            reg.pdpRegistration(status)
        ).doesNotThrowAnyException();
    }

    @Test
    void testPdpRegistration_SendFail() {
        when(client.send(status)).thenReturn(false);
        assertThatCode(() ->
            reg.pdpRegistration(status)
        ).doesNotThrowAnyException();
    }

    @Test
    void testPdpRegistration_SendEx() {
        when(client.send(status)).thenThrow(new IllegalStateException());
        assertThatCode(() ->
            reg.pdpRegistration(status)
        ).doesNotThrowAnyException();
    }
}
