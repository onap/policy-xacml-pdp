/*-
 * ============LICENSE_START=======================================================
 * ONAP
 * ================================================================================
 * Copyright (C) 2019-2020 AT&T Intellectual Property. All rights reserved.
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
import com.att.research.xacml.std.annotations.XACMLEnvironment;
import com.att.research.xacml.std.annotations.XACMLRequest;
import com.att.research.xacml.std.annotations.XACMLResource;
import com.att.research.xacml.std.annotations.XACMLSubject;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.OffsetTime;
import java.time.ZoneOffset;
import java.util.Map;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.onap.policy.models.decisions.concepts.DecisionRequest;
import org.onap.policy.pdp.xacml.application.common.ToscaPolicyConversionException;

@Getter
@Setter
@ToString
@XACMLRequest(ReturnPolicyIdList = true)
public class GuardPolicyRequest {
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

    @XACMLEnvironment(includeInResults = true, 
            attributeId = "urn:oasis:names:tc:xacml:1.0:environment:current-dateTime")
    private OffsetDateTime currentDateTime;

    @XACMLEnvironment(includeInResults = true, attributeId = "urn:oasis:names:tc:xacml:1.0:environment:current-date")
    private LocalDate currentDate;

    @XACMLEnvironment(includeInResults = true, attributeId = "urn:oasis:names:tc:xacml:1.0:environment:current-time")
    private OffsetTime currentTime;

    @XACMLEnvironment(includeInResults = true, attributeId = "urn:org:onap:guard:timezone", 
            datatype = "urn:com:att:research:datatype:zone-offset")
    private ZoneOffset timeZone;

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

    @XACMLResource(includeInResults = true, attributeId = "urn:org:onap:guard:target:generic-vnf.vnf-name")
    private String vnfName;

    @XACMLResource(includeInResults = true, attributeId = "urn:org:onap:guard:target:generic-vnf.vnf-id")
    private String vnfId;

    @XACMLResource(includeInResults = true, attributeId = "urn:org:onap:guard:target:generic-vnf.vnf-type")
    private String vnfType;

    @XACMLResource(includeInResults = true, attributeId = "urn:org:onap:guard:target:generic-vnf.nf-naming-code")
    private String vnfNfNamingCode;

    @XACMLResource(includeInResults = true, attributeId = "urn:org:onap:guard:target:vserver.vserver-id")
    private String vserverId;

    @XACMLResource(includeInResults = true, attributeId = "urn:org:onap:guard:target:cloud-region.cloud-region-id")
    private String cloudRegionId;

    public static final String PREFIX_RESOURCE_ATTRIBUTE_ID = "urn:org:onap:guard:target:";

    public GuardPolicyRequest() {
        super();
    }

    /**
     * Parses the DecisionRequest into a StdMetadataPolicyRequest.
     *
     * @param decisionRequest Input DecisionRequest
     * @return StdMetadataPolicyRequest
     * @throws ToscaPolicyConversionException If we cannot parse the request
     */
    @SuppressWarnings("unchecked")
    public static GuardPolicyRequest createInstance(DecisionRequest decisionRequest)
            throws ToscaPolicyConversionException {
        //
        // Create our return object
        //
        GuardPolicyRequest request = new GuardPolicyRequest();
        //
        // Add the subject attributes
        //
        request.onapName = decisionRequest.getOnapName();
        request.onapComponent = decisionRequest.getOnapComponent();
        request.onapInstance = decisionRequest.getOnapInstance();
        request.requestId = decisionRequest.getRequestId();
        request.currentDateTime = decisionRequest.getCurrentDateTime();
        request.currentDate = decisionRequest.getCurrentDate();
        request.currentTime = decisionRequest.getCurrentTime();
        request.timeZone = decisionRequest.getTimeZone();
        //
        // Now pull from the resources
        //
        Map<String, Object> resources = decisionRequest.getResource();
        //
        // Just in case nothing is in there
        //
        if (resources == null || resources.isEmpty() || !resources.containsKey(STR_GUARD)) {
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
        findFields(request, guard);
        findFilterFields(request, guard);
        return request;
    }

    private static GuardPolicyRequest findFields(GuardPolicyRequest request, Map<String, Object> guard)
            throws ToscaPolicyConversionException {
        if (guard.containsKey("actor")) {
            request.actorId = guard.get("actor").toString();
        }
        if (guard.containsKey("operation")) {
            request.operationId = guard.get("operation").toString();
        }
        if (guard.containsKey("clname")) {
            request.clnameId = guard.get("clname").toString();
        }
        if (guard.containsKey("target")) {
            request.targetId = guard.get("target").toString();
        }
        if (guard.containsKey("vfCount")) {
            try {
                request.vfCount = Integer.decode(guard.get("vfCount").toString());
            } catch (NumberFormatException e) {
                throw new ToscaPolicyConversionException("Failed to decode vfCount", e);
            }
        }

        return request;
    }

    private static GuardPolicyRequest findFilterFields(GuardPolicyRequest request, Map<String, Object> guard) {
        if (guard.containsKey("generic-vnf.vnf-name")) {
            request.vnfName = guard.get("generic-vnf.vnf-name").toString();
        }
        if (guard.containsKey("generic-vnf.vnf-id")) {
            request.vnfId = guard.get("generic-vnf.vnf-id").toString();
        }
        if (guard.containsKey("generic-vnf.vnf-type")) {
            request.vnfType = guard.get("generic-vnf.vnf-type").toString();
        }
        if (guard.containsKey("generic-vnf.nf-naming-code")) {
            request.vnfNfNamingCode = guard.get("generic-vnf.nf-naming-code").toString();
        }
        if (guard.containsKey("vserver.vserver-id")) {
            request.vserverId = guard.get("vserver.vserver-id").toString();
        }
        if (guard.containsKey("cloud-region.cloud-region-id")) {
            request.cloudRegionId = guard.get("cloud-region.cloud-region-id").toString();
        }
        return request;
    }
}
