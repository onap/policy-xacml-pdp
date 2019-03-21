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

package org.onap.policy.pdp.xacml.application.common;

import com.att.research.xacml.api.Request;
import com.att.research.xacml.api.Response;

import java.util.List;
import java.util.Map;

import oasis.names.tc.xacml._3_0.core.schema.wd_17.PolicyType;

import org.onap.policy.models.decisions.concepts.DecisionRequest;
import org.onap.policy.models.decisions.concepts.DecisionResponse;

public interface ToscaPolicyTranslator {

    /**
     * Implement this method to translate policies.
     *
     * @param toscaObject Incoming Tosca Policies object
     * @return List of translated policies
     * @throws ToscaPolicyConversionException Exception
     */
    List<PolicyType> scanAndConvertPolicies(Map<String, Object> toscaObject) throws ToscaPolicyConversionException;

    /**
     * Implement this method to convert an ONAP DecisionRequest into
     * a Xacml request.
     *
     * @param request Incoming DecisionRequest
     * @return Xacml Request object
     */
    Request convertRequest(DecisionRequest request);

    /**
     * Implement this method to convert a Xacml Response
     * into a ONAP DecisionResponse.
     *
     * @param xacmlResponse Input Xacml Response
     * @return DecisionResponse object
     */
    DecisionResponse convertResponse(Response xacmlResponse);

}
