/*-
 * ============LICENSE_START=======================================================
 * ONAP
 * ================================================================================
 * Copyright (C) 2020-2021 AT&T Intellectual Property. All rights reserved.
 * Modifications Copyright (C) 2020 Nordix Foundation.
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

package org.onap.policy.xacml.pdp.application.nativ;

import com.att.research.xacml.api.Identifier;
import com.att.research.xacml.api.Request;
import com.att.research.xacml.api.Response;
import com.att.research.xacml.api.XACML3;
import com.att.research.xacml.util.XACMLPolicyScanner;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import oasis.names.tc.xacml._3_0.core.schema.wd_17.AdviceExpressionType;
import oasis.names.tc.xacml._3_0.core.schema.wd_17.AdviceExpressionsType;
import oasis.names.tc.xacml._3_0.core.schema.wd_17.AnyOfType;
import oasis.names.tc.xacml._3_0.core.schema.wd_17.ApplyType;
import oasis.names.tc.xacml._3_0.core.schema.wd_17.AttributeAssignmentExpressionType;
import oasis.names.tc.xacml._3_0.core.schema.wd_17.AttributeDesignatorType;
import oasis.names.tc.xacml._3_0.core.schema.wd_17.AttributeValueType;
import oasis.names.tc.xacml._3_0.core.schema.wd_17.ConditionType;
import oasis.names.tc.xacml._3_0.core.schema.wd_17.DefaultsType;
import oasis.names.tc.xacml._3_0.core.schema.wd_17.EffectType;
import oasis.names.tc.xacml._3_0.core.schema.wd_17.FunctionType;
import oasis.names.tc.xacml._3_0.core.schema.wd_17.MatchType;
import oasis.names.tc.xacml._3_0.core.schema.wd_17.ObjectFactory;
import oasis.names.tc.xacml._3_0.core.schema.wd_17.PolicySetType;
import oasis.names.tc.xacml._3_0.core.schema.wd_17.PolicyType;
import oasis.names.tc.xacml._3_0.core.schema.wd_17.RuleType;
import oasis.names.tc.xacml._3_0.core.schema.wd_17.TargetType;
import org.onap.policy.common.parameters.annotations.NotBlank;
import org.onap.policy.common.parameters.annotations.NotNull;
import org.onap.policy.models.decisions.concepts.DecisionRequest;
import org.onap.policy.models.decisions.concepts.DecisionResponse;
import org.onap.policy.models.tosca.authorative.concepts.ToscaPolicy;
import org.onap.policy.pdp.xacml.application.common.ToscaPolicyConversionException;
import org.onap.policy.pdp.xacml.application.common.ToscaPolicyTranslator;
import org.onap.policy.pdp.xacml.application.common.ToscaPolicyTranslatorUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class implements one translator that interprets TOSCA policy and decision API request/response payload.
 *
 * @author Chenfei Gao (cgao@research.att.com)
 *
 */
@NoArgsConstructor
public class NativePdpApplicationTranslator implements ToscaPolicyTranslator {

    private static final Logger LOGGER = LoggerFactory.getLogger(NativePdpApplicationTranslator.class);

    private static final String TOSCA_XACML_POLICY_TYPE = "onap.policies.native.ToscaXacml";

    @Override
    public Object convertPolicy(ToscaPolicy toscaPolicy) throws ToscaPolicyConversionException {
        if (TOSCA_XACML_POLICY_TYPE.equals(toscaPolicy.getType())) {
            PolicySetType policySetType = new PolicySetType();
            policySetType.setPolicySetId(String.valueOf(toscaPolicy.getMetadata().get("policy-id")));
            policySetType.setPolicyCombiningAlgId(XACML3.ID_POLICY_FIRST_APPLICABLE.stringValue());
            policySetType.setVersion(String.valueOf(toscaPolicy.getMetadata().get("policy-version")));
            policySetType.setDescription(String.valueOf(toscaPolicy.getMetadata().get("description")));
            policySetType.setTarget(setPolicySetTarget(toscaPolicy.getMetadata().get("action")));
            for (Map<String, Object> type: (List<Map<String, Object>>) toscaPolicy.getProperties().get("policies")) {
                ToscaPolicy policy = new ToscaPolicy();
                policy.setMetadata((Map<String, Object>) type.get("metadata"));
                policy.setProperties((Map<String, Object>) type.get("properties"));
                ObjectFactory objectFactory = new ObjectFactory();
                policySetType.getPolicySetOrPolicyOrPolicySetIdReference()
                        .add(objectFactory.createPolicy(convertPolicyXacml(policy)));
            }
            return policySetType;
        } else {
            //
            // Extract the Base64 encoded policy xml string and decode it
            //
            String encodedXacmlPolicy = getNativeXacmlPolicy(toscaPolicy);
            String decodedXacmlPolicy;
            try {
                decodedXacmlPolicy = new String(Base64.getDecoder().decode(encodedXacmlPolicy), StandardCharsets.UTF_8);
            } catch (IllegalArgumentException exc) {
                throw new ToscaPolicyConversionException("error on Base64 decoding the native policy", exc);
            }
            LOGGER.debug("Decoded xacml policy {}", decodedXacmlPolicy);
            //
            // Scan the string and convert to xacml PolicyType
            //
            try (var is = new ByteArrayInputStream(decodedXacmlPolicy.getBytes(StandardCharsets.UTF_8))) {
                //
                // Read the Policy In
                //
                Object policy = XACMLPolicyScanner.readPolicy(is);
                if (policy == null) {
                    throw new ToscaPolicyConversionException("Invalid XACML Policy");
                }
                return policy;
            } catch (IOException exc) {
                throw new ToscaPolicyConversionException("Failed to read policy", exc);
            }
        }
    }

    protected String getNativeXacmlPolicy(ToscaPolicy toscaPolicy) throws ToscaPolicyConversionException {

        var nativeDefinition = ToscaPolicyTranslatorUtils.decodeProperties(toscaPolicy.getProperties(),
                NativeDefinition.class);

        LOGGER.debug("Base64 encoded native xacml policy {}", nativeDefinition.getPolicy());
        return nativeDefinition.getPolicy();
    }

    @Override
    public Request convertRequest(DecisionRequest request) throws ToscaPolicyConversionException {
        throw new ToscaPolicyConversionException("Do not call native convertRequest");
    }

    @Override
    public DecisionResponse convertResponse(Response xacmlResponse) {
        //
        // We do nothing to DecisionResponse for native xacml application
        //
        return null;
    }

    @Getter
    public static class NativeDefinition {
        @NotNull
        @NotBlank
        private String policy;

        public String getPolicy() {
            return policy;
        }
    }

    /**
     * Generate Xacml rule implementing specified CoordinationDirective.
     *
     * @param toscaPolicy Incoming Tosca Policy object
     * @return the generated Xacml policy type
     * @throws ToscaPolicyConversionException if check xacml identifier is not present
     */
    private PolicyType convertPolicyXacml(ToscaPolicy toscaPolicy) throws ToscaPolicyConversionException {
        var policyType = new PolicyType();
        policyType.setPolicyId(String.valueOf(toscaPolicy.getMetadata().get("policy-id")));
        policyType.setVersion(String.valueOf(toscaPolicy.getMetadata().get("policy-version")));
        policyType.setDescription(String.valueOf(toscaPolicy.getMetadata().get("description")));
        DefaultsType defaultsType = new DefaultsType();
        defaultsType.setXPathVersion("http://www.w3.org/TR/2007/REC-xpath20-20070123");
        policyType.setPolicyDefaults(defaultsType);

        Map<String, Object> properties = toscaPolicy.getProperties();

        if (properties.get("combiningAlgo") != null) {
            policyType.setRuleCombiningAlgId(validateFilterPropertyFunction((String)
                    properties.get("combiningAlgo")).stringValue());
        } else {
            policyType.setRuleCombiningAlgId(XACML3.ID_RULE_FIRST_APPLICABLE.stringValue());
        }

        if (properties.get("target") != null) {
            policyType.setTarget(setTargetType((Map<String, Object>) properties.get("target")));
        } else {
            policyType.setTarget(new TargetType());
        }

        try {
            List<Map<String, Object>> rules = (List<Map<String, Object>>) properties.get("rules");
            for (Map<String, Object> rule : rules) {
                var ruleType = new RuleType();
                if (rule.get("description") != null) {
                    ruleType.setDescription((String) rule.get("description"));
                }
                ruleType.setRuleId(UUID.randomUUID().toString());
                if (rule.get("target") != null) {
                    ruleType.setTarget(setTargetType((Map<String, Object>) rule.get("target")));
                }
                if (rule.get("condition") != null) {
                    ruleType.setCondition(setConditionType((Map<String, Object>) rule.get("condition")));
                }
                if (rule.get("decision") == null) {
                    throw new ToscaPolicyConversionException("decision is mandatory in a rule");
                }
                String decision = (String) rule.get("decision");
                if ("Deny".equalsIgnoreCase(decision)) {
                    ruleType.setEffect(EffectType.DENY);

                } else {
                    ruleType.setEffect(EffectType.PERMIT);
                }
                if (rule.get("advice") != null) {
                    ruleType.setAdviceExpressions(setAdvice((Map<String, Object>) rule.get("advice"), decision));
                }
                policyType.getCombinerParametersOrRuleCombinerParametersOrVariableDefinition().add(ruleType);
            }
        } catch (ToscaPolicyConversionException ex) {
            throw new ToscaPolicyConversionException("Invalid rule format");
        }

        if (properties.get("default") != null) {
            var defaultRule = new RuleType();
            defaultRule.setDescription("Default Rule if none of the rules evaluate to True");
            defaultRule.setRuleId(UUID.randomUUID().toString());
            String defaultDecision = (String) properties.get("default");
            if ("Deny".equalsIgnoreCase(defaultDecision)) {
                defaultRule.setEffect(EffectType.DENY);
            } else {
                defaultRule.setEffect(EffectType.PERMIT);
            }
            policyType.getCombinerParametersOrRuleCombinerParametersOrVariableDefinition().add(defaultRule);
        }
        return policyType;
    }

    private TargetType setTargetType(Map<String, Object> appliesTo) throws ToscaPolicyConversionException {
        List<MatchType> listMatch = new ArrayList<>();
        try {
            List<Map<String, Object>> allOffList = (List<Map<String, Object>>) appliesTo.get("anyOne");
            for (Map<String, Object> allOff : allOffList) {
                for (Map<String, Object> match : (List<Map<String, Object>>) allOff.get("allOf")) {
                    var matchType = new MatchType();
                    String operator = (String) match.get("operator");
                    String datatype = getDatatype(operator);
                    matchType.setMatchId(validateFilterPropertyFunction(operator).stringValue());
                    var valueType = setAttributeValueType(match.get("value"),
                            validateFilterPropertyFunction(datatype).stringValue());
                    matchType.setAttributeValue(valueType);
                    String attribute = "";
                    String category = "";
                    if (((String) match.get("key")).contains("action")) {
                        attribute = validateFilterPropertyFunction((String) match
                                .get("key")).stringValue();
                        category = XACML3.ID_ATTRIBUTE_CATEGORY_ACTION.stringValue();
                    } else {
                        attribute = (String) match.get("key");
                        category = XACML3.ID_ATTRIBUTE_CATEGORY_RESOURCE.stringValue();
                    }
                    var designator = setAttributeDesignatorType(attribute, category,
                            validateFilterPropertyFunction(datatype).stringValue(), false);
                    matchType.setAttributeDesignator(designator);
                    listMatch.add(matchType);
                }
            }
        } catch (NullPointerException ex) {
            throw new ToscaPolicyConversionException("Invalid target format");
        }
        var anyOfType = new AnyOfType();
        MatchType[] matchTypes = new MatchType[listMatch.size()];
        anyOfType.getAllOf().add(ToscaPolicyTranslatorUtils.buildAllOf(listMatch.toArray(matchTypes)));
        var target = new TargetType();
        target.getAnyOf().add(anyOfType);
        return target;
    }

    private TargetType setPolicySetTarget(Object value) {
        var matchType = new MatchType();
        matchType.setMatchId(XACML3.ID_FUNCTION_STRING_EQUAL.stringValue());
        var valueType = setAttributeValueType(value, XACML3.ID_DATATYPE_STRING.stringValue());
        matchType.setAttributeValue(valueType);
        var designator = setAttributeDesignatorType(XACML3.ID_ACTION_ACTION_ID.stringValue(),
                XACML3.ID_ATTRIBUTE_CATEGORY_ACTION.stringValue(),
                XACML3.ID_DATATYPE_STRING.stringValue(), false);
        matchType.setAttributeDesignator(designator);
        var anyOfType = new AnyOfType();
        anyOfType.getAllOf().add(ToscaPolicyTranslatorUtils.buildAllOf(new MatchType[] {matchType}));
        var target = new TargetType();
        target.getAnyOf().add(anyOfType);
        return target;
    }

    private ConditionType setConditionType(Map<String, Object> conditionMap) throws ToscaPolicyConversionException {
        var condition = new ConditionType();
        try {
            Map<String, Object> applyMap = (Map<String, Object>) conditionMap.get("apply");
            ApplyType parentApply = setApply(applyMap);
            condition.setExpression(new ObjectFactory().createApply(parentApply));
        } catch (NullPointerException ex) {
            throw new ToscaPolicyConversionException("Invalid condition format");
        }
        return condition;
    }

    private ApplyType setApply(Map<String, Object> applies) throws ToscaPolicyConversionException {
        var apply = new ApplyType();
        try {
            List<Object> keys = (List<Object>) applies.get("keys");
            String operator = (String) applies.get("operator");
            String datatype = getDatatype(operator);
            apply.setFunctionId(validateFilterPropertyFunction(operator).stringValue());
            var factory = new ObjectFactory();
            List<Object> keyList = new ArrayList<>();
            for (Object keyObject : keys) {
                if (keyObject instanceof Map<?, ?>) {
                    if (((Map<?, ?>) keyObject).get("list") != null) {
                        setBagApply(apply, (List<Object>) ((Map<?, ?>) keyObject).get("list"), datatype, factory);
                    } else if (((Map<?, ?>) keyObject).get("function") != null) {
                        setFunctionType(apply, ((Map<String, String>) keyObject).get("function"), factory);
                    } else if (((Map<?, ?>) keyObject).get("apply") != null) {
                        keyList.add(setApply((Map<String, Object>) ((Map<?, ?>) keyObject).get("apply")));
                    } else {
                        throw new ToscaPolicyConversionException(
                                "Invalid key entry, object does not contain list, function or apply");
                    }
                } else {
                    setAttributes(keyObject, keyList, datatype, factory);
                }
            }
            for (Object attributeTypeObject : keyList) {
                if (attributeTypeObject instanceof AttributeValueType) {
                    apply.getExpression().add(factory.createAttributeValue((AttributeValueType) attributeTypeObject));
                }
            }
            for (Object applyTypeObject : keyList) {
                if (applyTypeObject instanceof ApplyType) {
                    apply.getExpression().add(factory.createApply((ApplyType) applyTypeObject));
                }
            }
            Boolean data = switch (operator) {
                case "or", "and", "n-of", "not", "all-of", "any-of", "any-of-any", "all-of-any", "all-of-all",
                     "any-of-all" -> true;
                default -> false;
            };
            if (!data && applies.get("compareWith") != null) {
                setCompareWith(applies, apply, factory, getDatatype(operator));
            }
        } catch (NullPointerException ex) {
            throw new ToscaPolicyConversionException("Invalid apply format");
        }
        return apply;
    }

    private void setAttributes(Object key, List<Object> keyList, String datatype, ObjectFactory factory)
            throws ToscaPolicyConversionException {
        try {
            if (key instanceof String) {
                String value = (String) key;
                if (value.startsWith("'") && value.endsWith("'")) {
                    AttributeValueType attributeValue = setAttributeValueType(value.substring(1, value.length() - 1),
                            validateFilterPropertyFunction(datatype).stringValue());
                    keyList.add(attributeValue);
                } else {
                    var keyDesignator = setAttributeDesignatorType(value,
                            XACML3.ID_ATTRIBUTE_CATEGORY_RESOURCE.stringValue(),
                            validateFilterPropertyFunction(datatype).stringValue(), false);
                    ApplyType keyApply = new ApplyType();
                    keyApply.setFunctionId(validateFilterPropertyFunction(datatype + "-one-and-only").stringValue());
                    keyApply.getExpression().add(factory.createAttributeDesignator(keyDesignator));
                    keyList.add(keyApply);
                }
            } else {
                AttributeValueType attributeValue = setAttributeValueType(key,
                        validateFilterPropertyFunction(datatype).stringValue());
                keyList.add(attributeValue);
            }
        } catch (NullPointerException ex) {
            throw new ToscaPolicyConversionException("Invalid string value format in keys");
        }
    }

    private void setBagApply(ApplyType apply, List<Object> list, String datatype, ObjectFactory factory)
            throws ToscaPolicyConversionException {
        try {
            var bagApply = new ApplyType();
            bagApply.setFunctionId(validateFilterPropertyFunction(datatype + "-bag").stringValue());
            for (Object attribute : list) {
                if (attribute instanceof String && !(((String) attribute).startsWith("'")
                        && ((String) attribute).endsWith("'"))) {
                    var applyDesignator = new ApplyType();
                    applyDesignator.setFunctionId(
                            validateFilterPropertyFunction(datatype + "-one-and-only").stringValue());
                    var designator = setAttributeDesignatorType((String) attribute,
                            XACML3.ID_ATTRIBUTE_CATEGORY_RESOURCE.stringValue(),
                            validateFilterPropertyFunction(datatype).stringValue(), false);
                    applyDesignator.getExpression().add(factory.createAttributeDesignator(designator));
                    bagApply.getExpression().add(factory.createApply(applyDesignator));
                }
            }
            for (Object attribute : list) {
                if (attribute instanceof String) {
                    String value = (String) attribute;
                    if (value.startsWith("'") && value.endsWith("'")) {
                        var attributeValue = setAttributeValueType(value.substring(1, value.length() - 1),
                                validateFilterPropertyFunction(datatype).stringValue());
                        bagApply.getExpression().add(factory.createAttributeValue(attributeValue));
                    }
                } else {
                    var attributeValue = setAttributeValueType(attribute,
                            validateFilterPropertyFunction(datatype).stringValue());
                    bagApply.getExpression().add(factory.createAttributeValue(attributeValue));
                }
            }
            apply.getExpression().add(factory.createApply(bagApply));
        } catch (NullPointerException ex) {
            throw new ToscaPolicyConversionException("Invalid list format in keys");
        }
    }

    private void setFunctionType(ApplyType apply, String function, ObjectFactory factory)
            throws ToscaPolicyConversionException {
        try {
            var functionType = new FunctionType();
            functionType.setFunctionId(validateFilterPropertyFunction(function).stringValue());
            apply.getExpression().add(factory.createFunction(functionType));
        } catch (NullPointerException ex) {
            throw new ToscaPolicyConversionException("Invalid function format in keys");
        }
    }

    private void setCompareWith(Map<String, Object> compareWithMap, ApplyType apply, ObjectFactory factory,
                                String datatype) throws ToscaPolicyConversionException {
        try {
            Map<String, Object> compareWith = (Map<String, Object>) compareWithMap.get("compareWith");
            if (compareWith.get("apply") != null) {
                ApplyType compareApply = setApply((Map<String, Object>) compareWith.get("apply"));
                apply.getExpression().add(factory.createApply(compareApply));
            } else if (compareWith.get("value") != null) {
                var attributeValue = setAttributeValueType(compareWith.get("value"),
                        validateFilterPropertyFunction(datatype).stringValue());
                apply.getExpression().add(factory.createAttributeValue(attributeValue));
            } else if (compareWith.get("key") != null) {
                var keyDesignator = setAttributeDesignatorType((String) compareWith.get("key"),
                        XACML3.ID_ATTRIBUTE_CATEGORY_RESOURCE.stringValue(),
                        validateFilterPropertyFunction(datatype).stringValue(), false);
                var keyApply = new ApplyType();
                keyApply.setFunctionId(validateFilterPropertyFunction(datatype + "-one-and-only").stringValue());
                keyApply.getExpression().add(factory.createAttributeDesignator(keyDesignator));
                apply.getExpression().add(factory.createApply(keyApply));
            } else {
                throw new ToscaPolicyConversionException("compareWith does not contain apply, value or key");
            }
        } catch (NullPointerException ex) {
            throw new ToscaPolicyConversionException("Invalid compareWith format");
        }
    }

    private AdviceExpressionsType setAdvice(Map<String, Object> advice, String decision)
            throws ToscaPolicyConversionException {
        var adviceExpressions = new AdviceExpressionsType();
        try {
            var adviceExpression = new AdviceExpressionType();
            adviceExpression.setAdviceId(UUID.randomUUID().toString());
            var value = setAttributeValueType(advice.get("value"), XACML3.ID_DATATYPE_STRING.stringValue());
            var assignment = new AttributeAssignmentExpressionType();
            assignment.setAttributeId("urn:oasis:names:tc:xacml:2.0:example:attribute:text");
            assignment.setCategory(XACML3.ID_SUBJECT_CATEGORY_ACCESS_SUBJECT.stringValue());
            assignment.setExpression(new ObjectFactory().createAttributeValue(value));
            adviceExpression.getAttributeAssignmentExpression().add(assignment);
            if ("Deny".equalsIgnoreCase(decision)) {
                adviceExpression.setAppliesTo(EffectType.DENY);
            } else {
                adviceExpression.setAppliesTo(EffectType.PERMIT);
            }
            adviceExpressions.getAdviceExpression().add(adviceExpression);
        } catch (NullPointerException ex) {
            throw new ToscaPolicyConversionException("Invalid advice format");
        }
        return adviceExpressions;
    }

    private AttributeDesignatorType setAttributeDesignatorType(String attributeId, String category,
                                                               String dataType, Boolean mustBe) {
        var keyDesignator = new AttributeDesignatorType();
        keyDesignator.setAttributeId(attributeId);
        keyDesignator.setCategory(category);
        keyDesignator.setDataType(dataType);
        keyDesignator.setMustBePresent(mustBe);
        return keyDesignator;
    }

    private AttributeValueType setAttributeValueType(Object value, String dataType) {
        var attributeValue = new AttributeValueType();
        attributeValue.setDataType(dataType);
        attributeValue.getContent().add(value.toString());
        return attributeValue;
    }

    private String getDatatype(String operator) throws ToscaPolicyConversionException {
        try {
            if (operator.contains("-to-")) {
                return operator.split("-")[0];
            }
            if (operator.contains("-from-")) {
                return operator.split("-")[2];
            }
            if (operator.equals("round") || operator.equals("floor")) {
                return "double";
            }
            List<String> datatypes = Arrays.asList("string", "boolean", "integer", "double", "time", "date", "dateTime",
                    "dayTimeDuration", "yearMonthDuration", "anyURI", "hexBinary", "rfc822Name", "base64Binary",
                    "x500Name", "ipAddress", "dnsName");
            if (datatypes.stream().anyMatch(datatype -> operator.contains(datatype))) {
                return operator.split("-")[0];
            }
        } catch (NullPointerException ex) {
            throw new ToscaPolicyConversionException("Invalid operator");
        }
        return operator;
    }

    private Identifier validateFilterPropertyFunction(String operator) throws ToscaPolicyConversionException {
        return switch (operator.toLowerCase()) {
            //match ids
            case "string-equal" -> XACML3.ID_FUNCTION_STRING_EQUAL;
            case "integer-equal" -> XACML3.ID_FUNCTION_INTEGER_EQUAL;
            case "string-equal-ignore-case" -> XACML3.ID_FUNCTION_STRING_EQUAL_IGNORE_CASE;
            case "string-regexp-match" -> XACML3.ID_FUNCTION_STRING_REGEXP_MATCH;
            case "string-contains" -> XACML3.ID_FUNCTION_STRING_CONTAINS;
            case "string-greater-than" -> XACML3.ID_FUNCTION_STRING_GREATER_THAN;
            case "string-greater-than-or-equal" -> XACML3.ID_FUNCTION_STRING_GREATER_THAN_OR_EQUAL;
            case "string-less-than" -> XACML3.ID_FUNCTION_STRING_LESS_THAN;
            case "string-less-than-or-equal" -> XACML3.ID_FUNCTION_STRING_LESS_THAN_OR_EQUAL;
            case "string-starts-with" -> XACML3.ID_FUNCTION_STRING_STARTS_WITH;
            case "string-ends-with" -> XACML3.ID_FUNCTION_STRING_ENDS_WITH;
            case "integer-greater-than" -> XACML3.ID_FUNCTION_INTEGER_GREATER_THAN;
            case "integer-greater-than-or-equal" -> XACML3.ID_FUNCTION_INTEGER_GREATER_THAN_OR_EQUAL;
            case "integer-less-than" -> XACML3.ID_FUNCTION_INTEGER_LESS_THAN;
            case "integer-less-than-or-equal" -> XACML3.ID_FUNCTION_INTEGER_LESS_THAN_OR_EQUAL;
            case "double-greater-than" -> XACML3.ID_FUNCTION_DOUBLE_GREATER_THAN;
            case "double-greater-than-or-equal" -> XACML3.ID_FUNCTION_DOUBLE_GREATER_THAN_OR_EQUAL;
            case "double-less-than" -> XACML3.ID_FUNCTION_DOUBLE_LESS_THAN;
            case "double-less-than-or-equal" -> XACML3.ID_FUNCTION_DOUBLE_LESS_THAN_OR_EQUAL;
            case "datetime-add-daytimeduration" -> XACML3.ID_FUNCTION_DATETIME_ADD_DAYTIMEDURATION;
            case "datetime-add-yearmonthduration" -> XACML3.ID_FUNCTION_DATETIME_ADD_YEARMONTHDURATION;
            case "datetime-subtract-daytimeturation" -> XACML3.ID_FUNCTION_DATETIME_SUBTRACT_DAYTIMEDURATION;
            case "datetime-subtract-yearmonthduration" -> XACML3.ID_FUNCTION_DATETIME_SUBTRACT_YEARMONTHDURATION;
            case "date-add-yearmonthduration" -> XACML3.ID_FUNCTION_DATE_ADD_YEARMONTHDURATION;
            case "date-subtract-yearmonthduration" -> XACML3.ID_FUNCTION_DATE_SUBTRACT_YEARMONTHDURATION;
            case "time-greater-than" -> XACML3.ID_FUNCTION_TIME_GREATER_THAN;
            case "time-greater-than-or-equal" -> XACML3.ID_FUNCTION_TIME_GREATER_THAN_OR_EQUAL;
            case "time-less-than" -> XACML3.ID_FUNCTION_TIME_LESS_THAN;
            case "time-less-than-or-equal" -> XACML3.ID_FUNCTION_TIME_LESS_THAN_OR_EQUAL;
            case "datetime-greater-than" -> XACML3.ID_FUNCTION_DATETIME_GREATER_THAN;
            case "datetime-greater-than-or-equal" -> XACML3.ID_FUNCTION_DATETIME_GREATER_THAN_OR_EQUAL;
            case "datetime-less-than" -> XACML3.ID_FUNCTION_DATETIME_LESS_THAN;
            case "datetime-less-than-or-equal" -> XACML3.ID_FUNCTION_DATETIME_LESS_THAN_OR_EQUAL;
            case "date-greater-than" -> XACML3.ID_FUNCTION_DATE_GREATER_THAN;
            case "date-greater-than-or-equal" -> XACML3.ID_FUNCTION_DATE_GREATER_THAN_OR_EQUAL;
            case "date-less-than" -> XACML3.ID_FUNCTION_DATE_LESS_THAN;
            case "date-less-than-or-equal" -> XACML3.ID_FUNCTION_DATE_LESS_THAN_OR_EQUAL;
            case "boolean-one-and-only" -> XACML3.ID_FUNCTION_BOOLEAN_ONE_AND_ONLY;
            case "string-is-in" -> XACML3.ID_FUNCTION_STRING_IS_IN;
            case "integer-is-in" -> XACML3.ID_FUNCTION_INTEGER_IS_IN;
            case "boolean-is-in" -> XACML3.ID_FUNCTION_BOOLEAN_IS_IN;
            case "double-is-in" -> XACML3.ID_FUNCTION_DOUBLE_IS_IN;
            case "integer-add" -> XACML3.ID_FUNCTION_INTEGER_ADD;
            case "double-add" -> XACML3.ID_FUNCTION_DOUBLE_ADD;
            case "integer-subtract" -> XACML3.ID_FUNCTION_INTEGER_SUBTRACT;
            case "double-subtract" -> XACML3.ID_FUNCTION_DOUBLE_SUBTRACT;
            case "integer-multiply" -> XACML3.ID_FUNCTION_INTEGER_MULTIPLY;
            case "double-multiply" -> XACML3.ID_FUNCTION_DOUBLE_MULTIPLY;
            case "integer-divide" -> XACML3.ID_FUNCTION_INTEGER_DIVIDE;
            case "double-divide" -> XACML3.ID_FUNCTION_DOUBLE_DIVIDE;
            case "integer-mod" -> XACML3.ID_FUNCTION_INTEGER_MOD;
            case "integer-abs" -> XACML3.ID_FUNCTION_INTEGER_ABS;
            case "double-abs" -> XACML3.ID_FUNCTION_DOUBLE_ABS;
            case "integer-to-double" -> XACML3.ID_FUNCTION_INTEGER_TO_DOUBLE;
            case "yearmonthduration-equal" -> XACML3.ID_FUNCTION_YEARMONTHDURATION_EQUAL;
            case "anyuri-equal" -> XACML3.ID_FUNCTION_ANYURI_EQUAL;
            case "hexbinary-equal" -> XACML3.ID_FUNCTION_HEXBINARY_EQUAL;
            case "rfc822name-equal" -> XACML3.ID_FUNCTION_RFC822NAME_EQUAL;
            case "x500name-equal" -> XACML3.ID_FUNCTION_X500NAME_EQUAL;
            case "string-from-ipaddress" -> XACML3.ID_FUNCTION_STRING_FROM_IPADDRESS;
            case "string-from-dnsname" -> XACML3.ID_FUNCTION_STRING_FROM_DNSNAME;

            case "boolean-equal" -> XACML3.ID_FUNCTION_BOOLEAN_EQUAL;
            case "double-equal" -> XACML3.ID_FUNCTION_DOUBLE_EQUAL;
            case "date-equal" -> XACML3.ID_FUNCTION_DATE_EQUAL;
            case "time-equal" -> XACML3.ID_FUNCTION_TIME_EQUAL;
            case "datetime-equal" -> XACML3.ID_FUNCTION_DATETIME_EQUAL;
            case "daytimeduration-equal" -> XACML3.ID_FUNCTION_DAYTIMEDURATION_EQUAL;
            case "base64binary-equal" -> XACML3.ID_FUNCTION_BASE64BINARY_EQUAL;
            case "round" -> XACML3.ID_FUNCTION_ROUND;
            case "floor" -> XACML3.ID_FUNCTION_FLOOR;
            case "string-normalize-space" -> XACML3.ID_FUNCTION_STRING_NORMALIZE_SPACE;
            case "string-normalize-to-lower-case" -> XACML3.ID_FUNCTION_STRING_NORMALIZE_TO_LOWER_CASE;
            case "double-to-integer" -> XACML3.ID_FUNCTION_DOUBLE_TO_INTEGER;
            case "present" -> XACML3.ID_FUNCTION_PRESENT;
            case "time-in-range" -> XACML3.ID_FUNCTION_TIME_IN_RANGE;
            case "string-bag-size" -> XACML3.ID_FUNCTION_STRING_BAG_SIZE;
            case "boolean-bag-size" -> XACML3.ID_FUNCTION_BOOLEAN_BAG_SIZE;
            case "integer-bag-size" -> XACML3.ID_FUNCTION_INTEGER_BAG_SIZE;
            case "double-bag-size" -> XACML3.ID_FUNCTION_DOUBLE_BAG_SIZE;
            case "time-bag-size" -> XACML3.ID_FUNCTION_TIME_BAG_SIZE;
            case "time-is-in" -> XACML3.ID_FUNCTION_TIME_IS_IN;
            case "time-bag" -> XACML3.ID_FUNCTION_TIME_BAG;
            case "date-bag-size" -> XACML3.ID_FUNCTION_DATE_BAG_SIZE;
            case "date-is-in" -> XACML3.ID_FUNCTION_DATE_IS_IN;
            case "date-bag" -> XACML3.ID_FUNCTION_DATE_BAG;
            case "datetime-bag-size" -> XACML3.ID_FUNCTION_DATETIME_BAG_SIZE;
            case "datetime-is-in" -> XACML3.ID_FUNCTION_DATETIME_IS_IN;
            case "datetime-bag" -> XACML3.ID_FUNCTION_DATETIME_BAG;
            case "anyuri-bag-size" -> XACML3.ID_FUNCTION_ANYURI_BAG_SIZE;
            case "anyuri-is-in" -> XACML3.ID_FUNCTION_ANYURI_IS_IN;
            case "anyuri-bag" -> XACML3.ID_FUNCTION_ANYURI_BAG;
            case "hexbinary-bag-size" -> XACML3.ID_FUNCTION_HEXBINARY_BAG_SIZE;
            case "hexbinary-is-in" -> XACML3.ID_FUNCTION_HEXBINARY_IS_IN;
            case "hexbinary-bag" -> XACML3.ID_FUNCTION_HEXBINARY_BAG;
            case "base64binary-bag-size" -> XACML3.ID_FUNCTION_BASE64BINARY_BAG_SIZE;
            case "base64binary-is-in" -> XACML3.ID_FUNCTION_BASE64BINARY_IS_IN;
            case "base64binary-bag" -> XACML3.ID_FUNCTION_BASE64BINARY_BAG;
            case "daytimeduration-bag-size" -> XACML3.ID_FUNCTION_DAYTIMEDURATION_BAG_SIZE;
            case "daytimeduration-is-in" -> XACML3.ID_FUNCTION_DAYTIMEDURATION_IS_IN;
            case "daytimeduration-bag" -> XACML3.ID_FUNCTION_DAYTIMEDURATION_BAG;
            case "yearmonthduration-bag-size" -> XACML3.ID_FUNCTION_YEARMONTHDURATION_BAG_SIZE;
            case "yearmonthduration-is-in" -> XACML3.ID_FUNCTION_YEARMONTHDURATION_IS_IN;
            case "yearmonthduration-bag" -> XACML3.ID_FUNCTION_YEARMONTHDURATION_BAG;
            case "x500name-one-and-only" -> XACML3.ID_FUNCTION_X500NAME_ONE_AND_ONLY;
            case "x500name-bag-size" -> XACML3.ID_FUNCTION_X500NAME_BAG_SIZE;
            case "x500name-is-in" -> XACML3.ID_FUNCTION_X500NAME_IS_IN;
            case "x500name-bag" -> XACML3.ID_FUNCTION_X500NAME_BAG;
            case "rfc822name-one-and-only" -> XACML3.ID_FUNCTION_RFC822NAME_ONE_AND_ONLY;
            case "rfc822name-bag-size" -> XACML3.ID_FUNCTION_RFC822NAME_BAG_SIZE;
            case "rfc822name-is-in" -> XACML3.ID_FUNCTION_RFC822NAME_IS_IN;
            case "rfc822name-bag" -> XACML3.ID_FUNCTION_RFC822NAME_BAG;
            case "ipaddress-one-and-only" -> XACML3.ID_FUNCTION_IPADDRESS_ONE_AND_ONLY;
            case "ipaddress-bag-size" -> XACML3.ID_FUNCTION_IPADDRESS_BAG_SIZE;
            case "ipaddress-is-in" -> XACML3.ID_FUNCTION_IPADDRESS_IS_IN;
            case "ipaddress-bag" -> XACML3.ID_FUNCTION_IPADDRESS_BAG;
            case "dnsname-one-and-only" -> XACML3.ID_FUNCTION_DNSNAME_ONE_AND_ONLY;
            case "dnsname-bag-size" -> XACML3.ID_FUNCTION_DNSNAME_BAG_SIZE;
            case "dnsname-is-in" -> XACML3.ID_FUNCTION_DNSNAME_IS_IN;
            case "dnsname-bag" -> XACML3.ID_FUNCTION_DNSNAME_BAG;
            case "string-concatenate" -> XACML3.ID_FUNCTION_STRING_CONCATENATE;
            case "boolean-from-string" -> XACML3.ID_FUNCTION_BOOLEAN_FROM_STRING;
            case "string-from-boolean" -> XACML3.ID_FUNCTION_STRING_FROM_BOOLEAN;
            case "integer-from-string" -> XACML3.ID_FUNCTION_INTEGER_FROM_STRING;
            case "string-from-integer" -> XACML3.ID_FUNCTION_STRING_FROM_INTEGER;
            case "double-from-string" -> XACML3.ID_FUNCTION_DOUBLE_FROM_STRING;
            case "string-from-double" -> XACML3.ID_FUNCTION_STRING_FROM_DOUBLE;
            case "time-from-string" -> XACML3.ID_FUNCTION_TIME_FROM_STRING;
            case "string-from-time" -> XACML3.ID_FUNCTION_STRING_FROM_TIME;
            case "date-from-string" -> XACML3.ID_FUNCTION_DATE_FROM_STRING;
            case "string-from-date" -> XACML3.ID_FUNCTION_STRING_FROM_DATE;
            case "datetime-from-string" -> XACML3.ID_FUNCTION_DATETIME_FROM_STRING;
            case "string-from-datetime" -> XACML3.ID_FUNCTION_STRING_FROM_DATETIME;
            case "anyuri-from-string" -> XACML3.ID_FUNCTION_ANYURI_FROM_STRING;
            case "string-from-anyuri" -> XACML3.ID_FUNCTION_STRING_FROM_ANYURI;
            case "daytimeduration-from-string" -> XACML3.ID_FUNCTION_DAYTIMEDURATION_FROM_STRING;
            case "string-from-daytimeturation" -> XACML3.ID_FUNCTION_STRING_FROM_DAYTIMEDURATION;
            case "yearmonthduration-from-string" -> XACML3.ID_FUNCTION_YEARMONTHDURATION_FROM_STRING;
            case "string-from-yearmonthduration" -> XACML3.ID_FUNCTION_STRING_FROM_YEARMONTHDURATION;
            case "x500name-from-string" -> XACML3.ID_FUNCTION_X500NAME_FROM_STRING;
            case "string-from-x500name" -> XACML3.ID_FUNCTION_STRING_FROM_X500NAME;
            case "rfc822name-from-string" -> XACML3.ID_FUNCTION_RFC822NAME_FROM_STRING;
            case "string-from-rfc822name" -> XACML3.ID_FUNCTION_STRING_FROM_RFC822NAME;
            case "ipaddress-from-string" -> XACML3.ID_FUNCTION_IPADDRESS_FROM_STRING;
            case "dnsname-from-string" -> XACML3.ID_FUNCTION_DNSNAME_FROM_STRING;
            case "anyuri-starts-with" -> XACML3.ID_FUNCTION_ANYURI_STARTS_WITH;
            case "anyuri-ends-with" -> XACML3.ID_FUNCTION_ANYURI_ENDS_WITH;
            case "anyuri-contains" -> XACML3.ID_FUNCTION_ANYURI_CONTAINS;
            case "string-substring" -> XACML3.ID_FUNCTION_STRING_SUBSTRING;
            case "anyuri-substring" -> XACML3.ID_FUNCTION_ANYURI_SUBSTRING;
            case "map" -> XACML3.ID_FUNCTION_MAP;
            case "x500name-match" -> XACML3.ID_FUNCTION_X500NAME_MATCH;
            case "rfc822name-match" -> XACML3.ID_FUNCTION_RFC822NAME_MATCH;
            case "anyuri-regexp-match" -> XACML3.ID_FUNCTION_ANYURI_REGEXP_MATCH;
            case "ipaddress-regexp-match" -> XACML3.ID_FUNCTION_IPADDRESS_REGEXP_MATCH;
            case "dnsname-regexp-match" -> XACML3.ID_FUNCTION_DNSNAME_REGEXP_MATCH;
            case "rfc822name-regexp-match" -> XACML3.ID_FUNCTION_RFC822NAME_REGEXP_MATCH;
            case "x500name-regexp-match" -> XACML3.ID_FUNCTION_X500NAME_REGEXP_MATCH;
            case "xpath-node-count" -> XACML3.ID_FUNCTION_XPATH_NODE_COUNT;
            case "xpath-node-equal" -> XACML3.ID_FUNCTION_XPATH_NODE_EQUAL;
            case "xpath-node-match" -> XACML3.ID_FUNCTION_XPATH_NODE_MATCH;
            case "string-intersection" -> XACML3.ID_FUNCTION_STRING_INTERSECTION;
            case "string-at-least-one-member-of" -> XACML3.ID_FUNCTION_STRING_AT_LEAST_ONE_MEMBER_OF;
            case "string-union" -> XACML3.ID_FUNCTION_STRING_UNION;
            case "string-subset" -> XACML3.ID_FUNCTION_STRING_SUBSET;
            case "string-set-equals" -> XACML3.ID_FUNCTION_STRING_SET_EQUALS;
            case "boolean-intersection" -> XACML3.ID_FUNCTION_BOOLEAN_INTERSECTION;
            case "boolean-at-least-one-member-of" -> XACML3.ID_FUNCTION_BOOLEAN_AT_LEAST_ONE_MEMBER_OF;
            case "boolean-union" -> XACML3.ID_FUNCTION_BOOLEAN_UNION;
            case "boolean-subset" -> XACML3.ID_FUNCTION_BOOLEAN_SUBSET;
            case "boolean-set-equals" -> XACML3.ID_FUNCTION_BOOLEAN_SET_EQUALS;
            case "integer-intersection" -> XACML3.ID_FUNCTION_INTEGER_INTERSECTION;
            case "integer-at-least-one-member-of" -> XACML3.ID_FUNCTION_INTEGER_AT_LEAST_ONE_MEMBER_OF;
            case "integer-union" -> XACML3.ID_FUNCTION_INTEGER_UNION;
            case "integer-subset" -> XACML3.ID_FUNCTION_INTEGER_SUBSET;
            case "integer-set-equals" -> XACML3.ID_FUNCTION_INTEGER_SET_EQUALS;
            case "double-intersection" -> XACML3.ID_FUNCTION_DOUBLE_INTERSECTION;
            case "double-at-least-one-member-of" -> XACML3.ID_FUNCTION_DOUBLE_AT_LEAST_ONE_MEMBER_OF;
            case "double-union" -> XACML3.ID_FUNCTION_DOUBLE_UNION;
            case "double-subset" -> XACML3.ID_FUNCTION_DOUBLE_SUBSET;
            case "double-set-equals" -> XACML3.ID_FUNCTION_DOUBLE_SET_EQUALS;
            case "time-intersection" -> XACML3.ID_FUNCTION_TIME_INTERSECTION;
            case "time-at-least-one-member-of" -> XACML3.ID_FUNCTION_TIME_AT_LEAST_ONE_MEMBER_OF;
            case "time-union" -> XACML3.ID_FUNCTION_TIME_UNION;
            case "time-subset" -> XACML3.ID_FUNCTION_TIME_SUBSET;
            case "time-set-equals" -> XACML3.ID_FUNCTION_TIME_SET_EQUALS;
            case "date-intersection" -> XACML3.ID_FUNCTION_DATE_INTERSECTION;
            case "date-at-least-one-member-of" -> XACML3.ID_FUNCTION_DATE_AT_LEAST_ONE_MEMBER_OF;
            case "date-union" -> XACML3.ID_FUNCTION_DATE_UNION;
            case "date-subset" -> XACML3.ID_FUNCTION_DATE_SUBSET;
            case "date-set-equals" -> XACML3.ID_FUNCTION_DATE_SET_EQUALS;
            case "datetime-intersection" -> XACML3.ID_FUNCTION_DATETIME_INTERSECTION;
            case "datetime-at-least-one-member-of" -> XACML3.ID_FUNCTION_DATETIME_AT_LEAST_ONE_MEMBER_OF;
            case "datetime-union" -> XACML3.ID_FUNCTION_DATETIME_UNION;
            case "datetime-subset" -> XACML3.ID_FUNCTION_DATETIME_SUBSET;
            case "datetime-set-equals" -> XACML3.ID_FUNCTION_DATETIME_SET_EQUALS;
            case "anyuri-intersection" -> XACML3.ID_FUNCTION_ANYURI_INTERSECTION;
            case "anyuri-at-least-one-member-of" -> XACML3.ID_FUNCTION_ANYURI_AT_LEAST_ONE_MEMBER_OF;
            case "anyuri-union" -> XACML3.ID_FUNCTION_ANYURI_UNION;
            case "anyuri-subset" -> XACML3.ID_FUNCTION_ANYURI_SUBSET;
            case "anyuri-set-equals" -> XACML3.ID_FUNCTION_ANYURI_SET_EQUALS;
            case "hexbinary-intersection" -> XACML3.ID_FUNCTION_HEXBINARY_INTERSECTION;
            case "hexbinary-at-least-one-member-of" -> XACML3.ID_FUNCTION_HEXBINARY_AT_LEAST_ONE_MEMBER_OF;
            case "hexbinary-union" -> XACML3.ID_FUNCTION_HEXBINARY_UNION;
            case "hexbinary-subset" -> XACML3.ID_FUNCTION_HEXBINARY_SUBSET;
            case "hexbinary-set-equals" -> XACML3.ID_FUNCTION_HEXBINARY_SET_EQUALS;
            case "base64binary-intersection" -> XACML3.ID_FUNCTION_BASE64BINARY_INTERSECTION;
            case "base64binary-at-least-one-member-of" -> XACML3.ID_FUNCTION_BASE64BINARY_AT_LEAST_ONE_MEMBER_OF;
            case "base64binary-union" -> XACML3.ID_FUNCTION_BASE64BINARY_UNION;
            case "base64binary-subset" -> XACML3.ID_FUNCTION_BASE64BINARY_SUBSET;
            case "base64binary-set-equals" -> XACML3.ID_FUNCTION_BASE64BINARY_SET_EQUALS;
            case "daytimeduration-intersection" -> XACML3.ID_FUNCTION_DAYTIMEDURATION_INTERSECTION;
            case "daytimeduration-at-least-one-member-of" -> XACML3.ID_FUNCTION_DAYTIMEDURATION_AT_LEAST_ONE_MEMBER_OF;
            case "daytimeduration-union" -> XACML3.ID_FUNCTION_DAYTIMEDURATION_UNION;
            case "daytimeduration-subset" -> XACML3.ID_FUNCTION_DAYTIMEDURATION_SUBSET;
            case "daytimeduration-set-equals" -> XACML3.ID_FUNCTION_DAYTIMEDURATION_SET_EQUALS;
            case "yearmonthduration-intersection" -> XACML3.ID_FUNCTION_YEARMONTHDURATION_INTERSECTION;
            case "yearmonthduration-at-least-one-member-of" ->
                    XACML3.ID_FUNCTION_YEARMONTHDURATION_AT_LEAST_ONE_MEMBER_OF;
            case "yearmonthduration-union" -> XACML3.ID_FUNCTION_YEARMONTHDURATION_UNION;
            case "yearmonthduration-subset" -> XACML3.ID_FUNCTION_YEARMONTHDURATION_SUBSET;
            case "yearmonthduration-set-equals" -> XACML3.ID_FUNCTION_YEARMONTHDURATION_SET_EQUALS;
            case "x500name-intersection" -> XACML3.ID_FUNCTION_X500NAME_INTERSECTION;
            case "x500name-at-least-one-member-of" -> XACML3.ID_FUNCTION_X500NAME_AT_LEAST_ONE_MEMBER_OF;
            case "x500name-union" -> XACML3.ID_FUNCTION_X500NAME_UNION;
            case "x500name-subset" -> XACML3.ID_FUNCTION_X500NAME_SUBSET;
            case "x500name-set-equals" -> XACML3.ID_FUNCTION_X500NAME_SET_EQUALS;
            case "rfc822name-intersection" -> XACML3.ID_FUNCTION_RFC822NAME_INTERSECTION;
            case "rfc822name-at-least-one-member-of" -> XACML3.ID_FUNCTION_RFC822NAME_AT_LEAST_ONE_MEMBER_OF;
            case "rfc822name-union" -> XACML3.ID_FUNCTION_RFC822NAME_UNION;
            case "rfc822name-subset" -> XACML3.ID_FUNCTION_RFC822NAME_SUBSET;
            case "rfc822name-set-equals" -> XACML3.ID_FUNCTION_RFC822NAME_SET_EQUALS;
            case "ipaddress-intersection" -> XACML3.ID_FUNCTION_IPADDRESS_INTERSECTION;
            case "ipaddress-at-least-one-member-of" -> XACML3.ID_FUNCTION_IPADDRESS_AT_LEAST_ONE_MEMBER_OF;
            case "ipaddress-union" -> XACML3.ID_FUNCTION_IPADDRESS_UNION;
            case "ipaddress-subset" -> XACML3.ID_FUNCTION_IPADDRESS_SUBSET;
            case "ipaddress-set-equals" -> XACML3.ID_FUNCTION_IPADDRESS_SET_EQUALS;
            case "dnsname-intersection" -> XACML3.ID_FUNCTION_DNSNAME_INTERSECTION;
            case "dnsname-at-least-one-member-of" -> XACML3.ID_FUNCTION_DNSNAME_AT_LEAST_ONE_MEMBER_OF;
            case "dnsname-union" -> XACML3.ID_FUNCTION_DNSNAME_UNION;
            case "dnsname-subset" -> XACML3.ID_FUNCTION_DNSNAME_SUBSET;
            case "dnsname-set-equals" -> XACML3.ID_FUNCTION_DNSNAME_SET_EQUALS;
            case "access-permitted" -> XACML3.ID_FUNCTION_ACCESS_PERMITTED;

            // function condition
            case "or" -> XACML3.ID_FUNCTION_OR;
            case "and" -> XACML3.ID_FUNCTION_AND;
            case "n-of" -> XACML3.ID_FUNCTION_N_OF;
            case "not" -> XACML3.ID_FUNCTION_NOT;
            case "any-of" -> XACML3.ID_FUNCTION_ANY_OF;
            case "all-of" -> XACML3.ID_FUNCTION_ALL_OF;
            case "any-of-any" -> XACML3.ID_FUNCTION_ANY_OF_ANY;
            case "all-of-any" -> XACML3.ID_FUNCTION_ALL_OF_ANY;
            case "any-of-all" -> XACML3.ID_FUNCTION_ANY_OF_ALL;
            case "all-of-all" -> XACML3.ID_FUNCTION_ALL_OF_ALL;

            // function ids
            case "string-one-and-only" -> XACML3.ID_FUNCTION_STRING_ONE_AND_ONLY;
            case "integer-one-and-only" -> XACML3.ID_FUNCTION_INTEGER_ONE_AND_ONLY;
            case "double-one-and-only" -> XACML3.ID_FUNCTION_DOUBLE_ONE_AND_ONLY;
            case "time-one-and-only" -> XACML3.ID_FUNCTION_TIME_ONE_AND_ONLY;
            case "date-one-and-only" -> XACML3.ID_FUNCTION_DATE_ONE_AND_ONLY;
            case "datetime-one-and-only" -> XACML3.ID_FUNCTION_DATETIME_ONE_AND_ONLY;
            case "anyuri-one-and-only" -> XACML3.ID_FUNCTION_ANYURI_ONE_AND_ONLY;
            case "hexbinary-one-and-only" -> XACML3.ID_FUNCTION_HEXBINARY_ONE_AND_ONLY;
            case "base64binary-one-and-only" -> XACML3.ID_FUNCTION_BASE64BINARY_ONE_AND_ONLY;
            case "daytimeduration-one-and-only" -> XACML3.ID_FUNCTION_DAYTIMEDURATION_ONE_AND_ONLY;
            case "yearmonthduration-one-and-only" -> XACML3.ID_FUNCTION_YEARMONTHDURATION_ONE_AND_ONLY;

            //attribute ids
            case "action-id" -> XACML3.ID_ACTION_ACTION_ID;

            // algorithm
            case "first-applicable" -> XACML3.ID_RULE_FIRST_APPLICABLE;
            case "deny-overrides" -> XACML3.ID_RULE_DENY_UNLESS_PERMIT;
            case "permit-overrides" -> XACML3.ID_RULE_PERMIT_UNLESS_DENY;
            case "only-one-applicable" -> XACML3.ID_RULE_ONLY_ONE_APPLICABLE;

            // data types
            case "string" -> XACML3.ID_DATATYPE_STRING;
            case "boolean" -> XACML3.ID_DATATYPE_BOOLEAN;
            case "integer" -> XACML3.ID_DATATYPE_INTEGER;
            case "double" -> XACML3.ID_DATATYPE_DOUBLE;
            case "time" -> XACML3.ID_DATATYPE_TIME;
            case "date" -> XACML3.ID_DATATYPE_DATE;
            case "datetime" -> XACML3.ID_DATATYPE_DATETIME;
            case "daytimeduration" -> XACML3.ID_DATATYPE_DAYTIMEDURATION;
            case "yearmonthduration" -> XACML3.ID_DATATYPE_YEARMONTHDURATION;
            case "anyuri" -> XACML3.ID_DATATYPE_ANYURI;
            case "hexbinary" -> XACML3.ID_DATATYPE_HEXBINARY;
            case "base64binary" -> XACML3.ID_DATATYPE_BASE64BINARY;
            case "rfc822name" -> XACML3.ID_DATATYPE_RFC822NAME;
            case "x500name" -> XACML3.ID_DATATYPE_X500NAME;
            case "ipaddress" -> XACML3.ID_DATATYPE_IPADDRESS;
            case "dnsname" -> XACML3.ID_DATATYPE_DNSNAME;

            case "string-bag" -> XACML3.ID_FUNCTION_STRING_BAG;
            case "boolean-bag" -> XACML3.ID_FUNCTION_BOOLEAN_BAG;
            case "integer-bag" -> XACML3.ID_FUNCTION_INTEGER_BAG;
            case "double-bag" -> XACML3.ID_FUNCTION_DOUBLE_BAG;

            default -> throw new ToscaPolicyConversionException("Unexpected value " + operator);
        };
    }
}