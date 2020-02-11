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

import com.att.research.xacml.api.Request;
import com.att.research.xacml.api.Response;
import java.util.Arrays;
import java.util.List;
import org.onap.policy.models.tosca.authorative.concepts.ToscaPolicyTypeIdentifier;
import org.onap.policy.pdp.xacml.application.common.ToscaPolicyTranslator;
import org.onap.policy.pdp.xacml.application.common.std.StdXacmlApplicationServiceProvider;

/**
 * This class implements an application that handles onap.policies.native.Xacml policies.
 *
 * @author Chenfei Gao (cgao@research.att.com)
 *
 */
public class NativePdpApplication extends StdXacmlApplicationServiceProvider {

    private static final ToscaPolicyTypeIdentifier supportedPolicyType = new ToscaPolicyTypeIdentifier(
            "onap.policies.native.Xacml", "1.0.0");
    private NativePdpApplicationTranslator translator = new NativePdpApplicationTranslator();

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
        return Arrays.asList(supportedPolicyType);
    }

    @Override
    public boolean canSupportPolicyType(ToscaPolicyTypeIdentifier policyTypeId) {
        return supportedPolicyType.equals(policyTypeId);
    }

    @Override
    protected ToscaPolicyTranslator getTranslator(String type) {
        return translator;
    }

    /**
     * Makes decision for the incoming native xacml request.
     * @param request the native xacml request
     * @return the native xacml response
     */
    public Response makeNativeDecision(Request request) {
        return this.xacmlDecision(request);
    }
}
