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
import com.att.research.xacml.api.pdp.PDPEngine;
import com.att.research.xacml.api.pdp.PDPException;
import com.google.common.collect.Lists;

import java.io.IOException;
import java.io.InputStream;
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
import org.onap.policy.pdp.xacml.application.common.ToscaPolicyConverter;
import org.onap.policy.pdp.xacml.application.common.XacmlApplicationServiceProvider;
import org.onap.policy.pdp.xacml.application.common.XacmlPolicyUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class implements the onap.policies.controlloop.Guard policy implementations.
 *
 * @author pameladragosh
 *
 */
public class GuardPdpApplication implements ToscaPolicyConverter, XacmlApplicationServiceProvider {

    private static final Logger LOGGER = LoggerFactory.getLogger(GuardPdpApplication.class);
    private static final String STRING_VERSION100 = "1.0.0";
    private Map<String, String> supportedPolicyTypes = new HashMap<>();
    private Path pathForData;
    private Properties pdpProperties = null;
    private PDPEngine pdpEngine = null;

    /** Constructor.
     *
     */
    public GuardPdpApplication() {
        this.supportedPolicyTypes.put("onap.policies.controlloop.guard.FrequencyLimiter", STRING_VERSION100);
        this.supportedPolicyTypes.put("onap.policies.controlloop.guard.MinMax", STRING_VERSION100);
    }

    @Override
    public String applicationName() {
        return "Guard Application";
    }

    @Override
    public List<String> actionDecisionsSupported() {
        return Arrays.asList("guard");
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
        PDPEngine newEngine = XacmlPolicyUtils.createEngine(pdpProperties);
        if (newEngine != null) {
            pdpEngine = newEngine;
        }
    }

    @Override
    public List<String> supportedPolicyTypes() {
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
    public void loadPolicies(Map<String, Object> toscaPolicies) {
    }

    @Override
    public DecisionResponse makeDecision(DecisionRequest request) {
        //
        // Convert to a XacmlRequest
        //
        Request xacmlRequest = this.convertRequest(request);
        //
        // Now get a decision
        //
        Response xacmlResponse = this.xacmlDecision(xacmlRequest);
        //
        // Convert to a DecisionResponse
        //
        return this.convertResponse(xacmlResponse);
    }

    @Override
    public List<PolicyType> convertPolicies(InputStream isToscaPolicy) throws ToscaPolicyConversionException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public List<PolicyType> convertPolicies(Map<String, Object> toscaObject) throws ToscaPolicyConversionException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Request convertRequest(DecisionRequest request) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public DecisionResponse convertResponse(Response response) {
        // TODO Auto-generated method stub
        return null;
    }

    /**
     * Make a decision call.
     *
     * @param request Incoming request object
     * @return Response object
     */
    private synchronized Response xacmlDecision(Request request) {
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
