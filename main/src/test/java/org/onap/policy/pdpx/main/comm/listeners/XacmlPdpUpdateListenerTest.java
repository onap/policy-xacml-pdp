/*-
 * ============LICENSE_START=======================================================
 * Copyright (C) 2019-2021 AT&T Intellectual Property. All rights reserved.
 * Modifications Copyright (C) 2023-2024 Nordix Foundation.
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

package org.onap.policy.pdpx.main.comm.listeners;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.onap.policy.common.message.bus.event.Topic.CommInfrastructure;
import org.onap.policy.common.message.bus.event.client.TopicSinkClient;
import org.onap.policy.models.pdp.concepts.PdpUpdate;
import org.onap.policy.pdpx.main.XacmlState;
import org.onap.policy.pdpx.main.comm.XacmlPdpHearbeatPublisher;
import org.onap.policy.pdpx.main.comm.XacmlPdpUpdatePublisher;
import org.onap.policy.pdpx.main.rest.XacmlPdpApplicationManager;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class XacmlPdpUpdateListenerTest {
    private static final String EXPECTED_EXCEPTION = "expected exception";
    private static final String TOPIC = "my-topic";
    private static final long HB_INTERVAL = 100L;

    @Mock
    private TopicSinkClient client;

    @Mock
    private XacmlState state;

    @Mock
    private XacmlPdpHearbeatPublisher heartbeat;

    @Mock
    private XacmlPdpApplicationManager appmgr;

    @Mock
    private XacmlPdpUpdatePublisher publisher;

    private PdpUpdate update;

    private XacmlPdpUpdateListener listener;

    /**
     * Initializes objects, including the listener.
     */
    @BeforeEach
    void setUp() {
        listener = new MyListener(client, state, heartbeat, appmgr);
        update = new PdpUpdate();

        lenient().when(state.shouldHandle(update)).thenReturn(true);

        update.setPdpHeartbeatIntervalMs(HB_INTERVAL);
    }

    @Test
    void testOnTopicEvent_Unhandled() {
        lenient().when(state.shouldHandle(update)).thenReturn(false);
        listener.onTopicEvent(CommInfrastructure.NOOP, TOPIC, null, update);

        verify(publisher, never()).handlePdpUpdate(any());
        verify(heartbeat, never()).restart(anyLong());
    }

    @Test
    void testOnTopicEvent_SendOk() {
        listener.onTopicEvent(CommInfrastructure.NOOP, TOPIC, null, update);

        verify(publisher).handlePdpUpdate(update);
        verify(heartbeat).restart(HB_INTERVAL);
    }

    @Test
    void testOnTopicEvent_SendEx() {
        lenient().doThrow(new RuntimeException(EXPECTED_EXCEPTION)).when(publisher).handlePdpUpdate(update);

        listener.onTopicEvent(CommInfrastructure.NOOP, TOPIC, null, update);

        verify(publisher).handlePdpUpdate(update);
        verify(heartbeat, never()).restart(anyLong());
    }

    @Test
    void testMakePublisher() {
        // create a plain listener to test the "real" makePublisher() method
        listener = new XacmlPdpUpdateListener(client, state, heartbeat, appmgr);
        assertNotNull(ReflectionTestUtils.getField(listener, "publisher"));
    }

    private class MyListener extends XacmlPdpUpdateListener {

        MyListener(TopicSinkClient client, XacmlState state, XacmlPdpHearbeatPublisher heartbeat,
                   XacmlPdpApplicationManager appManager) {
            super(client, state, heartbeat, appManager);
        }

        @Override
        protected XacmlPdpUpdatePublisher makePublisher(TopicSinkClient client, XacmlState state,
                                                        XacmlPdpApplicationManager appManager) {
            return publisher;
        }
    }
}
