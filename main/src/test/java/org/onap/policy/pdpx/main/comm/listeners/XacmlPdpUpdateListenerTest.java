/*-
 * ============LICENSE_START=======================================================
 * Copyright (C) 2019-2020 AT&T Intellectual Property. All rights reserved.
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

import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.onap.policy.common.endpoints.event.comm.Topic.CommInfrastructure;
import org.onap.policy.common.endpoints.event.comm.client.TopicSinkClient;
import org.onap.policy.models.pdp.concepts.PdpUpdate;
import org.onap.policy.pdpx.main.XacmlState;
import org.onap.policy.pdpx.main.comm.XacmlPdpHearbeatPublisher;
import org.onap.policy.pdpx.main.comm.XacmlPdpUpdatePublisher;
import org.onap.policy.pdpx.main.rest.XacmlPdpApplicationManager;
import org.powermock.reflect.Whitebox;

public class XacmlPdpUpdateListenerTest {
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
    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        listener = new MyListener(client, state, heartbeat, appmgr);
        update = new PdpUpdate();

        when(state.shouldHandle(update)).thenReturn(true);

        update.setPdpHeartbeatIntervalMs(HB_INTERVAL);
    }

    @Test
    public void testOnTopicEvent_Unhandled() {
        when(state.shouldHandle(update)).thenReturn(false);
        listener.onTopicEvent(CommInfrastructure.NOOP, TOPIC, null, update);

        verify(publisher, never()).handlePdpUpdate(any());
        verify(heartbeat, never()).restart(anyLong());
    }

    @Test
    public void testOnTopicEvent_SendOk() {
        listener.onTopicEvent(CommInfrastructure.NOOP, TOPIC, null, update);

        verify(publisher).handlePdpUpdate(update);
        verify(heartbeat).restart(HB_INTERVAL);
    }

    @Test
    public void testOnTopicEvent_SendEx() {
        doThrow(new RuntimeException(EXPECTED_EXCEPTION)).when(publisher).handlePdpUpdate(update);

        listener.onTopicEvent(CommInfrastructure.NOOP, TOPIC, null, update);

        verify(publisher).handlePdpUpdate(update);
        verify(heartbeat, never()).restart(anyLong());
    }

    @Test
    public void testMakePublisher() {
        // create a plain listener to test the "real" makePublisher() method
        listener = new XacmlPdpUpdateListener(client, state, heartbeat, appmgr);
        assertNotNull(Whitebox.getInternalState(listener, "publisher"));
    }

    private class MyListener extends XacmlPdpUpdateListener {

        public MyListener(TopicSinkClient client, XacmlState state, XacmlPdpHearbeatPublisher heartbeat,
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
