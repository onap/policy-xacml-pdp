/*-
 * ============LICENSE_START=======================================================
 * ONAP
 * ================================================================================
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

package org.onap.policy.xacml.pdp.application.monitoring;

import com.att.research.xacml.api.Request;
import com.att.research.xacml.api.Response;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import org.apache.commons.lang3.tuple.Pair;
import org.onap.policy.models.decisions.concepts.DecisionRequest;
import org.onap.policy.models.decisions.concepts.DecisionResponse;
import org.onap.policy.models.tosca.authorative.concepts.ToscaPolicyTypeIdentifier;
import org.onap.policy.pdp.xacml.application.common.ToscaPolicyTranslator;
import org.onap.policy.pdp.xacml.application.common.std.StdCombinedPolicyResultsTranslator;
import org.onap.policy.pdp.xacml.application.common.std.StdXacmlApplicationServiceProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This is the engine class that manages the instance of the XACML PDP engine.
 *
 * <p>It is responsible for initializing it and shutting it down properly in a thread-safe manner.
 *
 *
 * @author pameladragosh
 *
 */
public class MonitoringPdpApplication extends StdXacmlApplicationServiceProvider {
    private static final Logger LOGGER = LoggerFactory.getLogger(MonitoringPdpApplication.class);

    // Note: this requirement is temporary; it will no longer be necessary once the PDPs and PAP
    // are updated to use the PDP Group name instead of the supported types.
    private static final String ONAP_MONITORING_OTHER_POLICY_TYPE = "onap.policies.monitoring.*";

    private static final String ONAP_MONITORING_BASE_POLICY_TYPE = "onap.Monitoring";
    private static final String ONAP_MONITORING_CDAP = "onap.policies.monitoring.cdap.tca.hi.lo.app";
    private static final String ONAP_MONITORING_APPSERVER =
            "onap.policies.monitoring.dcaegen2.collectors.datafile.datafile-app-server";
    private static final String ONAP_MONITORING_SONHANDLER = "onap.policies.monitoring.docker.sonhandler.app";
    private static final String ONAP_MONITORING_DERIVED_POLICY_TYPE = "onap.policies.monitoring";
    private static final String VERSION_100 = "1.0.0";

    private StdCombinedPolicyResultsTranslator translator = new StdCombinedPolicyResultsTranslator();
    private List<ToscaPolicyTypeIdentifier> supportedPolicyTypes = new ArrayList<>();

    /**
     * Constructor.
     */
    public MonitoringPdpApplication() {
        //
        // By default this supports just Monitoring policy types
        //
        supportedPolicyTypes.add(new ToscaPolicyTypeIdentifier(ONAP_MONITORING_BASE_POLICY_TYPE, VERSION_100));
        supportedPolicyTypes.add(new ToscaPolicyTypeIdentifier(ONAP_MONITORING_CDAP, VERSION_100));
        supportedPolicyTypes.add(new ToscaPolicyTypeIdentifier(ONAP_MONITORING_APPSERVER, VERSION_100));
        supportedPolicyTypes.add(new ToscaPolicyTypeIdentifier(ONAP_MONITORING_SONHANDLER, VERSION_100));

        // temporary requirement
        supportedPolicyTypes.add(new ToscaPolicyTypeIdentifier(ONAP_MONITORING_OTHER_POLICY_TYPE, VERSION_100));
    }

    @Override
    public String applicationName() {
        return "monitoring";
    }

    @Override
    public List<String> actionDecisionsSupported() {
        return Arrays.asList("configure");
    }

    @Override
    public synchronized List<ToscaPolicyTypeIdentifier> supportedPolicyTypes() {
        return supportedPolicyTypes;
    }

    @Override
    public boolean canSupportPolicyType(ToscaPolicyTypeIdentifier policyTypeId) {
        //
        // For Monitoring, we will attempt to support all versions
        // of the policy type. Since we are only packaging a decision
        // back with a JSON payload of the property contents.
        //
        return (policyTypeId.getName().equals(ONAP_MONITORING_BASE_POLICY_TYPE)
                || policyTypeId.getName().equals(ONAP_MONITORING_CDAP)
                || policyTypeId.getName().equals(ONAP_MONITORING_APPSERVER)
                || policyTypeId.getName().equals(ONAP_MONITORING_SONHANDLER)
                || policyTypeId.getName().startsWith(ONAP_MONITORING_DERIVED_POLICY_TYPE)
                || policyTypeId.getName().startsWith("onap.policies.monitoring."));
    }

    @Override
    public Pair<DecisionResponse, Response> makeDecision(DecisionRequest request,
            Map<String, String[]> requestQueryParams) {
        //
        // Convert to a XacmlRequest
        //
        Request xacmlRequest = this.getTranslator().convertRequest(request);
        //
        // Now get a decision
        //
        Response xacmlResponse = this.xacmlDecision(xacmlRequest);
        //
        // Convert to a DecisionResponse
        //
        DecisionResponse decisionResponse = this.getTranslator().convertResponse(xacmlResponse);
        //
        // Abbreviate results if needed
        //
        if (checkAbbreviateResults(requestQueryParams) && decisionResponse.getPolicies() != null
                && !decisionResponse.getPolicies().isEmpty()) {
            LOGGER.info("Abbreviating decision results {}", decisionResponse);
            for (Entry<String, Object> entry : decisionResponse.getPolicies().entrySet()) {
                if (entry.getValue() instanceof Map) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> policy = (Map<String, Object>) entry.getValue();
                    policy.remove("type_version");
                    policy.remove("properties");
                    policy.remove("name");
                    policy.remove("version");
                }
            }
        }
        return Pair.of(decisionResponse, xacmlResponse);
    }

    @Override
    protected ToscaPolicyTranslator getTranslator(String type) {
        return translator;
    }

    /**
     * Checks the query parameters to determine whether the decision results should be abbreviated.
     *
     * @param queryParams - http request query parameters
     */
    private boolean checkAbbreviateResults(Map<String, String[]> queryParams) {
        if (queryParams != null && !queryParams.isEmpty()) {
            // Check if query params contains "abbrev" flag
            if (queryParams.containsKey("abbrev")) {
                return Arrays.asList(queryParams.get("abbrev")).contains("true");
            } else {
                LOGGER.info("Unsupported query param for Monitoring application: {}", queryParams);
                return false;
            }
        }
        LOGGER.info("Query parameters empty");
        return false;
    }
}
