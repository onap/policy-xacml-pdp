/*-
 * ============LICENSE_START=======================================================
 * ONAP
 * ================================================================================
 * Copyright (C) 2019-2021 AT&T Intellectual Property. All rights reserved.
 * Modifications Copyright (C) 2021 Nordix Foundation.
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

import java.util.Arrays;
import org.onap.policy.models.tosca.authorative.concepts.ToscaConceptIdentifier;
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
    protected static final String STRING_VERSION100 = "1.0.0";

    private GuardTranslator guardTranslator = new GuardTranslator();
    private CoordinationGuardTranslator coordinationTranslator = new CoordinationGuardTranslator();

    /**
     * Constructor.
     *
     */
    public GuardPdpApplication() {
        super();

        applicationName = "guard";
        actions = Arrays.asList("guard");

        this.supportedPolicyTypes.add(new ToscaConceptIdentifier(
                GuardTranslator.POLICYTYPE_FREQUENCY,
                STRING_VERSION100));
        this.supportedPolicyTypes.add(new ToscaConceptIdentifier(
                GuardTranslator.POLICYTYPE_MINMAX,
                STRING_VERSION100));
        this.supportedPolicyTypes.add(new ToscaConceptIdentifier(
                GuardTranslator.POLICYTYPE_BLACKLIST,
                STRING_VERSION100));
        this.supportedPolicyTypes.add(new ToscaConceptIdentifier(
                GuardTranslator.POLICYTYPE_FILTER,
                STRING_VERSION100));
        this.supportedPolicyTypes.add(new ToscaConceptIdentifier(
                "onap.policies.controlloop.guard.coordination.FirstBlocksSecond",
                STRING_VERSION100));
    }

    @Override
    public boolean canSupportPolicyType(ToscaConceptIdentifier policyTypeId) {
        //
        // For the time being, restrict this if the version isn't known.
        // Could be too difficult to support changing of versions dynamically.
        //
        for (ToscaConceptIdentifier supported : this.supportedPolicyTypes) {
            if (policyTypeId.equals(supported)) {
                return true;
            }
        }
        return false;
    }

    @Override
    protected ToscaPolicyTranslator getTranslator(String type) {
        LOGGER.debug("Policy type {}", type);
        if (type.contains("coordination")) {
            LOGGER.debug("returning coordinationTranslator");
            return coordinationTranslator;
        } else {
            LOGGER.debug("returning guardTranslator");
            return guardTranslator;
        }
    }

}
