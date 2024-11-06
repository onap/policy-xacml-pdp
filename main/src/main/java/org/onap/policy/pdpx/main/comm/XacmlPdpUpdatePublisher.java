/*-
 * ============LICENSE_START=======================================================
 * Copyright (C) 2019-2022 AT&T Intellectual Property. All rights reserved.
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

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import org.onap.policy.common.message.bus.event.client.TopicSinkClient;
import org.onap.policy.models.pdp.concepts.PdpStatus;
import org.onap.policy.models.pdp.concepts.PdpUpdate;
import org.onap.policy.models.tosca.authorative.concepts.ToscaConceptIdentifier;
import org.onap.policy.models.tosca.authorative.concepts.ToscaPolicy;
import org.onap.policy.pdp.xacml.application.common.XacmlApplicationException;
import org.onap.policy.pdp.xacml.application.common.XacmlPolicyUtils;
import org.onap.policy.pdpx.main.XacmlState;
import org.onap.policy.pdpx.main.rest.XacmlPdpApplicationManager;
import org.onap.policy.pdpx.main.rest.XacmlPdpStatisticsManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@AllArgsConstructor
public class XacmlPdpUpdatePublisher {

    private static final Logger LOGGER = LoggerFactory.getLogger(XacmlPdpUpdatePublisher.class);

    private final TopicSinkClient client;
    private final XacmlState state;
    private final XacmlPdpApplicationManager appManager;

    /**
     * Handle the PDP Update message.
     *
     * @param message Incoming message
     */
    public synchronized void handlePdpUpdate(PdpUpdate message) {

        // current data
        Map<ToscaConceptIdentifier, ToscaPolicy> deployedPolicies = policyToMap(appManager.getToscaPolicies().keySet());

        // incoming data
        Map<ToscaConceptIdentifier, ToscaPolicy> toBeDeployedPolicies = policyToMap(message.getPoliciesToBeDeployed());
        List<ToscaConceptIdentifier> toBeUndeployedIds =
            Optional.ofNullable(message.getPoliciesToBeUndeployed()).orElse(Collections.emptyList());

        var stats = XacmlPdpStatisticsManager.getCurrent();

        // Undeploy policies
        for (ToscaConceptIdentifier policyId : toBeUndeployedIds) {
            ToscaPolicy policy = deployedPolicies.get(policyId);
            if (policy == null) {
                LOGGER.warn("attempt to undeploy policy that has not been previously deployed: {}", policyId);
                stats.updateUndeployFailureCount();
            } else if (toBeDeployedPolicies.containsKey(policyId)) {
                LOGGER.warn("not undeploying policy, as it also appears in the deployment list: {}", policyId);
                stats.updateUndeployFailureCount();
            } else {
                appManager.removeUndeployedPolicy(policy);
                stats.updateUndeploySuccessCount();
            }
        }

        var errorMessage = new StringBuilder();
        // Deploy a policy
        // if deployed policies do not contain the incoming policy load it
        for (ToscaPolicy policy : toBeDeployedPolicies.values()) {
            if (!deployedPolicies.containsKey(policy.getIdentifier())) {
                try {
                    appManager.loadDeployedPolicy(policy);
                    stats.updateDeploySuccessCount();
                } catch (XacmlApplicationException e) {
                    // Failed to load policy, return error(s) to PAP
                    LOGGER.error("Failed to load policy: {}", policy, e);
                    errorMessage.append("Failed to load policy: ").append(policy).append(": ").append(e.getMessage())
                        .append(XacmlPolicyUtils.LINE_SEPARATOR);
                    stats.updateDeployFailureCount();
                }
            }
        }

        // update the policy count statistic
        stats.setTotalPolicyCount(appManager.getPolicyCount());

        PdpStatus status = state.updateInternalState(message, errorMessage.toString());
        LOGGER.debug("Returning current deployed policies: {} ", status.getPolicies());

        sendPdpUpdate(status);
    }

    private Map<ToscaConceptIdentifier, ToscaPolicy> policyToMap(Collection<ToscaPolicy> policies) {
        if (policies == null) {
            return Collections.emptyMap();
        }

        return policies.stream().collect(Collectors.toMap(ToscaPolicy::getIdentifier, policy -> policy));
    }

    private void sendPdpUpdate(PdpStatus status) {
        // Send PdpStatus Change to PAP
        if (!client.send(status)) {
            LOGGER.error("failed to send to topic sink {}", client.getTopic());
        }
    }
}
