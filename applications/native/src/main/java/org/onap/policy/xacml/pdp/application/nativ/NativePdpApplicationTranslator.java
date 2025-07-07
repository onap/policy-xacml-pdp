/*-
 * ============LICENSE_START=======================================================
 * ONAP
 * ================================================================================
 * Copyright (C) 2020-2021 AT&T Intellectual Property. All rights reserved.
 * Modifications Copyright (C) 2020, 2024 Nordix Foundation.
 * Modifications Copyright (C) 2025 Deutsche Telekom AG.
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
import java.io.StreamTokenizer;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;
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
import oasis.names.tc.xacml._3_0.core.schema.wd_17.IdReferenceType;
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

    private static final String EXPRESSION = "expr";

    private static final String ONE_AND_ONLY = "-one-and-only";

    private static final String DOUBLE = "double";

    private static final String CONVERSION_INTEGER = "integer(";

    private static final String CONVERSION_DOUBLE = "double(";

    private static final String CONVERSION_DOUBLE_ABS = "double-abs(";

    private static final String CONVERSION_INTEGER_ABS = "integer-abs(";

    private static final String CONVERSION_FLOOR = "floor(";

    private static final String CONVERSION_ROUND = "round(";

    private static final String XPATH_VERSION = "http://www.w3.org/TR/2007/REC-xpath20-20070123";

    private Map<String, Identifier> identifierMap;

    private HashMap<String, Integer> operatorPrecedenceMap;

    @Override
    public Object convertPolicy(ToscaPolicy toscaPolicy) throws ToscaPolicyConversionException {

        if (TOSCA_XACML_POLICY_TYPE.equals(toscaPolicy.getType())) {
            setIdentifierMap();
            setOperatorPrecedenceMap();
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
                LOGGER.error("error on Base64 decoding the native policy");
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
                    LOGGER.error("Invalid XACML Policy");
                    throw new ToscaPolicyConversionException("Invalid XACML Policy");
                }
                return policy;
            } catch (Exception exc) {
                LOGGER.error("Failed to read policy");
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

    private PolicySetType setPolicySetType(ToscaPolicy toscaPolicy) throws ToscaPolicyConversionException {
        PolicySetType policySetType = new PolicySetType();
        try {
            final ObjectFactory objectFactory = new ObjectFactory();
            if (toscaPolicy.getMetadata().get("policy-id") != null) {
                policySetType.setPolicySetId(String.valueOf(toscaPolicy.getMetadata().get("policy-id")));
            }
            policySetType.setPolicyCombiningAlgId(XACML3.ID_POLICY_FIRST_APPLICABLE.stringValue());
            if (toscaPolicy.getMetadata().get("policy-version") != null) {
                policySetType.setVersion(String.valueOf(toscaPolicy.getMetadata().get("policy-version")));
            }
            if (toscaPolicy.getMetadata().get(DESCRIPTION) != null) {
                policySetType.setDescription(String.valueOf(toscaPolicy.getMetadata().get(DESCRIPTION)));
            }
            if ((toscaPolicy.getMetadata().get("action") != null)) {
                policySetType.setTarget(setPolicySetTarget(toscaPolicy.getMetadata().get("action")));
            }
            if (toscaPolicy.getProperties().get("policySetIdRefs") != null) {
                for (Map<String, String> type : (List<Map<String, String>>) toscaPolicy.getProperties()
                    .get("policySetIdRefs")) {
                    IdReferenceType ref = objectFactory.createIdReferenceType();
                    if (type.get("id") == null || type.get("version") == null
                        || type.get("id").isEmpty() || type.get("version").isEmpty()) {
                        LOGGER.error("Invalid policy set reference , missing ID or version");
                        throw new ToscaPolicyConversionException(
                            "Invalid policy set reference , missing ID or version");
                    }
                    ref.setValue(type.get("id"));
                    ref.setVersion(type.get("version"));
                    policySetType.getPolicySetOrPolicyOrPolicySetIdReference()
                        .add(objectFactory.createPolicySetIdReference(ref));
                }
            }
            if (toscaPolicy.getProperties().get("policies") != null) {
                for (Map<String, Object> type : (List<Map<String, Object>>) toscaPolicy.getProperties()
                    .get("policies")) {
                    ToscaPolicy policy = new ToscaPolicy();
                    policy.setMetadata((Map<String, Object>) type.get("metadata"));
                    policy.setProperties((Map<String, Object>) type.get("properties"));
                    policySetType.getPolicySetOrPolicyOrPolicySetIdReference()
                        .add(objectFactory.createPolicy(convertPolicyXacml(policy)));
                }
            }
        } catch (ToscaPolicyConversionException ex) {
            LOGGER.error("Invalid PolicySet structure");
            throw new ToscaPolicyConversionException("Invalid PolicySet structure");
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
            throw new ToscaPolicyConversionException("Invalid rule structure");
        }
        if (properties.get("default") != null) {
            setDefaultRule((String) properties.get("default"), policyType);
        }
        return policyType;
    }

    private void setPolicyType(ToscaPolicy toscaPolicy, PolicyType policyType) throws ToscaPolicyConversionException {
        try {
            policyType.setPolicyId(String.valueOf(toscaPolicy.getMetadata().get("policy-id")));
            policyType.setVersion(String.valueOf(toscaPolicy.getMetadata().get("policy-version")));
            policyType.setDescription(String.valueOf(toscaPolicy.getMetadata().get(DESCRIPTION)));
            DefaultsType defaultsType = new DefaultsType();
            defaultsType.setXPathVersion(XPATH_VERSION);
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
        } catch (Exception ex) {
            LOGGER.error("Invalid Policy structure");
            throw new ToscaPolicyConversionException("Invalid Policy structure");
        }
    }

    private void setAdviceExpression(RuleType ruleType, Map<String, Object> rule)
        throws ToscaPolicyConversionException {
        try {
            String decision = "Deny";
            if (rule.get("decision") != null) {
                decision = (String) rule.get("decision");
            }
            if ("Deny".equalsIgnoreCase(decision)) {
                ruleType.setEffect(EffectType.DENY);
            } else {
                ruleType.setEffect(EffectType.PERMIT);
            }
            if (rule.get("advice") != null) {
                ruleType.setAdviceExpressions(setAdvice((Map<String, Object>) rule.get("advice"), decision));
            }
        } catch (Exception ex) {
            LOGGER.error("Invalid advice structure");
            throw new ToscaPolicyConversionException("Invalid advice structure");
        }
    }

    private void setDefaultRule(String defaultDecision, PolicyType policyType) {
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
            if (appliesTo.get("anyOne") != null) {
                List<Map<String, Object>> allOffList = (List<Map<String, Object>>) appliesTo.get("anyOne");
                for (Map<String, Object> allOff : allOffList) {
                    if (allOff.get("allOf") != null) {
                        for (Map<String, Object> match : (List<Map<String, Object>>) allOff.get("allOf")) {
                            var matchType = new MatchType();
                            String operator = "";
                            if (match.get("operator") != null) {
                                operator = (String) match.get("operator");
                            }
                            String datatype = getDatatype(operator);
                            matchType.setMatchId(validateFilterPropertyFunction(operator).stringValue());
                            var valueType = setAttributeValueType(match.get(VALUE),
                                validateFilterPropertyFunction(datatype).stringValue());
                            matchType.setAttributeValue(valueType);
                            String attribute = "";
                            String category = "";
                            if (match.get("key") != null) {
                                if (((String) match.get("key")).contains("action")) {
                                    attribute = validateFilterPropertyFunction((String) match
                                        .get("key")).stringValue();
                                    category = XACML3.ID_ATTRIBUTE_CATEGORY_ACTION.stringValue();
                                } else {
                                    attribute = (String) match.get("key");
                                    category = XACML3.ID_ATTRIBUTE_CATEGORY_RESOURCE.stringValue();
                                }
                            }
                            var designator = setAttributeDesignatorType(attribute, category,
                                validateFilterPropertyFunction(datatype).stringValue(), false);
                            matchType.setAttributeDesignator(designator);
                            listMatch.add(matchType);
                        }
                    }
                }
            }
        } catch (Exception ex) {
            LOGGER.error("Invalid target format");
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
        var factory = new ObjectFactory();
        try {
            if (conditionMap.get(APPLY) != null) {
                Map<String, Object> applyMap = (Map<String, Object>) conditionMap.get(APPLY);
                ApplyType parentApply = setApply(applyMap);
                condition.setExpression(factory.createApply(parentApply));
            } else if (conditionMap.get(EXPRESSION) != null) {
                String expr = conditionMap.get(EXPRESSION).toString();
                ApplyType parentApply = convertToPrefixXacmlApply(expr, factory);
                condition.setExpression(factory.createApply(parentApply));
            } else {
                LOGGER.error("Invalid condition structure");
                throw new ToscaPolicyConversionException("Invalid condition structure");
            }
        } catch (Exception ex) {
            LOGGER.error("Invalid condition structure");
            throw new ToscaPolicyConversionException("Invalid condition structure");
        }
        return condition;
    }

    private ApplyType setApply(Map<String, Object> applies) throws ToscaPolicyConversionException {
        var apply = new ApplyType();
        var factory = new ObjectFactory();
        if ((applies.get("keys") != null) && (applies.get("operator") != null)) {
            try {
                List<Object> keys = (List<Object>) applies.get("keys");
                String operator = (String) applies.get("operator");
                String datatype = "";
                boolean isHigherOrder = switch (operator) {
                    case "all-of", "any-of", "any-of-any", "all-of-any", "all-of-all",
                         "any-of-all", "map" -> true;
                    default -> false;
                };
                if (!(isHigherOrder)) {
                    datatype = getDatatype(operator);
                }
                apply.setFunctionId(validateFilterPropertyFunction(operator).stringValue());
                List<Object> keyList = new ArrayList<>();
                getApplyKeys(keyList, keys, datatype, factory, apply);
                setApplyKeys(keyList, apply, factory);

                if (applies.get("compareWith") != null) {
                    setCompareWith(applies, apply, factory, getDatatype(operator));
                }
            } catch (Exception ex) {
                LOGGER.error("Invalid apply structure");
                throw new ToscaPolicyConversionException("Invalid apply structure");
            }
        } else {
            LOGGER.error("Keys or operator missing in apply");
            throw new ToscaPolicyConversionException("Keys or operator missing in apply");
        }
        return apply;
    }

    private void getApplyKeys(List<Object> keyList, List<Object> keys, String datatype,
                              ObjectFactory factory, ApplyType apply) throws ToscaPolicyConversionException {
        try {
            for (Object keyObject : keys) {
                if (keyObject instanceof Map<?, ?>) {
                    if (((Map<?, ?>) keyObject).get("function") != null) {
                        String fun = ((Map<String, String>) keyObject).get("function");
                        datatype = getDatatype(fun);
                    }
                }
            }
            String originalDatatype = datatype;
            for (int i = 0; i < keys.size(); i++) {
                if (originalDatatype.equals("n-of")) {
                    if (i == 0) {
                        datatype = "integer";
                    } else {
                        datatype = "boolean";
                    }
                }
                Object keyObject = keys.get(i);
                if (keyObject instanceof Map<?, ?>) {
                    if (((Map<?, ?>) keyObject).get("list") != null) {
                        keyList.add(setBagApply((List<Object>) ((Map<?, ?>) keyObject).get("list"), datatype, factory));
                    } else if (((Map<?, ?>) keyObject).get("function") != null) {
                        keyList.add(setFunctionType(apply, ((Map<String, String>) keyObject).get("function"), factory));
                    } else if (((Map<?, ?>) keyObject).get(APPLY) != null) {
                        keyList.add(setApply((Map<String, Object>) ((Map<?, ?>) keyObject).get(APPLY)));
                    } else if (((Map<?, ?>) keyObject).get(EXPRESSION) != null) {
                        String expr = ((Map<String, String>) keyObject).get(EXPRESSION);
                        apply = convertToPrefixXacmlApply(expr, factory);
                        keyList.add(apply);
                    } else {
                        throw new ToscaPolicyConversionException(
                            "Invalid key entry, object does not contain list, function, expr or apply");
                    }
                } else {
                    setAttributes(keyObject, keyList, datatype, factory);
                }
            }
        } catch (Exception ex) {
            LOGGER.error("Invalid keys in apply");
            throw new ToscaPolicyConversionException("Invalid keys in apply");
        }
    }

    private void setApplyKeys(List<Object> keyList, ApplyType apply, ObjectFactory factory) {
        if (keyList != null) {
            keyList.stream()
                .forEach(key -> {
                    if (key instanceof AttributeValueType) {
                        apply.getExpression().add(factory.createAttributeValue((AttributeValueType) key));
                    }
                    if (key instanceof ApplyType) {
                        apply.getExpression().add(factory.createApply((ApplyType) key));
                    }
                    if (key instanceof FunctionType) {
                        apply.getExpression().add(factory.createFunction((FunctionType) key));
                    }
                });
        }
    }

    /**
     * Create AttributeValue if it is simple value.
     * Create AttributeDesignator if it is an input parameter name.
     * Differentiate between a simple string value and a parameter name by checking
     * if the string is enclosed by single quote , which means it is a simple value
     */
    private void setAttributes(Object key, List<Object> keyList, String datatype, ObjectFactory factory)
        throws ToscaPolicyConversionException {
        try {
            if (key instanceof String value) {
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
        }  catch (Exception ex) {
            LOGGER.error("Invalid string value format in keys");
            throw new ToscaPolicyConversionException("Invalid string value format in keys");
        }
    }

    private ApplyType setBagApply(List<Object> list, String datatype, ObjectFactory factory)
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
                if (attribute instanceof String value) {
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
            return bagApply;
        } catch (Exception ex) {
            LOGGER.error("Invalid list format in keys");
            throw new ToscaPolicyConversionException("Invalid list format in keys");
        }
    }

    private FunctionType setFunctionType(ApplyType apply, String function, ObjectFactory factory)
        throws ToscaPolicyConversionException {
        try {
            var functionType = new FunctionType();
            functionType.setFunctionId(validateFilterPropertyFunction(function).stringValue());
            return functionType;
        } catch (Exception ex) {
            LOGGER.error("Invalid function format in keys {}", function);
            throw new ToscaPolicyConversionException("Invalid function format in keys " + function);
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
        } catch (Exception ex) {
            LOGGER.error("Invalid compareWith format");
            throw new ToscaPolicyConversionException("Invalid compareWith format");
        }
    }

    private AdviceExpressionsType setAdvice(Map<String, Object> advice, String decision) {
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
        var adviceExpressions = new AdviceExpressionsType();
        adviceExpressions.getAdviceExpression().add(adviceExpression);
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

    //
    // datatype of an attribute is derived from the operator
    //
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
        } catch (Exception ex) {
            LOGGER.error("Unexpected operator {}", operator);
            throw new ToscaPolicyConversionException("Invalid operator " + operator);
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
        identifierMap.put("time-greater-than", XACML3.ID_FUNCTION_TIME_GREATER_THAN);
        identifierMap.put("time-greater-than-or-equal", XACML3.ID_FUNCTION_TIME_GREATER_THAN_OR_EQUAL);
        identifierMap.put("time-less-than", XACML3.ID_FUNCTION_TIME_LESS_THAN);
        identifierMap.put("time-less-than-or-equal", XACML3.ID_FUNCTION_TIME_LESS_THAN_OR_EQUAL);
        identifierMap.put("dateTime-greater-than", XACML3.ID_FUNCTION_DATETIME_GREATER_THAN);
        identifierMap.put("dateTime-greater-than-or-equal", XACML3.ID_FUNCTION_DATETIME_GREATER_THAN_OR_EQUAL);
        identifierMap.put("dateTime-less-than", XACML3.ID_FUNCTION_DATETIME_LESS_THAN);
        identifierMap.put("dateTime-less-than-or-equal", XACML3.ID_FUNCTION_DATETIME_LESS_THAN_OR_EQUAL);
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
        identifierMap.put("yearMonthDuration-equal", XACML3.ID_FUNCTION_YEARMONTHDURATION_EQUAL);
        identifierMap.put("anyURI-equal", XACML3.ID_FUNCTION_ANYURI_EQUAL);
        identifierMap.put("hexBinary-equal", XACML3.ID_FUNCTION_HEXBINARY_EQUAL);
        identifierMap.put("rfc822Name-equal", XACML3.ID_FUNCTION_RFC822NAME_EQUAL);
        identifierMap.put("x500Name-equal", XACML3.ID_FUNCTION_X500NAME_EQUAL);
        identifierMap.put("string-from-ipAddress", XACML3.ID_FUNCTION_STRING_FROM_IPADDRESS);
        identifierMap.put("string-from-dnsName", XACML3.ID_FUNCTION_STRING_FROM_DNSNAME);
        identifierMap.put("boolean-equal", XACML3.ID_FUNCTION_BOOLEAN_EQUAL);
        identifierMap.put("double-equal", XACML3.ID_FUNCTION_DOUBLE_EQUAL);
        identifierMap.put("date-equal", XACML3.ID_FUNCTION_DATE_EQUAL);
        identifierMap.put("time-equal", XACML3.ID_FUNCTION_TIME_EQUAL);
        identifierMap.put("dateTime-equal", XACML3.ID_FUNCTION_DATETIME_EQUAL);
        identifierMap.put("dayTimeDuration-equal", XACML3.ID_FUNCTION_DAYTIMEDURATION_EQUAL);
        identifierMap.put("base64Binary-equal", XACML3.ID_FUNCTION_BASE64BINARY_EQUAL);
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
        identifierMap.put("dateTime-bag-size", XACML3.ID_FUNCTION_DATETIME_BAG_SIZE);
        identifierMap.put("dateTime-is-in", XACML3.ID_FUNCTION_DATETIME_IS_IN);
        identifierMap.put("dateTime-bag", XACML3.ID_FUNCTION_DATETIME_BAG);
        identifierMap.put("anyURI-bag-size", XACML3.ID_FUNCTION_ANYURI_BAG_SIZE);
        identifierMap.put("anyURI-is-in", XACML3.ID_FUNCTION_ANYURI_IS_IN);
        identifierMap.put("anyURI-bag", XACML3.ID_FUNCTION_ANYURI_BAG);
        identifierMap.put("hexBinary-bag-size", XACML3.ID_FUNCTION_HEXBINARY_BAG_SIZE);
        identifierMap.put("hexBinary-is-in", XACML3.ID_FUNCTION_HEXBINARY_IS_IN);
        identifierMap.put("hexBinary-bag", XACML3.ID_FUNCTION_HEXBINARY_BAG);
        identifierMap.put("base64Binary-bag-size", XACML3.ID_FUNCTION_BASE64BINARY_BAG_SIZE);
        identifierMap.put("base64Binary-is-in", XACML3.ID_FUNCTION_BASE64BINARY_IS_IN);
        identifierMap.put("base64Binary-bag", XACML3.ID_FUNCTION_BASE64BINARY_BAG);
        identifierMap.put("dayTimeDuration-bag-size", XACML3.ID_FUNCTION_DAYTIMEDURATION_BAG_SIZE);
        identifierMap.put("dayTimeDuration-is-in", XACML3.ID_FUNCTION_DAYTIMEDURATION_IS_IN);
        identifierMap.put("dayTimeDuration-bag", XACML3.ID_FUNCTION_DAYTIMEDURATION_BAG);
        identifierMap.put("yearMonthDuration-bag-size", XACML3.ID_FUNCTION_YEARMONTHDURATION_BAG_SIZE);
        identifierMap.put("yearMonthDuration-is-in", XACML3.ID_FUNCTION_YEARMONTHDURATION_IS_IN);
        identifierMap.put("yearMonthDuration-bag", XACML3.ID_FUNCTION_YEARMONTHDURATION_BAG);
        identifierMap.put("x500Name-one-and-only", XACML3.ID_FUNCTION_X500NAME_ONE_AND_ONLY);
        identifierMap.put("x500Name-bag-size", XACML3.ID_FUNCTION_X500NAME_BAG_SIZE);
        identifierMap.put("x500Name-is-in", XACML3.ID_FUNCTION_X500NAME_IS_IN);
        identifierMap.put("x500Name-bag", XACML3.ID_FUNCTION_X500NAME_BAG);
        identifierMap.put("rfc822Name-one-and-only", XACML3.ID_FUNCTION_RFC822NAME_ONE_AND_ONLY);
        identifierMap.put("rfc822Name-bag-size", XACML3.ID_FUNCTION_RFC822NAME_BAG_SIZE);
        identifierMap.put("rfc822Name-is-in", XACML3.ID_FUNCTION_RFC822NAME_IS_IN);
        identifierMap.put("rfc822Name-bag", XACML3.ID_FUNCTION_RFC822NAME_BAG);
        identifierMap.put("ipAddress-one-and-only", XACML3.ID_FUNCTION_IPADDRESS_ONE_AND_ONLY);
        identifierMap.put("ipAddress-bag-size", XACML3.ID_FUNCTION_IPADDRESS_BAG_SIZE);
        identifierMap.put("ipAddress-is-in", XACML3.ID_FUNCTION_IPADDRESS_IS_IN);
        identifierMap.put("ipAddress-bag", XACML3.ID_FUNCTION_IPADDRESS_BAG);
        identifierMap.put("dnsName-one-and-only", XACML3.ID_FUNCTION_DNSNAME_ONE_AND_ONLY);
        identifierMap.put("dnsName-bag-size", XACML3.ID_FUNCTION_DNSNAME_BAG_SIZE);
        identifierMap.put("dnsName-is-in", XACML3.ID_FUNCTION_DNSNAME_IS_IN);
        identifierMap.put("dnsName-bag", XACML3.ID_FUNCTION_DNSNAME_BAG);
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
        identifierMap.put("dateTime-from-string", XACML3.ID_FUNCTION_DATETIME_FROM_STRING);
        identifierMap.put("string-from-dateTime", XACML3.ID_FUNCTION_STRING_FROM_DATETIME);
        identifierMap.put("anyURI-from-string", XACML3.ID_FUNCTION_ANYURI_FROM_STRING);
        identifierMap.put("string-from-anyURI", XACML3.ID_FUNCTION_STRING_FROM_ANYURI);
        identifierMap.put("dayTimeDuration-from-string", XACML3.ID_FUNCTION_DAYTIMEDURATION_FROM_STRING);
        identifierMap.put("string-from-daytimeDuration", XACML3.ID_FUNCTION_STRING_FROM_DAYTIMEDURATION);
        identifierMap.put("yearMonthDuration-from-string", XACML3.ID_FUNCTION_YEARMONTHDURATION_FROM_STRING);
        identifierMap.put("string-from-yearMonthDuration", XACML3.ID_FUNCTION_STRING_FROM_YEARMONTHDURATION);
        identifierMap.put("x500Name-from-string", XACML3.ID_FUNCTION_X500NAME_FROM_STRING);
        identifierMap.put("string-from-x500Name", XACML3.ID_FUNCTION_STRING_FROM_X500NAME);
        identifierMap.put("rfc822Name-from-string", XACML3.ID_FUNCTION_RFC822NAME_FROM_STRING);
        identifierMap.put("string-from-rfc822Name", XACML3.ID_FUNCTION_STRING_FROM_RFC822NAME);
        identifierMap.put("ipAddress-from-string", XACML3.ID_FUNCTION_IPADDRESS_FROM_STRING);
        identifierMap.put("dnsName-from-string", XACML3.ID_FUNCTION_DNSNAME_FROM_STRING);
        identifierMap.put("anyURI-starts-with", XACML3.ID_FUNCTION_ANYURI_STARTS_WITH);
        identifierMap.put("anyURI-ends-with", XACML3.ID_FUNCTION_ANYURI_ENDS_WITH);
        identifierMap.put("anyURI-contains", XACML3.ID_FUNCTION_ANYURI_CONTAINS);
        identifierMap.put("string-substring", XACML3.ID_FUNCTION_STRING_SUBSTRING);
        identifierMap.put("anyURI-substring", XACML3.ID_FUNCTION_ANYURI_SUBSTRING);
        identifierMap.put("map", XACML3.ID_FUNCTION_MAP);
        identifierMap.put("n-of", XACML3.ID_FUNCTION_N_OF);
        identifierMap.put("x500Name-match", XACML3.ID_FUNCTION_X500NAME_MATCH);
        identifierMap.put("rfc822Name-match", XACML3.ID_FUNCTION_RFC822NAME_MATCH);
        identifierMap.put("anyURI-regexp-match", XACML3.ID_FUNCTION_ANYURI_REGEXP_MATCH);
        identifierMap.put("ipAddress-regexp-match", XACML3.ID_FUNCTION_IPADDRESS_REGEXP_MATCH);
        identifierMap.put("dnsName-regexp-match", XACML3.ID_FUNCTION_DNSNAME_REGEXP_MATCH);
        identifierMap.put("rfc822Name-regexp-match", XACML3.ID_FUNCTION_RFC822NAME_REGEXP_MATCH);
        identifierMap.put("x500Name-regexp-match", XACML3.ID_FUNCTION_X500NAME_REGEXP_MATCH);
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
        identifierMap.put("dateTime-intersection", XACML3.ID_FUNCTION_DATETIME_INTERSECTION);
        identifierMap.put("dateTime-at-least-one-member-of", XACML3.ID_FUNCTION_DATETIME_AT_LEAST_ONE_MEMBER_OF);
        identifierMap.put("dateTime-union", XACML3.ID_FUNCTION_DATETIME_UNION);
        identifierMap.put("dateTime-subset", XACML3.ID_FUNCTION_DATETIME_SUBSET);
        identifierMap.put("dateTime-set-equals", XACML3.ID_FUNCTION_DATETIME_SET_EQUALS);
        identifierMap.put("anyURI-intersection", XACML3.ID_FUNCTION_ANYURI_INTERSECTION);
        identifierMap.put("anyURI-at-least-one-member-of", XACML3.ID_FUNCTION_ANYURI_AT_LEAST_ONE_MEMBER_OF);
        identifierMap.put("anyURI-union", XACML3.ID_FUNCTION_ANYURI_UNION);
        identifierMap.put("anyURI-subset", XACML3.ID_FUNCTION_ANYURI_SUBSET);
        identifierMap.put("anyURI-set-equals", XACML3.ID_FUNCTION_ANYURI_SET_EQUALS);
        identifierMap.put("hexBinary-intersection", XACML3.ID_FUNCTION_HEXBINARY_INTERSECTION);
        identifierMap.put("hexBinary-at-least-one-member-of", XACML3.ID_FUNCTION_HEXBINARY_AT_LEAST_ONE_MEMBER_OF);
        identifierMap.put("hexBinary-union", XACML3.ID_FUNCTION_HEXBINARY_UNION);
        identifierMap.put("hexBinary-subset", XACML3.ID_FUNCTION_HEXBINARY_SUBSET);
        identifierMap.put("hexBinary-set-equals", XACML3.ID_FUNCTION_HEXBINARY_SET_EQUALS);
        identifierMap.put("base64Binary-intersection", XACML3.ID_FUNCTION_BASE64BINARY_INTERSECTION);
        identifierMap.put("string-from-dayTimeDuration", XACML3.ID_FUNCTION_STRING_FROM_DAYTIMEDURATION);
        identifierMap.put("string-from-rfc822Name", XACML3.ID_FUNCTION_STRING_FROM_RFC822NAME);
        identifierMap.put("string-from-ipAddress", XACML3.ID_FUNCTION_STRING_FROM_IPADDRESS);
        identifierMap.put("base64Binary-at-least-one-member-of",
            XACML3.ID_FUNCTION_BASE64BINARY_AT_LEAST_ONE_MEMBER_OF);
        identifierMap.put("base64Binary-union", XACML3.ID_FUNCTION_BASE64BINARY_UNION);
        identifierMap.put("base64Binary-subset", XACML3.ID_FUNCTION_BASE64BINARY_SUBSET);
        identifierMap.put("base64Binary-set-equals", XACML3.ID_FUNCTION_BASE64BINARY_SET_EQUALS);
        identifierMap.put("dayTimeDuration-intersection", XACML3.ID_FUNCTION_DAYTIMEDURATION_INTERSECTION);
        identifierMap.put("dayTimeDuration-at-least-one-member-of",
            XACML3.ID_FUNCTION_DAYTIMEDURATION_AT_LEAST_ONE_MEMBER_OF);
        identifierMap.put("dayTimeDuration-union", XACML3.ID_FUNCTION_DAYTIMEDURATION_UNION);
        identifierMap.put("dayTimeDuration-subset", XACML3.ID_FUNCTION_DAYTIMEDURATION_SUBSET);
        identifierMap.put("dayTimeDuration-set-equals", XACML3.ID_FUNCTION_DAYTIMEDURATION_SET_EQUALS);
        identifierMap.put("yearMonthDuration-intersection", XACML3.ID_FUNCTION_YEARMONTHDURATION_INTERSECTION);
        identifierMap.put("yearMonthDuration-at-least-one-member-of",
            XACML3.ID_FUNCTION_YEARMONTHDURATION_AT_LEAST_ONE_MEMBER_OF);
        identifierMap.put("yearMonthDuration-union", XACML3.ID_FUNCTION_YEARMONTHDURATION_UNION);
        identifierMap.put("yearMonthDuration-subset", XACML3.ID_FUNCTION_YEARMONTHDURATION_SUBSET);
        identifierMap.put("yearMonthDuration-set-equals", XACML3.ID_FUNCTION_YEARMONTHDURATION_SET_EQUALS);
        identifierMap.put("x500Name-intersection", XACML3.ID_FUNCTION_X500NAME_INTERSECTION);
        identifierMap.put("x500Name-at-least-one-member-of", XACML3.ID_FUNCTION_X500NAME_AT_LEAST_ONE_MEMBER_OF);
        identifierMap.put("x500Name-union", XACML3.ID_FUNCTION_X500NAME_UNION);
        identifierMap.put("x500Name-subset", XACML3.ID_FUNCTION_X500NAME_SUBSET);
        identifierMap.put("x500Name-set-equals", XACML3.ID_FUNCTION_X500NAME_SET_EQUALS);
        identifierMap.put("rfc822Name-intersection", XACML3.ID_FUNCTION_RFC822NAME_INTERSECTION);
        identifierMap.put("rfc822Name-at-least-one-member-of", XACML3.ID_FUNCTION_RFC822NAME_AT_LEAST_ONE_MEMBER_OF);
        identifierMap.put("rfc822Name-union", XACML3.ID_FUNCTION_RFC822NAME_UNION);
        identifierMap.put("rfc822Name-subset", XACML3.ID_FUNCTION_RFC822NAME_SUBSET);
        identifierMap.put("rfc822Name-set-equals", XACML3.ID_FUNCTION_RFC822NAME_SET_EQUALS);
        identifierMap.put("rfc822Name-equal", XACML3.ID_FUNCTION_RFC822NAME_EQUAL);
        identifierMap.put("ipAddress-intersection", XACML3.ID_FUNCTION_IPADDRESS_INTERSECTION);
        identifierMap.put("ipAddress-at-least-one-member-of", XACML3.ID_FUNCTION_IPADDRESS_AT_LEAST_ONE_MEMBER_OF);
        identifierMap.put("ipAddress-union", XACML3.ID_FUNCTION_IPADDRESS_UNION);
        identifierMap.put("ipAddress-subset", XACML3.ID_FUNCTION_IPADDRESS_SUBSET);
        identifierMap.put("ipAddress-set-equals", XACML3.ID_FUNCTION_IPADDRESS_SET_EQUALS);
        identifierMap.put("dnsName-intersection", XACML3.ID_FUNCTION_DNSNAME_INTERSECTION);
        identifierMap.put("dnsName-at-least-one-member-of", XACML3.ID_FUNCTION_DNSNAME_AT_LEAST_ONE_MEMBER_OF);
        identifierMap.put("dnsName-union", XACML3.ID_FUNCTION_DNSNAME_UNION);
        identifierMap.put("dnsName-subset", XACML3.ID_FUNCTION_DNSNAME_SUBSET);
        identifierMap.put("dnsName-set-equals", XACML3.ID_FUNCTION_DNSNAME_SET_EQUALS);
        identifierMap.put("access-permitted", XACML3.ID_FUNCTION_ACCESS_PERMITTED);
        identifierMap.put("string-one-and-only", XACML3.ID_FUNCTION_STRING_ONE_AND_ONLY);
        identifierMap.put("integer-one-and-only", XACML3.ID_FUNCTION_INTEGER_ONE_AND_ONLY);
        identifierMap.put("double-one-and-only", XACML3.ID_FUNCTION_DOUBLE_ONE_AND_ONLY);
        identifierMap.put("time-one-and-only", XACML3.ID_FUNCTION_TIME_ONE_AND_ONLY);
        identifierMap.put("date-one-and-only", XACML3.ID_FUNCTION_DATE_ONE_AND_ONLY);
        identifierMap.put("dateTime-one-and-only", XACML3.ID_FUNCTION_DATETIME_ONE_AND_ONLY);
        identifierMap.put("anyURI-one-and-only", XACML3.ID_FUNCTION_ANYURI_ONE_AND_ONLY);
        identifierMap.put("hexBinary-one-and-only", XACML3.ID_FUNCTION_HEXBINARY_ONE_AND_ONLY);
        identifierMap.put("base64Binary-one-and-only", XACML3.ID_FUNCTION_BASE64BINARY_ONE_AND_ONLY);
        identifierMap.put("dayTimeDuration-one-and-only", XACML3.ID_FUNCTION_DAYTIMEDURATION_ONE_AND_ONLY);
        identifierMap.put("yearMonthDuration-one-and-only", XACML3.ID_FUNCTION_YEARMONTHDURATION_ONE_AND_ONLY);
        identifierMap.put("or", XACML3.ID_FUNCTION_OR);
        identifierMap.put("and", XACML3.ID_FUNCTION_AND);
        identifierMap.put("not", XACML3.ID_FUNCTION_NOT);
        identifierMap.put("any-of", XACML3.ID_FUNCTION_ANY_OF);
        identifierMap.put("all-of", XACML3.ID_FUNCTION_ALL_OF);
        identifierMap.put("any-of-any", XACML3.ID_FUNCTION_ANY_OF_ANY);
        identifierMap.put("all-of-any", XACML3.ID_FUNCTION_ALL_OF_ANY);
        identifierMap.put("any-of-all", XACML3.ID_FUNCTION_ANY_OF_ALL);
        identifierMap.put("all-of-all", XACML3.ID_FUNCTION_ALL_OF_ALL);
        identifierMap.put("dateTime-add-dayTimeDuration", XACML3.ID_FUNCTION_DATETIME_ADD_DAYTIMEDURATION);
        identifierMap.put("dateTime-add-yearMonthDuration", XACML3.ID_FUNCTION_DATETIME_ADD_YEARMONTHDURATION);
        identifierMap.put("dateTime-subtract-yearMonthDuration",
            XACML3.ID_FUNCTION_DATETIME_SUBTRACT_YEARMONTHDURATION);
        identifierMap.put("dateTime-subtract-dayTimeDuration",
            XACML3.ID_FUNCTION_DATETIME_SUBTRACT_DAYTIMEDURATION);
        identifierMap.put("date-add-yearMonthDuration", XACML3.ID_FUNCTION_DATE_ADD_YEARMONTHDURATION);
        identifierMap.put("date-subtract-yearMonthDuration", XACML3.ID_FUNCTION_DATE_SUBTRACT_YEARMONTHDURATION);

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
        identifierMap.put("dateTime", XACML3.ID_DATATYPE_DATETIME);
        identifierMap.put("dayTimeDuration", XACML3.ID_DATATYPE_DAYTIMEDURATION);
        identifierMap.put("yearMonthDuration", XACML3.ID_DATATYPE_YEARMONTHDURATION);
        identifierMap.put("anyURI", XACML3.ID_DATATYPE_ANYURI);
        identifierMap.put("hexBinary", XACML3.ID_DATATYPE_HEXBINARY);
        identifierMap.put("base64Binary", XACML3.ID_DATATYPE_BASE64BINARY);
        identifierMap.put("rfc822Name", XACML3.ID_DATATYPE_RFC822NAME);
        identifierMap.put("x500Name", XACML3.ID_DATATYPE_X500NAME);
        identifierMap.put("ipAddress", XACML3.ID_DATATYPE_IPADDRESS);
        identifierMap.put("dnsName", XACML3.ID_DATATYPE_DNSNAME);
        identifierMap.put("string-bag", XACML3.ID_FUNCTION_STRING_BAG);
        identifierMap.put("boolean-bag", XACML3.ID_FUNCTION_BOOLEAN_BAG);
        identifierMap.put("integer-bag", XACML3.ID_FUNCTION_INTEGER_BAG);
        identifierMap.put("double-bag", XACML3.ID_FUNCTION_DOUBLE_BAG);
    }

    private Identifier validateFilterPropertyFunction(String operator) throws ToscaPolicyConversionException {
        if (identifierMap.containsKey(operator)) {
            return identifierMap.get(operator);
        } else {
            LOGGER.error("Unsupported operator {}", operator);
            throw new ToscaPolicyConversionException("Unexpected operator " + operator);
        }
    }

    private void setOperatorPrecedenceMap() {
        operatorPrecedenceMap = new HashMap<>();
        operatorPrecedenceMap.put("*", 4);  // Multiplication
        operatorPrecedenceMap.put("/", 4);  // Division same as multiplication
        operatorPrecedenceMap.put("+", 3);  // Addition
        operatorPrecedenceMap.put("-", 3);  // Subtraction same as addition
        operatorPrecedenceMap.put("(", 1);  // Parentheses
        operatorPrecedenceMap.put(")", 2);  // Closing parentheses same level
        operatorPrecedenceMap.put("<", 1);   // Less than
        operatorPrecedenceMap.put("<=", 1);  // Less than or equal
        operatorPrecedenceMap.put(">", 1);   // Greater than
        operatorPrecedenceMap.put(">=", 1);  // Greater than or equal
        operatorPrecedenceMap.put("==", 1);  // Equal to
        operatorPrecedenceMap.put("!=", 1);  // Not equal to
        operatorPrecedenceMap.put("double(", 1);  // Conversion low precedence
        operatorPrecedenceMap.put("integer(", 1);  // Conversion low precedence
        operatorPrecedenceMap.put("double-abs(", 1);  // Absolute low precedence
        operatorPrecedenceMap.put("integer-abs(", 1);  // Absolute low precedence
        operatorPrecedenceMap.put("floor(", 1);  // Floor low precedence
        operatorPrecedenceMap.put("round(", 1);  // Round low precedence
    }

    private Identifier getOperatorXacmlMap(String operator) throws ToscaPolicyConversionException {
        if (operator.equals("*")) {
            return XACML3.ID_FUNCTION_DOUBLE_MULTIPLY;
        } else if (operator.equals("/")) {
            return XACML3.ID_FUNCTION_DOUBLE_DIVIDE;
        } else if (operator.equals("+")) {
            return XACML3.ID_FUNCTION_DOUBLE_ADD;
        } else if (operator.equals("-")) {
            return XACML3.ID_FUNCTION_DOUBLE_SUBTRACT;
        } else if (operator.equals("<")) {
            return XACML3.ID_FUNCTION_DOUBLE_LESS_THAN;
        } else if (operator.equals("<=")) {
            return XACML3.ID_FUNCTION_DOUBLE_LESS_THAN_OR_EQUAL;
        } else if (operator.equals(">")) {
            return XACML3.ID_FUNCTION_DOUBLE_GREATER_THAN;
        } else if (operator.equals(">=")) {
            return XACML3.ID_FUNCTION_DOUBLE_GREATER_THAN_OR_EQUAL;
        } else if (operator.equals("==")) {
            return XACML3.ID_FUNCTION_DOUBLE_EQUAL;
        } else if (operator.equals("double(")) {
            return XACML3.ID_FUNCTION_INTEGER_TO_DOUBLE;
        } else if (operator.equals("integer(")) {
            return XACML3.ID_FUNCTION_DOUBLE_TO_INTEGER;
        } else {
            LOGGER.error("Unsupported operator {}", operator);
            throw new ToscaPolicyConversionException("Unsupported operator " + operator);
        }
    }

    private Boolean singleOperandExpression(String expression) {
        return expression.equals(CONVERSION_INTEGER)
            || expression.equals(CONVERSION_DOUBLE)
            || expression.equals(CONVERSION_INTEGER_ABS)
            || expression.equals(CONVERSION_DOUBLE_ABS)
            || expression.equals(CONVERSION_FLOOR)
            || expression.equals(CONVERSION_ROUND);
    }

    private ApplyType convertToPrefixXacmlApply(String expression, ObjectFactory factory)
        throws ToscaPolicyConversionException {

        LOGGER.debug("Got expression to parse : {}", expression);
        StreamTokenizer tokenizer = new StreamTokenizer(new StringReader(expression));
        tokenizer.eolIsSignificant(true);
        tokenizer.wordChars('.', '_');
        tokenizer.ordinaryChar('(');
        tokenizer.ordinaryChar(')');
        tokenizer.ordinaryChar('+');
        tokenizer.ordinaryChar('-');
        tokenizer.ordinaryChar('*');
        tokenizer.ordinaryChar('/');
        tokenizer.ordinaryChar('=');
        tokenizer.ordinaryChar('<');
        tokenizer.ordinaryChar('>');

        Stack<Object> operators = new Stack<>();
        Stack<Object> operands = new Stack<>();

        Boolean isWordOperator = false;
        Boolean isOperand = false;
        Boolean isProcessed = false;
        Boolean isLeftPar = false;

        try {
            int tokenType = tokenizer.nextToken();

            while (tokenType != StreamTokenizer.TT_EOF) {
                LOGGER.debug("Current token {}", tokenType);
                isWordOperator = false;
                isOperand = false;
                isProcessed = false;
                isLeftPar = false;
                Object token = null;
                if (tokenType == StreamTokenizer.TT_WORD) {
                    token = tokenizer.sval;
                    LOGGER.debug("String token {}", token);
                    if (token.toString().equals("double")) {
                        if (tokenizer.nextToken() == '(') {
                            token = CONVERSION_DOUBLE;
                            isWordOperator = true;
                        } else {
                            tokenizer.pushBack();
                            LOGGER.error("Keyword double should be followed by (");
                            throw new ToscaPolicyConversionException("Keyword double should be followed by ( ");
                        }
                    } else if (token.toString().equals("integer")) {
                        if (tokenizer.nextToken() == '(') {
                            token = CONVERSION_INTEGER;
                            isWordOperator = true;
                        } else {
                            tokenizer.pushBack();
                            LOGGER.error("Keyword integer should be followed by ( ");
                            throw new ToscaPolicyConversionException("Keyword integer should be followed by ( ");
                        }
                    } else {
                        if (!(token.toString().equals("/"))) {
                            LOGGER.debug("Pushing String token into operand stack {}", token);
                            operands.push(token);
                            isOperand = true;
                        }
                    }
                } else if (tokenType == StreamTokenizer.TT_NUMBER) {
                    token = Double.valueOf(tokenizer.nval);
                    LOGGER.debug("Pushing Number token {}", token);
                    operands.push(token);
                    isOperand = true;
                }
                if (!isOperand) {
                    if (!isWordOperator) {
                        token = new Character((char) tokenType);
                        Boolean doubleOp = false;
                        LOGGER.debug("Char token {}", token);
                        char value = ((Character) token).charValue();
                        // Check for double character operators
                        if (value == '<' || value == '>' || value == '=' || value == '!') {
                            int checkNextToken = tokenizer.nextToken();
                            if (checkNextToken != StreamTokenizer.TT_NUMBER
                                && checkNextToken != StreamTokenizer.TT_WORD) {
                                if ((char) checkNextToken == '=') {
                                    token = token + "=";
                                }
                                doubleOp = true;
                            }
                            if (!doubleOp) {
                                tokenizer.pushBack();
                            }

                        } else if (value == '(') {
                            operators.push(token);
                            LOGGER.debug("Pushing Character token into operator stack {}", token);
                            isProcessed = true;
                        } else if (value == ')') {
                            Boolean single = false;
                            if (singleOperandExpression(operators.peek().toString())) {
                                single = true;
                            }
                            while (!operators.isEmpty() && !(isPreviousOpLeftPar(operators))) {
                                operands = processOperator(operators, operands, factory);

                            }
                            if (!operators.isEmpty() && !single) {
                                LOGGER.debug("Popping (");
                                operators.pop(); // Remove "("
                            }
                            isProcessed = true;

                        }

                    }
                    if (!isProcessed) {
                        if (isValidToken(token)) {
                            while (!operators.isEmpty()
                                && !(isPreviousOpLeftPar(operators))
                                && (getPrecedence(operators.peek()) >= getPrecedence(token))) {
                                operands = processOperator(operators, operands, factory);

                            }
                            operators.push(token);
                            LOGGER.debug("Pushing Character token into operator stack {}", token);
                        }
                    }
                }
                LOGGER.debug("Finished processing current token, going to next");
                tokenType = tokenizer.nextToken();
            }
            LOGGER.debug("Last token {}", tokenType);
            while (!operators.isEmpty()) {
                LOGGER.debug(
                    //when TT_EOF, process remaining tokens in stack
                    "Tokens are processed, now processing remaining operators");
                operands = processOperator(operators, operands, factory);
            }
        } catch (java.io.IOException ex) {
            LOGGER.error("convertToPrefixXACMLApply: Error while parsing expr");
            throw new ToscaPolicyConversionException("Error while parsing expr ");
        }
        if (!(operands.isEmpty())) {
            Object operand = operands.pop();
            if (operand instanceof String) {
                LOGGER.error("convertToPrefixXACMLApply: Extra operands. {}", operand);
                throw new ToscaPolicyConversionException("convertToPrefixXACMLApply: Extra operands.");
            } else {
                LOGGER.debug("Popped operand " + ((ApplyType) operand).getFunctionId());
                return (ApplyType) operand;
            }
        }
        return null;
    }

    private Boolean isPreviousOpLeftPar(Stack<Object> operators) {
        Object nextOp = operators.peek();
        if (nextOp instanceof Character) {
            if (((Character) nextOp).charValue() == '(') {
                LOGGER.debug("Previous operator is (");
                return true;
            }
        }
        return false;
    }

    private Boolean isValidToken(Object token) {
        String key = "";
        if (token instanceof Character) {
            key = ((Character) token).toString();
        } else if (token instanceof String) {
            key = token.toString();
        }
        return (operatorPrecedenceMap.containsKey(key));
    }

    private Integer getPrecedence(Object token)  {
        String key = "";
        if (token instanceof Character) {
            key = ((Character) token).toString();
        } else if (token instanceof String) {
            key = token.toString();
        }
        Integer precedence = operatorPrecedenceMap.get(key);
        LOGGER.debug("Precedence of operator " + key + " is " + precedence);
        return (precedence);
    }

    private Stack<Object> processOperator(Stack<Object> operators, Stack<Object> operands, ObjectFactory factory)
        throws ToscaPolicyConversionException {
        try {
            String op = "";
            Object opObj = operators.pop();
            if (opObj instanceof Character) {
                op = ((Character) opObj).toString();
            } else if (opObj instanceof String) {
                op = opObj.toString();
            } else {
                LOGGER.error("Invalid token in expr string {}", opObj.toString());
                throw new ToscaPolicyConversionException("Invalid token in expr string");
            }
            LOGGER.debug("Process Operator {}", op);

            if (singleOperandExpression(op)) {
                LOGGER.debug("processOperator: singleOperandExpression operator {}", op);
                Object val = operands.pop();
                if (val instanceof ApplyType) {
                    LOGGER.debug("Popped {}", ((ApplyType) val).getFunctionId());
                }
                var opApply = new ApplyType();
                if (val instanceof String) {
                    LOGGER.debug("processOperator: singleOperandExpression operand {}", val);
                    if (op.equals(CONVERSION_DOUBLE)) {
                        opApply =
                            createIntegerPropertyToDoubleConversionExpression(val.toString(), opApply, op, factory);
                    } else if (op.equals(CONVERSION_INTEGER)) {
                        opApply =
                            createDoublePropertyToIntegerConversionExpression(val.toString(), opApply, op, factory);
                    }
                } else {
                    opApply = createApplyExpression(val, opApply, op, factory);
                }
                opApply.setFunctionId(getOperatorXacmlMap(op).stringValue());
                operands.push(opApply);
                LOGGER.debug("Pushing operand {}", opApply.getFunctionId());
            } else {
                LOGGER.debug("processOperator: twoOperandExpression operator {}", op);
                Object val2 = operands.pop();
                if (val2 instanceof ApplyType) {
                    LOGGER.debug("Popped {}", ((ApplyType) val2).getFunctionId());
                }
                Object val1 = operands.pop();
                if (val1 instanceof ApplyType) {
                    LOGGER.debug("Popped {}", ((ApplyType) val1).getFunctionId());
                }
                var opApply = new ApplyType();
                opApply = createApplyExpression(val1, opApply, op, factory);
                opApply = createApplyExpression(val2, opApply, op, factory);
                opApply.setFunctionId(getOperatorXacmlMap(op).stringValue());
                operands.push(opApply);
                LOGGER.debug("Pushing operand {}", opApply.getFunctionId());
            }
        } catch (Exception ex) {
            LOGGER.error("Error while processing operator and operands in expr");
            throw new ToscaPolicyConversionException("Error while processing operator and operands in expr");
        }
        return operands;
    }

    private ApplyType createIntegerPropertyToDoubleConversionExpression(String val, ApplyType opApply, String op,
                                                                        ObjectFactory factory)
        throws ToscaPolicyConversionException {
        try {
            var oneAndOnlyApply = new ApplyType();
            var designator = setAttributeDesignatorType(val, XACML3.ID_ATTRIBUTE_CATEGORY_RESOURCE.stringValue(),
                XACML3.ID_DATATYPE_INTEGER.stringValue(), false);
            oneAndOnlyApply.getExpression().add(factory.createAttributeDesignator(designator));
            oneAndOnlyApply.setFunctionId(validateFilterPropertyFunction("integer" + ONE_AND_ONLY).stringValue());
            opApply.getExpression().add(factory.createApply(oneAndOnlyApply));
            opApply.setFunctionId(getOperatorXacmlMap(op).stringValue());
        } catch (ToscaPolicyConversionException ex) {
            LOGGER.error("Invalid integer property to double conversion, operator {} , value {}", op, val);
            throw new ToscaPolicyConversionException(
                "Error while parsing expr: invalid integer property to double conversion, operator "
                    + op
                    + ", value "
                    + val);
        }
        return opApply;
    }

    private ApplyType createDoublePropertyToIntegerConversionExpression(String val, ApplyType opApply, String op,
                                                                        ObjectFactory factory)
        throws ToscaPolicyConversionException {
        try {
            var oneAndOnlyApply = new ApplyType();
            var designator = setAttributeDesignatorType(val, XACML3.ID_ATTRIBUTE_CATEGORY_RESOURCE.stringValue(),
                XACML3.ID_DATATYPE_DOUBLE.stringValue(), false);
            oneAndOnlyApply.getExpression().add(factory.createAttributeDesignator(designator));
            oneAndOnlyApply.setFunctionId(validateFilterPropertyFunction("double" + ONE_AND_ONLY).stringValue());
            opApply.getExpression().add(factory.createApply(oneAndOnlyApply));
            opApply.setFunctionId(getOperatorXacmlMap(op).stringValue());
        } catch (ToscaPolicyConversionException ex) {
            LOGGER.error("Invalid integer property to double conversion, operator {} , value {}", op, val);
            throw new ToscaPolicyConversionException(
                "Error while parsing expr: invalid double property to integer conversion, operator "
                    + op
                    + ", value "
                    + val);
        }
        return opApply;
    }

    private ApplyType createApplyExpression(Object val, ApplyType opApply, String op, ObjectFactory factory)
        throws ToscaPolicyConversionException {
        try {
            if (val instanceof String) {
                var designator =
                    setAttributeDesignatorType((String) val, XACML3.ID_ATTRIBUTE_CATEGORY_RESOURCE.stringValue(),
                        XACML3.ID_DATATYPE_DOUBLE.stringValue(), false);
                var oneAndOnlyApply = new ApplyType();
                oneAndOnlyApply.setFunctionId(validateFilterPropertyFunction("double" + ONE_AND_ONLY).stringValue());
                oneAndOnlyApply.getExpression().add(factory.createAttributeDesignator(designator));
                opApply.getExpression().add(factory.createApply(oneAndOnlyApply));
            } else if (val instanceof Double) {
                var attributeValue = setAttributeValueType(val, XACML3.ID_DATATYPE_DOUBLE.stringValue());
                opApply.getExpression().add(factory.createAttributeValue(attributeValue));
            } else {
                opApply.getExpression().add(factory.createApply((ApplyType) val));
            }
            opApply.setFunctionId(getOperatorXacmlMap(op).stringValue());
        } catch (ToscaPolicyConversionException ex) {
            LOGGER.error("Error while parsing expr: creation of apply type in expr, operator {}", op);
            throw new ToscaPolicyConversionException(
                "Error while parsing expr: creation of apply type in expr, operator " + op);
        }
        return opApply;
    }

    @Getter
    public static class NativeDefinition {
        @NotNull
        @NotBlank
        private String policy;
    }

}