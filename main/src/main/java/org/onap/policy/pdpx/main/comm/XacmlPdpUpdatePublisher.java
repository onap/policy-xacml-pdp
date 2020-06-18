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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import org.onap.policy.common.endpoints.event.comm.client.TopicSinkClient;
import org.onap.policy.models.pdp.concepts.PdpStatus;
import org.onap.policy.models.pdp.concepts.PdpUpdate;
import org.onap.policy.models.tosca.authorative.concepts.ToscaPolicy;
import org.onap.policy.pdp.xacml.application.common.XacmlApplicationException;
import org.onap.policy.pdp.xacml.application.common.XacmlPolicyUtils;
import org.onap.policy.pdpx.main.XacmlState;
import org.onap.policy.pdpx.main.rest.XacmlPdpApplicationManager;
import org.onap.policy.pdpx.main.rest.XacmlPdpStatisticsManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class XacmlPdpUpdatePublisher {

    private static final Logger LOGGER = LoggerFactory.getLogger(XacmlPdpUpdatePublisher.class);

    private final TopicSinkClient client;
    private final XacmlState state;
    private final XacmlPdpApplicationManager appManager;

    /**
     * Constructs the object.
     * @param client messages are published to this client
     * @param state tracks the state of this PDP
     * @param appManager application manager
     */
    public XacmlPdpUpdatePublisher(TopicSinkClient client, XacmlState state, XacmlPdpApplicationManager appManager) {
        this.client = client;
        this.state = state;
        this.appManager = appManager;
    }

    /**
     * Handle the PDP Update message.
     *
     * @param message Incoming message
     */
    public void handlePdpUpdate(PdpUpdate message) {

        Set<ToscaPolicy> incomingPolicies =
                new HashSet<>(message.getPolicies() == null ? Collections.emptyList() : message.getPolicies());
        Set<ToscaPolicy> deployedPolicies =
                new HashSet<>(appManager.getToscaPolicies().keySet());

        // Undeploy a policy
        // if incoming policies do not contain the deployed policy then remove it from PDP
        for (ToscaPolicy policy : deployedPolicies) {
            if (!incomingPolicies.contains(policy)) {
                appManager.removeUndeployedPolicy(policy);
            }
        }

        StringBuilder errorMessage = new StringBuilder();
        // Deploy a policy
        // if deployed policies do not contain the incoming policy load it
        for (ToscaPolicy policy : incomingPolicies) {
            if (!deployedPolicies.contains(policy)) {
                try {
                    appManager.loadDeployedPolicy(policy);
                } catch (XacmlApplicationException e) {
                    // Failed to load policy, return error(s) to PAP
                    LOGGER.error("Failed to load policy: {}", policy, e);
                    errorMessage.append("Failed to load policy: " + policy + ": "
                            + e.getMessage() + XacmlPolicyUtils.LINE_SEPARATOR);
                }
            }
        }
        // Return current deployed policies
        message.setPolicies(new ArrayList<>(appManager.getToscaPolicies().keySet()));
        LOGGER.debug("Returning current deployed policies: {} ", message.getPolicies());

        // update the policy count statistic
        XacmlPdpStatisticsManager stats = XacmlPdpStatisticsManager.getCurrent();
        if (stats != null) {
            stats.setTotalPolicyCount(appManager.getPolicyCount());
        }

        sendPdpUpdate(state.updateInternalState(message, errorMessage.toString()));
    }

    private void sendPdpUpdate(PdpStatus status) {
        // Send PdpStatus Change to PAP
        if (!client.send(status)) {
            LOGGER.error("failed to send to topic sink {}", client.getTopic());
        }
    }

}
