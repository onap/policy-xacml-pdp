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

import org.onap.policy.common.endpoints.event.comm.client.TopicSinkClient;
import org.onap.policy.models.pdp.concepts.PdpStatus;
import org.onap.policy.models.pdp.concepts.PdpUpdate;
import org.onap.policy.models.tosca.authorative.concepts.ToscaPolicy;
import org.onap.policy.pdpx.main.rest.XacmlPdpApplicationManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class XacmlPdpUpdatePublisher {

    private static final Logger LOGGER = LoggerFactory.getLogger(XacmlPdpUpdatePublisher.class);

    private XacmlPdpUpdatePublisher() {
        throw new IllegalStateException("Please do not create private instance of XacmlPdpUpdatePublisher");
    }

    /**
     * Handle the PDP Update message.
     *
     * @param message Incoming message
     * @param client TopicSinkClient
     */
    public static void handlePdpUpdate(PdpUpdate message, TopicSinkClient client) {

        if (!message.getPolicies().isEmpty() || message.getPolicies() != null) {
            // Load the policies on PDP applications
            for (ToscaPolicy toscaPolicy : message.getPolicies()) {
                XacmlPdpApplicationManager.loadDeployedPolicy(toscaPolicy);
            }
        }

        XacmlPdpMessage updatePdpMessage = new XacmlPdpMessage();
        PdpStatus statusMessage = updatePdpMessage.formatPdpUpdateMessage(message, XacmlPdpHearbeatPublisher.pdpState);
        sendPdpUpdate(statusMessage, client);
    }

    private static void sendPdpUpdate(PdpStatus status, TopicSinkClient client) {
        // Send PdpStatus Change to PAP
        if (!client.send(status)) {
            LOGGER.error("failed to send to topic sink {}", client.getTopic());
        }
    }

}
