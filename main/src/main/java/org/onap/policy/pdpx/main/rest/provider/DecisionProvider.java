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
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.http.entity.ContentType;
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
    private static final String APPLICATION_XACML_XML = "application/xacml+xml";

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
        this.calculateStatistic(decision.getValue());
        //
        // Return the decision
        //
        return decision.getKey();
    }

    /**
     * Retrieves the policy decision for the native xacml request.
     *
     * @param request the raw http servlet request
     * @return the xacml response in string
     */
    public String fetchNativeDecision(HttpServletRequest request) {
        LOGGER.debug("Fetching decision {}", request);
        //
        // Assign native request to native application directly
        //
        XacmlApplicationServiceProvider nativeApp = new NativePdpApplication();
        //
        // Get and validate the content-type
        //
        ContentType contentType = ContentType.parse(request.getContentType());
        validateContentType(contentType);
        //
        // Get the raw request
        //
        String incomingRequestString;
        try {
            incomingRequestString = getRequestAsString(request);
        } catch (IOException exc) {
            String errMsg = "failed to get the raw request as string";
            throw new DecisionException(javax.ws.rs.core.Response.Status.BAD_REQUEST, errMsg);
        }
        //
        // Make xacml decision
        //
        Pair<String, Response> decision = nativeApp.makeDecision(contentType, incomingRequestString);
        LOGGER.debug("Xacml decision {}", decision);
        //
        // Calculate statistics
        this.calculateStatistic(decision.getValue());
        //
        // Return the string decision
        //
        return decision.getKey();
    }

    private void validateContentType(ContentType contentType) {
        if (!(contentType.getMimeType().equalsIgnoreCase(ContentType.APPLICATION_JSON.getMimeType())
                || contentType.getMimeType().equalsIgnoreCase(APPLICATION_XACML_XML))) {
            String errMsg = "unsupported content type";
            throw new DecisionException(javax.ws.rs.core.Response.Status.BAD_REQUEST, errMsg);
        }
    }

    private String getRequestAsString(HttpServletRequest request) throws IOException {
        StringBuilder buffer = new StringBuilder();
        BufferedReader reader = new BufferedReader(new InputStreamReader(request.getInputStream()));
        String line;
        while((line = reader.readLine()) != null){
            buffer.append(line);
        }
        return buffer.toString();
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
