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

package org.onap.policy.pdpx.main.comm.listeners;

import org.onap.policy.common.endpoints.event.comm.Topic.CommInfrastructure;
import org.onap.policy.common.endpoints.event.comm.client.TopicSinkClient;
import org.onap.policy.common.endpoints.event.comm.client.TopicSinkClientException;
import org.onap.policy.common.endpoints.listeners.ScoListener;
import org.onap.policy.common.utils.coder.StandardCoderObject;
import org.onap.policy.models.pdp.concepts.PdpStateChange;
import org.onap.policy.models.pdp.concepts.PdpStatus;
import org.onap.policy.models.pdp.enums.PdpState;
import org.onap.policy.pdpx.main.comm.XacmlPdpHeartbeatPublisher;
import org.onap.policy.pdpx.main.comm.XacmlPdpMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class XacmlPdpStateChangeListener extends ScoListener<PdpStateChange> {

    private static final Logger LOGGER = LoggerFactory.getLogger(XacmlPdpStateChangeListener.class);

    private TopicSinkClient client;

    private XacmlPdpHeartbeatPublisher heartbeat;

    /**
     * Constructs the object.
     *
     * @param client used to send back response after receiving state change message
     */
    public XacmlPdpStateChangeListener(TopicSinkClient client) {
        super(PdpStateChange.class);
        heartbeat = new XacmlPdpHeartbeatPublisher(client, PdpState.PASSIVE);
        this.client = client;
    }

    @Override
    public void onTopicEvent(CommInfrastructure infra, String topic, StandardCoderObject sco, PdpStateChange message) {

        XacmlPdpMessage newMessage = new XacmlPdpMessage();
        try {
            PdpStatus newStatus = newMessage.formatStatusMessage(message.getState());

            // Send State Change Status to PAP
            if (!client.send(newStatus)) {
                LOGGER.error("failed to send to topic sink " + client.getTopic());
                throw new TopicSinkClientException("failed to send to topic sink " + client.getTopic());
            }

            // Update the heartbeat internal state if publisher is running else create new publisher
            if (XacmlPdpHeartbeatPublisher.isAlive()) {
                heartbeat.updateInternalState(message.getState());
            } else {
                heartbeat = new XacmlPdpHeartbeatPublisher(client, message.getState());
            }

        } catch (final Exception e) {
            LOGGER.error("failed to handle the PDP State Change message.", e);
        }

    }
}
