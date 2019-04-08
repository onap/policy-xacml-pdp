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
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

import oasis.names.tc.xacml._3_0.core.schema.wd_17.PolicyType;

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
    private Properties pdpProperties = null;
    private PDPEngine pdpEngine = null;

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
    public void initialize(Path pathForData) throws XacmlApplicationException {
        //
        // Save our path
        //
        this.pathForData = pathForData;
        LOGGER.info("New Path is {}", this.pathForData.toAbsolutePath());
        //
        // Ensure properties exist
        //
        Path propertiesPath = XacmlPolicyUtils.getPropertiesPath(pathForData);
        if (! propertiesPath.toFile().exists()) {
            LOGGER.info("Copying src/main/resources/xacml.properties to path");
            //
            // Properties do not exist, by default we will copy ours over
            // from src/main/resources
            //
            try {
                Files.copy(Paths.get("src/main/resources/xacml.properties"), propertiesPath);
            } catch (IOException e) {
                throw new XacmlApplicationException("Failed to copy xacml.propertis", e);
            }
        }
        //
        // Look for and load the properties object
        //
        try {
            pdpProperties = XacmlPolicyUtils.loadXacmlProperties(XacmlPolicyUtils.getPropertiesPath(pathForData));
            LOGGER.debug("{}", pdpProperties);
        } catch (IOException e) {
            throw new XacmlApplicationException("Failed to load xacml.propertis", e);
        }
        //
        // Create an engine
        //
        createEngine(pdpProperties);
    }

    @Override
    public List<ToscaPolicyTypeIdentifier> supportedPolicyTypes() {
        return Collections.emptyList();
    }

    @Override
    public boolean canSupportPolicyType(ToscaPolicyTypeIdentifier policyTypeId) {
        throw new UnsupportedOperationException("Please override and implement canSupportPolicyType");
    }

    @Override
    public synchronized void loadPolicy(ToscaPolicy toscaPolicy) {
        try {
            //
            // Convert the policies first
            //
            PolicyType xacmlPolicy = this.getTranslator(toscaPolicy.getType())
                .convertPolicy(toscaPolicy);
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
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Xacml Policy is {}{}", System.lineSeparator(), new String(Files.readAllBytes(refPath)));
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
        } catch (IOException | ToscaPolicyConversionException e) {
            LOGGER.error("Failed to loadPolicies {}", e);
        }
    }

    @Override
    public synchronized DecisionResponse makeDecision(DecisionRequest request) {
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
        return this.getTranslator().convertResponse(xacmlResponse);
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
