/*-
 * ============LICENSE_START=======================================================
 * Copyright (C) 2020 AT&T Intellectual Property. All rights reserved.
 * Modifications Copyright (C) 2021, 2023 Nordix Foundation.
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
 * ============LICENSE_END=========================================================
 */

package org.onap.policy.tutorial.tutorial;

import java.util.List;
import org.onap.policy.models.tosca.authorative.concepts.ToscaConceptIdentifier;
import org.onap.policy.pdp.xacml.application.common.ToscaPolicyTranslator;
import org.onap.policy.pdp.xacml.application.common.std.StdXacmlApplicationServiceProvider;

public class TutorialApplication extends StdXacmlApplicationServiceProvider {

    private final ToscaConceptIdentifier supportedPolicyType =
            new ToscaConceptIdentifier("onap.policies.Authorization", "1.0.0");
    private final TutorialTranslator translator = new TutorialTranslator();

    @Override
    public String applicationName() {
        return "tutorial";
    }

    @Override
    public List<String> actionDecisionsSupported() {
        return List.of("authorize");
    }

    @Override
    public synchronized List<ToscaConceptIdentifier> supportedPolicyTypes() {
        return List.of(supportedPolicyType);
    }

    @Override
    public boolean canSupportPolicyType(ToscaConceptIdentifier policyTypeId) {
        return supportedPolicyType.equals(policyTypeId);
    }

    @Override
    protected ToscaPolicyTranslator getTranslator(String type) {
        return translator;
    }

}
