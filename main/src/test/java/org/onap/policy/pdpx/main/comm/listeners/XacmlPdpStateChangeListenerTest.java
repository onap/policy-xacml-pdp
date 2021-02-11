/*-
 * ============LICENSE_START=======================================================
 * Copyright (C) 2019-2021 AT&T Intellectual Property. All rights reserved.
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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.onap.policy.common.endpoints.event.comm.Topic.CommInfrastructure;
import org.onap.policy.common.endpoints.event.comm.client.TopicSinkClient;
import org.onap.policy.models.pdp.concepts.PdpStateChange;
import org.onap.policy.models.pdp.concepts.PdpStatus;
import org.onap.policy.pdpx.main.XacmlState;

@RunWith(MockitoJUnitRunner.class)
public class XacmlPdpStateChangeListenerTest {
    private static final String TOPIC = "my-topic";

    @Mock
    private TopicSinkClient client;

    @Mock
    private XacmlState state;

    @Mock
    private PdpStatus status;

    @Mock
    private PdpStateChange change;

    private XacmlPdpStateChangeListener listener;

    /**
     * Initializes objects, including the listener.
     */
    @Before
    public void setUp() {
        listener = new XacmlPdpStateChangeListener(client, state);

        when(state.shouldHandle(change)).thenReturn(true);
        when(state.updateInternalState(change)).thenReturn(status);

        when(client.send(status)).thenReturn(true);
    }

    @Test
    public void testOnTopicEvent_Unhandled() {
        when(state.shouldHandle(change)).thenReturn(false);
        listener.onTopicEvent(CommInfrastructure.NOOP, TOPIC, null, change);

        verify(state, never()).updateInternalState(any(PdpStateChange.class));
        verify(client, never()).send(any());
    }

    @Test
    public void testOnTopicEvent_SendFailed() {
        when(client.send(status)).thenReturn(false);

        listener.onTopicEvent(CommInfrastructure.NOOP, TOPIC, null, change);

        verify(state).updateInternalState(change);
        verify(client).send(status);
    }

    @Test
    public void testOnTopicEvent_SendOk() {
        listener.onTopicEvent(CommInfrastructure.NOOP, TOPIC, null, change);

        verify(state).updateInternalState(change);
        verify(client).send(status);
    }

}
