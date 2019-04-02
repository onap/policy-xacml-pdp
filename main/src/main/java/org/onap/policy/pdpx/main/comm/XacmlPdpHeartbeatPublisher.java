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
import org.onap.policy.models.pdp.enums.PdpState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class XacmlPdpHeartbeatPublisher extends TimerTask {

    private static final Logger LOGGER = LoggerFactory.getLogger(XacmlPdpHeartbeatPublisher.class);

    private TopicSinkClient topicSinkClient;
    private Timer timer;
    private XacmlPdpMessage heartbeatMessage;
    private PdpState pdpState;

    private static volatile  boolean alive = false;

    /**
     * Constructor for instantiating XacmlPdpHeartbeatPublisher.
     *
     * @param state of the PDP
     * @param topicSinkClient used to send heartbeat message
     */
    public XacmlPdpHeartbeatPublisher(TopicSinkClient topicSinkClient, PdpState state) {
        this.topicSinkClient = topicSinkClient;
        this.heartbeatMessage = new XacmlPdpMessage();
        this.pdpState = state;
        timer = new Timer(false);
        timer.scheduleAtFixedRate(this, 0, 60000); // time interval temp hard coded now but will be parameterized
        setAlive(true);
    }

    @Override
    public void run() {
        topicSinkClient.send(heartbeatMessage.formatStatusMessage(pdpState));
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

    public void updateInternalState(PdpState state) {
        this.pdpState = state;
    }

    public static boolean isAlive() {
        return alive;
    }

    public void setAlive(boolean alive) {
        this.alive = alive;
    }

}
