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

import com.att.research.xacml.api.Request;
import com.att.research.xacml.api.Response;
import com.att.research.xacml.util.XACMLPolicyWriter;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import oasis.names.tc.xacml._3_0.core.schema.wd_17.PolicyType;

import org.onap.policy.models.decisions.concepts.DecisionRequest;
import org.onap.policy.models.decisions.concepts.DecisionResponse;
import org.onap.policy.pdp.xacml.application.common.ToscaPolicyConversionException;
import org.onap.policy.pdp.xacml.application.common.ToscaPolicyTranslator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class StdMetadataTranslator implements ToscaPolicyTranslator {

    private static final Logger LOGGER = LoggerFactory.getLogger(StdMetadataTranslator.class);

    public StdMetadataTranslator() {
        super();
    }

    @SuppressWarnings("unchecked")
    @Override
    public List<PolicyType> scanAndConvertPolicies(Map<String, Object> toscaObject)
            throws ToscaPolicyConversionException {
        //
        // Our return object
        //
        List<PolicyType> scannedPolicies = new ArrayList<>();
        //
        // Iterate each of the Policies
        //
        List<Object> policies = (List<Object>) toscaObject.get("policies");
        for (Object policyObject : policies) {
            //
            // Get the contents
            //
            LOGGER.debug("Found policy {}", policyObject.getClass());
            Map<String, Object> policyContents = (Map<String, Object>) policyObject;
            for (Entry<String, Object> entrySet : policyContents.entrySet()) {
                LOGGER.debug("Entry set {}", entrySet);
                //
                // Convert this policy
                //
                PolicyType policy = this.convertPolicy(entrySet);
                try (ByteArrayOutputStream os = new ByteArrayOutputStream()) {
                    XACMLPolicyWriter.writePolicyFile(os, policy);
                    LOGGER.debug("{}", os);
                } catch (IOException e) {
                    LOGGER.error("Failed to convert {}", e);
                }
                //
                // Convert and add in the new policy
                //
                scannedPolicies.add(policy);
            }
        }

        return scannedPolicies;
    }

    @Override
    public Request convertRequest(DecisionRequest request) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public DecisionResponse convertResponse(Response xacmlResponse) {
        // TODO Auto-generated method stub
        return null;
    }

    private PolicyType convertPolicy(Entry<String, Object> entrySet) throws ToscaPolicyConversionException {

        return null;
    }

}
