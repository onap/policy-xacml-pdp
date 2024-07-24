/*-
 * ============LICENSE_START=======================================================
 * ONAP
 * ================================================================================
 * Copyright (C) 2019 AT&T Intellectual Property. All rights reserved.
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

package org.onap.policy.pdp.xacml.application.common;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.att.research.xacml.api.AttributeAssignment;
import com.att.research.xacml.api.Obligation;
import com.att.research.xacml.api.XACML3;
import java.util.Arrays;
import oasis.names.tc.xacml._3_0.core.schema.wd_17.ObligationExpressionType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.onap.policy.common.utils.resources.ResourceUtils;

class OnapObligationTest {

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
     * setup - create test data.
     */
    @BeforeEach
    void setup() {
        policyJson = ResourceUtils.getResourceAsString("test.policy.json");
        policyBadJson = ResourceUtils.getResourceAsString("test.policy.bad.json");

        assignmentPolicyId = TestUtilsCommon.createAttributeAssignment(
            ToscaDictionary.ID_OBLIGATION_POLICY_ID.stringValue(),
            ToscaDictionary.ID_OBLIGATION_POLICY_ID_CATEGORY.stringValue(),
            policyJson
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
            Arrays.asList(assignmentPolicyId, assignmentPolicy, assignmentWeight, assignmentPolicyType,
                assignmentUnknown));
    }

    @Test
    void testObligation() {
        OnapObligation onapObligation = new OnapObligation(obligation);
        assertNotNull(onapObligation);
        assertThat(onapObligation.getPolicyId()).isEqualTo(assignmentPolicyId.getAttributeValue().getValue());
        assertThat(onapObligation.getPolicyType()).isEqualTo(assignmentPolicyType.getAttributeValue().getValue());
        assertThat(onapObligation.getPolicyContent()).isEqualTo(assignmentPolicy.getAttributeValue().getValue());
        assertThat(onapObligation.getWeight()).isEqualTo(assignmentWeight.getAttributeValue().getValue());
    }

    @Test
    void testSimplePolicy() {
        OnapObligation onapObligation = new OnapObligation("my.policy.id", policyJson);
        assertNotNull(onapObligation);
        assertThat(onapObligation.getPolicyId()).isEqualTo("my.policy.id");
        assertThat(onapObligation.getPolicyContent()).isEqualTo(policyJson);
        assertThat(onapObligation.getPolicyType()).isNull();
        assertThat(onapObligation.getWeight()).isNull();
        //
        // Create an obligation from it
        //
        ObligationExpressionType newObligation = onapObligation.generateObligation();
        assertNotNull(newObligation);
        assertThat(newObligation.getAttributeAssignmentExpression()).hasSize(2);
    }


    @Test
    void testWeightedPolicy() {
        OnapObligation onapObligation = new OnapObligation("my.policy.id", policyJson, "onap.policies.Test", 5);
        assertNotNull(onapObligation);
        assertThat(onapObligation.getPolicyId()).isEqualTo("my.policy.id");
        assertThat(onapObligation.getPolicyContent()).isEqualTo(policyJson);
        assertThat(onapObligation.getPolicyType()).isEqualTo("onap.policies.Test");
        assertThat(onapObligation.getWeight()).isEqualTo(5);
        //
        // Create an obligation from it
        //
        ObligationExpressionType newObligation = onapObligation.generateObligation();
        assertNotNull(newObligation);
        assertThat(newObligation.getAttributeAssignmentExpression()).hasSize(4);
    }

}
