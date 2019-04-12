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

package org.onap.policy.pdpx.main.rest.provider;

import com.att.research.xacml.api.Response;
import com.att.research.xacml.api.Result;

import org.apache.commons.lang3.tuple.Pair;
import org.onap.policy.models.decisions.concepts.DecisionException;
import org.onap.policy.models.decisions.concepts.DecisionRequest;
import org.onap.policy.models.decisions.concepts.DecisionResponse;
import org.onap.policy.pdp.xacml.application.common.XacmlApplicationServiceProvider;
import org.onap.policy.pdpx.main.rest.XacmlPdpApplicationManager;
import org.onap.policy.pdpx.main.rest.XacmlPdpStatisticsManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class DecisionProvider {
    private static final Logger LOGGER = LoggerFactory.getLogger(DecisionProvider.class);

    /**
     * Retrieves the policy decision for the specified parameters.
     * @param body
     *
     * @return the Decision object
     */
    public DecisionResponse fetchDecision(DecisionRequest request) {
        LOGGER.debug("Fetching decision {}", request);
        //
        // Find application for this decision
        //
        XacmlApplicationServiceProvider application = findApplication(request);
        //
        // Found application for action
        //
        Pair<DecisionResponse, Response> decision;
        try {
            decision = application.makeDecision(request);
        } catch (Exception e) {
            LOGGER.error("makeDecision failed", e);
            throw e;
        }
        //
        // Calculate statistics
        //
        this.calculateStatistic(decision.getValue());
        //
        // Return the decision
        //
        return decision.getKey();
    }

    private XacmlApplicationServiceProvider findApplication(DecisionRequest request) {
        XacmlApplicationServiceProvider application = XacmlPdpApplicationManager.findApplication(request);
        if (application != null) {
            return application;
        }
        throw new DecisionException(javax.ws.rs.core.Response.Status.BAD_REQUEST,
                "No application for action " + request.getAction());
    }

    private void calculateStatistic(Response xacmlResponse) {

        for (Result result : xacmlResponse.getResults()) {
            switch (result.getDecision()) {
                case PERMIT:
                    XacmlPdpStatisticsManager.updatePermitDecisionsCount();
                    break;

                case DENY:
                    XacmlPdpStatisticsManager.updateDenyDecisionsCount();
                    break;

                case INDETERMINATE:
                case INDETERMINATE_DENY:
                case INDETERMINATE_DENYPERMIT:
                case INDETERMINATE_PERMIT:
                    XacmlPdpStatisticsManager.updateIndeterminantDecisionsCount();
                    break;

                case NOTAPPLICABLE:
                    XacmlPdpStatisticsManager.updateNotApplicableDecisionsCount();
                    break;

                default:
                    break;

            }
        }
    }

}
