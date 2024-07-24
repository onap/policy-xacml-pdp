/*-
 * ============LICENSE_START=======================================================
 * Copyright (C) 2019-2021 AT&T Intellectual Property. All rights reserved.
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
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.onap.policy.common.endpoints.event.comm.TopicSink;
import org.onap.policy.common.endpoints.event.comm.client.BidirectionalTopicClient;
import org.onap.policy.common.utils.coder.CoderException;
import org.onap.policy.models.pdp.concepts.PdpStatus;
import org.onap.policy.pdpx.main.XacmlState;

@ExtendWith(MockitoExtension.class)
class XacmlPdpHearbeatPublisherTest {

    private static final long INTERVAL1 = 1000L;
    private static final long INTERVAL2 = 2000L;
    private static final long INTERVAL_INVALID = 0;

    @Mock
    private TopicSink sink;

    @Mock
    private BidirectionalTopicClient checker;

    @Mock
    private XacmlState state;

    @Mock
    private ScheduledExecutorService executor;

    @Mock
    private ScheduledFuture<?> timer1;

    @Mock
    private ScheduledFuture<?> timer2;

    private PdpStatus status;

    private Queue<ScheduledFuture<?>> timers;

    private XacmlPdpHearbeatPublisher publisher;


    /**
     * Initializes objects, including the publisher.
     */
    @BeforeEach
    void setUp() {
        lenient().when(sink.getTopic()).thenReturn("my-topic");
        lenient().when(checker.getSink()).thenReturn(sink);
        lenient().when(checker.isReady()).thenReturn(true);
        lenient().when(state.genHeartbeat()).thenReturn(status);

        status = new PdpStatus();
        timers = new LinkedList<>(Arrays.asList(timer1, timer2));

        lenient().when(executor.scheduleWithFixedDelay(any(), anyLong(), anyLong(), any()))
            .thenAnswer(args -> timers.remove());

        publisher = new MyPublisher(checker, 10, state);
    }

    @Test
    void testRun() {
        publisher.run();

        verify(state).genHeartbeat();
        verify(checker).send(any());
    }

    /**
     * Tests the run() method when the probe is disabled.
     */
    @Test
    void testRunNoProbe() throws CoderException {
        publisher = new MyPublisher(checker, 0, state);

        publisher.run();

        verify(checker, never()).isReady();
        verify(checker, never()).awaitReady(any(), anyLong());

        verify(state).genHeartbeat();
        verify(checker).send(any());
    }

    /**
     * Tests the run() method when the topic is not ready, and then becomes ready.
     */
    @Test
    void testRunNotReady() throws CoderException {
        // not ready yet
        lenient().when(checker.isReady()).thenReturn(false);
        lenient().when(checker.awaitReady(any(), anyLong())).thenReturn(false);

        publisher.run();
        verify(state, never()).genHeartbeat();
        verify(checker, never()).send(any());

        // isReady is still false, but awaitReady is now true - should generate heartbeat
        lenient().when(checker.awaitReady(any(), anyLong())).thenReturn(true);

        publisher.run();
        verify(state).genHeartbeat();
        verify(checker).send(any());

        // now isReady is true, too - should not rerun awaitReady
        lenient().when(checker.isReady()).thenReturn(true);

        publisher.run();
        verify(state, times(2)).genHeartbeat();
        verify(checker, times(2)).send(any());
        verify(checker, times(2)).awaitReady(any(), anyLong());
    }

    /**
     * Tests the run() method when the checker throws an exception.
     */
    @Test
    void testRunCheckerEx() throws CoderException {
        // force it to call awaitReady
        lenient().when(checker.isReady()).thenReturn(false);

        lenient().when(checker.awaitReady(any(), anyLong()))
            .thenThrow(new CoderException("expected exception"))
            .thenReturn(true);

        // exception thrown - should not generate heartbeat
        publisher.run();
        verify(state, never()).genHeartbeat();
        verify(checker, never()).send(any());

        // no exception this time - SHOULD generate heartbeat
        publisher.run();
        verify(state).genHeartbeat();
        verify(checker).send(any());
    }

    @Test
    void testTerminate() {
        // not yet started
        publisher.terminate();

        verify(checker).stopWaiting();


        // now start it and then try again
        publisher.start();
        publisher.terminate();

        // timer2 should still be in the queue
        assertSame(timer2, timers.peek());


        // repeat - nothing more should happen
        publisher.terminate();

        // timer2 should still be in the queue
        assertSame(timer2, timers.peek());
    }

    @Test
    void testRestart() {
        // not started yet - should only update the interval
        publisher.restart(INTERVAL1);

        assertEquals(INTERVAL1, publisher.getIntervalMs());
        assertSame(timer1, timers.peek());

        // now start it
        publisher.start();
        verify(executor).scheduleWithFixedDelay(publisher, 0, INTERVAL1, TimeUnit.MILLISECONDS);

        // null interval - no changes
        publisher.restart(null);
        verify(executor, times(1)).scheduleWithFixedDelay(any(), anyLong(), anyLong(), any());
        assertSame(timer2, timers.peek());

        // same interval - no changes
        publisher.restart(INTERVAL1);
        verify(executor, times(1)).scheduleWithFixedDelay(any(), anyLong(), anyLong(), any());
        assertSame(timer2, timers.peek());

        // invalid interval - no changes
        publisher.restart(INTERVAL_INVALID);
        verify(executor, times(1)).scheduleWithFixedDelay(any(), anyLong(), anyLong(), any());
        assertSame(timer2, timers.peek());

        // new interval - old timer should be cancelled and new started
        publisher.restart(INTERVAL2);
        verify(timer1).cancel(anyBoolean());
        verify(executor).scheduleWithFixedDelay(publisher, 0, INTERVAL2, TimeUnit.MILLISECONDS);
    }

    @Test
    void testStart() {
        publisher.start();

        verify(executor).scheduleWithFixedDelay(publisher, 0, XacmlPdpHearbeatPublisher.DEFAULT_HB_INTERVAL_MS,
            TimeUnit.MILLISECONDS);

        // repeat - nothing more should happen
        publisher.start();
        verify(executor, times(1)).scheduleWithFixedDelay(any(), anyLong(), anyLong(), any());
        verify(timer1, never()).cancel(anyBoolean());
    }

    @Test
    void testMakeTimerThread() {
        // create a plain listener to test the "real" makeTimer() method
        publisher = new XacmlPdpHearbeatPublisher(checker, 1, state);

        assertThatCode(() -> {
            publisher.start();
            publisher.restart(100L);
            publisher.terminate();
        }).doesNotThrowAnyException();
    }

    private class MyPublisher extends XacmlPdpHearbeatPublisher {

        MyPublisher(BidirectionalTopicClient topicChecker, long probeHeartbeatTopicMs, XacmlState state) {
            super(topicChecker, probeHeartbeatTopicMs, state);
        }

        @Override
        protected ScheduledExecutorService makeTimerThread() {
            return executor;
        }
    }
}
