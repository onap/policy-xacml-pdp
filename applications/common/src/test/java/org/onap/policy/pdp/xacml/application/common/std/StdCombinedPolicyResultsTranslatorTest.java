/*-
 * ============LICENSE_START=======================================================
 * ONAP
 * ================================================================================
 * Copyright (C) 2019-2020 AT&T Intellectual Property. All rights reserved.
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
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.att.research.xacml.api.AttributeAssignment;
import com.att.research.xacml.api.Decision;
import com.att.research.xacml.api.IdReference;
import com.att.research.xacml.api.Obligation;
import com.att.research.xacml.api.Response;
import com.att.research.xacml.std.StdStatusCode;
import java.text.ParseException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.onap.policy.common.utils.coder.CoderException;
import org.onap.policy.common.utils.coder.StandardCoder;
import org.onap.policy.common.utils.resources.ResourceUtils;
import org.onap.policy.models.decisions.concepts.DecisionRequest;
import org.onap.policy.models.decisions.concepts.DecisionResponse;
import org.onap.policy.models.tosca.authorative.concepts.ToscaPolicy;
import org.onap.policy.models.tosca.authorative.concepts.ToscaServiceTemplate;
import org.onap.policy.models.tosca.simple.concepts.JpaToscaServiceTemplate;
import org.onap.policy.pdp.xacml.application.common.TestUtilsCommon;
import org.onap.policy.pdp.xacml.application.common.ToscaDictionary;
import org.onap.policy.pdp.xacml.application.common.ToscaPolicyConversionException;

class StdCombinedPolicyResultsTranslatorTest {

    String policyJson;
    String policyBadJson;

    AttributeAssignment assignmentPolicyId;
    AttributeAssignment assignmentPolicy;
    AttributeAssignment assignmentBadPolicy;

    Obligation obligation;

    /**
     * setup - preload policies.
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


        obligation = TestUtilsCommon.createXacmlObligation(
            ToscaDictionary.ID_OBLIGATION_REST_BODY.stringValue(),
            Arrays.asList(assignmentPolicyId, assignmentPolicy));

    }

    @Test
    void test() throws ParseException {
        StdCombinedPolicyResultsTranslator translator = new StdCombinedPolicyResultsTranslator();

        assertNotNull(translator);
        assertThatThrownBy(() -> translator.convertPolicy(null)).isInstanceOf(ToscaPolicyConversionException.class);

        assertThat(translator.generateTargetType("policy.id", "onap.policy.type", "1.0.0")).isNotNull();

        Map<String, String> ids = new HashMap<>();
        ids.put("onap.policies.Test", "1.0.0");
        Collection<IdReference> policyIds = TestUtilsCommon.createPolicyIdList(ids);

        Response xacmlResponse = TestUtilsCommon.createXacmlResponse(StdStatusCode.STATUS_CODE_OK, null,
            Decision.PERMIT, Collections.singletonList(obligation), policyIds);

        DecisionResponse decision = translator.convertResponse(xacmlResponse);

        assertNotNull(decision);

        assertThat(decision.getPolicies()).isNotNull();
        assertThat(decision.getPolicies()).hasSize(1);
    }

    @Test
    void testConvert() throws ToscaPolicyConversionException, CoderException {
        StdCombinedPolicyResultsTranslator translator = new StdCombinedPolicyResultsTranslator();

        assertThatThrownBy(() -> translator.convertPolicy(null)).isInstanceOf(ToscaPolicyConversionException.class)
            .hasMessageContaining("Cannot convert a NULL policy");


        assertThatThrownBy(() -> translator.convertPolicy(
            new ToscaPolicy())).isInstanceOf(ToscaPolicyConversionException.class)
            .hasMessageContaining("missing metadata");

        StandardCoder coder = new StandardCoder();

        ToscaServiceTemplate template = coder.decode(policyJson, ToscaServiceTemplate.class);
        //
        // Make sure all the fields are setup properly
        //
        JpaToscaServiceTemplate jtst = new JpaToscaServiceTemplate();
        jtst.fromAuthorative(template);
        ToscaServiceTemplate completedJtst = jtst.toAuthorative();
        for (Map<String, ToscaPolicy> policies : completedJtst.getToscaTopologyTemplate().getPolicies()) {
            for (ToscaPolicy policy : policies.values()) {
                translator.convertPolicy(policy);
            }
        }
    }

    @Test
    void testDecision() throws ToscaPolicyConversionException {
        StdCombinedPolicyResultsTranslator translator = new StdCombinedPolicyResultsTranslator();

        DecisionRequest decision = new DecisionRequest();
        Map<String, Object> resource = new HashMap<>();
        decision.setResource(resource);
        assertNotNull(translator.convertRequest(decision));
    }
}
