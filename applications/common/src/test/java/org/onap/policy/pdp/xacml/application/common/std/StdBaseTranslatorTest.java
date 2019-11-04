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

package org.onap.policy.pdp.xacml.application.common.std;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import com.att.research.xacml.api.Obligation;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import oasis.names.tc.xacml._3_0.core.schema.wd_17.PolicyType;
import org.junit.Test;
import org.onap.policy.models.decisions.concepts.DecisionResponse;
import org.onap.policy.models.tosca.authorative.concepts.ToscaPolicy;
import org.onap.policy.pdp.xacml.application.common.ToscaPolicyConversionException;

public class StdBaseTranslatorTest {

    @Test
    public void test() {
        StdBaseTranslator translator = new MyStdBaseTranslator();
        assertNotNull(translator);
        assertThatThrownBy(() -> translator.convertPolicy(null)).isInstanceOf(ToscaPolicyConversionException.class);
        assertNull(translator.convertRequest(null));
    }

    @Test
    public void testBadData() throws ToscaPolicyConversionException {
        TestTranslator translator = new TestTranslator();

        assertThatThrownBy(() -> translator.convertPolicy(
                new ToscaPolicy())).isInstanceOf(ToscaPolicyConversionException.class)
                    .hasMessageContaining("missing metadata");

        translator.metadata.put(StdBaseTranslator.POLICY_ID, "random.policy.id");

        assertThatThrownBy(() -> translator.convertPolicy(
                new ToscaPolicy())).isInstanceOf(ToscaPolicyConversionException.class)
                    .hasMessageContaining("missing metadata");

        translator.metadata.put(StdBaseTranslator.POLICY_VERSION, "1.0.0");

        ToscaPolicy policy = new ToscaPolicy();
        assertEquals("1.0.0", translator.convertPolicy(policy).getVersion());

    }

    private class MyStdBaseTranslator extends StdBaseTranslator {

        @Override
        protected void scanObligations(Collection<Obligation> obligations, DecisionResponse decisionResponse) {
            // TODO Auto-generated method stub

        }

    }

    private class TestTranslator extends StdBaseTranslator {
        public Map<String, String> metadata = new HashMap<>();

        @Override
        protected void scanObligations(Collection<Obligation> obligations, DecisionResponse decisionResponse) {
            // TODO Auto-generated method stub

        }

        @Override
        public PolicyType convertPolicy(ToscaPolicy toscaPolicy) throws ToscaPolicyConversionException {
            PolicyType xacmlPolicy = new PolicyType();
            this.fillMetadataSection(xacmlPolicy, metadata);
            return xacmlPolicy;
        }
    }

}
