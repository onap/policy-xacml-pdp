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

import java.util.Timer;
import java.util.TimerTask;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import org.onap.policy.common.endpoints.event.comm.client.TopicSinkClient;
import org.onap.policy.pdpx.main.XacmlState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class XacmlPdpHearbeatPublisher extends TimerTask {

    private static final Logger LOGGER = LoggerFactory.getLogger(XacmlPdpHearbeatPublisher.class);

    private final TopicSinkClient topicSinkClient;

    /**
     * Tracks the state of this PDP.
     */
    private final XacmlState currentState;

    /**
     * Current timer interval, in milliseconds.
     */
    private long intervalMs = 60000;

    private Timer timer;

    @Getter
    @Setter(AccessLevel.PRIVATE)
    private volatile boolean alive = false;


    /**
     * Constructor for instantiating XacmlPdpPublisher.
     *
     * @param topicSinkClient used to send heart beat message
     * @param state tracks the state of this PDP
     */
    public XacmlPdpHearbeatPublisher(TopicSinkClient topicSinkClient, XacmlState state) {
        this.topicSinkClient = topicSinkClient;
        this.currentState = state;
    }

    @Override
    public void run() {
        LOGGER.info("Sending Xacml PDP heartbeat to the PAP");

        topicSinkClient.send(currentState.genHeartbeat());
    }

    /**
     * Method to terminate the heart beat.
     */
    public void terminate() {
        if (isAlive()) {
            timer.cancel();
            timer.purge();
            setAlive(false);
        }
    }

    /**
     * Restarts the timer if the interval has changed.
     *
     * @param intervalMs desired interval, or {@code null} to leave it unchanged
     */
    public void restart(Long intervalMs) {
        if (intervalMs != null && intervalMs > 0 && intervalMs != this.intervalMs) {
            terminate();

            this.intervalMs = intervalMs;
            start();
        }
    }

    /**
     * Starts the timer.
     */
    public void start() {
        if (!isAlive()) {
            this.timer = new Timer(false);
            this.timer.scheduleAtFixedRate(this, 0, this.intervalMs);
            setAlive(true);
        }
    }
}
