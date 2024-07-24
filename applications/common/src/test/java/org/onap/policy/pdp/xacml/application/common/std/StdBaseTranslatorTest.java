/*-
 * ============LICENSE_START=======================================================
 * ONAP
 * ================================================================================
 * Copyright (C) 2019-2022 AT&T Intellectual Property. All rights reserved.
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

package org.onap.policy.pdp.xacml.application.common.std;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.att.research.xacml.api.Advice;
import com.att.research.xacml.api.AttributeAssignment;
import com.att.research.xacml.api.Decision;
import com.att.research.xacml.api.IdReference;
import com.att.research.xacml.api.Obligation;
import com.att.research.xacml.api.Response;
import com.att.research.xacml.api.XACML3;
import com.att.research.xacml.std.StdStatusCode;
import java.text.ParseException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import oasis.names.tc.xacml._3_0.core.schema.wd_17.PolicySetType;
import oasis.names.tc.xacml._3_0.core.schema.wd_17.PolicyType;
import oasis.names.tc.xacml._3_0.core.schema.wd_17.RuleType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.onap.policy.common.utils.coder.CoderException;
import org.onap.policy.common.utils.resources.ResourceUtils;
import org.onap.policy.models.decisions.concepts.DecisionResponse;
import org.onap.policy.models.tosca.authorative.concepts.ToscaPolicy;
import org.onap.policy.pdp.xacml.application.common.TestUtilsCommon;
import org.onap.policy.pdp.xacml.application.common.ToscaDictionary;
import org.onap.policy.pdp.xacml.application.common.ToscaPolicyConversionException;

class StdBaseTranslatorTest {

    String policyJson;
    String policyBadJson;

    AttributeAssignment assignmentPolicyId;
    AttributeAssignment assignmentPolicy;
    AttributeAssignment assignmentBadPolicy;
    AttributeAssignment assignmentWeight;
    AttributeAssignment assignmentPolicyType;
    AttributeAssignment assignmentUnknown;

    Obligation obligation;

    /**
     * beforeSetup - loads and creates objects used later by the tests.
     *
     * @throws CoderException CoderException
     */
    @BeforeEach
    void beforeSetup() throws CoderException {
        policyJson = ResourceUtils.getResourceAsString("test.policy.json");
        policyBadJson = ResourceUtils.getResourceAsString("test.policy.bad.json");

        assignmentPolicyId = TestUtilsCommon.createAttributeAssignment(
            ToscaDictionary.ID_OBLIGATION_POLICY_ID.stringValue(),
            ToscaDictionary.ID_OBLIGATION_POLICY_ID_CATEGORY.stringValue(),
            "policy.id"
        );

        assignmentPolicy = TestUtilsCommon.createAttributeAssignment(
            ToscaDictionary.ID_OBLIGATION_POLICY_CONTENT.stringValue(),
            ToscaDictionary.ID_OBLIGATION_POLICY_CONTENT_CATEGORY.stringValue(),
            policyJson
        );

        assignmentBadPolicy = TestUtilsCommon.createAttributeAssignment(
            ToscaDictionary.ID_OBLIGATION_POLICY_CONTENT.stringValue(),
            ToscaDictionary.ID_OBLIGATION_POLICY_CONTENT_CATEGORY.stringValue(),
            policyBadJson
        );

        assignmentWeight = TestUtilsCommon.createAttributeAssignment(
            ToscaDictionary.ID_OBLIGATION_POLICY_WEIGHT.stringValue(),
            ToscaDictionary.ID_OBLIGATION_POLICY_WEIGHT_CATEGORY.stringValue(),
            0
        );

        assignmentPolicyType = TestUtilsCommon.createAttributeAssignment(
            ToscaDictionary.ID_OBLIGATION_POLICY_TYPE.stringValue(),
            ToscaDictionary.ID_OBLIGATION_POLICY_TYPE_CATEGORY.stringValue(),
            "onap.policies.Test"
        );

        assignmentUnknown = TestUtilsCommon.createAttributeAssignment(
            "foo:bar",
            XACML3.ID_SUBJECT_CATEGORY_ACCESS_SUBJECT.stringValue(),
            10.2
        );

        obligation = TestUtilsCommon.createXacmlObligation(
            ToscaDictionary.ID_OBLIGATION_REST_BODY.stringValue(),
            Arrays.asList(assignmentPolicyId, assignmentPolicy, assignmentWeight, assignmentPolicyType));
    }

    @Test
    void testTranslatorNormalFlow() throws Exception {
        StdBaseTranslator translator = new MyStdBaseTranslator();
        assertNotNull(translator);
        assertThatThrownBy(() -> translator.convertPolicy(null)).isInstanceOf(ToscaPolicyConversionException.class);
        assertNull(translator.convertRequest(null));

        assertThat(translator.generateAnyOfForPolicyType("foo.bar")).isNotNull();
        assertThat(translator.generateAnyOfForPolicyType("foo.bar").getAllOf()).hasSize(1);

        assertThat(translator.generateConditionForPolicyType("foo.bar")).isNotNull();
        assertThat(translator.generateConditionForPolicyType("foo.bar").getExpression()).isNotNull();

        //
        // Test the addObligation method
        //
        PolicySetType policySet = new PolicySetType();

        translator.addObligation(policySet, "policy.id", policyJson, 0, "foo.bar");

        assertThat(policySet.getObligationExpressions().getObligationExpression()).hasSize(1);
        assertThat(policySet.getObligationExpressions().getObligationExpression().get(0)
            .getAttributeAssignmentExpression()).hasSize(4);

        PolicyType policy = new PolicyType();
        translator.addObligation(policy, null, policyJson, null, null);

        assertThat(policy.getObligationExpressions().getObligationExpression()).hasSize(1);
        assertThat(policy.getObligationExpressions().getObligationExpression().get(0)
            .getAttributeAssignmentExpression()).hasSize(1);

        RuleType rule = new RuleType();
        translator.addObligation(rule, "policy.id", null, null, "foo.bar");

        assertThat(rule.getObligationExpressions().getObligationExpression()).hasSize(1);
        assertThat(rule.getObligationExpressions().getObligationExpression().get(0)
            .getAttributeAssignmentExpression()).hasSize(2);

        rule = new RuleType();
        translator.addObligation(rule, null, null, null, null);

        assertThat(rule.getObligationExpressions().getObligationExpression()).hasSize(1);
        assertThat(rule.getObligationExpressions().getObligationExpression().get(0)
            .getAttributeAssignmentExpression()).isEmpty();

        //
        // Should not throw an exception
        //
        translator.addObligation("", "policy.id", policyJson, null, "foo.bar");

        //
        // Test the response conversion
        //
        Map<String, String> ids = new HashMap<>();
        ids.put("onap.policies.Test", "1.0.0");
        Collection<IdReference> policyIds = TestUtilsCommon.createPolicyIdList(ids);

        Response xacmlResponse = TestUtilsCommon.createXacmlResponse(StdStatusCode.STATUS_CODE_OK, null,
            Decision.PERMIT, Collections.singletonList(obligation), policyIds);

        DecisionResponse decision = translator.convertResponse(xacmlResponse);

        assertNotNull(decision);

        assertThat(decision.getPolicies()).isNotNull();
        assertThat(decision.getPolicies()).isEmpty();
    }

    @Test
    void testBadData() throws ToscaPolicyConversionException, ParseException {
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

        Map<String, String> ids = new HashMap<>();
        ids.put("onap.policies.Test", "1.0.0");
        Collection<IdReference> policyIds = TestUtilsCommon.createPolicyIdList(ids);

        Response xacmlResponse = TestUtilsCommon.createXacmlResponse(StdStatusCode.STATUS_CODE_OK, null,
            Decision.PERMIT, Collections.singletonList(obligation), policyIds);

        DecisionResponse decision = translator.convertResponse(xacmlResponse);

        assertNotNull(decision);

        assertThat(decision.getPolicies()).isNotNull();
        assertThat(decision.getPolicies()).isEmpty();

        Obligation badObligation = TestUtilsCommon.createXacmlObligation(
            ToscaDictionary.ID_OBLIGATION_REST_BODY.stringValue(),
            Arrays.asList(assignmentBadPolicy, assignmentUnknown));

        xacmlResponse = TestUtilsCommon.createXacmlResponse(StdStatusCode.STATUS_CODE_MISSING_ATTRIBUTE, null,
            Decision.PERMIT, List.of(badObligation), policyIds);

        decision = translator.convertResponse(xacmlResponse);

        assertNotNull(decision);

        xacmlResponse = TestUtilsCommon.createXacmlResponse(StdStatusCode.STATUS_CODE_PROCESSING_ERROR,
            "Bad obligation", Decision.DENY, List.of(badObligation), policyIds);

        decision = translator.convertResponse(xacmlResponse);

        assertNotNull(decision);
        assertThat(decision.getStatus()).isEqualTo("error");
        assertThat(decision.getMessage()).isEqualTo("Bad obligation");
    }

    private static class MyStdBaseTranslator extends StdBaseTranslator {

        @Override
        protected void scanObligations(Collection<Obligation> obligations, DecisionResponse decisionResponse) {
            // do nothing for unit test
        }

        @Override
        protected void scanAdvice(Collection<Advice> advice, DecisionResponse decisionResponse) {
            // do nothing for unit test
        }

    }

    private static class TestTranslator extends StdBaseTranslator {
        Map<String, Object> metadata = new HashMap<>();

        @Override
        protected void scanObligations(Collection<Obligation> obligations, DecisionResponse decisionResponse) {
            // do nothing for unit test
        }

        @Override
        protected void scanAdvice(Collection<Advice> advice, DecisionResponse decisionResponse) {
            // do nothing for unit test
        }

        @Override
        public PolicyType convertPolicy(ToscaPolicy toscaPolicy) throws ToscaPolicyConversionException {
            PolicyType xacmlPolicy = new PolicyType();
            this.fillMetadataSection(xacmlPolicy, metadata);
            return xacmlPolicy;
        }
    }

}
