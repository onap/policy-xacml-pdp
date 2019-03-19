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
import com.att.research.xacml.api.pdp.PDPEngine;
import com.att.research.xacml.api.pdp.PDPEngineFactory;
import com.att.research.xacml.api.pdp.PDPException;
import com.att.research.xacml.util.FactoryException;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import oasis.names.tc.xacml._3_0.core.schema.wd_17.PolicyType;

import org.onap.policy.models.decisions.concepts.DecisionRequest;
import org.onap.policy.models.decisions.concepts.DecisionResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yaml.snakeyaml.Yaml;

public abstract class ToscaPolicyTranslator implements XacmlApplicationServiceProvider {

    private static final Logger LOGGER = LoggerFactory.getLogger(ToscaPolicyTranslator.class);
    private Path pathForData = null;
    private Properties pdpProperties = null;
    private PDPEngine pdpEngine = null;

    public ToscaPolicyTranslator() {
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
    public void initialize(Path pathForData) {
        //
        // Save our path
        //
        this.pathForData = pathForData;
        LOGGER.debug("New Path is {}", this.pathForData.toAbsolutePath());
        //
        // Look for and load the properties object
        //
        try {
            pdpProperties = XacmlPolicyUtils.loadXacmlProperties(XacmlPolicyUtils.getPropertiesPath(pathForData));
            LOGGER.debug("{}", pdpProperties);
        } catch (IOException e) {
            LOGGER.error("{}", e);
        }
        //
        // Create an engine
        //
        createEngine(pdpProperties);
    }

    @Override
    public List<String> supportedPolicyTypes() {
        return Collections.emptyList();
    }

    @Override
    public boolean canSupportPolicyType(String policyType, String policyTypeVersion) {
        return false;
    }

    @Override
    public void loadPolicies(Map<String, Object> toscaPolicies) {
        //
        // THIS SHOULD BE OVERRIDDEN
        //
    }

    @Override
    public DecisionResponse makeDecision(DecisionRequest request) {
        //
        // We should have a standard error response to return
        //
        return null;
    }

    protected synchronized PDPEngine getEngine() {
        return this.pdpEngine;
    }

    protected synchronized Properties getProperties() {
        return new Properties(pdpProperties);
    }

    protected synchronized Path getDataPath() {
        return pathForData;
    }

    protected List<PolicyType> convertPolicies(InputStream isToscaPolicy) throws ToscaPolicyConversionException {
        //
        // Have snakeyaml parse the object
        //
        Yaml yaml = new Yaml();
        Map<String, Object> toscaObject = yaml.load(isToscaPolicy);
        //
        // Return the policies
        //
        return scanAndConvertPolicies(toscaObject);
    }

    protected List<PolicyType> convertPolicies(Map<String, Object> toscaObject) throws ToscaPolicyConversionException {
        //
        // Return the policies
        //
        return scanAndConvertPolicies(toscaObject);
    }

    /**
     * Implement this method to translate policies.
     *
     * @param toscaObject Incoming Tosca Policies object
     * @return List of translated policies
     * @throws ToscaPolicyConversionException Exception
     */
    protected abstract List<PolicyType> scanAndConvertPolicies(Map<String, Object> toscaObject)
            throws ToscaPolicyConversionException;

    /**
     * Implement this method to convert an ONAP DecisionRequest into
     * a Xacml request.
     *
     * @param request Incoming DecisionRequest
     * @return Xacml Request object
     */
    protected abstract Request convertRequest(DecisionRequest request);

    /**
     * Implement this method to convert a Xacml Response
     * into a ONAP DecisionResponse.
     *
     * @param xacmlResponse Input Xacml Response
     * @return DecisionResponse object
     */
    protected abstract DecisionResponse convertResponse(Response xacmlResponse);

    /**
     * Load properties from given file.
     *
     * @throws IOException If unable to read file
     */
    protected synchronized Properties loadXacmlProperties() throws IOException {
        LOGGER.debug("Loading xacml properties {}", pathForData);
        try (InputStream is = Files.newInputStream(pathForData)) {
            Properties properties = new Properties();
            properties.load(is);
            return properties;
        }
    }

    /**
     * Stores the XACML Properties to the given file location.
     *
     * @throws IOException If unable to store the file.
     */
    protected synchronized void storeXacmlProperties() throws IOException {
        try (OutputStream os = Files.newOutputStream(pathForData)) {
            String strComments = "#";
            pdpProperties.store(os, strComments);
        }
    }

    /**
     * Appends 'xacml.properties' to a root Path object
     *
     * @return Path to rootPath/xacml.properties file
     */
    protected synchronized Path getPropertiesPath() {
        return Paths.get(pathForData.toAbsolutePath().toString(), "xacml.properties");
    }

    /**
     * Creates an instance of PDP engine given the Properties object.
     */
    protected synchronized void createEngine(Properties properties) {
        //
        // Now initialize the XACML PDP Engine
        //
        try {
            PDPEngineFactory factory = PDPEngineFactory.newInstance();
            PDPEngine engine = factory.newEngine(properties);
            if (engine != null) {
                this.pdpEngine = engine;
                this.pdpProperties = new Properties(properties);
            }
        } catch (FactoryException e) {
            LOGGER.error("Failed to create XACML PDP Engine {}", e);
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
            LOGGER.error("Xacml PDP Engine failed {}", e);
        } finally {
            //
            // Track the end of timing
            //
            long timeEnd = System.currentTimeMillis();
            LOGGER.info("Elapsed Time: {}ms", (timeEnd - timeStart));
        }
        return response;
    }

}
