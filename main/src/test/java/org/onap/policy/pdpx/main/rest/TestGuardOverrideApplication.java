/*-
 * ============LICENSE_START=======================================================
 * ONAP
 * ================================================================================
 * Copyright (C) 2021 AT&T Intellectual Property. All rights reserved.
 * Modifications Copyright (C) 2024 Nordix Foundation.
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

package org.onap.policy.pdpx.main.rest;

import org.onap.policy.models.tosca.authorative.concepts.ToscaConceptIdentifier;
import org.onap.policy.pdp.xacml.application.common.ToscaPolicyTranslator;
import org.onap.policy.xacml.pdp.application.guard.GuardPdpApplication;
import org.onap.policy.xacml.pdp.application.guard.GuardTranslator;

public class TestGuardOverrideApplication extends GuardPdpApplication {
    public static final String MY_EXTRAGUARD_POLICY_TYPE = "onap.policies.controlloop.guard.common.myGuard";

    private static class MyTranslator extends GuardTranslator {

    }

    private final GuardTranslator myTranslator = new MyTranslator();

    /**
     * Constructor calls the super to add all the default policy types,
     * and then adds the extra supported guard policy type.
     */
    public TestGuardOverrideApplication() {
        super();
        this.supportedPolicyTypes.add(new ToscaConceptIdentifier(
                MY_EXTRAGUARD_POLICY_TYPE,
                STRING_VERSION100));

    }

    @Override
    protected ToscaPolicyTranslator getTranslator(String type) {
        if (MY_EXTRAGUARD_POLICY_TYPE.equals(type)) {
            return myTranslator;
        }
        return super.getTranslator(type);
    }

    @Override
    public boolean canSupportPolicyType(ToscaConceptIdentifier policyTypeId) {
        if (super.canSupportPolicyType(policyTypeId)) {
            return true;
        }
        return MY_EXTRAGUARD_POLICY_TYPE.equals(policyTypeId.getName());
    }
}
