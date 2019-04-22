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
import org.onap.policy.common.utils.network.NetworkUtil;
import org.onap.policy.models.pdp.concepts.PdpStateChange;
import org.onap.policy.models.pdp.concepts.PdpStatus;
import org.onap.policy.models.pdp.enums.PdpState;
import org.onap.policy.pdpx.main.comm.XacmlPdpHearbeatPublisher;
import org.onap.policy.pdpx.main.comm.XacmlPdpMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class XacmlPdpStateChangeListener extends ScoListener<PdpStateChange> {

    private static final Logger LOGGER = LoggerFactory.getLogger(XacmlPdpStateChangeListener.class);

    private TopicSinkClient client;

    private XacmlPdpHearbeatPublisher heartbeat;
    private XacmlPdpMessage pdpInternalStatus;

    /**
     * Constructs the object.
     *
     * @param client used to send back response after receiving state change message
     */
    public XacmlPdpStateChangeListener(TopicSinkClient client, XacmlPdpMessage pdpStatusMessage) {
        super(PdpStateChange.class);
        PdpStateChange message = new PdpStateChange();
        message.setState(PdpState.PASSIVE);
        this.pdpInternalStatus = pdpStatusMessage;
        this.client = client;
        this.heartbeat = new XacmlPdpHearbeatPublisher(client, pdpStatusMessage);
    }

    @Override
    public void onTopicEvent(CommInfrastructure infra, String topic, StandardCoderObject sco, PdpStateChange message) {

        try {

            if (message.appliesTo(pdpInternalStatus.getPdpName(), pdpInternalStatus.getPdpGroup(),
                    pdpInternalStatus.getPdpSubGroup())) {

                pdpInternalStatus.updateInternalStatus(message);
                PdpStatus newStatus = pdpInternalStatus.formatPdpStatusMessage();

                // Send State Change Status to PAP
                if (!client.send(newStatus)) {
                    LOGGER.error("failed to send to topic sink {}", client.getTopic());
                    throw new TopicSinkClientException("failed to send to topic sink " + client.getTopic());
                }

                // Update the heartbeat internal state if publisher is running else create new publisher
                if (XacmlPdpHearbeatPublisher.isAlive()) {
                    heartbeat.updateInternalState(pdpInternalStatus);
                } else {
                    heartbeat = new XacmlPdpHearbeatPublisher(client, pdpInternalStatus);
                }
            }
        } catch (final Exception e) {
            LOGGER.error("failed to handle the PDP State Change message.", e);
        }

    }
}
