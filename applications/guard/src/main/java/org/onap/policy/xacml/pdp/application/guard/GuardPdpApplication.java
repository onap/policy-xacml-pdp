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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.onap.policy.models.tosca.authorative.concepts.ToscaPolicyTypeIdentifier;
import org.onap.policy.pdp.xacml.application.common.ToscaPolicyTranslator;
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
    protected ToscaPolicyTranslator getTranslator() {
        return translator;
    }
}
