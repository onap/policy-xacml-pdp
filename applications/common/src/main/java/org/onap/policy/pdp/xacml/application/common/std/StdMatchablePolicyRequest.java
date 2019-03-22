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

package org.onap.policy.pdp.xacml.application.common.std;

import com.att.research.xacml.std.annotations.XACMLAction;
import com.att.research.xacml.std.annotations.XACMLRequest;
import com.att.research.xacml.std.annotations.XACMLResource;
import com.att.research.xacml.std.annotations.XACMLSubject;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Map.Entry;

import org.onap.policy.models.decisions.concepts.DecisionRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@XACMLRequest(ReturnPolicyIdList = true)
public class StdMatchablePolicyRequest {

    private static final Logger LOGGER = LoggerFactory.getLogger(StdMatchablePolicyRequest.class);

    public StdMatchablePolicyRequest() {
        super();
    }

    @XACMLSubject(includeInResults = true)
    String onapName;

    //
    // Unfortunately the annotations won't take an object.toString()
    //
    @XACMLResource(attributeId = "urn:org:onap:policy-scope-property", includeInResults = true)
    Collection<String> policyScopes = new ArrayList<>();

    @XACMLResource(attributeId = "urn:org:onap:policy-type-property", includeInResults = true)
    Collection<String> policyTypes = new ArrayList<>();

    @XACMLAction()
    String action;


    /**
     * Parses the DecisionRequest into a MonitoringRequest.
     *
     * @param decisionRequest Input DecisionRequest
     * @return MonitoringRequest
     */
    @SuppressWarnings("rawtypes")
    public static StdMatchablePolicyRequest createInstance(DecisionRequest decisionRequest) {
        StdMatchablePolicyRequest request = new StdMatchablePolicyRequest();
        request.onapName = decisionRequest.getOnapName();
        request.action = decisionRequest.getAction();

        Map<String, Object> resources = decisionRequest.getResource();
        for (Entry<String, Object> entry : resources.entrySet()) {
            if ("policyScope".equals(entry.getKey())) {
                if (entry.getValue() instanceof Collection) {
                    for (Object scope : (Collection) entry.getValue()) {
                        request.policyScopes.add(scope.toString());
                    }
                }
                if (entry.getValue() instanceof String) {
                    request.policyScopes.add(entry.getValue().toString());
                }
                continue;
            }
            if ("policyType".equals(entry.getKey())) {
                if (entry.getValue() instanceof Collection) {
                    for (Object scope : (Collection) entry.getValue()) {
                        request.policyTypes.add(scope.toString());
                    }
                }
                if (entry.getValue() instanceof String) {
                    request.policyTypes.add(entry.getValue().toString());
                }
            }
        }
        //
        // TODO handle a bad incoming request. Do that here?
        //
        return request;
    }
}
