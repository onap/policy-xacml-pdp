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
import org.onap.policy.common.endpoints.listeners.ScoListener;
import org.onap.policy.common.utils.coder.StandardCoderObject;
import org.onap.policy.models.pdp.concepts.PdpUpdate;
import org.onap.policy.pdpx.main.comm.XacmlPdpMessage;
import org.onap.policy.pdpx.main.comm.XacmlPdpUpdatePublisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class XacmlPdpUpdateListener extends ScoListener<PdpUpdate> {

    private static final Logger LOGGER = LoggerFactory.getLogger(XacmlPdpStateChangeListener.class);

    private TopicSinkClient client;
    private XacmlPdpMessage pdpInternalStatus;

    /**
     * Constructs the object.
     *
     * @param client used to send back response after receiving state change message
     */
    public XacmlPdpUpdateListener(TopicSinkClient client, XacmlPdpMessage pdpStatusMessage) {
        super(PdpUpdate.class);
        this.client = client;
        this.pdpInternalStatus = pdpStatusMessage;
    }

    @Override
    public void onTopicEvent(CommInfrastructure infra, String topic, StandardCoderObject sco, PdpUpdate message) {

        try {

            LOGGER.info("PDP update message has been received from the PAP - {}", message.toString());

            if (message.appliesTo(pdpInternalStatus.getPdpName(), pdpInternalStatus.getPdpGroup(),
                    pdpInternalStatus.getPdpSubGroup())) {

                XacmlPdpUpdatePublisher.handlePdpUpdate(message, client, pdpInternalStatus);
            }

        } catch (final Exception e) {
            LOGGER.error("failed to handle the PDP Update message.", e);
        }

    }

}
