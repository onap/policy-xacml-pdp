/*-
 * ============LICENSE_START=======================================================
 * ONAP
 * ================================================================================
 * Copyright (C) 2020-2021 AT&T Intellectual Property. All rights reserved.
 * Modifications Copyright (C) 2020 Nordix Foundation.
 * Modifications Copyright (C) 2024 Deutsche Telekom AG.
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
import java.util.HashMap;
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

    private static final String DESCRIPTION = "description";

    private static final String TARGET = "target";

    private static final String VALUE = "value";

    private static final String APPLY = "apply";

    private static final String ONE_AND_ONLY = "-one-and-only";

    private static final String DOUBLE = "double";

    private Map<String, Identifier> identifierMap;

    @Override
    public Object convertPolicy(ToscaPolicy toscaPolicy) throws ToscaPolicyConversionException {
        if (TOSCA_XACML_POLICY_TYPE.equals(toscaPolicy.getType())) {
            setIdentifierMap();
            return setPolicySetType(toscaPolicy);
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

    private PolicySetType setPolicySetType(ToscaPolicy toscaPolicy) throws ToscaPolicyConversionException {
        PolicySetType policySetType = new PolicySetType();
        policySetType.setPolicySetId(String.valueOf(toscaPolicy.getMetadata().get("policy-id")));
        policySetType.setPolicyCombiningAlgId(XACML3.ID_POLICY_FIRST_APPLICABLE.stringValue());
        policySetType.setVersion(String.valueOf(toscaPolicy.getMetadata().get("policy-version")));
        policySetType.setDescription(String.valueOf(toscaPolicy.getMetadata().get(DESCRIPTION)));
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
        Map<String, Object> properties = toscaPolicy.getProperties();
        setPolicyType(toscaPolicy, policyType);
        try {
            List<Map<String, Object>> rules = (List<Map<String, Object>>) properties.get("rules");
            for (Map<String, Object> rule : rules) {
                var ruleType = new RuleType();
                if (rule.get(DESCRIPTION) != null) {
                    ruleType.setDescription((String) rule.get(DESCRIPTION));
                }
                ruleType.setRuleId(UUID.randomUUID().toString());
                if (rule.get(TARGET) != null) {
                    ruleType.setTarget(setTargetType((Map<String, Object>) rule.get(TARGET)));
                }
                if (rule.get("condition") != null) {
                    ruleType.setCondition(setConditionType((Map<String, Object>) rule.get("condition")));
                }
                if (rule.get("decision") == null) {
                    throw new ToscaPolicyConversionException("decision is mandatory in a rule");
                }
                setAdviceExpression(ruleType, rule);
                policyType.getCombinerParametersOrRuleCombinerParametersOrVariableDefinition().add(ruleType);
            }
        } catch (ToscaPolicyConversionException ex) {
            ex.printStackTrace();
            throw new ToscaPolicyConversionException("Invalid rule format");
        }
        if (properties.get("default") != null) {
            setDefaultTule((String) properties.get("default"), policyType);
        }
        return policyType;
    }

    private void setPolicyType(ToscaPolicy toscaPolicy, PolicyType policyType) throws ToscaPolicyConversionException {
        policyType.setPolicyId(String.valueOf(toscaPolicy.getMetadata().get("policy-id")));
        policyType.setVersion(String.valueOf(toscaPolicy.getMetadata().get("policy-version")));
        policyType.setDescription(String.valueOf(toscaPolicy.getMetadata().get(DESCRIPTION)));
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
        if (properties.get(TARGET) != null) {
            policyType.setTarget(setTargetType((Map<String, Object>) properties.get(TARGET)));
        } else {
            policyType.setTarget(new TargetType());
        }

    }

    private void setAdviceExpression(RuleType ruleType, Map<String, Object> rule)
            throws ToscaPolicyConversionException {
        String decision = (String) rule.get("decision");
        if ("Deny".equalsIgnoreCase(decision)) {
            ruleType.setEffect(EffectType.DENY);

        } else {
            ruleType.setEffect(EffectType.PERMIT);
        }
        if (rule.get("advice") != null) {
            ruleType.setAdviceExpressions(setAdvice((Map<String, Object>) rule.get("advice"), decision));
        }
    }

    private void setDefaultTule(String defaultDecision, PolicyType policyType) {
        var defaultRule = new RuleType();
        defaultRule.setDescription("Default Rule if none of the rules evaluate to True");
        defaultRule.setRuleId(UUID.randomUUID().toString());
        if ("Deny".equalsIgnoreCase(defaultDecision)) {
            defaultRule.setEffect(EffectType.DENY);
        } else {
            defaultRule.setEffect(EffectType.PERMIT);
        }
        policyType.getCombinerParametersOrRuleCombinerParametersOrVariableDefinition().add(defaultRule);
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
                    var valueType = setAttributeValueType(match.get(VALUE),
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
        anyOfType.getAllOf().add(ToscaPolicyTranslatorUtils.buildAllOf(matchType));
        var target = new TargetType();
        target.getAnyOf().add(anyOfType);
        return target;
    }

    private ConditionType setConditionType(Map<String, Object> conditionMap) throws ToscaPolicyConversionException {
        var condition = new ConditionType();
        try {
            Map<String, Object> applyMap = (Map<String, Object>) conditionMap.get(APPLY);
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
            setApplyKeys(keyList, keys, datatype, factory, apply);
            setAttributeAndDesignator(keyList, apply, factory);
            boolean data = switch (operator) {
                case "or", "and", "n-of", "not", "all-of", "any-of", "any-of-any", "all-of-any", "all-of-all",
                     "any-of-all" -> false;
                default -> true;
            };
            if (data && applies.get("compareWith") != null) {
                setCompareWith(applies, apply, factory, getDatatype(operator));
            }
        } catch (NullPointerException ex) {
            throw new ToscaPolicyConversionException("Invalid apply format");
        }
        return apply;
    }

    private void setApplyKeys(List<Object> keyList, List<Object> keys, String datatype,
                              ObjectFactory factory, ApplyType apply) throws ToscaPolicyConversionException {
        for (Object keyObject : keys) {
            if (keyObject instanceof Map<?, ?>) {
                if (((Map<?, ?>) keyObject).get("list") != null) {
                    setBagApply(apply, (List<Object>) ((Map<?, ?>) keyObject).get("list"), datatype, factory);
                } else if (((Map<?, ?>) keyObject).get("function") != null) {
                    setFunctionType(apply, ((Map<String, String>) keyObject).get("function"), factory);
                } else if (((Map<?, ?>) keyObject).get(APPLY) != null) {
                    keyList.add(setApply((Map<String, Object>) ((Map<?, ?>) keyObject).get(APPLY)));
                } else {
                    throw new ToscaPolicyConversionException(
                            "Invalid key entry, object does not contain list, function or apply");
                }
            } else {
                setAttributes(keyObject, keyList, datatype, factory);
            }
        }
    }

    private void setAttributeAndDesignator(List<Object> keyList, ApplyType apply, ObjectFactory factory) {
        keyList.stream()
                .sorted((firstKey, secondKey) -> {
                    if (firstKey instanceof AttributeValueType) {
                        return -1;
                    } else if (firstKey instanceof ApplyType) {
                        return 1;
                    }
                    return 0;
                })
                .forEach(key -> {
                    if (key instanceof AttributeValueType) {
                        apply.getExpression().add(factory.createAttributeValue((AttributeValueType) key));
                    }
                    if (key instanceof ApplyType) {
                        apply.getExpression().add(factory.createApply((ApplyType) key));
                    }
                });
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
                    keyApply.setFunctionId(validateFilterPropertyFunction(datatype + ONE_AND_ONLY).stringValue());
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
                            validateFilterPropertyFunction(datatype + ONE_AND_ONLY).stringValue());
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
            if (compareWith.get(APPLY) != null) {
                ApplyType compareApply = setApply((Map<String, Object>) compareWith.get(APPLY));
                apply.getExpression().add(factory.createApply(compareApply));
            } else if (compareWith.get(VALUE) != null) {
                var attributeValue = setAttributeValueType(compareWith.get(VALUE),
                        validateFilterPropertyFunction(datatype).stringValue());
                apply.getExpression().add(factory.createAttributeValue(attributeValue));
            } else if (compareWith.get("key") != null) {
                var keyDesignator = setAttributeDesignatorType((String) compareWith.get("key"),
                        XACML3.ID_ATTRIBUTE_CATEGORY_RESOURCE.stringValue(),
                        validateFilterPropertyFunction(datatype).stringValue(), false);
                var keyApply = new ApplyType();
                keyApply.setFunctionId(validateFilterPropertyFunction(datatype + ONE_AND_ONLY).stringValue());
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
            var value = setAttributeValueType(advice.get(VALUE), XACML3.ID_DATATYPE_STRING.stringValue());
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
                return DOUBLE;
            }
            List<String> datatypes = Arrays.asList("string", "boolean", "integer", DOUBLE, "time", "date", "dateTime",
                    "dayTimeDuration", "yearMonthDuration", "anyURI", "hexBinary", "rfc822Name", "base64Binary",
                    "x500Name", "ipAddress", "dnsName");
            if (datatypes.stream().anyMatch(operator::contains)) {
                return operator.split("-")[0];
            }
        } catch (NullPointerException ex) {
            throw new ToscaPolicyConversionException("Invalid operator");
        }
        return operator;
    }

    private void setIdentifierMap() {
        identifierMap = new HashMap<>();
        identifierMap.put("string-equal", XACML3.ID_FUNCTION_STRING_EQUAL);
        identifierMap.put("integer-equal", XACML3.ID_FUNCTION_INTEGER_EQUAL);
        identifierMap.put("string-equal-ignore-case", XACML3.ID_FUNCTION_STRING_EQUAL_IGNORE_CASE);
        identifierMap.put("string-regexp-match", XACML3.ID_FUNCTION_STRING_REGEXP_MATCH);
        identifierMap.put("string-contains", XACML3.ID_FUNCTION_STRING_CONTAINS);
        identifierMap.put("string-greater-than", XACML3.ID_FUNCTION_STRING_GREATER_THAN);
        identifierMap.put("string-greater-than-or-equal", XACML3.ID_FUNCTION_STRING_GREATER_THAN_OR_EQUAL);
        identifierMap.put("string-less-than", XACML3.ID_FUNCTION_STRING_LESS_THAN);
        identifierMap.put("string-less-than-or-equal", XACML3.ID_FUNCTION_STRING_LESS_THAN_OR_EQUAL);
        identifierMap.put("string-starts-with", XACML3.ID_FUNCTION_STRING_STARTS_WITH);
        identifierMap.put("string-ends-with", XACML3.ID_FUNCTION_STRING_ENDS_WITH);
        identifierMap.put("integer-greater-than", XACML3.ID_FUNCTION_INTEGER_GREATER_THAN);
        identifierMap.put("integer-greater-than-or-equal", XACML3.ID_FUNCTION_INTEGER_GREATER_THAN_OR_EQUAL);
        identifierMap.put("integer-less-than", XACML3.ID_FUNCTION_INTEGER_LESS_THAN);
        identifierMap.put("integer-less-than-or-equal", XACML3.ID_FUNCTION_INTEGER_LESS_THAN_OR_EQUAL);
        identifierMap.put("double-greater-than", XACML3.ID_FUNCTION_DOUBLE_GREATER_THAN);
        identifierMap.put("double-greater-than-or-equal", XACML3.ID_FUNCTION_DOUBLE_GREATER_THAN_OR_EQUAL);
        identifierMap.put("double-less-than", XACML3.ID_FUNCTION_DOUBLE_LESS_THAN);
        identifierMap.put("double-less-than-or-equal", XACML3.ID_FUNCTION_DOUBLE_LESS_THAN_OR_EQUAL);
        identifierMap.put("datetime-add-daytimeduration", XACML3.ID_FUNCTION_DATETIME_ADD_DAYTIMEDURATION);
        identifierMap.put("datetime-add-yearmonthduration", XACML3.ID_FUNCTION_DATETIME_ADD_YEARMONTHDURATION);
        identifierMap.put("datetime-subtract-daytimeturation", XACML3.ID_FUNCTION_DATETIME_SUBTRACT_DAYTIMEDURATION);
        identifierMap.put("datetime-subtract-yearmonthduration",
                XACML3.ID_FUNCTION_DATETIME_SUBTRACT_YEARMONTHDURATION);
        identifierMap.put("date-add-yearmonthduration", XACML3.ID_FUNCTION_DATE_ADD_YEARMONTHDURATION);
        identifierMap.put("date-subtract-yearmonthduration", XACML3.ID_FUNCTION_DATE_SUBTRACT_YEARMONTHDURATION);
        identifierMap.put("time-greater-than", XACML3.ID_FUNCTION_TIME_GREATER_THAN);
        identifierMap.put("time-greater-than-or-equal", XACML3.ID_FUNCTION_TIME_GREATER_THAN_OR_EQUAL);
        identifierMap.put("time-less-than", XACML3.ID_FUNCTION_TIME_LESS_THAN);
        identifierMap.put("time-less-than-or-equal", XACML3.ID_FUNCTION_TIME_LESS_THAN_OR_EQUAL);
        identifierMap.put("datetime-greater-than", XACML3.ID_FUNCTION_DATETIME_GREATER_THAN);
        identifierMap.put("datetime-greater-than-or-equal", XACML3.ID_FUNCTION_DATETIME_GREATER_THAN_OR_EQUAL);
        identifierMap.put("datetime-less-than", XACML3.ID_FUNCTION_DATETIME_LESS_THAN);
        identifierMap.put("datetime-less-than-or-equal", XACML3.ID_FUNCTION_DATETIME_LESS_THAN_OR_EQUAL);
        identifierMap.put("date-greater-than", XACML3.ID_FUNCTION_DATE_GREATER_THAN);
        identifierMap.put("date-greater-than-or-equal", XACML3.ID_FUNCTION_DATE_GREATER_THAN_OR_EQUAL);
        identifierMap.put("date-less-than", XACML3.ID_FUNCTION_DATE_LESS_THAN);
        identifierMap.put("date-less-than-or-equal", XACML3.ID_FUNCTION_DATE_LESS_THAN_OR_EQUAL);
        identifierMap.put("boolean-one-and-only", XACML3.ID_FUNCTION_BOOLEAN_ONE_AND_ONLY);
        identifierMap.put("string-is-in", XACML3.ID_FUNCTION_STRING_IS_IN);
        identifierMap.put("integer-is-in", XACML3.ID_FUNCTION_INTEGER_IS_IN);
        identifierMap.put("boolean-is-in", XACML3.ID_FUNCTION_BOOLEAN_IS_IN);
        identifierMap.put("double-is-in", XACML3.ID_FUNCTION_DOUBLE_IS_IN);
        identifierMap.put("integer-add", XACML3.ID_FUNCTION_INTEGER_ADD);
        identifierMap.put("double-add", XACML3.ID_FUNCTION_DOUBLE_ADD);
        identifierMap.put("integer-subtract", XACML3.ID_FUNCTION_INTEGER_SUBTRACT);
        identifierMap.put("double-subtract", XACML3.ID_FUNCTION_DOUBLE_SUBTRACT);
        identifierMap.put("integer-multiply", XACML3.ID_FUNCTION_INTEGER_MULTIPLY);
        identifierMap.put("double-multiply", XACML3.ID_FUNCTION_DOUBLE_MULTIPLY);
        identifierMap.put("integer-divide", XACML3.ID_FUNCTION_INTEGER_DIVIDE);
        identifierMap.put("double-divide", XACML3.ID_FUNCTION_DOUBLE_DIVIDE);
        identifierMap.put("integer-mod", XACML3.ID_FUNCTION_INTEGER_MOD);
        identifierMap.put("integer-abs", XACML3.ID_FUNCTION_INTEGER_ABS);
        identifierMap.put("double-abs", XACML3.ID_FUNCTION_DOUBLE_ABS);
        identifierMap.put("integer-to-double", XACML3.ID_FUNCTION_INTEGER_TO_DOUBLE);
        identifierMap.put("yearmonthduration-equal", XACML3.ID_FUNCTION_YEARMONTHDURATION_EQUAL);
        identifierMap.put("anyuri-equal", XACML3.ID_FUNCTION_ANYURI_EQUAL);
        identifierMap.put("hexbinary-equal", XACML3.ID_FUNCTION_HEXBINARY_EQUAL);
        identifierMap.put("rfc822name-equal", XACML3.ID_FUNCTION_RFC822NAME_EQUAL);
        identifierMap.put("x500name-equal", XACML3.ID_FUNCTION_X500NAME_EQUAL);
        identifierMap.put("string-from-ipaddress", XACML3.ID_FUNCTION_STRING_FROM_IPADDRESS);
        identifierMap.put("string-from-dnsname", XACML3.ID_FUNCTION_STRING_FROM_DNSNAME);

        identifierMap.put("boolean-equal", XACML3.ID_FUNCTION_BOOLEAN_EQUAL);
        identifierMap.put("double-equal", XACML3.ID_FUNCTION_DOUBLE_EQUAL);
        identifierMap.put("date-equal", XACML3.ID_FUNCTION_DATE_EQUAL);
        identifierMap.put("time-equal", XACML3.ID_FUNCTION_TIME_EQUAL);
        identifierMap.put("datetime-equal", XACML3.ID_FUNCTION_DATETIME_EQUAL);
        identifierMap.put("daytimeduration-equal", XACML3.ID_FUNCTION_DAYTIMEDURATION_EQUAL);
        identifierMap.put("base64binary-equal", XACML3.ID_FUNCTION_BASE64BINARY_EQUAL);
        identifierMap.put("round", XACML3.ID_FUNCTION_ROUND);
        identifierMap.put("floor", XACML3.ID_FUNCTION_FLOOR);
        identifierMap.put("string-normalize-space", XACML3.ID_FUNCTION_STRING_NORMALIZE_SPACE);
        identifierMap.put("string-normalize-to-lower-case", XACML3.ID_FUNCTION_STRING_NORMALIZE_TO_LOWER_CASE);
        identifierMap.put("double-to-integer", XACML3.ID_FUNCTION_DOUBLE_TO_INTEGER);
        identifierMap.put("present", XACML3.ID_FUNCTION_PRESENT);
        identifierMap.put("time-in-range", XACML3.ID_FUNCTION_TIME_IN_RANGE);
        identifierMap.put("string-bag-size", XACML3.ID_FUNCTION_STRING_BAG_SIZE);
        identifierMap.put("boolean-bag-size", XACML3.ID_FUNCTION_BOOLEAN_BAG_SIZE);
        identifierMap.put("integer-bag-size", XACML3.ID_FUNCTION_INTEGER_BAG_SIZE);
        identifierMap.put("double-bag-size", XACML3.ID_FUNCTION_DOUBLE_BAG_SIZE);
        identifierMap.put("time-bag-size", XACML3.ID_FUNCTION_TIME_BAG_SIZE);
        identifierMap.put("time-is-in", XACML3.ID_FUNCTION_TIME_IS_IN);
        identifierMap.put("time-bag", XACML3.ID_FUNCTION_TIME_BAG);
        identifierMap.put("date-bag-size", XACML3.ID_FUNCTION_DATE_BAG_SIZE);
        identifierMap.put("date-is-in", XACML3.ID_FUNCTION_DATE_IS_IN);
        identifierMap.put("date-bag", XACML3.ID_FUNCTION_DATE_BAG);
        identifierMap.put("datetime-bag-size", XACML3.ID_FUNCTION_DATETIME_BAG_SIZE);
        identifierMap.put("datetime-is-in", XACML3.ID_FUNCTION_DATETIME_IS_IN);
        identifierMap.put("datetime-bag", XACML3.ID_FUNCTION_DATETIME_BAG);
        identifierMap.put("anyuri-bag-size", XACML3.ID_FUNCTION_ANYURI_BAG_SIZE);
        identifierMap.put("anyuri-is-in", XACML3.ID_FUNCTION_ANYURI_IS_IN);
        identifierMap.put("anyuri-bag", XACML3.ID_FUNCTION_ANYURI_BAG);
        identifierMap.put("hexbinary-bag-size", XACML3.ID_FUNCTION_HEXBINARY_BAG_SIZE);
        identifierMap.put("hexbinary-is-in", XACML3.ID_FUNCTION_HEXBINARY_IS_IN);
        identifierMap.put("hexbinary-bag", XACML3.ID_FUNCTION_HEXBINARY_BAG);
        identifierMap.put("base64binary-bag-size", XACML3.ID_FUNCTION_BASE64BINARY_BAG_SIZE);
        identifierMap.put("base64binary-is-in", XACML3.ID_FUNCTION_BASE64BINARY_IS_IN);
        identifierMap.put("base64binary-bag", XACML3.ID_FUNCTION_BASE64BINARY_BAG);
        identifierMap.put("daytimeduration-bag-size", XACML3.ID_FUNCTION_DAYTIMEDURATION_BAG_SIZE);
        identifierMap.put("daytimeduration-is-in", XACML3.ID_FUNCTION_DAYTIMEDURATION_IS_IN);
        identifierMap.put("daytimeduration-bag", XACML3.ID_FUNCTION_DAYTIMEDURATION_BAG);
        identifierMap.put("yearmonthduration-bag-size", XACML3.ID_FUNCTION_YEARMONTHDURATION_BAG_SIZE);
        identifierMap.put("yearmonthduration-is-in", XACML3.ID_FUNCTION_YEARMONTHDURATION_IS_IN);
        identifierMap.put("yearmonthduration-bag", XACML3.ID_FUNCTION_YEARMONTHDURATION_BAG);
        identifierMap.put("x500name-one-and-only", XACML3.ID_FUNCTION_X500NAME_ONE_AND_ONLY);
        identifierMap.put("x500name-bag-size", XACML3.ID_FUNCTION_X500NAME_BAG_SIZE);
        identifierMap.put("x500name-is-in", XACML3.ID_FUNCTION_X500NAME_IS_IN);
        identifierMap.put("x500name-bag", XACML3.ID_FUNCTION_X500NAME_BAG);
        identifierMap.put("rfc822name-one-and-only", XACML3.ID_FUNCTION_RFC822NAME_ONE_AND_ONLY);
        identifierMap.put("rfc822name-bag-size", XACML3.ID_FUNCTION_RFC822NAME_BAG_SIZE);
        identifierMap.put("rfc822name-is-in", XACML3.ID_FUNCTION_RFC822NAME_IS_IN);
        identifierMap.put("rfc822name-bag", XACML3.ID_FUNCTION_RFC822NAME_BAG);
        identifierMap.put("ipaddress-one-and-only", XACML3.ID_FUNCTION_IPADDRESS_ONE_AND_ONLY);
        identifierMap.put("ipaddress-bag-size", XACML3.ID_FUNCTION_IPADDRESS_BAG_SIZE);
        identifierMap.put("ipaddress-is-in", XACML3.ID_FUNCTION_IPADDRESS_IS_IN);
        identifierMap.put("ipaddress-bag", XACML3.ID_FUNCTION_IPADDRESS_BAG);
        identifierMap.put("dnsname-one-and-only", XACML3.ID_FUNCTION_DNSNAME_ONE_AND_ONLY);
        identifierMap.put("dnsname-bag-size", XACML3.ID_FUNCTION_DNSNAME_BAG_SIZE);
        identifierMap.put("dnsname-is-in", XACML3.ID_FUNCTION_DNSNAME_IS_IN);
        identifierMap.put("dnsname-bag", XACML3.ID_FUNCTION_DNSNAME_BAG);
        identifierMap.put("string-concatenate", XACML3.ID_FUNCTION_STRING_CONCATENATE);
        identifierMap.put("boolean-from-string", XACML3.ID_FUNCTION_BOOLEAN_FROM_STRING);
        identifierMap.put("string-from-boolean", XACML3.ID_FUNCTION_STRING_FROM_BOOLEAN);
        identifierMap.put("integer-from-string", XACML3.ID_FUNCTION_INTEGER_FROM_STRING);
        identifierMap.put("string-from-integer", XACML3.ID_FUNCTION_STRING_FROM_INTEGER);
        identifierMap.put("double-from-string", XACML3.ID_FUNCTION_DOUBLE_FROM_STRING);
        identifierMap.put("string-from-double", XACML3.ID_FUNCTION_STRING_FROM_DOUBLE);
        identifierMap.put("time-from-string", XACML3.ID_FUNCTION_TIME_FROM_STRING);
        identifierMap.put("string-from-time", XACML3.ID_FUNCTION_STRING_FROM_TIME);
        identifierMap.put("date-from-string", XACML3.ID_FUNCTION_DATE_FROM_STRING);
        identifierMap.put("string-from-date", XACML3.ID_FUNCTION_STRING_FROM_DATE);
        identifierMap.put("datetime-from-string", XACML3.ID_FUNCTION_DATETIME_FROM_STRING);
        identifierMap.put("string-from-datetime", XACML3.ID_FUNCTION_STRING_FROM_DATETIME);
        identifierMap.put("anyuri-from-string", XACML3.ID_FUNCTION_ANYURI_FROM_STRING);
        identifierMap.put("string-from-anyuri", XACML3.ID_FUNCTION_STRING_FROM_ANYURI);
        identifierMap.put("daytimeduration-from-string", XACML3.ID_FUNCTION_DAYTIMEDURATION_FROM_STRING);
        identifierMap.put("string-from-daytimeturation", XACML3.ID_FUNCTION_STRING_FROM_DAYTIMEDURATION);
        identifierMap.put("yearmonthduration-from-string", XACML3.ID_FUNCTION_YEARMONTHDURATION_FROM_STRING);
        identifierMap.put("string-from-yearmonthduration", XACML3.ID_FUNCTION_STRING_FROM_YEARMONTHDURATION);
        identifierMap.put("x500name-from-string", XACML3.ID_FUNCTION_X500NAME_FROM_STRING);
        identifierMap.put("string-from-x500name", XACML3.ID_FUNCTION_STRING_FROM_X500NAME);
        identifierMap.put("rfc822name-from-string", XACML3.ID_FUNCTION_RFC822NAME_FROM_STRING);
        identifierMap.put("string-from-rfc822name", XACML3.ID_FUNCTION_STRING_FROM_RFC822NAME);
        identifierMap.put("ipaddress-from-string", XACML3.ID_FUNCTION_IPADDRESS_FROM_STRING);
        identifierMap.put("dnsname-from-string", XACML3.ID_FUNCTION_DNSNAME_FROM_STRING);
        identifierMap.put("anyuri-starts-with", XACML3.ID_FUNCTION_ANYURI_STARTS_WITH);
        identifierMap.put("anyuri-ends-with", XACML3.ID_FUNCTION_ANYURI_ENDS_WITH);
        identifierMap.put("anyuri-contains", XACML3.ID_FUNCTION_ANYURI_CONTAINS);
        identifierMap.put("string-substring", XACML3.ID_FUNCTION_STRING_SUBSTRING);
        identifierMap.put("anyuri-substring", XACML3.ID_FUNCTION_ANYURI_SUBSTRING);
        identifierMap.put("map", XACML3.ID_FUNCTION_MAP);
        identifierMap.put("x500name-match", XACML3.ID_FUNCTION_X500NAME_MATCH);
        identifierMap.put("rfc822name-match", XACML3.ID_FUNCTION_RFC822NAME_MATCH);
        identifierMap.put("anyuri-regexp-match", XACML3.ID_FUNCTION_ANYURI_REGEXP_MATCH);
        identifierMap.put("ipaddress-regexp-match", XACML3.ID_FUNCTION_IPADDRESS_REGEXP_MATCH);
        identifierMap.put("dnsname-regexp-match", XACML3.ID_FUNCTION_DNSNAME_REGEXP_MATCH);
        identifierMap.put("rfc822name-regexp-match", XACML3.ID_FUNCTION_RFC822NAME_REGEXP_MATCH);
        identifierMap.put("x500name-regexp-match", XACML3.ID_FUNCTION_X500NAME_REGEXP_MATCH);
        identifierMap.put("xpath-node-count", XACML3.ID_FUNCTION_XPATH_NODE_COUNT);
        identifierMap.put("xpath-node-equal", XACML3.ID_FUNCTION_XPATH_NODE_EQUAL);
        identifierMap.put("xpath-node-match", XACML3.ID_FUNCTION_XPATH_NODE_MATCH);
        identifierMap.put("string-intersection", XACML3.ID_FUNCTION_STRING_INTERSECTION);
        identifierMap.put("string-at-least-one-member-of", XACML3.ID_FUNCTION_STRING_AT_LEAST_ONE_MEMBER_OF);
        identifierMap.put("string-union", XACML3.ID_FUNCTION_STRING_UNION);
        identifierMap.put("string-subset", XACML3.ID_FUNCTION_STRING_SUBSET);
        identifierMap.put("string-set-equals", XACML3.ID_FUNCTION_STRING_SET_EQUALS);
        identifierMap.put("boolean-intersection", XACML3.ID_FUNCTION_BOOLEAN_INTERSECTION);
        identifierMap.put("boolean-at-least-one-member-of", XACML3.ID_FUNCTION_BOOLEAN_AT_LEAST_ONE_MEMBER_OF);
        identifierMap.put("boolean-union", XACML3.ID_FUNCTION_BOOLEAN_UNION);
        identifierMap.put("boolean-subset", XACML3.ID_FUNCTION_BOOLEAN_SUBSET);
        identifierMap.put("boolean-set-equals", XACML3.ID_FUNCTION_BOOLEAN_SET_EQUALS);
        identifierMap.put("integer-intersection", XACML3.ID_FUNCTION_INTEGER_INTERSECTION);
        identifierMap.put("integer-at-least-one-member-of", XACML3.ID_FUNCTION_INTEGER_AT_LEAST_ONE_MEMBER_OF);
        identifierMap.put("integer-union", XACML3.ID_FUNCTION_INTEGER_UNION);
        identifierMap.put("integer-subset", XACML3.ID_FUNCTION_INTEGER_SUBSET);
        identifierMap.put("integer-set-equals", XACML3.ID_FUNCTION_INTEGER_SET_EQUALS);
        identifierMap.put("double-intersection", XACML3.ID_FUNCTION_DOUBLE_INTERSECTION);
        identifierMap.put("double-at-least-one-member-of", XACML3.ID_FUNCTION_DOUBLE_AT_LEAST_ONE_MEMBER_OF);
        identifierMap.put("double-union", XACML3.ID_FUNCTION_DOUBLE_UNION);
        identifierMap.put("double-subset", XACML3.ID_FUNCTION_DOUBLE_SUBSET);
        identifierMap.put("double-set-equals", XACML3.ID_FUNCTION_DOUBLE_SET_EQUALS);
        identifierMap.put("time-intersection", XACML3.ID_FUNCTION_TIME_INTERSECTION);
        identifierMap.put("time-at-least-one-member-of", XACML3.ID_FUNCTION_TIME_AT_LEAST_ONE_MEMBER_OF);
        identifierMap.put("time-union", XACML3.ID_FUNCTION_TIME_UNION);
        identifierMap.put("time-subset", XACML3.ID_FUNCTION_TIME_SUBSET);
        identifierMap.put("time-set-equals", XACML3.ID_FUNCTION_TIME_SET_EQUALS);
        identifierMap.put("date-intersection", XACML3.ID_FUNCTION_DATE_INTERSECTION);
        identifierMap.put("date-at-least-one-member-of", XACML3.ID_FUNCTION_DATE_AT_LEAST_ONE_MEMBER_OF);
        identifierMap.put("date-union", XACML3.ID_FUNCTION_DATE_UNION);
        identifierMap.put("date-subset", XACML3.ID_FUNCTION_DATE_SUBSET);
        identifierMap.put("date-set-equals", XACML3.ID_FUNCTION_DATE_SET_EQUALS);
        identifierMap.put("datetime-intersection", XACML3.ID_FUNCTION_DATETIME_INTERSECTION);
        identifierMap.put("datetime-at-least-one-member-of", XACML3.ID_FUNCTION_DATETIME_AT_LEAST_ONE_MEMBER_OF);
        identifierMap.put("datetime-union", XACML3.ID_FUNCTION_DATETIME_UNION);
        identifierMap.put("datetime-subset", XACML3.ID_FUNCTION_DATETIME_SUBSET);
        identifierMap.put("datetime-set-equals", XACML3.ID_FUNCTION_DATETIME_SET_EQUALS);

        identifierMap.put("anyuri-intersection", XACML3.ID_FUNCTION_ANYURI_INTERSECTION);
        identifierMap.put("anyuri-at-least-one-member-of", XACML3.ID_FUNCTION_ANYURI_AT_LEAST_ONE_MEMBER_OF);
        identifierMap.put("anyuri-union", XACML3.ID_FUNCTION_ANYURI_UNION);
        identifierMap.put("anyuri-subset", XACML3.ID_FUNCTION_ANYURI_SUBSET);
        identifierMap.put("anyuri-set-equals", XACML3.ID_FUNCTION_ANYURI_SET_EQUALS);
        identifierMap.put("hexbinary-intersection", XACML3.ID_FUNCTION_HEXBINARY_INTERSECTION);
        identifierMap.put("hexbinary-at-least-one-member-of", XACML3.ID_FUNCTION_HEXBINARY_AT_LEAST_ONE_MEMBER_OF);
        identifierMap.put("hexbinary-union", XACML3.ID_FUNCTION_HEXBINARY_UNION);
        identifierMap.put("hexbinary-subset", XACML3.ID_FUNCTION_HEXBINARY_SUBSET);
        identifierMap.put("hexbinary-set-equals", XACML3.ID_FUNCTION_HEXBINARY_SET_EQUALS);
        identifierMap.put("base64binary-intersection", XACML3.ID_FUNCTION_BASE64BINARY_INTERSECTION);
        identifierMap.put("base64binary-at-least-one-member-of",
                XACML3.ID_FUNCTION_BASE64BINARY_AT_LEAST_ONE_MEMBER_OF);
        identifierMap.put("base64binary-union", XACML3.ID_FUNCTION_BASE64BINARY_UNION);
        identifierMap.put("base64binary-subset", XACML3.ID_FUNCTION_BASE64BINARY_SUBSET);
        identifierMap.put("base64binary-set-equals", XACML3.ID_FUNCTION_BASE64BINARY_SET_EQUALS);
        identifierMap.put("daytimeduration-intersection", XACML3.ID_FUNCTION_DAYTIMEDURATION_INTERSECTION);
        identifierMap.put("daytimeduration-at-least-one-member-of",
                XACML3.ID_FUNCTION_DAYTIMEDURATION_AT_LEAST_ONE_MEMBER_OF);
        identifierMap.put("daytimeduration-union", XACML3.ID_FUNCTION_DAYTIMEDURATION_UNION);
        identifierMap.put("daytimeduration-subset", XACML3.ID_FUNCTION_DAYTIMEDURATION_SUBSET);
        identifierMap.put("daytimeduration-set-equals", XACML3.ID_FUNCTION_DAYTIMEDURATION_SET_EQUALS);
        identifierMap.put("yearmonthduration-intersection", XACML3.ID_FUNCTION_YEARMONTHDURATION_INTERSECTION);
        identifierMap.put("yearmonthduration-at-least-one-member-of",
                XACML3.ID_FUNCTION_YEARMONTHDURATION_AT_LEAST_ONE_MEMBER_OF);
        identifierMap.put("yearmonthduration-union", XACML3.ID_FUNCTION_YEARMONTHDURATION_UNION);
        identifierMap.put("yearmonthduration-subset", XACML3.ID_FUNCTION_YEARMONTHDURATION_SUBSET);
        identifierMap.put("yearmonthduration-set-equals", XACML3.ID_FUNCTION_YEARMONTHDURATION_SET_EQUALS);
        identifierMap.put("x500name-intersection", XACML3.ID_FUNCTION_X500NAME_INTERSECTION);
        identifierMap.put("x500name-at-least-one-member-of", XACML3.ID_FUNCTION_X500NAME_AT_LEAST_ONE_MEMBER_OF);
        identifierMap.put("x500name-union", XACML3.ID_FUNCTION_X500NAME_UNION);
        identifierMap.put("x500name-subset", XACML3.ID_FUNCTION_X500NAME_SUBSET);
        identifierMap.put("x500name-set-equals", XACML3.ID_FUNCTION_X500NAME_SET_EQUALS);
        identifierMap.put("rfc822name-intersection", XACML3.ID_FUNCTION_RFC822NAME_INTERSECTION);
        identifierMap.put("rfc822name-at-least-one-member-of", XACML3.ID_FUNCTION_RFC822NAME_AT_LEAST_ONE_MEMBER_OF);
        identifierMap.put("rfc822name-union", XACML3.ID_FUNCTION_RFC822NAME_UNION);
        identifierMap.put("rfc822name-subset", XACML3.ID_FUNCTION_RFC822NAME_SUBSET);
        identifierMap.put("rfc822name-set-equals", XACML3.ID_FUNCTION_RFC822NAME_SET_EQUALS);
        identifierMap.put("ipaddress-intersection", XACML3.ID_FUNCTION_IPADDRESS_INTERSECTION);
        identifierMap.put("ipaddress-at-least-one-member-of", XACML3.ID_FUNCTION_IPADDRESS_AT_LEAST_ONE_MEMBER_OF);
        identifierMap.put("ipaddress-union", XACML3.ID_FUNCTION_IPADDRESS_UNION);
        identifierMap.put("ipaddress-subset", XACML3.ID_FUNCTION_IPADDRESS_SUBSET);
        identifierMap.put("ipaddress-set-equals", XACML3.ID_FUNCTION_IPADDRESS_SET_EQUALS);
        identifierMap.put("dnsname-intersection", XACML3.ID_FUNCTION_DNSNAME_INTERSECTION);
        identifierMap.put("dnsname-at-least-one-member-of", XACML3.ID_FUNCTION_DNSNAME_AT_LEAST_ONE_MEMBER_OF);
        identifierMap.put("dnsname-union", XACML3.ID_FUNCTION_DNSNAME_UNION);
        identifierMap.put("dnsname-subset", XACML3.ID_FUNCTION_DNSNAME_SUBSET);
        identifierMap.put("dnsname-set-equals", XACML3.ID_FUNCTION_DNSNAME_SET_EQUALS);
        identifierMap.put("access-permitted", XACML3.ID_FUNCTION_ACCESS_PERMITTED);

        // function condition
        identifierMap.put("or", XACML3.ID_FUNCTION_OR);
        identifierMap.put("and", XACML3.ID_FUNCTION_AND);
        identifierMap.put("n-of", XACML3.ID_FUNCTION_N_OF);
        identifierMap.put("not", XACML3.ID_FUNCTION_NOT);
        identifierMap.put("any-of", XACML3.ID_FUNCTION_ANY_OF);
        identifierMap.put("all-of", XACML3.ID_FUNCTION_ALL_OF);
        identifierMap.put("any-of-any", XACML3.ID_FUNCTION_ANY_OF_ANY);
        identifierMap.put("all-of-any", XACML3.ID_FUNCTION_ALL_OF_ANY);
        identifierMap.put("any-of-all", XACML3.ID_FUNCTION_ANY_OF_ALL);
        identifierMap.put("all-of-all", XACML3.ID_FUNCTION_ALL_OF_ALL);

        // function ids
        identifierMap.put("string-one-and-only", XACML3.ID_FUNCTION_STRING_ONE_AND_ONLY);
        identifierMap.put("integer-one-and-only", XACML3.ID_FUNCTION_INTEGER_ONE_AND_ONLY);
        identifierMap.put("double-one-and-only", XACML3.ID_FUNCTION_DOUBLE_ONE_AND_ONLY);
        identifierMap.put("time-one-and-only", XACML3.ID_FUNCTION_TIME_ONE_AND_ONLY);
        identifierMap.put("date-one-and-only", XACML3.ID_FUNCTION_DATE_ONE_AND_ONLY);
        identifierMap.put("datetime-one-and-only", XACML3.ID_FUNCTION_DATETIME_ONE_AND_ONLY);
        identifierMap.put("anyuri-one-and-only", XACML3.ID_FUNCTION_ANYURI_ONE_AND_ONLY);
        identifierMap.put("hexbinary-one-and-only", XACML3.ID_FUNCTION_HEXBINARY_ONE_AND_ONLY);
        identifierMap.put("base64binary-one-and-only", XACML3.ID_FUNCTION_BASE64BINARY_ONE_AND_ONLY);
        identifierMap.put("daytimeduration-one-and-only", XACML3.ID_FUNCTION_DAYTIMEDURATION_ONE_AND_ONLY);
        identifierMap.put("yearmonthduration-one-and-only", XACML3.ID_FUNCTION_YEARMONTHDURATION_ONE_AND_ONLY);

        //attribute ids
        identifierMap.put("action-id", XACML3.ID_ACTION_ACTION_ID);

        // algorithm
        identifierMap.put("first-applicable", XACML3.ID_RULE_FIRST_APPLICABLE);
        identifierMap.put("deny-overrides", XACML3.ID_RULE_DENY_UNLESS_PERMIT);
        identifierMap.put("permit-overrides", XACML3.ID_RULE_PERMIT_UNLESS_DENY);
        identifierMap.put("only-one-applicable", XACML3.ID_RULE_ONLY_ONE_APPLICABLE);

        // data types
        identifierMap.put("string", XACML3.ID_DATATYPE_STRING);
        identifierMap.put("boolean", XACML3.ID_DATATYPE_BOOLEAN);
        identifierMap.put("integer", XACML3.ID_DATATYPE_INTEGER);
        identifierMap.put(DOUBLE, XACML3.ID_DATATYPE_DOUBLE);
        identifierMap.put("time", XACML3.ID_DATATYPE_TIME);
        identifierMap.put("date", XACML3.ID_DATATYPE_DATE);
        identifierMap.put("datetime", XACML3.ID_DATATYPE_DATETIME);
        identifierMap.put("daytimeduration", XACML3.ID_DATATYPE_DAYTIMEDURATION);
        identifierMap.put("yearmonthduration", XACML3.ID_DATATYPE_YEARMONTHDURATION);
        identifierMap.put("anyuri", XACML3.ID_DATATYPE_ANYURI);
        identifierMap.put("hexbinary", XACML3.ID_DATATYPE_HEXBINARY);
        identifierMap.put("base64binary", XACML3.ID_DATATYPE_BASE64BINARY);
        identifierMap.put("rfc822name", XACML3.ID_DATATYPE_RFC822NAME);
        identifierMap.put("x500name", XACML3.ID_DATATYPE_X500NAME);
        identifierMap.put("ipaddress", XACML3.ID_DATATYPE_IPADDRESS);
        identifierMap.put("dnsname", XACML3.ID_DATATYPE_DNSNAME);

        identifierMap.put("string-bag", XACML3.ID_FUNCTION_STRING_BAG);
        identifierMap.put("boolean-bag", XACML3.ID_FUNCTION_BOOLEAN_BAG);
        identifierMap.put("integer-bag", XACML3.ID_FUNCTION_INTEGER_BAG);
        identifierMap.put("double-bag", XACML3.ID_FUNCTION_DOUBLE_BAG);
    }

    private Identifier validateFilterPropertyFunction(String operator) throws ToscaPolicyConversionException {
        if (identifierMap.containsKey(operator.toLowerCase())) {
            return identifierMap.get(operator.toLowerCase());
        } else {
            throw new ToscaPolicyConversionException("Unexpected value " + operator);
        }
    }
}