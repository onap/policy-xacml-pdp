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

import com.att.research.xacml.api.Request;
import com.att.research.xacml.api.Response;
import com.att.research.xacml.util.XACMLPolicyWriter;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import oasis.names.tc.xacml._3_0.core.schema.wd_17.PolicyType;

import org.onap.policy.models.decisions.concepts.DecisionRequest;
import org.onap.policy.models.decisions.concepts.DecisionResponse;
import org.onap.policy.models.tosca.authorative.concepts.ToscaPolicyTypeIdentifier;
import org.onap.policy.pdp.xacml.application.common.ToscaPolicyConversionException;
import org.onap.policy.pdp.xacml.application.common.XacmlApplicationException;
import org.onap.policy.pdp.xacml.application.common.XacmlPolicyUtils;
import org.onap.policy.pdp.xacml.application.common.std.StdXacmlApplicationServiceProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class implements the onap.policies.controlloop.Guard policy implementations.
 *
 * @author pameladragosh
 *
 */
public class GuardPdpApplication extends StdXacmlApplicationServiceProvider {

    private static final Logger LOGGER = LoggerFactory.getLogger(GuardPdpApplication.class);
    private static final String STRING_VERSION100 = "1.0.0";
    private List<ToscaPolicyTypeIdentifier> supportedPolicyTypes = new ArrayList<>();
    private LegacyGuardTranslator translator = new LegacyGuardTranslator();

    /** Constructor.
     *
     */
    public GuardPdpApplication() {
        this.supportedPolicyTypes.add(new ToscaPolicyTypeIdentifier("onap.policies.controlloop.guard.FrequencyLimiter",
                STRING_VERSION100));
        this.supportedPolicyTypes.add(new ToscaPolicyTypeIdentifier("onap.policies.controlloop.guard.MinMax",
                STRING_VERSION100));
    }

    @Override
    public String applicationName() {
        return "guard";
    }

    @Override
    public List<String> actionDecisionsSupported() {
        return Arrays.asList("guard");
    }

    @Override
    public List<ToscaPolicyTypeIdentifier> supportedPolicyTypes() {
        return supportedPolicyTypes;
    }

    @Override
    public boolean canSupportPolicyType(ToscaPolicyTypeIdentifier policyTypeId) {
        //
        // For the time being, restrict this if the version isn't known.
        // Could be too difficult to support changing of versions dynamically.
        //
        for (ToscaPolicyTypeIdentifier supported : this.supportedPolicyTypes) {
            if (policyTypeId.equals(supported)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void loadPolicies(Map<String, Object> toscaPolicies) throws XacmlApplicationException {
        try {
            //
            // Convert the policies first
            //
            List<PolicyType> listPolicies = translator.scanAndConvertPolicies(toscaPolicies);
            if (listPolicies.isEmpty()) {
                throw new XacmlApplicationException("Converted 0 policies");
            }
            //
            // Create a copy of the properties object
            //
            Properties newProperties = this.getProperties();
            //
            // Iterate through the policies
            //
            for (PolicyType newPolicy : listPolicies) {
                //
                // Construct the filename
                //
                Path refPath = XacmlPolicyUtils.constructUniquePolicyFilename(newPolicy, this.getDataPath());
                //
                // Write the policy to disk
                // Maybe check for an error
                //
                XACMLPolicyWriter.writePolicyFile(refPath, newPolicy);
                //
                // Add root policy to properties object
                //
                XacmlPolicyUtils.addRootPolicy(newProperties, refPath);
            }
            //
            // Write the properties to disk
            //
            XacmlPolicyUtils.storeXacmlProperties(newProperties,
                    XacmlPolicyUtils.getPropertiesPath(this.getDataPath()));
            //
            // Reload the engine
            //
            this.createEngine(newProperties);
        } catch (IOException | ToscaPolicyConversionException e) {
            LOGGER.error("Failed to loadPolicies {}", e);
        }
    }

    @Override
    public DecisionResponse makeDecision(DecisionRequest request) {
        //
        // Convert to a XacmlRequest
        //
        Request xacmlRequest = translator.convertRequest(request);
        //
        // Now get a decision
        //
        Response xacmlResponse = this.xacmlDecision(xacmlRequest);
        //
        // Convert to a DecisionResponse
        //
        return translator.convertResponse(xacmlResponse);
    }

}
