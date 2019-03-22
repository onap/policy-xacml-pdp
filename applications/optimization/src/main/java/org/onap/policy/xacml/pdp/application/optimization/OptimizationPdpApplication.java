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

package org.onap.policy.xacml.pdp.application.optimization;

import com.att.research.xacml.api.Request;
import com.att.research.xacml.api.Response;
import com.att.research.xacml.util.XACMLPolicyWriter;
import com.google.common.collect.Lists;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import oasis.names.tc.xacml._3_0.core.schema.wd_17.PolicyType;

import org.onap.policy.models.decisions.concepts.DecisionRequest;
import org.onap.policy.models.decisions.concepts.DecisionResponse;
import org.onap.policy.pdp.xacml.application.common.ToscaPolicyConversionException;
import org.onap.policy.pdp.xacml.application.common.XacmlPolicyUtils;
import org.onap.policy.pdp.xacml.application.common.std.StdMatchableTranslator;
import org.onap.policy.pdp.xacml.application.common.std.StdXacmlApplicationServiceProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OptimizationPdpApplication extends StdXacmlApplicationServiceProvider {

    private static final Logger LOGGER = LoggerFactory.getLogger(OptimizationPdpApplication.class);
    private static final String STRING_VERSION100 = "1.0.0";

    private StdMatchableTranslator translator = new StdMatchableTranslator();
    private Map<String, String> supportedPolicyTypes = new HashMap<>();

    /**
     * Constructor.
     */
    public OptimizationPdpApplication() {
        this.supportedPolicyTypes.put("onap.policies.optimization.AffinityPolicy", STRING_VERSION100);
        this.supportedPolicyTypes.put("onap.policies.optimization.DistancePolicy", STRING_VERSION100);
        this.supportedPolicyTypes.put("onap.policies.optimization.HpaPolicy", STRING_VERSION100);
        this.supportedPolicyTypes.put("onap.policies.optimization.OptimizationPolicy", STRING_VERSION100);
        this.supportedPolicyTypes.put("onap.policies.optimization.PciPolicy", STRING_VERSION100);
        this.supportedPolicyTypes.put("onap.policies.optimization.QueryPolicy", STRING_VERSION100);
        this.supportedPolicyTypes.put("onap.policies.optimization.SubscriberPolicy", STRING_VERSION100);
        this.supportedPolicyTypes.put("onap.policies.optimization.Vim_fit", STRING_VERSION100);
        this.supportedPolicyTypes.put("onap.policies.optimization.VnfPolicy", STRING_VERSION100);
    }

    @Override
    public String applicationName() {
        return "Optimization Application";
    }

    @Override
    public List<String> actionDecisionsSupported() {
        return Arrays.asList("optimize");
    }

    @Override
    public synchronized List<String> supportedPolicyTypes() {
        return Lists.newArrayList(supportedPolicyTypes.keySet());
    }

    @Override
    public boolean canSupportPolicyType(String policyType, String policyTypeVersion) {
        //
        // For the time being, restrict this if the version isn't known.
        // Could be too difficult to support changing of versions dynamically.
        //
        if (! this.supportedPolicyTypes.containsKey(policyType)) {
            return false;
        }
        //
        // Must match version exactly
        //
        return this.supportedPolicyTypes.get(policyType).equals(policyTypeVersion);
    }

    @Override
    public synchronized void loadPolicies(Map<String, Object> toscaPolicies) {
        try {
            //
            // Convert the policies first
            //
            List<PolicyType> listPolicies = translator.scanAndConvertPolicies(toscaPolicies);
            if (listPolicies.isEmpty()) {
                throw new ToscaPolicyConversionException("Converted 0 policies");
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
    public synchronized DecisionResponse makeDecision(DecisionRequest request) {
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
