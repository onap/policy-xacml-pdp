/*-
 * ============LICENSE_START=======================================================
 * ONAP
 * ================================================================================
 * Copyright (C) 2020-2021 AT&T Intellectual Property. All rights reserved.
 * Modifications Copyright (C) 2020, 2024 Nordix Foundation.
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

import com.att.research.xacml.api.Decision;
import com.att.research.xacml.api.Request;
import com.att.research.xacml.std.StdMutableResponse;
import com.att.research.xacml.std.StdMutableResult;
import com.att.research.xacml.std.StdStatus;
import com.att.research.xacml.std.StdStatusCode;
import com.att.research.xacml.util.XACMLPolicyWriter;
import java.io.ByteArrayOutputStream;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import oasis.names.tc.xacml._3_0.core.schema.wd_17.AllOfType;
import oasis.names.tc.xacml._3_0.core.schema.wd_17.AnyOfType;
import oasis.names.tc.xacml._3_0.core.schema.wd_17.MatchType;
import oasis.names.tc.xacml._3_0.core.schema.wd_17.PolicyType;
import oasis.names.tc.xacml._3_0.core.schema.wd_17.RuleType;
import oasis.names.tc.xacml._3_0.core.schema.wd_17.VariableDefinitionType;
import org.junit.jupiter.api.Test;
import org.onap.policy.common.utils.coder.StandardCoder;
import org.onap.policy.common.utils.coder.StandardYamlCoder;
import org.onap.policy.common.utils.resources.ResourceUtils;
import org.onap.policy.common.utils.resources.TextFileUtils;
import org.onap.policy.models.decisions.concepts.DecisionRequest;
import org.onap.policy.models.decisions.concepts.DecisionResponse;
import org.onap.policy.models.tosca.authorative.concepts.ToscaPolicy;
import org.onap.policy.models.tosca.authorative.concepts.ToscaServiceTemplate;
import org.onap.policy.models.tosca.simple.concepts.JpaToscaServiceTemplate;
import org.onap.policy.pdp.xacml.application.common.ToscaDictionary;
import org.onap.policy.pdp.xacml.application.common.ToscaPolicyConversionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class GuardTranslatorTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(GuardTranslatorTest.class);
    private static final StandardYamlCoder yamlCoder = new StandardYamlCoder();
    private static final StandardCoder gson = new StandardCoder();

    private final GuardTranslator translator = new GuardTranslator();

    @Test
    void testRequest() throws Exception {
        DecisionRequest decisionRequest = gson.decode(
            TextFileUtils.getTextFileAsString(
                "src/test/resources/requests/guard.vfCount.json"),
            DecisionRequest.class);
        Request xacmlRequest = translator.convertRequest(decisionRequest);

        assertThat(xacmlRequest).isNotNull();
    }

    @Test
    void testResponse() {
        StdStatus status = new StdStatus(StdStatusCode.STATUS_CODE_OK);
        StdMutableResult result = new StdMutableResult(Decision.PERMIT, status);
        StdMutableResponse response = new StdMutableResponse(result);

        DecisionResponse decisionResponse = translator.convertResponse(response);
        assertThat(decisionResponse).isNotNull();
        assertThat(decisionResponse.getStatus()).isEqualTo(Decision.PERMIT.toString());

        result = new StdMutableResult(Decision.DENY, status);
        response = new StdMutableResponse(result);
        decisionResponse = translator.convertResponse(response);
        assertThat(decisionResponse).isNotNull();
        assertThat(decisionResponse.getStatus()).isEqualTo(Decision.DENY.toString());

        result = new StdMutableResult(Decision.INDETERMINATE, status);
        response = new StdMutableResponse(result);
        decisionResponse = translator.convertResponse(response);
        assertThat(decisionResponse).isNotNull();
        assertThat(decisionResponse.getStatus()).isEqualTo(Decision.PERMIT.toString());
    }


    @Test
    void testBadPolicies() throws Exception {
        String policyYaml = ResourceUtils.getResourceAsString("src/test/resources/test-bad-policies.yaml");
        //
        // Serialize it into a class
        //
        ToscaServiceTemplate serviceTemplate = yamlCoder.decode(policyYaml, ToscaServiceTemplate.class);
        //
        // Make sure all the fields are setup properly
        //
        JpaToscaServiceTemplate jtst = new JpaToscaServiceTemplate();
        jtst.fromAuthorative(serviceTemplate);
        final ToscaServiceTemplate completedJtst = jtst.toAuthorative();
        //
        // Expected message for given policy name
        //
        final Map<String, String> name2message = new HashMap<>();
        name2message.put("frequency-missing-properties", "item \"limit\"");
        name2message.put("frequency-timewindow", "Cannot decode FrequencyDefinition");
        name2message.put("frequency-badtimerange_start", "item \"start_time\"");
        name2message.put("frequency-badtimerange_end", "item \"end_time\"");
        name2message.put("frequency-badtimerange_value", "timestamp 99:99:99 could not be parsed");
        name2message.put("minmax-notarget", "item \"target\"");
        name2message.put("minmax-nominmax", "Missing min or max field in minmax policy");
        name2message.put("blacklist-noblacklist", "item \"blacklist\"");
        name2message.put("filter-noalgorithm", "item \"algorithm\"");
        name2message.put("filter-badalgorithm",
            "Unexpected value for algorithm, should be whitelist-overrides or blacklist-overrides");
        name2message.put("filter-nofilter", "item \"filters\"");
        name2message.put("filter-nocollection", "Cannot decode FilterDefinition");
        name2message.put("filter-noarray", "Cannot decode FilterDefinition");
        name2message.put("filter-missingfield", "item \"field\"");
        name2message.put("filter-badfield", "Unexpected value for field in filter");
        name2message.put("filter-missingfilter", "item \"filter\"");
        name2message.put("filter-missingfunction", "item \"function\"");
        name2message.put("filter-badfunction", "Unexpected value for function in filter");
        name2message.put("filter-missingblacklist", "item \"blacklist\"");
        //
        // Get the policies
        //
        for (Map<String, ToscaPolicy> policies : completedJtst.getToscaTopologyTemplate().getPolicies()) {
            for (ToscaPolicy policy : policies.values()) {
                LOGGER.info("Testing policy {}", policy.getName());
                String expectedMsg = name2message.get(policy.getName());
                assertThat(expectedMsg).as(policy.getName()).isNotNull();

                assertThatExceptionOfType(ToscaPolicyConversionException.class).isThrownBy(() ->
                    translator.convertPolicy(policy)
                ).as(policy.getName()).withMessageContaining(expectedMsg);
            }
        }
    }

    @Test
    void testPolicyConversion() throws Exception {
        String policyYaml = ResourceUtils.getResourceAsString("src/test/resources/test-policies.yaml");
        //
        // Serialize it into a class
        //
        ToscaServiceTemplate serviceTemplate = yamlCoder.decode(policyYaml, ToscaServiceTemplate.class);
        //
        // Make sure all the fields are setup properly
        //
        JpaToscaServiceTemplate jtst = new JpaToscaServiceTemplate();
        jtst.fromAuthorative(serviceTemplate);
        ToscaServiceTemplate completedJtst = jtst.toAuthorative();
        //
        // Get the policies
        //
        for (Map<String, ToscaPolicy> policies : completedJtst.getToscaTopologyTemplate().getPolicies()) {
            for (ToscaPolicy policy : policies.values()) {
                //
                // Convert the policy
                //
                if ("onap.policies.controlloop.guard.common.Unknown".equals(policy.getType())) {
                    assertThatExceptionOfType(ToscaPolicyConversionException.class).isThrownBy(() ->
                        translator.convertPolicy(policy));
                    continue;
                }
                PolicyType xacmlPolicy = (PolicyType) translator.convertPolicy(policy);
                assertThat(xacmlPolicy).isNotNull();
                //
                // Let's dump it out
                //
                ByteArrayOutputStream os = new ByteArrayOutputStream();
                XACMLPolicyWriter.writePolicyFile(os, xacmlPolicy);
                LOGGER.info(os.toString());
                //
                // Validate the policy
                //
                assertThat(xacmlPolicy.getPolicyId()).isEqualTo(policy.getName());
                assertThat(xacmlPolicy.getVersion()).isEqualTo(policy.getVersion());
                assertThat(xacmlPolicy.getRuleCombiningAlgId()).isNotNull();
                validateCommon(policy, xacmlPolicy);
                //
                // Validate each policy type
                //
                if (GuardTranslator.POLICYTYPE_FREQUENCY.equals(policy.getType())) {
                    validateFrequency(xacmlPolicy);
                } else if (GuardTranslator.POLICYTYPE_MINMAX.equals(policy.getType())) {
                    validateMinMax(policy, xacmlPolicy);
                } else if (GuardTranslator.POLICYTYPE_BLACKLIST.equals(policy.getType())) {
                    validateBlacklist(policy, xacmlPolicy);
                } else if (GuardTranslator.POLICYTYPE_FILTER.equals(policy.getType())) {
                    validateFilter(xacmlPolicy);
                }
            }
        }

        ToscaPolicy testPol = completedJtst.getToscaTopologyTemplate().getPolicies().get(0).values().iterator().next();

        testPol.setProperties(new LinkedHashMap<>());
        assertThatExceptionOfType(ToscaPolicyConversionException.class)
            .isThrownBy(() -> translator.convertPolicy(testPol));

        testPol.setProperties(null);
        assertThatExceptionOfType(ToscaPolicyConversionException.class)
            .isThrownBy(() -> translator.convertPolicy(testPol));
    }

    private void validateCommon(ToscaPolicy policy, PolicyType xacmlPolicy) {
        boolean foundActor = false;
        boolean foundOperation = false;
        boolean foundTarget = false;
        boolean foundControlLoop = false;

        assertThat(xacmlPolicy.getTarget()).isNotNull();
        assertThat(xacmlPolicy.getTarget().getAnyOf()).isNotEmpty();
        for (AnyOfType anyOf : xacmlPolicy.getTarget().getAnyOf()) {
            assertThat(anyOf.getAllOf()).isNotEmpty();
            for (AllOfType allOf : anyOf.getAllOf()) {
                assertThat(allOf.getMatch()).isNotEmpty();
                for (MatchType match : allOf.getMatch()) {
                    //
                    // These fields are required
                    //
                    if (ToscaDictionary.ID_RESOURCE_GUARD_ACTOR.toString().equals(
                        match.getAttributeDesignator().getAttributeId())) {
                        assertThat(match.getAttributeValue().getContent()).isNotNull();
                        foundActor = true;
                    } else if (ToscaDictionary.ID_RESOURCE_GUARD_RECIPE.toString().equals(
                        match.getAttributeDesignator().getAttributeId())) {
                        assertThat(match.getAttributeValue().getContent()).isNotNull();
                        foundOperation = true;
                    } else {
                        //
                        // These fields are optional
                        //
                        if (ToscaDictionary.ID_RESOURCE_GUARD_TARGETID.toString().equals(
                            match.getAttributeDesignator().getAttributeId())) {
                            assertThat(policy.getProperties()).containsKey("target");
                            foundTarget = true;
                        }
                        if (ToscaDictionary.ID_RESOURCE_GUARD_CLNAME.toString().equals(
                            match.getAttributeDesignator().getAttributeId())) {
                            assertThat(policy.getProperties()).containsKey(GuardTranslator.FIELD_CONTROLLOOP);
                            foundControlLoop = true;
                        }

                    }
                }
            }
        }
        assertThat(foundActor && foundOperation).isTrue();
        if (policy.getProperties().containsKey("target")) {
            assertThat(foundTarget).isTrue();
        }
        if (policy.getProperties().containsKey(GuardTranslator.FIELD_CONTROLLOOP)) {
            assertThat(foundControlLoop).isTrue();
        }
        if (policy.getProperties().containsKey(GuardTranslator.FIELD_TIMERANGE)) {
            assertThat(xacmlPolicy.getCombinerParametersOrRuleCombinerParametersOrVariableDefinition())
                .hasAtLeastOneElementOfType(VariableDefinitionType.class);
        }
    }

    private void validateFrequency(PolicyType xacmlPolicy) {
        for (Object rule : xacmlPolicy.getCombinerParametersOrRuleCombinerParametersOrVariableDefinition()) {
            if (!(rule instanceof RuleType)) {
                continue;
            }
            assertThat(((RuleType) rule).getCondition()).isNotNull();
            assertThat(((RuleType) rule).getCondition().getExpression()).isNotNull();
        }
    }

    private void validateMinMax(ToscaPolicy policy, PolicyType xacmlPolicy) {
        boolean foundTarget = false;
        boolean foundMinOrMax = false;
        for (Object rule : xacmlPolicy.getCombinerParametersOrRuleCombinerParametersOrVariableDefinition()) {
            if (!(rule instanceof RuleType)) {
                continue;
            }
            for (AnyOfType anyOf : ((RuleType) rule).getTarget().getAnyOf()) {
                assertThat(anyOf.getAllOf()).isNotEmpty();
                for (AllOfType allOf : anyOf.getAllOf()) {
                    assertThat(allOf.getMatch()).isNotEmpty();
                    for (MatchType match : allOf.getMatch()) {
                        if (ToscaDictionary.ID_RESOURCE_GUARD_TARGETID.toString().equals(
                            match.getAttributeDesignator().getAttributeId())) {
                            assertThat(policy.getProperties()).containsKey(GuardTranslator.FIELD_TARGET);
                            foundTarget = true;
                        } else if (ToscaDictionary.ID_RESOURCE_GUARD_VFCOUNT.toString().equals(
                            match.getAttributeDesignator().getAttributeId())) {
                            assertThat(policy.getProperties().keySet()).containsAnyOf(GuardTranslator.FIELD_MIN,
                                GuardTranslator.FIELD_MAX);
                            foundMinOrMax = true;
                        }
                    }
                }
            }
        }
        assertThat(foundTarget && foundMinOrMax).isTrue();
    }

    private void validateBlacklist(ToscaPolicy policy, PolicyType xacmlPolicy) {
        boolean foundBlacklist = false;
        for (Object rule : xacmlPolicy.getCombinerParametersOrRuleCombinerParametersOrVariableDefinition()) {
            if (!(rule instanceof RuleType)) {
                continue;
            }
            assertThat(((RuleType) rule).getTarget()).isNotNull();
            assertThat(((RuleType) rule).getTarget().getAnyOf()).hasSize(1);
            for (AnyOfType anyOf : ((RuleType) rule).getTarget().getAnyOf()) {
                assertThat(anyOf.getAllOf()).isNotEmpty();
                for (AllOfType allOf : anyOf.getAllOf()) {
                    assertThat(allOf.getMatch()).isNotEmpty();
                    assertThat(allOf.getMatch()).hasSize(1);
                    for (MatchType match : allOf.getMatch()) {
                        assertThat(match.getAttributeDesignator().getAttributeId())
                            .isEqualTo(ToscaDictionary.ID_RESOURCE_GUARD_TARGETID.toString());
                        assertThat(match.getAttributeValue().getContent()).containsAnyOf("vnf1", "vnf2");
                        //
                        // This just checks that policy did have a blacklist in it.
                        //
                        assertThat(policy.getProperties()).containsKey(GuardTranslator.FIELD_BLACKLIST);
                        foundBlacklist = true;
                    }
                }
            }
        }
        assertThat(foundBlacklist).isTrue();
    }

    private void validateFilter(PolicyType xacmlPolicy) {
        assertThat(xacmlPolicy.getRuleCombiningAlgId()).endsWith("-overrides");
        for (Object rule : xacmlPolicy.getCombinerParametersOrRuleCombinerParametersOrVariableDefinition()) {
            if (!(rule instanceof RuleType)) {
                continue;
            }
            assertThat(((RuleType) rule).getTarget()).isNotNull();
            assertThat(((RuleType) rule).getTarget().getAnyOf()).hasSize(1);
            for (AnyOfType anyOf : ((RuleType) rule).getTarget().getAnyOf()) {
                assertThat(anyOf.getAllOf()).isNotEmpty();
                for (AllOfType allOf : anyOf.getAllOf()) {
                    assertThat(allOf.getMatch()).isNotEmpty();
                    assertThat(allOf.getMatch()).hasSize(1);
                    for (MatchType match : allOf.getMatch()) {
                        assertThat(match.getAttributeDesignator().getAttributeId())
                            .startsWith(GuardPolicyRequest.PREFIX_RESOURCE_ATTRIBUTE_ID);
                    }
                }
            }
        }
    }
}
