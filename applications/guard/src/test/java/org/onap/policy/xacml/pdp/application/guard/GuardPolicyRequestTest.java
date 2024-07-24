/*-
 * ============LICENSE_START=======================================================
 * ONAP
 * ================================================================================
 * Copyright (C) 2020 AT&T Intellectual Property. All rights reserved.
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

package org.onap.policy.xacml.pdp.application.guard;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.onap.policy.common.utils.coder.StandardCoder;
import org.onap.policy.common.utils.resources.TextFileUtils;
import org.onap.policy.models.decisions.concepts.DecisionRequest;
import org.onap.policy.pdp.xacml.application.common.ToscaPolicyConversionException;

class GuardPolicyRequestTest {

    @Test
    void testAnomalies() throws ToscaPolicyConversionException {
        DecisionRequest decisionRequest = new DecisionRequest();
        assertThat(GuardPolicyRequest.createInstance(decisionRequest)).isNotNull();

        Map<String, Object> resources = new HashMap<>();
        decisionRequest.setResource(resources);
        assertThat(GuardPolicyRequest.createInstance(decisionRequest)).isNotNull();

        resources.put("notguard", "foo");
        decisionRequest.setResource(resources);
        assertThat(GuardPolicyRequest.createInstance(decisionRequest)).isNotNull();

        resources.put("guard", null);
        decisionRequest.setResource(resources);
        assertThat(GuardPolicyRequest.createInstance(decisionRequest)).isNotNull();

        Map<String, Object> guard = new HashMap<>();
        resources.put("guard", guard);
        decisionRequest.setResource(resources);
        assertThat(GuardPolicyRequest.createInstance(decisionRequest)).isNotNull();

        guard.put("crap", "notused");
        resources.put("guard", guard);
        decisionRequest.setResource(resources);
        assertThat(GuardPolicyRequest.createInstance(decisionRequest)).isNotNull();

        guard.put("actor", "notused");
        resources.put("guard", guard);
        decisionRequest.setResource(resources);
        assertThat(GuardPolicyRequest.createInstance(decisionRequest)).isNotNull();

        guard.put("recipe", "notused");
        resources.put("guard", guard);
        decisionRequest.setResource(resources);
        assertThat(GuardPolicyRequest.createInstance(decisionRequest)).isNotNull();

        guard.put("clname", "notused");
        resources.put("guard", guard);
        decisionRequest.setResource(resources);
        assertThat(GuardPolicyRequest.createInstance(decisionRequest)).isNotNull();

        guard.put("target", "notused");
        resources.put("guard", guard);
        decisionRequest.setResource(resources);
        assertThat(GuardPolicyRequest.createInstance(decisionRequest)).isNotNull();

        guard.put("vfCount", 1);
        resources.put("guard", guard);
        decisionRequest.setResource(resources);
        assertThat(GuardPolicyRequest.createInstance(decisionRequest)).isNotNull();

        guard.put("vfCount", "I am not valid");
        resources.put("guard", guard);
        decisionRequest.setResource(resources);
        assertThatExceptionOfType(ToscaPolicyConversionException.class).isThrownBy(() ->
            GuardPolicyRequest.createInstance(decisionRequest));
    }

    @Test
    void testFilterResources() throws Exception {
        StandardCoder gson = new StandardCoder();

        DecisionRequest request = gson.decode(
            TextFileUtils.getTextFileAsString("src/test/resources/requests/guard.filter.json"),
            DecisionRequest.class);

        GuardPolicyRequest guardRequest = GuardPolicyRequest.createInstance(request);

        assertThat(guardRequest.getVnfName()).isEqualTo("my-name");
        assertThat(guardRequest.getVnfId()).isEqualTo("my-id");
        assertThat(guardRequest.getVnfType()).isEqualTo("my-type");
        assertThat(guardRequest.getVnfNfNamingCode()).isEqualTo("my-naming-code");
        assertThat(guardRequest.getVserverId()).isEqualTo("my-server-id");
        assertThat(guardRequest.getCloudRegionId()).isEqualTo("my-region");
    }

}
