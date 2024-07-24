/*-
 * ============LICENSE_START=======================================================
 * ONAP
 * ================================================================================
 * Copyright (C) 2019-2021 AT&T Intellectual Property. All rights reserved.
 * Modifications Copyright (C) 2023-2024 Nordix Foundation.
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
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.att.research.xacml.api.XACML3;
import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;
import java.util.Map;
import lombok.Getter;
import oasis.names.tc.xacml._3_0.core.schema.wd_17.AllOfType;
import oasis.names.tc.xacml._3_0.core.schema.wd_17.AnyOfType;
import oasis.names.tc.xacml._3_0.core.schema.wd_17.ApplyType;
import oasis.names.tc.xacml._3_0.core.schema.wd_17.AttributeDesignatorType;
import oasis.names.tc.xacml._3_0.core.schema.wd_17.AttributeValueType;
import oasis.names.tc.xacml._3_0.core.schema.wd_17.ConditionType;
import oasis.names.tc.xacml._3_0.core.schema.wd_17.MatchType;
import oasis.names.tc.xacml._3_0.core.schema.wd_17.ObjectFactory;
import oasis.names.tc.xacml._3_0.core.schema.wd_17.TargetType;
import oasis.names.tc.xacml._3_0.core.schema.wd_17.VariableReferenceType;
import org.junit.jupiter.api.Test;
import org.onap.policy.common.parameters.annotations.NotNull;
import org.onap.policy.common.utils.coder.CoderException;

class ToscaPolicyTranslatorUtilsTest {
    private static final ObjectFactory factory = new ObjectFactory();

    @Test
    void test() throws NoSuchMethodException, SecurityException {
        final Constructor<ToscaPolicyTranslatorUtils> constructor
            = ToscaPolicyTranslatorUtils.class.getDeclaredConstructor();
        assertTrue(Modifier.isPrivate(constructor.getModifiers()));

    }

    @Test
    void testTimeInRange() {
        ApplyType apply = ToscaPolicyTranslatorUtils.generateTimeInRange("00:00:00Z", "08:00:00Z", true);
        assertThat(apply).isNotNull();
        assertThat(apply.getExpression()).hasSize(3);
    }

    @Test
    void testBuildAndAppend() {
        assertThat(ToscaPolicyTranslatorUtils.buildAndAppendAllof(null, new MatchType())).isInstanceOf(AnyOfType.class);
        assertThat(ToscaPolicyTranslatorUtils.buildAndAppendAllof(null, new AllOfType())).isInstanceOf(AnyOfType.class);
        assertThat(ToscaPolicyTranslatorUtils.buildAndAppendAllof(null, "")).isNull();

        assertThat(ToscaPolicyTranslatorUtils.buildAndAppendTarget(new TargetType(),
            new AnyOfType()).getAnyOf()).hasSize(1);
        assertThat(ToscaPolicyTranslatorUtils.buildAndAppendTarget(new TargetType(),
            new MatchType()).getAnyOf()).hasSize(1);
        assertThat(ToscaPolicyTranslatorUtils.buildAndAppendTarget(new TargetType(),
            "").getAnyOf()).isEmpty();
    }

    @Test
    void testInteger() {
        assertThat(ToscaPolicyTranslatorUtils.parseInteger("foo")).isNull();
        assertThat(ToscaPolicyTranslatorUtils.parseInteger("1")).isEqualTo(1);
        assertThat(ToscaPolicyTranslatorUtils.parseInteger("1.0")).isEqualTo(1);
    }

    @Test
    void testAddingVariables() {
        ApplyType applyType = new ApplyType();
        applyType.setFunctionId(XACML3.ID_FUNCTION_STRING_EQUAL.stringValue());

        AttributeValueType value = new AttributeValueType();
        value.setDataType(XACML3.ID_DATATYPE_STRING.stringValue());
        value.getContent().add("1");
        applyType.getExpression().add(factory.createAttributeValue(value));

        AttributeDesignatorType designator = new AttributeDesignatorType();
        designator.setAttributeId(XACML3.ID_RESOURCE.stringValue());
        designator.setCategory(XACML3.ID_ATTRIBUTE_CATEGORY_RESOURCE.stringValue());
        designator.setDataType(XACML3.ID_DATATYPE_STRING.stringValue());
        applyType.getExpression().add(factory.createAttributeDesignator(designator));

        ConditionType condition = new ConditionType();
        condition.setExpression(factory.createApply(applyType));

        VariableReferenceType variable = new VariableReferenceType();

        variable.setVariableId("my-variable-id");

        ConditionType newCondition = ToscaPolicyTranslatorUtils.addVariableToCondition(condition, variable,
            XACML3.ID_FUNCTION_AND);

        assertThat(newCondition.getExpression().getValue()).isInstanceOf(ApplyType.class);
        Object obj = newCondition.getExpression().getValue();
        assertThat(((ApplyType) obj).getFunctionId()).isEqualTo(XACML3.ID_FUNCTION_AND.stringValue());
        assertThat(((ApplyType) obj).getExpression()).hasSize(2);
    }

    @SuppressWarnings("deprecation")
    @Test
    void testDecodeProperties() throws ToscaPolicyConversionException {
        Data data = ToscaPolicyTranslatorUtils.decodeProperties(Map.of("value", 20), Data.class);
        assertThat(data.getValue()).isEqualTo(20);

        // null value - invalid
        assertThatThrownBy(() -> ToscaPolicyTranslatorUtils.decodeProperties(Map.of(), Data.class))
            .isInstanceOf(ToscaPolicyConversionException.class).hasMessageContaining("item \"value\"");

        // value is not an integer - cannot even decode it
        assertThatThrownBy(() -> ToscaPolicyTranslatorUtils.decodeProperties(Map.of("value", "abc"), Data.class))
            .isInstanceOf(ToscaPolicyConversionException.class).getCause()
            .isInstanceOf(CoderException.class);

        // null properties - cannot even decode
        assertThatThrownBy(() -> ToscaPolicyTranslatorUtils.decodeProperties(null, Data.class))
            .isInstanceOf(ToscaPolicyConversionException.class)
            .hasMessage("Cannot decode Data from null properties");
    }

    @Getter
    @NotNull
    public static class Data {
        private Integer value;
    }
}
