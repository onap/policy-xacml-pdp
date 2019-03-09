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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import com.att.research.xacml.std.annotations.XACMLAction;
import com.att.research.xacml.std.annotations.XACMLRequest;
import com.att.research.xacml.std.annotations.XACMLResource;
import com.att.research.xacml.std.annotations.XACMLSubject;

import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class GuardPdpApplicationTest {

    @ClassRule
    public static final TemporaryFolder policyFolder = new TemporaryFolder();

    /**
     * This is a simple annotation class to simulate
     * requests coming in.
     */
    @XACMLRequest(ReturnPolicyIdList = true)
    public class MyXacmlRequest {

        @XACMLSubject(includeInResults = true)
        String onapName = "Drools";

        @XACMLResource(includeInResults = true)
        String resource = "onap.policies.Guard";

        @XACMLAction()
        String action = "guard";
    }

    @Before
    public void setUp() throws Exception {

    }

    @Test
    public void testBasics() {
        assertThatCode(() -> {
            GuardPdpApplication guard = new GuardPdpApplication();
            //
            // Application name
            //
            assertThat(guard.applicationName()).isNotEmpty();
            //
            // Decisions
            //
            assertThat(guard.actionDecisionsSupported().size()).isEqualTo(1);
            assertThat(guard.actionDecisionsSupported()).contains("guard");
            //
            // Supported policy types
            //
            assertThat(guard.supportedPolicyTypes()).isNotEmpty();
            assertThat(guard.supportedPolicyTypes().size()).isEqualTo(2);
            assertThat(guard.canSupportPolicyType("onap.policies.controlloop.guard.FrequencyLimiter", "1.0.0"))
                .isTrue();
            assertThat(guard.canSupportPolicyType("onap.policies.controlloop.guard.FrequencyLimiter", "1.0.1"))
                .isFalse();
            assertThat(guard.canSupportPolicyType("onap.policies.controlloop.guard.MinMax", "1.0.0")).isTrue();
            assertThat(guard.canSupportPolicyType("onap.policies.controlloop.guard.MinMax", "1.0.1")).isFalse();
        }).doesNotThrowAnyException();
    }
}
