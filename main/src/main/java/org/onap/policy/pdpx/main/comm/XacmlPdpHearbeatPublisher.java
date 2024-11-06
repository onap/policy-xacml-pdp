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

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import lombok.Getter;
import org.onap.policy.common.message.bus.event.client.BidirectionalTopicClient;
import org.onap.policy.common.utils.coder.Coder;
import org.onap.policy.common.utils.coder.CoderException;
import org.onap.policy.common.utils.coder.StandardCoder;
import org.onap.policy.models.pdp.concepts.PdpStatus;
import org.onap.policy.models.pdp.concepts.PdpTopicCheck;
import org.onap.policy.pdpx.main.XacmlState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class XacmlPdpHearbeatPublisher implements Runnable {
    public static final int DEFAULT_HB_INTERVAL_MS = 60000;

    private static final Logger LOGGER = LoggerFactory.getLogger(XacmlPdpHearbeatPublisher.class);
    private static final Coder CODER = new StandardCoder();

    private final BidirectionalTopicClient topicChecker;
    private final long probeHeartbeatTopicMs;

    /**
     * Tracks the state of this PDP.
     */
    private final XacmlState currentState;

    /**
     * Current timer interval, in milliseconds.
     */
    @Getter
    private long intervalMs = DEFAULT_HB_INTERVAL_MS;

    private ScheduledExecutorService timerThread;

    private ScheduledFuture<?> timer;


    /**
     * Constructor for instantiating XacmlPdpPublisher.
     *
     * @param topicChecker used to check the topic before sending heart beat message
     * @param probeHeartbeatTopicMs frequency, in milliseconds, with which to probe the
     *        heartbeat topic before sending the first heartbeat. Zero disables probing
     * @param state tracks the state of this PDP
     */
    public XacmlPdpHearbeatPublisher(BidirectionalTopicClient topicChecker, long probeHeartbeatTopicMs,
                                     XacmlState state) {
        LOGGER.info("heartbeat topic probe {}ms", probeHeartbeatTopicMs);
        this.topicChecker = topicChecker;
        this.probeHeartbeatTopicMs = probeHeartbeatTopicMs;
        this.currentState = state;
    }

    @Override
    public void run() {
        try {
            if (!isTopicReady()) {
                return;
            }

            PdpStatus message = currentState.genHeartbeat();
            LOGGER.info("Sending Xacml PDP heartbeat to the PAP - {}", message);

            String json = CODER.encode(message);
            topicChecker.send(json);

        } catch (RuntimeException | CoderException e) {
            LOGGER.warn("send to {} failed because of {}", topicChecker.getSink().getTopic(), e.getMessage(), e);
        }
    }

    private boolean isTopicReady() throws CoderException {
        if (probeHeartbeatTopicMs <= 0 || topicChecker.isReady()) {
            return true;
        }

        var check = new PdpTopicCheck();
        check.setName(XacmlState.PDP_NAME);
        return topicChecker.awaitReady(check, probeHeartbeatTopicMs);
    }

    /**
     * Method to terminate the heart beat.
     */
    public synchronized void terminate() {
        topicChecker.stopWaiting();

        if (timerThread != null) {
            timerThread.shutdownNow();
            timerThread = null;
            timer = null;
        }
    }

    /**
     * Restarts the timer if the interval has changed. If the timer is not currently
     * running, then it updates the interval, but does not start the timer.
     *
     * @param intervalMs desired interval, or {@code null} to leave it unchanged
     */
    public synchronized void restart(Long intervalMs) {
        if (intervalMs != null && intervalMs > 0 && intervalMs != this.intervalMs) {
            this.intervalMs = intervalMs;

            if (timerThread != null) {
                timer.cancel(false);
                timer = timerThread.scheduleWithFixedDelay(this, 0, this.intervalMs, TimeUnit.MILLISECONDS);
            }
        }
    }

    /**
     * Starts the timer.
     */
    public synchronized void start() {
        if (timerThread == null) {
            timerThread = makeTimerThread();
            timer = timerThread.scheduleWithFixedDelay(this, 0, this.intervalMs, TimeUnit.MILLISECONDS);
        }
    }

    // these may be overridden by junit tests

    protected ScheduledExecutorService makeTimerThread() {
        return Executors.newScheduledThreadPool(1);
    }
}
