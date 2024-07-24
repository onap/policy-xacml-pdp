/*-
 * ============LICENSE_START=======================================================
 * ONAP
 * ================================================================================
 * Copyright (C) 2020 AT&T Intellectual Property. All rights reserved.
 * Modifications Copyright (C) 2020, 2024 Nordix Foundation
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

package org.onap.policy.pdp.xacml.application.common.matchable;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.att.research.xacml.api.Identifier;
import com.att.research.xacml.api.XACML3;
import com.att.research.xacml.std.IdentifierImpl;
import com.att.research.xacml.util.XACMLPolicyWriter;
import java.io.ByteArrayOutputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import oasis.names.tc.xacml._3_0.core.schema.wd_17.AllOfType;
import oasis.names.tc.xacml._3_0.core.schema.wd_17.AnyOfType;
import oasis.names.tc.xacml._3_0.core.schema.wd_17.MatchType;
import oasis.names.tc.xacml._3_0.core.schema.wd_17.PolicyType;
import oasis.names.tc.xacml._3_0.core.schema.wd_17.TargetType;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.onap.policy.common.utils.coder.CoderException;
import org.onap.policy.common.utils.coder.StandardYamlCoder;
import org.onap.policy.common.utils.resources.ResourceUtils;
import org.onap.policy.models.tosca.authorative.concepts.ToscaDataType;
import org.onap.policy.models.tosca.authorative.concepts.ToscaPolicy;
import org.onap.policy.models.tosca.authorative.concepts.ToscaPolicyType;
import org.onap.policy.models.tosca.authorative.concepts.ToscaProperty;
import org.onap.policy.models.tosca.authorative.concepts.ToscaSchemaDefinition;
import org.onap.policy.models.tosca.authorative.concepts.ToscaServiceTemplate;
import org.onap.policy.models.tosca.simple.concepts.JpaToscaServiceTemplate;
import org.onap.policy.pdp.xacml.application.common.ToscaDictionary;
import org.onap.policy.pdp.xacml.application.common.ToscaPolicyConversionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class MatchablePolicyTypeTest implements MatchableCallback {
    private static final Logger LOGGER = LoggerFactory.getLogger(MatchablePolicyTypeTest.class);
    private static final StandardYamlCoder yamlCoder = new StandardYamlCoder();
    private static final String TEST_POLICYTYPE_FILE = "src/test/resources/matchable/onap.policies.Test-1.0.0.yaml";
    private static final String TEST_POLICY_FILE = "src/test/resources/matchable/test.policies.input.tosca.yaml";
    private static final String TEST_POLICYTYPE = "onap.policies.base.middle.Test";
    private static ToscaServiceTemplate testTemplate;
    private static ToscaPolicy testPolicy;

    /**
     * Loads our resources.
     *
     * @throws CoderException object
     */
    @BeforeAll
    static void setupLoadPolicy() throws CoderException {
        //
        // Load our test policy type
        //
        String policyType = ResourceUtils.getResourceAsString(TEST_POLICYTYPE_FILE);
        //
        // Serialize it into a class
        //
        ToscaServiceTemplate serviceTemplate = yamlCoder.decode(policyType, ToscaServiceTemplate.class);
        //
        // Make sure all the fields are setup properly
        //
        JpaToscaServiceTemplate jtst = new JpaToscaServiceTemplate();
        jtst.fromAuthorative(serviceTemplate);
        testTemplate = jtst.toAuthorative();
        //
        // Make sure the Policy Types are there
        //
        assertEquals(3, testTemplate.getPolicyTypes().size());
        assertNotNull(testTemplate.getPolicyTypes().get("onap.policies.Base"));
        assertNotNull(testTemplate.getPolicyTypes().get("onap.policies.base.Middle"));
        assertNotNull(testTemplate.getPolicyTypes().get(TEST_POLICYTYPE));
        //
        // Load our test policy
        //
        String policy = ResourceUtils.getResourceAsString(TEST_POLICY_FILE);
        //
        // Serialize it into a class
        //
        serviceTemplate = yamlCoder.decode(policy, ToscaServiceTemplate.class);
        //
        // Make sure all the fields are setup properly
        //
        jtst = new JpaToscaServiceTemplate();
        jtst.fromAuthorative(serviceTemplate);
        ToscaServiceTemplate policyTemplate = jtst.toAuthorative();
        assertEquals(1, policyTemplate.getToscaTopologyTemplate().getPolicies().size());
        testPolicy = policyTemplate.getToscaTopologyTemplate().getPolicies().get(0).get("Test.policy");
        assertNotNull(testPolicy);
    }

    @Test
    void testAllCodeCoverage() {
        assertThatExceptionOfType(NullPointerException.class).isThrownBy(() ->
            new MatchablePolicyType(null, null));

        assertThatExceptionOfType(NullPointerException.class).isThrownBy(() ->
            new MatchablePropertyTypeMap(null));
        assertThatExceptionOfType(NullPointerException.class).isThrownBy(() ->
            MatchablePolicyType.isMatchable(null));
        assertThat(MatchablePolicyType.isMatchable(new ToscaProperty())).isFalse();
        //
        // Unlikely these would be called - just get code coverage on them
        //
        ToscaSchemaDefinition schema = new ToscaSchemaDefinition();
        schema.setType("integer");
        assertThat(MatchablePolicyType.handlePrimitive("foo", schema)).isNotNull();
        schema.setType("float");
        assertThat(MatchablePolicyType.handlePrimitive("foo", schema)).isNotNull();
        schema.setType("boolean");
        assertThat(MatchablePolicyType.handlePrimitive("foo", schema)).isNotNull();
        schema.setType("timestamp");
        assertThat(MatchablePolicyType.handlePrimitive("foo", schema)).isNotNull();
        schema.setType("footype");
        assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() ->
            MatchablePolicyType.handlePrimitive("foo", schema)
        );
        assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() ->
            MatchablePolicyType.handlePrimitive("foo", schema)
        );
        ToscaProperty toscaProperty = new ToscaProperty();
        Map<String, String> metadata = new HashMap<>();
        metadata.put("matchable", "true");
        toscaProperty.setMetadata(metadata);
        toscaProperty.setType("garbage");
        assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() ->
            MatchablePolicyType.handlePrimitive("foo", toscaProperty)
        );
        Map<String, MatchableProperty> matchables = null;
        assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() ->
            MatchablePolicyType.handleList("foo", toscaProperty, matchables, this)
        );
        assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() ->
            MatchablePolicyType.handleMap("foo", toscaProperty, matchables, this)
        );
    }

    @Test
    void testPrimitiveValidation() throws Exception {
        ToscaProperty property = new ToscaProperty();
        MatchablePropertyTypeBoolean booleanValue = new MatchablePropertyTypeBoolean(property);
        assertThat(booleanValue.validate(Boolean.TRUE)).isEqualTo(Boolean.TRUE);
        assertThat(booleanValue.validate("no")).isEqualTo(Boolean.FALSE);
        assertThat(booleanValue.validate("foo")).isEqualTo(Boolean.FALSE);

        MatchablePropertyTypeInteger integerValue = new MatchablePropertyTypeInteger(property);
        assertThat(integerValue.validate("5")).isEqualTo(5);
        assertThatExceptionOfType(ToscaPolicyConversionException.class).isThrownBy(() -> integerValue.validate("foo"));

        MatchablePropertyTypeFloat floatValue = new MatchablePropertyTypeFloat(property);
        assertThat(floatValue.validate("5")).isEqualTo(5);
        assertThat(floatValue.validate(Float.MIN_NORMAL)).isEqualTo(Float.MIN_NORMAL);
        assertThatExceptionOfType(ToscaPolicyConversionException.class).isThrownBy(() -> floatValue.validate("foo"));

        MatchablePropertyTypeTimestamp timestampValue = new MatchablePropertyTypeTimestamp(property);
        assertThat(timestampValue.validate("2018-10-11T22:12:44").getHour()).isEqualTo(22);
        assertThatExceptionOfType(ToscaPolicyConversionException.class).isThrownBy(() ->
            timestampValue.validate("foo"));

        ToscaSchemaDefinition schema = new ToscaSchemaDefinition();
        schema.setType("string");
        property.setEntrySchema(schema);
        MatchablePropertyTypeMap mapValue = new MatchablePropertyTypeMap(property);
        assertThat(mapValue.validate("foo")).isEmpty();

        MatchablePropertyTypeList listValue = new MatchablePropertyTypeList(property);
        assertThat(listValue.validate("foo")).isEmpty();
    }

    @Test
    void testMatchables() throws ToscaPolicyConversionException {
        //
        // Step 1: Create matchables from the PolicyType
        //
        MatchablePolicyType matchablePolicyType = new MatchablePolicyType(testTemplate.getPolicyTypes()
            .get(TEST_POLICYTYPE), this);
        assertThat(matchablePolicyType).isNotNull();
        assertThat(matchablePolicyType.getPolicyId()).isNotNull();
        assertThat(matchablePolicyType.getPolicyId().getName()).isEqualTo(TEST_POLICYTYPE);
        //
        // Dump them out to see what we have
        //
        matchablePolicyType.getMatchables().forEach((matchable, property) -> {
            LOGGER.info("matchable: {}: {}", matchable, property);
        });
        //
        // Sanity check - these are the total possible match types available
        //
        assertThat(matchablePolicyType.getMatchables()).hasSize(19);
        //
        // Step 2) Go through example policy and generate data for our Target
        //
        final TargetType target = new TargetType();
        target.getAnyOf().add(new AnyOfType());
        generateTargetType(target, matchablePolicyType, testPolicy.getProperties());
        //
        // Stuff results in a simple Policy
        //
        final PolicyType policy = new PolicyType();
        policy.setTarget(target);
        policy.setPolicyId("foo");
        policy.setVersion("1");
        policy.setRuleCombiningAlgId(XACML3.DENY_UNLESS_PERMIT);
        //
        // Dump it out so we can see what was created
        //
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        XACMLPolicyWriter.writePolicyFile(os, policy);
        LOGGER.info("{}", os);
        //
        // Sanity check - the example policy should have each possible match type plus
        // an extra one for the list and an extra one for the map.
        //
        assertThat(policy.getTarget().getAnyOf()).hasSize(20);
    }

    @Override
    public ToscaPolicyType retrievePolicyType(String derivedFrom) {
        for (Entry<String, ToscaPolicyType> entrySet : testTemplate.getPolicyTypes().entrySet()) {
            if (entrySet.getValue().getName().equals(derivedFrom)) {
                return entrySet.getValue();
            }
        }
        return null;
    }

    @Override
    public ToscaDataType retrieveDataType(String datatype) {
        return testTemplate.getDataTypes().get(datatype);
    }

    @SuppressWarnings("unchecked")
    private void generateTargetType(TargetType target, MatchablePolicyType matchablePolicyType,
                                    Map<String, Object> properties) throws ToscaPolicyConversionException {
        for (Entry<String, Object> entrySet : properties.entrySet()) {
            String propertyName = entrySet.getKey();
            Object propertyValue = entrySet.getValue();
            MatchableProperty matchable = matchablePolicyType.get(propertyName);
            if (matchable != null) {
                Identifier id = new IdentifierImpl(ToscaDictionary.ID_RESOURCE_MATCHABLE + propertyName);
                Object object = matchable.getType().generate(propertyValue, id);
                //
                // Depending on what type it is, add it into the target
                //
                if (object instanceof AnyOfType) {
                    target.getAnyOf().add((AnyOfType) object);
                } else if (object instanceof MatchType) {
                    AllOfType allOf = new AllOfType();
                    allOf.getMatch().add((MatchType) object);
                    AnyOfType anyOf = new AnyOfType();
                    anyOf.getAllOf().add(allOf);
                    target.getAnyOf().add(anyOf);
                }
            } else {
                //
                // Here is the special case where we look for a Collection of values that may
                // contain potential matchables
                //
                if (propertyValue instanceof List) {
                    for (Object listValue : ((List<?>) propertyValue)) {
                        if (listValue instanceof Map) {
                            generateTargetType(target, matchablePolicyType, (Map<String, Object>) listValue);
                        }
                    }
                } else if (propertyValue instanceof Map) {
                    generateTargetType(target, matchablePolicyType, (Map<String, Object>) propertyValue);
                }
            }
        }
    }
}
