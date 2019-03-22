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

import com.google.common.collect.Lists;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.onap.policy.pdp.xacml.application.common.std.StdXacmlApplicationServiceProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OptimizationPdpApplication extends StdXacmlApplicationServiceProvider {

    private static final Logger LOGGER = LoggerFactory.getLogger(OptimizationPdpApplication.class);
    private static final String ONAP_OPTIMIZATION_BASE_POLICY_TYPE = "onap.policies.Optimization";
    private static final String ONAP_OPTIMIZATION_DERIVED_POLICY_TYPE = "onap.policies.optimization";

    private Map<String, String> supportedPolicyTypes = new HashMap<>();

    /**
     * Constructor.
     */
    public OptimizationPdpApplication() {
        //
        // By default this supports just Monitoring policy types
        //
        supportedPolicyTypes.put(ONAP_OPTIMIZATION_BASE_POLICY_TYPE, "1.0.0");
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
        // For Optimization, we will attempt to support all versions
        // of the policy type.
        //
        return (policyType.equals(ONAP_OPTIMIZATION_BASE_POLICY_TYPE)
                || policyType.startsWith(ONAP_OPTIMIZATION_DERIVED_POLICY_TYPE));
    }
}
