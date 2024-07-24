/*-
 * ============LICENSE_START=======================================================
 * ONAP
 * ================================================================================
 * Copyright (C) 2019-2021 AT&T Intellectual Property. All rights reserved.
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

package org.onap.policy.pdp.xacml.application.common.std;

import com.att.research.xacml.std.annotations.XACMLAction;
import com.att.research.xacml.std.annotations.XACMLRequest;
import com.att.research.xacml.std.annotations.XACMLResource;
import com.att.research.xacml.std.annotations.XACMLSubject;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.Map.Entry;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.onap.policy.models.decisions.concepts.DecisionRequest;

@Getter
@Setter
@ToString
@NoArgsConstructor
@XACMLRequest(ReturnPolicyIdList = true)
public class StdCombinedPolicyRequest {

    public static final String POLICY_TYPE_KEY = "policy-type";
    public static final String POLICY_ID_KEY = "policy-id";

    @XACMLSubject(includeInResults = true)
    private String onapName;

    @XACMLSubject(attributeId = "urn:org:onap:onap-component", includeInResults = true)
    private String onapComponent;

    @XACMLSubject(attributeId = "urn:org:onap:onap-instance", includeInResults = true)
    private String onapInstance;

    @XACMLAction()
    private String action;

    @XACMLResource(includeInResults = true)
    private Collection<String> resource = new ArrayList<>();

    @XACMLResource(attributeId = "urn:org:onap:policy-type", includeInResults = true)
    private Collection<String> resourcePolicyType = new ArrayList<>();

    /**
     * Parses the DecisionRequest into a MonitoringRequest.
     *
     * @param decisionRequest Input DecisionRequest
     * @return MonitoringRequest
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    public static StdCombinedPolicyRequest createInstance(DecisionRequest decisionRequest) {
        //
        // Create our request object
        //
        var request = new StdCombinedPolicyRequest();
        //
        // Add the subject attributes
        //
        request.onapName = decisionRequest.getOnapName();
        request.onapComponent = decisionRequest.getOnapComponent();
        request.onapInstance = decisionRequest.getOnapInstance();
        //
        // Add the action attribute
        //
        request.action = decisionRequest.getAction();
        //
        // Add the resource attributes
        //
        Map<String, Object> resources = decisionRequest.getResource();
        for (Entry<String, Object> entrySet : resources.entrySet()) {
            if (POLICY_ID_KEY.equals(entrySet.getKey())) {
                if (entrySet.getValue() instanceof Collection collection) {
                    addPolicyIds(request, collection);
                } else if (entrySet.getValue() instanceof String stringValue) {
                    request.resource.add(stringValue);
                }
                continue;
            }
            if (POLICY_TYPE_KEY.equals(entrySet.getKey())) {
                if (entrySet.getValue() instanceof Collection collection) {
                    addPolicyTypes(request, collection);
                } else if (entrySet.getValue() instanceof String stringValue) {
                    request.resourcePolicyType.add(stringValue);
                }
            }
        }
        return request;
    }

    protected static StdCombinedPolicyRequest addPolicyIds(StdCombinedPolicyRequest request, Collection<Object> ids) {
        for (Object id : ids) {
            request.resource.add(id.toString());
        }
        return request;
    }

    protected static StdCombinedPolicyRequest addPolicyTypes(StdCombinedPolicyRequest request,
                                                             Collection<Object> types) {
        for (Object type : types) {
            request.resourcePolicyType.add(type.toString());
        }
        return request;
    }
}
