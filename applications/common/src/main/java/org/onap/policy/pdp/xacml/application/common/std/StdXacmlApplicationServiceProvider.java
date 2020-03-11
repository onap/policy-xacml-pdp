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
import com.att.research.xacml.api.pdp.PDPEngine;
import com.att.research.xacml.api.pdp.PDPEngineFactory;
import com.att.research.xacml.api.pdp.PDPException;
import com.att.research.xacml.util.FactoryException;
import com.att.research.xacml.util.XACMLPolicyWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import lombok.Getter;
import oasis.names.tc.xacml._3_0.core.schema.wd_17.PolicyType;
import org.apache.commons.lang3.tuple.Pair;
import org.onap.policy.common.endpoints.parameters.RestServerParameters;
import org.onap.policy.models.decisions.concepts.DecisionRequest;
import org.onap.policy.models.decisions.concepts.DecisionResponse;
import org.onap.policy.models.tosca.authorative.concepts.ToscaPolicy;
import org.onap.policy.models.tosca.authorative.concepts.ToscaPolicyTypeIdentifier;
import org.onap.policy.pdp.xacml.application.common.ToscaPolicyConversionException;
import org.onap.policy.pdp.xacml.application.common.ToscaPolicyTranslator;
import org.onap.policy.pdp.xacml.application.common.XacmlApplicationException;
import org.onap.policy.pdp.xacml.application.common.XacmlApplicationServiceProvider;
import org.onap.policy.pdp.xacml.application.common.XacmlPolicyUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class StdXacmlApplicationServiceProvider implements XacmlApplicationServiceProvider {

    private static final Logger LOGGER = LoggerFactory.getLogger(StdXacmlApplicationServiceProvider.class);
    private Path pathForData = null;
    @Getter
    private RestServerParameters policyApiParameters;
    private Properties pdpProperties = null;
    private PDPEngine pdpEngine = null;
    private Map<ToscaPolicy, Path> mapLoadedPolicies = new HashMap<>();

    public StdXacmlApplicationServiceProvider() {
        super();
    }

    @Override
    public String applicationName() {
        return "Please Override";
    }

    @Override
    public List<String> actionDecisionsSupported() {
        return Collections.emptyList();
    }

    @Override
    public void initialize(Path pathForData, RestServerParameters policyApiParameters)
            throws XacmlApplicationException {
        //
        // Save our path
        //
        this.pathForData = pathForData;
        LOGGER.info("New Path is {}", this.pathForData.toAbsolutePath());
        //
        // Save our params
        //
        this.policyApiParameters = policyApiParameters;
        //
        // Look for and load the properties object
        //
        try {
            pdpProperties = XacmlPolicyUtils.loadXacmlProperties(XacmlPolicyUtils.getPropertiesPath(pathForData));
            LOGGER.info("{}", pdpProperties);
        } catch (IOException e) {
            throw new XacmlApplicationException("Failed to load " + XacmlPolicyUtils.XACML_PROPERTY_FILE, e);
        }
        //
        // Create an engine
        //
        createEngine(pdpProperties);
    }

    @Override
    public List<ToscaPolicyTypeIdentifier> supportedPolicyTypes() {
        throw new UnsupportedOperationException("Please override and implement supportedPolicyTypes");
    }

    @Override
    public boolean canSupportPolicyType(ToscaPolicyTypeIdentifier policyTypeId) {
        throw new UnsupportedOperationException("Please override and implement canSupportPolicyType");
    }

    @Override
    public synchronized void loadPolicy(ToscaPolicy toscaPolicy) throws XacmlApplicationException {
        try {
            //
            // Convert the policies first
            //
            PolicyType xacmlPolicy = this.getTranslator(toscaPolicy.getType()).convertPolicy(toscaPolicy);
            if (xacmlPolicy == null) {
                throw new ToscaPolicyConversionException("Failed to convert policy");
            }
            //
            // Create a copy of the properties object
            //
            Properties newProperties = this.getProperties();
            //
            // Construct the filename
            //
            Path refPath = XacmlPolicyUtils.constructUniquePolicyFilename(xacmlPolicy, this.getDataPath());
            //
            // Write the policy to disk
            // Maybe check for an error
            //
            XACMLPolicyWriter.writePolicyFile(refPath, xacmlPolicy);
            if (LOGGER.isInfoEnabled()) {
                LOGGER.info("Xacml Policy is {}{}", XacmlPolicyUtils.LINE_SEPARATOR,
                    new String(Files.readAllBytes(refPath), StandardCharsets.UTF_8));
            }
            //
            // Add root policy to properties object
            //
            XacmlPolicyUtils.addRootPolicy(newProperties, refPath);
            //
            // Write the properties to disk
            //
            XacmlPolicyUtils.storeXacmlProperties(newProperties,
                    XacmlPolicyUtils.getPropertiesPath(this.getDataPath()));
            //
            // Reload the engine
            //
            this.createEngine(newProperties);
            //
            // Save the properties
            //
            this.pdpProperties = newProperties;
            //
            // Save in our map
            //
            this.mapLoadedPolicies.put(toscaPolicy, refPath);
        } catch (IOException | ToscaPolicyConversionException e) {
            throw new XacmlApplicationException("loadPolicy failed", e);
        }
    }

    @Override
    public synchronized boolean unloadPolicy(ToscaPolicy toscaPolicy) throws XacmlApplicationException {
        //
        // Find it in our map
        //
        Path refPolicy = this.mapLoadedPolicies.get(toscaPolicy);
        if (refPolicy == null) {
            LOGGER.error("Failed to find ToscaPolicy {} in our map size {}", toscaPolicy.getMetadata(),
                    this.mapLoadedPolicies.size());
            return false;
        }
        //
        // Create a copy of the properties object
        //
        Properties newProperties = this.getProperties();
        //
        // Remove it from the properties
        //
        XacmlPolicyUtils.removeRootPolicy(newProperties, refPolicy);
        //
        // We can delete the file
        //
        try {
            Files.delete(refPolicy);
        } catch (IOException e) {
            LOGGER.error("Failed to delete policy {} from disk {}", toscaPolicy.getMetadata(),
                    refPolicy.toAbsolutePath(), e);
        }
        //
        // Write the properties to disk
        //
        try {
            XacmlPolicyUtils.storeXacmlProperties(newProperties,
                    XacmlPolicyUtils.getPropertiesPath(this.getDataPath()));
        } catch (IOException e) {
            LOGGER.error("Failed to save the properties to disk {}", newProperties, e);
        }
        //
        // Reload the engine
        //
        this.createEngine(newProperties);
        //
        // Save the properties
        //
        this.pdpProperties = newProperties;
        //
        // Save in our map
        //
        if (this.mapLoadedPolicies.remove(toscaPolicy) == null) {
            LOGGER.error("Failed to remove toscaPolicy {} from internal map size {}", toscaPolicy.getMetadata(),
                    this.mapLoadedPolicies.size());
        }
        //
        // Not sure if any of the errors above warrant returning false
        //
        return true;
    }

    @Override
    public Pair<DecisionResponse, Response> makeDecision(DecisionRequest request,
            Map<String, String[]> requestQueryParams) {
        //
        // Convert to a XacmlRequest
        //
        Request xacmlRequest = this.getTranslator().convertRequest(request);
        //
        // Now get a decision
        //
        Response xacmlResponse = this.xacmlDecision(xacmlRequest);
        //
        // Convert to a DecisionResponse
        //
        return Pair.of(this.getTranslator().convertResponse(xacmlResponse), xacmlResponse);
    }

    protected abstract ToscaPolicyTranslator getTranslator(String type);

    protected ToscaPolicyTranslator getTranslator() {
        return this.getTranslator("");
    }

    protected synchronized PDPEngine getEngine() {
        return this.pdpEngine;
    }

    protected synchronized Properties getProperties() {
        Properties newProperties = new Properties();
        newProperties.putAll(pdpProperties);
        return newProperties;
    }

    protected synchronized Path getDataPath() {
        return pathForData;
    }

    /**
     * Creates an instance of PDP engine given the Properties object.
     */
    protected synchronized void createEngine(Properties properties) {
        //
        // Now initialize the XACML PDP Engine
        //
        try {
            PDPEngineFactory factory = getPdpEngineFactory();
            PDPEngine engine = factory.newEngine(properties);
            if (engine != null) {
                this.pdpEngine = engine;
            }
        } catch (FactoryException e) {
            LOGGER.error("Failed to create XACML PDP Engine", e);
        }
    }

    /**
     * Make a decision call.
     *
     * @param request Incoming request object
     * @return Response object
     */
    protected synchronized Response xacmlDecision(Request request) {
        //
        // This is what we need to return
        //
        Response response = null;
        //
        // Track some timing
        //
        long timeStart = System.currentTimeMillis();
        try {
            response = this.pdpEngine.decide(request);
        } catch (PDPException e) {
            LOGGER.error("Xacml PDP Engine decide failed", e);
        } finally {
            //
            // Track the end of timing
            //
            long timeEnd = System.currentTimeMillis();
            LOGGER.info("Elapsed Time: {}ms", (timeEnd - timeStart));
        }
        return response;
    }

    // these may be overridden by junit tests

    protected PDPEngineFactory getPdpEngineFactory() throws FactoryException {
        return PDPEngineFactory.newInstance();
    }
}
