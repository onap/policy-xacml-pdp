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
import java.util.Arrays;
import java.util.Map;
import java.util.Map.Entry;
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

    private boolean abbreviateResults = false;

    public DecisionProvider() {}

    public DecisionProvider(Map<String, String[]> queryParams) {
        checkQueryParams(queryParams);
    }

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
        Pair<DecisionResponse, Response> decision = application.makeDecision(request);
        //
        // Calculate statistics
        //
        this.calculateStatistic(decision.getValue());
        //
        // Abbreviate results if needed
        //
        if (abbreviateResults && decision.getKey().getPolicies() != null
                && !decision.getKey().getPolicies().isEmpty()) {
            LOGGER.info("Abbreviating decision results {}", decision);
            for (Entry<String, Object> entry : decision.getKey().getPolicies().entrySet()) {
                if (entry.getValue() instanceof Map) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> policy = (Map<String, Object>) entry.getValue();
                    policy.remove("properties");
                    policy.remove("name");
                    policy.remove("version");
                }
            }
        }
        //
        // Return the decision
        //
        return decision.getKey();
    }

    /**
     * Checks the query parameters to determine what option(s) were specified in the request.
     *
     * @param decisionQueryParams - http request query parameters
     */
    private void checkQueryParams(Map<String, String[]> decisionQueryParams) {
        // Check if query params contains "abbrev" flag
        if (decisionQueryParams.containsKey("abbrev")) {
            this.abbreviateResults = Arrays.asList(decisionQueryParams.get("abbrev")).contains("true");
        } else {
            LOGGER.info("Invalid query param - no matches: {}", decisionQueryParams.toString());
        }
    }

    private XacmlApplicationServiceProvider findApplication(DecisionRequest request) {
        XacmlApplicationServiceProvider application = XacmlPdpApplicationManager.getCurrent().findApplication(request);
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
                    XacmlPdpStatisticsManager.getCurrent().updatePermitDecisionsCount();
                    break;

                case DENY:
                    XacmlPdpStatisticsManager.getCurrent().updateDenyDecisionsCount();
                    break;

                case INDETERMINATE:
                case INDETERMINATE_DENY:
                case INDETERMINATE_DENYPERMIT:
                case INDETERMINATE_PERMIT:
                    XacmlPdpStatisticsManager.getCurrent().updateIndeterminantDecisionsCount();
                    break;

                case NOTAPPLICABLE:
                    XacmlPdpStatisticsManager.getCurrent().updateNotApplicableDecisionsCount();
                    break;

                default:
                    break;

            }
        }
    }

}
