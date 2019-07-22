/*-
 * ============LICENSE_START=======================================================
 * Copyright (C) 2019 AT&T Intellectual Property. All rights reserved.
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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Timer;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.onap.policy.common.endpoints.event.comm.client.TopicSinkClient;
import org.onap.policy.models.pdp.concepts.PdpStatus;
import org.onap.policy.pdpx.main.XacmlState;

public class XacmlPdpHearbeatPublisherTest {

    private static final long INTERVAL1 = 1000L;
    private static final long INTERVAL2 = 2000L;
    private static final long INTERVAL_INVALID = 0;

    @Mock
    private TopicSinkClient client;

    @Mock
    private XacmlState state;

    @Mock
    private Timer timer1;

    @Mock
    private Timer timer2;

    @Mock
    private PdpStatus status;

    private Queue<Timer> timers;

    private XacmlPdpHearbeatPublisher publisher;


    /**
     * Initializes objects, including the publisher.
     */
    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        when(state.genHeartbeat()).thenReturn(status);

        timers = new LinkedList<>(Arrays.asList(timer1, timer2));

        publisher = new MyPublisher(client, state);
    }

    @Test
    public void testRun() {
        publisher.run();

        verify(state).genHeartbeat();
        verify(client).send(status);
    }

    @Test
    public void testTerminate() {
        // not yet started
        publisher.terminate();

        verify(timer1, never()).cancel();
        verify(timer2, never()).cancel();


        // now start it and then try again
        publisher.start();
        publisher.terminate();

        verify(timer1).cancel();

        // timer2 should still be in the queue
        assertSame(timer2, timers.peek());


        // repeat - nothing more should happen
        publisher.terminate();

        verify(timer1, times(1)).cancel();

        // timer2 should still be in the queue
        assertSame(timer2, timers.peek());
    }

    @Test
    public void testRestart() {
        // not started yet - should only update the interval
        publisher.restart(INTERVAL1);

        assertEquals(INTERVAL1, publisher.getIntervalMs());
        assertSame(timer1, timers.peek());

        // now start it
        publisher.start();
        verify(timer1).scheduleAtFixedRate(publisher, 0, INTERVAL1);

        // null interval - no changes
        publisher.restart(null);
        verify(timer1, times(1)).scheduleAtFixedRate(any(), anyLong(), anyLong());
        assertSame(timer2, timers.peek());

        // same interval - no changes
        publisher.restart(INTERVAL1);
        verify(timer1, times(1)).scheduleAtFixedRate(any(), anyLong(), anyLong());
        assertSame(timer2, timers.peek());

        // invalid interval - no changes
        publisher.restart(INTERVAL_INVALID);
        verify(timer1, times(1)).scheduleAtFixedRate(any(), anyLong(), anyLong());
        assertSame(timer2, timers.peek());

        // new interval - old timer should be cancelled and new started
        publisher.restart(INTERVAL2);
        verify(timer1).cancel();
        verify(timer2).scheduleAtFixedRate(publisher, 0, INTERVAL2);
    }

    @Test
    public void testStart() {
        publisher.start();

        verify(timer1).scheduleAtFixedRate(publisher, 0, XacmlPdpHearbeatPublisher.DEFAULT_INTERVAL_MS);

        // repeat - nothing more should happen
        publisher.start();
        verify(timer1, times(1)).scheduleAtFixedRate(any(), anyLong(), anyLong());
        verify(timer1, never()).cancel();
    }

    @Test
    public void testMakeTimer() {
        // create a plain listener to test the "real" makeTimer() method
        publisher = new XacmlPdpHearbeatPublisher(client, state);

        publisher.start();
        publisher.terminate();
    }

    private class MyPublisher extends XacmlPdpHearbeatPublisher {

        public MyPublisher(TopicSinkClient topicSinkClient, XacmlState state) {
            super(topicSinkClient, state);
        }

        @Override
        protected Timer makeTimer() {
            return timers.remove();
        }
    }
}
