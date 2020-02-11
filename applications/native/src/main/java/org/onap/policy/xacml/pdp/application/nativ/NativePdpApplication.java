/*-
 * ============LICENSE_START=======================================================
 * ONAP
 * ================================================================================
 * Copyright (C) 2020 AT&T Intellectual Property. All rights reserved.
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

package org.onap.policy.xacml.pdp.application.nativ;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.onap.policy.models.tosca.authorative.concepts.ToscaPolicyTypeIdentifier;
import org.onap.policy.pdp.xacml.application.common.ToscaPolicyTranslator;
import org.onap.policy.pdp.xacml.application.common.std.StdXacmlApplicationServiceProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class implements an application that handles onap.policies.native.Xacml policies.
 *
 * @author Chenfei Gao (cgao@research.att.com)
 *
 */
public class NativePdpApplication extends StdXacmlApplicationServiceProvider {

    private static final Logger LOGGER = LoggerFactory.getLogger(NativePdpApplication.class);
    private static final String STRING_VERSION100 = "1.0.0";

    private List<ToscaPolicyTypeIdentifier> supportedPolicyTypes = new ArrayList<>();
    private NativePdpApplicationTranslator translator = new NativePdpApplicationTranslator();

    /**
     * Constructor.
     */
    public NativePdpApplication() {
        this.supportedPolicyTypes().add(new ToscaPolicyTypeIdentifier(
                "onap.policies.native.Xacml",
                STRING_VERSION100));
    }

    @Override
    public String applicationName() {
        return "native";
    }

    @Override
    public List<String> actionDecisionsSupported() {
        return Arrays.asList("evaluate");
    }

    @Override
    public synchronized List<ToscaPolicyTypeIdentifier> supportedPolicyTypes() {
        return Collections.unmodifiableList(supportedPolicyTypes);
    }

    @Override
    public boolean canSupportPolicyType(ToscaPolicyTypeIdentifier policyTypeId) {
        for (ToscaPolicyTypeIdentifier supported : this.supportedPolicyTypes) {
            if (policyTypeId.equals(supported)) {
                LOGGER.info("native application can support {}", supported);
                return true;
            }
        }
        return false;
    }

    @Override
    protected ToscaPolicyTranslator getTranslator(String type) {
        return translator;
    }
}
