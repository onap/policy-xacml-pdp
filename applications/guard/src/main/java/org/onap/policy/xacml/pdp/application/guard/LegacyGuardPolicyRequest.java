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

package org.onap.policy.xacml.pdp.application.guard;

import com.att.research.xacml.std.annotations.XACMLAction;
import com.att.research.xacml.std.annotations.XACMLRequest;
import com.att.research.xacml.std.annotations.XACMLResource;
import com.att.research.xacml.std.annotations.XACMLSubject;

import java.util.Map;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import org.onap.policy.models.decisions.concepts.DecisionRequest;

@Getter
@Setter
@ToString
@XACMLRequest(ReturnPolicyIdList = true)
public class LegacyGuardPolicyRequest {

    private static final String STR_GUARD = "guard";

    @XACMLSubject(includeInResults = true)
    private String onapName;

    @XACMLSubject(includeInResults = true, attributeId = "urn:org:onap:onap-component")
    private String onapComponent;

    @XACMLSubject(includeInResults = true, attributeId = "urn:org:onap:onap-instance")
    private String onapInstance;

    @XACMLSubject(includeInResults = true, attributeId = "urn:org:onap:guard:request:request-id")
    private String requestId;

    @XACMLAction
    private String action = STR_GUARD;

    @XACMLResource(includeInResults = true, attributeId = "urn:org:onap:guard:clname:clname-id")
    private String clnameId;

    @XACMLResource(includeInResults = true, attributeId = "urn:org:onap:guard:actor:actor-id")
    private String actorId;

    @XACMLResource(includeInResults = true, attributeId = "urn:org:onap:guard:operation:operation-id")
    private String operationId;

    @XACMLResource(includeInResults = true, attributeId = "urn:org:onap:guard:target:target-id")
    private String targetId;

    @XACMLResource(includeInResults = true, attributeId = "urn:org:onap:guard:target:vf-count")
    private Integer vfCount;

    @XACMLResource(includeInResults = true, attributeId = "urn:org:onap:guard:target:min")
    private Integer min;

    @XACMLResource(includeInResults = true, attributeId = "urn:org:onap:guard:target:max")
    private Integer max;

    @XACMLResource(includeInResults = true, attributeId = "urn:org:onap:guard:operation:operation-count")
    private Integer operationCount;

    public LegacyGuardPolicyRequest() {
        super();
    }

    /**
     * Parses the DecisionRequest into a StdMetadataPolicyRequest.
     *
     * @param decisionRequest Input DecisionRequest
     * @return StdMetadataPolicyRequest
     */
    @SuppressWarnings("unchecked")
    public static LegacyGuardPolicyRequest createInstance(DecisionRequest decisionRequest) {
        //
        // Create our return object
        //
        LegacyGuardPolicyRequest request = new LegacyGuardPolicyRequest();
        //
        // Add the subject attributes
        //
        request.onapName = decisionRequest.getOnapName();
        request.onapComponent = decisionRequest.getOnapComponent();
        request.onapInstance = decisionRequest.getOnapInstance();
        request.requestId = decisionRequest.getRequestId();
        //
        // Now pull from the resources
        //
        Map<String, Object> resources = decisionRequest.getResource();
        //
        // Just in case nothing is in there
        //
        if (resources == null || resources.isEmpty() || ! resources.containsKey(STR_GUARD)) {
            //
            // Perhaps we throw an exception and then caller
            // can put together a response
            //
            return request;
        }
        Map<String, Object> guard = (Map<String, Object>) resources.get(STR_GUARD);
        if (guard == null || guard.isEmpty()) {
            //
            // again, same problem throw an exception?
            //
            return request;
        }
        //
        // Find our fields
        //
        if (guard.containsKey("actor")) {
            request.actorId = guard.get("actor").toString();
        }
        if (guard.containsKey("recipe")) {
            request.operationId = guard.get("recipe").toString();
        }
        if (guard.containsKey("clname")) {
            request.clnameId = guard.get("clname").toString();
        }
        if (guard.containsKey("targets")) {
            request.targetId = guard.get("targets").toString();
        }
        if (guard.containsKey("vfCount")) {
            request.vfCount = Integer.decode(guard.get("vfCount").toString());
        }
        if (guard.containsKey("min")) {
            request.min = Integer.decode(guard.get("min").toString());
        }
        if (guard.containsKey("max")) {
            request.max = Integer.decode(guard.get("max").toString());
        }
        //
        // TODO - remove this when the PIP is hooked up
        //
        if (guard.containsKey("operationCount")) {
            request.operationCount = Integer.decode(guard.get("operationCount").toString());
        }

        return request;
    }

}
