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

import java.util.Map;
import java.util.Map.Entry;

import org.onap.policy.models.decisions.concepts.DecisionRequest;

@XACMLRequest(ReturnPolicyIdList = true)
public class StdCombinedPolicyRequest {

    public StdCombinedPolicyRequest() {
        super();
    }

    @XACMLSubject(includeInResults = true)
    String onapName = "DCAE";

    @XACMLResource(includeInResults = true)
    String resource = "onap.policies.Monitoring";

    @XACMLAction()
    String action = "configure";


    /**
     * Parses the DecisionRequest into a MonitoringRequest.
     *
     * @param decisionRequest Input DecisionRequest
     * @return MonitoringRequest
     */
    public static StdCombinedPolicyRequest createInstance(DecisionRequest decisionRequest) {
        StdCombinedPolicyRequest request = new StdCombinedPolicyRequest();
        request.onapName = decisionRequest.getOnapName();
        request.action = decisionRequest.getAction();

        Map<String, Object> resources = decisionRequest.getResource();
        for (Entry<String, Object> entry : resources.entrySet()) {
            if ("policy-id".equals(entry.getKey())) {
                //
                // TODO handle lists of policies
                //
                request.resource = entry.getValue().toString();
                continue;
            }
            if ("policy-type".equals(entry.getKey())) {
                //
                // TODO handle lists of policies
                //
                request.resource = entry.getValue().toString();
            }
        }
        //
        // TODO handle a bad incoming request. Do that here?
        //
        return request;
    }
}
