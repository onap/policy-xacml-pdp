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

import com.google.common.collect.Lists;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.json.JSONObject;
import org.onap.policy.pdp.xacml.application.common.XacmlApplicationServiceProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class implements the onap.policies.controlloop.Guard policy implementations.
 *
 * @author pameladragosh
 *
 */
public class GuardPdpApplication implements XacmlApplicationServiceProvider {

    private static final Logger LOGGER = LoggerFactory.getLogger(GuardPdpApplication.class);
    private static final String STRING_VERSION100 = "1.0.0";
    private Map<String, String> supportedPolicyTypes = new HashMap<>();
    private Path pathForData;

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
        // Save the path
        //
        this.pathForData = pathForData;
        LOGGER.debug("New Path is {}", this.pathForData.toAbsolutePath());
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
    public JSONObject makeDecision(JSONObject jsonSchema) {
        return null;
    }

}
