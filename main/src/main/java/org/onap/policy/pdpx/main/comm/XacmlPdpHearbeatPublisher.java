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
import org.onap.policy.common.endpoints.event.comm.client.TopicSinkClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class XacmlPdpHearbeatPublisher extends TimerTask {

    private static final Logger LOGGER = LoggerFactory.getLogger(XacmlPdpHearbeatPublisher.class);

    private Timer timer;
    private XacmlPdpMessage heartbeatMessage;
    private static TopicSinkClient topicSinkClient;
    private static volatile  boolean alive = false;

    /**
     * Constructor for instantiating XacmlPdpPublisher.
     *
     * @param message of the PDP
     * @param topicSinkClient used to send heartbeat message
     */
    public XacmlPdpHearbeatPublisher(TopicSinkClient topicSinkClient, XacmlPdpMessage message ) {
        this.topicSinkClient = topicSinkClient;
        this.heartbeatMessage = message;
        timer = new Timer(false);
        timer.scheduleAtFixedRate(this, 0, 60000); // time interval temp hard coded now but will be parameterized
        setAlive(true);
    }

    @Override
    public void run() {
        topicSinkClient.send(heartbeatMessage.formatPdpStatusMessage());
        LOGGER.info("Sending Xacml PDP heartbeat to the PAP");
    }

    /**
     * Method to terminate the heartbeat.
     */
    public void terminate() {
        timer.cancel();
        timer.purge();
        setAlive(false);
    }

    public static boolean isAlive() {
        return alive;
    }

    public void setAlive(boolean alive) {
        this.alive = alive;
    }
}
