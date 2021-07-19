/*-
 * ============LICENSE_START=======================================================
 * ONAP
 * ================================================================================
 * Copyright (C) 2020-2021 AT&T Intellectual Property. All rights reserved.
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

package org.onap.policy.xacml.pdp.application.match;

import java.nio.file.Path;
import java.util.Arrays;
import org.onap.policy.common.endpoints.parameters.RestClientParameters;
import org.onap.policy.models.tosca.authorative.concepts.ToscaConceptIdentifier;
import org.onap.policy.pdp.xacml.application.common.ToscaPolicyTranslator;
import org.onap.policy.pdp.xacml.application.common.XacmlApplicationException;
import org.onap.policy.pdp.xacml.application.common.std.StdMatchableTranslator;
import org.onap.policy.pdp.xacml.application.common.std.StdXacmlApplicationServiceProvider;

public class MatchPdpApplication extends StdXacmlApplicationServiceProvider {

    public static final String ONAP_MATCH_BASE_POLICY_TYPE = "onap.policies.Match";
    public static final String ONAP_MATCH_DERIVED_POLICY_TYPE = "onap.policies.match.";

    private static final ToscaConceptIdentifier supportedPolicy = new ToscaConceptIdentifier(
            ONAP_MATCH_BASE_POLICY_TYPE, "1.0.0");

    private StdMatchableTranslator translator = new StdMatchableTranslator();

    /**
     * Constructor.
     */
    public MatchPdpApplication() {
        super();

        applicationName = "match";
        actions = Arrays.asList("match");
        supportedPolicyTypes.add(supportedPolicy);
    }

    @Override
    public void initialize(Path pathForData, RestClientParameters policyApiParameters)
            throws XacmlApplicationException {
        //
        // Store our API parameters and path for translator so it
        // can go get Policy Types
        //
        this.translator.setPathForData(pathForData);
        this.translator.setApiRestParameters(policyApiParameters);
        //
        // Let our super class do its thing
        //
        super.initialize(pathForData, policyApiParameters);
    }

    @Override
    public boolean canSupportPolicyType(ToscaConceptIdentifier policyTypeId) {
        return policyTypeId.getName().startsWith(ONAP_MATCH_DERIVED_POLICY_TYPE);
    }

    @Override
    protected ToscaPolicyTranslator getTranslator(String type) {
        return translator;
    }

}
