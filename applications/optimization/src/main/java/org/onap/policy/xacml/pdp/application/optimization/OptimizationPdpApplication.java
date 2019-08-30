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

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.onap.policy.common.endpoints.parameters.RestServerParameters;
import org.onap.policy.models.tosca.authorative.concepts.ToscaPolicyTypeIdentifier;
import org.onap.policy.pdp.xacml.application.common.ToscaPolicyTranslator;
import org.onap.policy.pdp.xacml.application.common.XacmlApplicationException;
import org.onap.policy.pdp.xacml.application.common.std.StdMatchableTranslator;
import org.onap.policy.pdp.xacml.application.common.std.StdXacmlApplicationServiceProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OptimizationPdpApplication extends StdXacmlApplicationServiceProvider {

    private static final Logger LOGGER = LoggerFactory.getLogger(OptimizationPdpApplication.class);
    private static final String STRING_VERSION100 = "1.0.0";

    private StdMatchableTranslator translator = new StdMatchableTranslator();
    private List<ToscaPolicyTypeIdentifier> supportedPolicyTypes = new ArrayList<>();

    /**
     * Constructor.
     */
    public OptimizationPdpApplication() {
        this.supportedPolicyTypes.add(new ToscaPolicyTypeIdentifier(
                "onap.policies.optimization.AffinityPolicy", STRING_VERSION100));
        this.supportedPolicyTypes.add(new ToscaPolicyTypeIdentifier(
                "onap.policies.optimization.DistancePolicy", STRING_VERSION100));
        this.supportedPolicyTypes.add(new ToscaPolicyTypeIdentifier(
                "onap.policies.optimization.HpaPolicy", STRING_VERSION100));
        this.supportedPolicyTypes.add(new ToscaPolicyTypeIdentifier(
                "onap.policies.optimization.OptimizationPolicy", STRING_VERSION100));
        this.supportedPolicyTypes.add(new ToscaPolicyTypeIdentifier(
                "onap.policies.optimization.PciPolicy", STRING_VERSION100));
        this.supportedPolicyTypes.add(new ToscaPolicyTypeIdentifier(
                "onap.policies.optimization.QueryPolicy", STRING_VERSION100));
        this.supportedPolicyTypes.add(new ToscaPolicyTypeIdentifier(
                "onap.policies.optimization.SubscriberPolicy", STRING_VERSION100));
        this.supportedPolicyTypes.add(new ToscaPolicyTypeIdentifier(
                "onap.policies.optimization.Vim_fit", STRING_VERSION100));
        this.supportedPolicyTypes.add(new ToscaPolicyTypeIdentifier(
                "onap.policies.optimization.VnfPolicy", STRING_VERSION100));
    }

    @Override
    public String applicationName() {
        return "optimization";
    }

    @Override
    public List<String> actionDecisionsSupported() {
        return Arrays.asList("optimize");
    }

    @Override
    public void initialize(Path pathForData, RestServerParameters policyApiParameters)
            throws XacmlApplicationException {
        //
        // Store our API parameters
        //
        this.translator.setApiRestParameters(policyApiParameters);
        //
        // Let our super class do its thing
        //
        super.initialize(pathForData, policyApiParameters);
    }

    @Override
    public synchronized List<ToscaPolicyTypeIdentifier> supportedPolicyTypes() {
        return Collections.unmodifiableList(supportedPolicyTypes);
    }

    @Override
    public boolean canSupportPolicyType(ToscaPolicyTypeIdentifier policyTypeId) {
        //
        // For the time being, restrict this if the version isn't known.
        // Could be too difficult to support changing of versions dynamically.
        //
        //
        // For the time being, restrict this if the version isn't known.
        // Could be too difficult to support changing of versions dynamically.
        //
        for (ToscaPolicyTypeIdentifier supported : this.supportedPolicyTypes) {
            if (policyTypeId.equals(supported)) {
                LOGGER.info("optimization can support {}", supported);
                return true;
            }
        }
        return false;
    }

    @Override
    protected ToscaPolicyTranslator getTranslator(String type) {
        //
        // Return translator
        //
        return translator;
    }

}
