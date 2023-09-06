/*-
 * ============LICENSE_START=======================================================
 * Copyright (C) 2019-2020, 2022 AT&T Intellectual Property. All rights reserved.
 * Modifications Copyright (C) 2023 Nordix Foundation.
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

import com.att.research.xacml.api.Request;
import com.att.research.xacml.api.Response;
import com.att.research.xacml.api.Result;
import java.util.Map;
import org.apache.commons.lang3.tuple.Pair;
import org.onap.policy.models.decisions.concepts.DecisionException;
import org.onap.policy.models.decisions.concepts.DecisionRequest;
import org.onap.policy.models.decisions.concepts.DecisionResponse;
import org.onap.policy.pdp.xacml.application.common.XacmlApplicationServiceProvider;
import org.onap.policy.pdpx.main.rest.XacmlPdpApplicationManager;
import org.onap.policy.pdpx.main.rest.XacmlPdpStatisticsManager;
import org.onap.policy.xacml.pdp.application.nativ.NativePdpApplication;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class DecisionProvider {
    private static final Logger LOGGER = LoggerFactory.getLogger(DecisionProvider.class);

    /**
     * Retrieves the policy decision for the specified parameters.
     *
     * @param request DecisionRequest
     * @param queryParams Map of parameters
     * @return DecisionResponse
     */
    public DecisionResponse fetchDecision(DecisionRequest request, Map<String, String[]> queryParams) {
        LOGGER.debug("Fetching decision {}", request);
        //
        // Find application for this decision
        //
        XacmlApplicationServiceProvider application = findApplication(request);
        //
        // Found application for action
        //
        Pair<DecisionResponse, Response> decision = application.makeDecision(request, queryParams);
        //
        // Calculate statistics
        //
        this.calculateStatistic(decision.getValue(), application.applicationName());
        //
        // Return the decision
        //
        return decision.getKey();
    }

    /**
     * Retrieves the policy decision for the native xacml request.
     *
     * @param request the xacml request
     * @return the xacml response
     */
    public Response fetchNativeDecision(Request request) {
        LOGGER.debug("Fetching decision {}", request);
        //
        // Assign native request to native application directly
        //
        XacmlApplicationServiceProvider nativeApp = findNativeApplication();
        //
        // Make xacml decision
        //
        Response decision = ((NativePdpApplication) nativeApp).makeNativeDecision(request);
        LOGGER.debug("Xacml decision {}", decision);
        //
        // Calculate statistics
        //
        this.calculateStatistic(decision, nativeApp.applicationName());
        //
        // Return the string decision
        //
        return decision;
    }

    private XacmlApplicationServiceProvider findApplication(DecisionRequest request) {
        XacmlApplicationServiceProvider application = XacmlPdpApplicationManager.getCurrent().findApplication(request);
        if (application != null) {
            return application;
        }
        throw new DecisionException(jakarta.ws.rs.core.Response.Status.BAD_REQUEST,
                "No application for action " + request.getAction());
    }

    private XacmlApplicationServiceProvider findNativeApplication() {
        XacmlApplicationServiceProvider application = XacmlPdpApplicationManager.getCurrent().findNativeApplication();
        if (application instanceof NativePdpApplication) {
            return application;
        }
        throw new DecisionException(jakarta.ws.rs.core.Response.Status.INTERNAL_SERVER_ERROR,
                "Native PDP application cannot be found");
    }

    private void calculateStatistic(Response xacmlResponse, String appName) {
        if (xacmlResponse == null) {
            XacmlPdpStatisticsManager.getCurrent().updateErrorCount();
            return;
        }
        for (Result result : xacmlResponse.getResults()) {
            switch (result.getDecision()) {
                case PERMIT:
                    XacmlPdpStatisticsManager.getCurrent().updatePermitDecisionsCount(appName);
                    break;

                case DENY:
                    XacmlPdpStatisticsManager.getCurrent().updateDenyDecisionsCount(appName);
                    break;

                case INDETERMINATE, INDETERMINATE_DENY, INDETERMINATE_DENYPERMIT, INDETERMINATE_PERMIT:
                    XacmlPdpStatisticsManager.getCurrent().updateIndeterminantDecisionsCount(appName);
                    break;

                case NOTAPPLICABLE:
                    XacmlPdpStatisticsManager.getCurrent().updateNotApplicableDecisionsCount(appName);
                    break;

                default:
                    break;
            }
        }
    }
}
