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
import com.att.research.xacml.util.XACMLPolicyScanner;
import com.att.research.xacml.util.XACMLPolicyWriter;
import com.att.research.xacml.util.XACMLProperties;

import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import oasis.names.tc.xacml._3_0.core.schema.wd_17.PolicySetType;
import oasis.names.tc.xacml._3_0.core.schema.wd_17.PolicyType;

import org.onap.policy.models.decisions.concepts.DecisionRequest;
import org.onap.policy.models.decisions.concepts.DecisionResponse;
import org.onap.policy.models.tosca.authorative.concepts.ToscaPolicyTypeIdentifier;
import org.onap.policy.pdp.xacml.application.common.ToscaPolicyConversionException;
import org.onap.policy.pdp.xacml.application.common.XacmlPolicyUtils;
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
    private static final String ONAP_MONITORING_BASE_POLICY_TYPE = "onap.Monitoring";
    private static final String ONAP_MONITORING_DERIVED_POLICY_TYPE = "onap.policies.monitoring";

    private StdCombinedPolicyResultsTranslator translator = new StdCombinedPolicyResultsTranslator();
    private List<ToscaPolicyTypeIdentifier> supportedPolicyTypes = new ArrayList<>();

    /**
     * Constructor.
     */
    public MonitoringPdpApplication() {
        //
        // By default this supports just Monitoring policy types
        //
        supportedPolicyTypes.add(new ToscaPolicyTypeIdentifier(ONAP_MONITORING_BASE_POLICY_TYPE, "1.0.0"));
    }

    @Override
    public String applicationName() {
        return "Monitoring";
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
                || policyTypeId.getName().startsWith(ONAP_MONITORING_DERIVED_POLICY_TYPE));
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
            // Get our properties because we are going to update
            //
            Properties currentProperties = this.getProperties();
            //
            // Read in our Root Policy
            //
            Set<String> roots = XACMLProperties.getRootPolicyIDs(currentProperties);
            if (roots.isEmpty()) {
                throw new ToscaPolicyConversionException("There are NO root policies defined");
            }
            //
            // Really only should be one
            //
            String rootFile = currentProperties.getProperty(roots.iterator().next() + ".file");
            try (InputStream is = new FileInputStream(rootFile)) {
                //
                // Read the Root Policy into memory
                //
                Object policyData = XACMLPolicyScanner.readPolicy(is);
                //
                // Should be a PolicySet
                //
                if (policyData instanceof PolicySetType) {
                    //
                    // Add the referenced policies into a new Root Policy
                    //
                    PolicyType[] newPolicies = listPolicies.toArray(new PolicyType[listPolicies.size()]);
                    PolicySetType newRootPolicy = XacmlPolicyUtils.addPoliciesToXacmlRootPolicy(
                            (PolicySetType) policyData, newPolicies);
                    LOGGER.debug("New ROOT Policy");
                    try (ByteArrayOutputStream os = new ByteArrayOutputStream()) {
                        XACMLPolicyWriter.writePolicyFile(os, newRootPolicy);
                        LOGGER.debug("{}", os);
                    } catch (IOException e) {
                        LOGGER.error("Failed to convert {}", e);
                    }
                    //
                    // Save the new Policies to disk
                    //
                    for (PolicyType policy : newPolicies) {
                        //
                        // Construct the filename
                        //
                        Path refPath = XacmlPolicyUtils.constructUniquePolicyFilename(policy, this.getDataPath());
                        //
                        // Write the policy to disk
                        // Maybe check for an error
                        //
                        XACMLPolicyWriter.writePolicyFile(refPath, policy);
                        //
                        // Save it off
                        //
                        XacmlPolicyUtils.addReferencedPolicy(currentProperties, refPath);
                    }
                    //
                    // Save the root policy to disk
                    //
                    XACMLPolicyWriter.writePolicyFile(Paths.get(rootFile), newRootPolicy);
                    //
                    // Write the policies to disk
                    //
                    XacmlPolicyUtils.storeXacmlProperties(currentProperties,
                            XacmlPolicyUtils.getPropertiesPath(this.getDataPath()));
                    //
                    // Reload the engine
                    //
                    this.createEngine(currentProperties);
                } else {
                    throw new ToscaPolicyConversionException("Root policy isn't a PolicySet");
                }
            }
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
