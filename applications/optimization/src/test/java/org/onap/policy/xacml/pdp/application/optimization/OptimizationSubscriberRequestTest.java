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

package org.onap.policy.xacml.pdp.application.optimization;

import static org.assertj.core.api.Assertions.assertThat;

import com.att.research.xacml.api.Request;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.onap.policy.common.utils.coder.StandardCoder;
import org.onap.policy.common.utils.resources.TextFileUtils;
import org.onap.policy.models.decisions.concepts.DecisionRequest;

class OptimizationSubscriberRequestTest {
    private static StandardCoder gson = new StandardCoder();
    private DecisionRequest request;

    /**
     * setupLoadDecision loads the decision request object for testing.
     *
     * @throws Exception Exception if unable to load
     */
    @BeforeEach
    void setupLoadDecision() throws Exception {
        request = gson.decode(
            TextFileUtils
                .getTextFileAsString(
                    "src/test/resources/decision.optimization.input.json"),
            DecisionRequest.class);

        assertThat(request).isNotNull();
    }

    @Test
    void testDecisionRequest() throws Exception {
        //
        // Add context
        //
        Map<String, Object> context = new HashMap<>();
        context.put("subscriberRole", "role1");
        request.setContext(context);
        //
        // Check the return object
        //
        Request xacml = OptimizationSubscriberRequest.createInstance(request);

        assertThat(xacml).isNotNull();
        assertThat(xacml.getRequestAttributes()).hasSize(4);

        List<String> roles = new ArrayList<>();
        roles.add("role-A");
        roles.add("role-B");
        context.put("subscriberRole", "role1");
        request.setContext(context);

        xacml = OptimizationSubscriberRequest.createInstance(request);
        assertThat(xacml).isNotNull();
        assertThat(xacml.getRequestAttributes()).hasSize(4);
    }
}
