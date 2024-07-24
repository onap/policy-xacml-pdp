/*-
 * ============LICENSE_START=======================================================
 * ONAP
 * ================================================================================
 * Copyright (C) 2020-2021 AT&T Intellectual Property. All rights reserved.
 * Modifications Copyright (C) 2020, 2023-2024 Nordix Foundation.
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

import com.att.research.xacml.api.DataTypeException;
import com.att.research.xacml.api.Decision;
import com.att.research.xacml.api.Identifier;
import com.att.research.xacml.api.Request;
import com.att.research.xacml.api.Response;
import com.att.research.xacml.api.Result;
import com.att.research.xacml.api.XACML3;
import com.att.research.xacml.std.IdentifierImpl;
import com.att.research.xacml.std.annotations.RequestParser;
import com.google.gson.annotations.SerializedName;
import java.time.OffsetDateTime;
import java.time.OffsetTime;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import lombok.Getter;
import lombok.NoArgsConstructor;
import oasis.names.tc.xacml._3_0.core.schema.wd_17.AllOfType;
import oasis.names.tc.xacml._3_0.core.schema.wd_17.AnyOfType;
import oasis.names.tc.xacml._3_0.core.schema.wd_17.ApplyType;
import oasis.names.tc.xacml._3_0.core.schema.wd_17.AttributeDesignatorType;
import oasis.names.tc.xacml._3_0.core.schema.wd_17.AttributeValueType;
import oasis.names.tc.xacml._3_0.core.schema.wd_17.ConditionType;
import oasis.names.tc.xacml._3_0.core.schema.wd_17.EffectType;
import oasis.names.tc.xacml._3_0.core.schema.wd_17.MatchType;
import oasis.names.tc.xacml._3_0.core.schema.wd_17.ObjectFactory;
import oasis.names.tc.xacml._3_0.core.schema.wd_17.PolicyType;
import oasis.names.tc.xacml._3_0.core.schema.wd_17.RuleType;
import oasis.names.tc.xacml._3_0.core.schema.wd_17.TargetType;
import oasis.names.tc.xacml._3_0.core.schema.wd_17.VariableDefinitionType;
import oasis.names.tc.xacml._3_0.core.schema.wd_17.VariableReferenceType;
import org.onap.policy.common.parameters.annotations.NotBlank;
import org.onap.policy.common.parameters.annotations.NotNull;
import org.onap.policy.common.parameters.annotations.Valid;
import org.onap.policy.models.decisions.concepts.DecisionRequest;
import org.onap.policy.models.decisions.concepts.DecisionResponse;
import org.onap.policy.models.tosca.authorative.concepts.ToscaPolicy;
import org.onap.policy.pdp.xacml.application.common.ToscaDictionary;
import org.onap.policy.pdp.xacml.application.common.ToscaPolicyConversionException;
import org.onap.policy.pdp.xacml.application.common.ToscaPolicyTranslator;
import org.onap.policy.pdp.xacml.application.common.ToscaPolicyTranslatorUtils;
import org.onap.policy.pdp.xacml.application.common.operationshistory.CountRecentOperationsPip;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@NoArgsConstructor
public class GuardTranslator implements ToscaPolicyTranslator {
    private static final Logger LOGGER = LoggerFactory.getLogger(GuardTranslator.class);

    //
    // common guard property fields
    //
    public static final String FIELD_CONTROLLOOP = "id";
    public static final String FIELD_TIMERANGE = "timeRange";

    //
    // minmax property fields
    //
    public static final String FIELD_TARGET = "target";
    public static final String FIELD_MIN = "min";
    public static final String FIELD_MAX = "max";

    //
    // blacklist property fields
    //
    public static final String FIELD_BLACKLIST = "blacklist";

    public static final String POLICYTYPE_FREQUENCY = "onap.policies.controlloop.guard.common.FrequencyLimiter";
    public static final String POLICYTYPE_MINMAX = "onap.policies.controlloop.guard.common.MinMax";
    public static final String POLICYTYPE_BLACKLIST = "onap.policies.controlloop.guard.common.Blacklist";
    public static final String POLICYTYPE_FILTER = "onap.policies.controlloop.guard.common.Filter";

    //
    // Variable definitions
    //
    private static final String VARIABLE_TIMEINRANGE = "timeInRange";


    /**
     * Convert the policy.
     */
    @Override
    public Object convertPolicy(ToscaPolicy toscaPolicy) throws ToscaPolicyConversionException {
        //
        // Policy name should be at the root
        //
        String policyName = String.valueOf(toscaPolicy.getMetadata().get("policy-id"));
        //
        // Set it as the policy ID
        //
        var newPolicyType = new PolicyType();
        newPolicyType.setPolicyId(policyName);
        //
        // Optional description
        //
        newPolicyType.setDescription(toscaPolicy.getDescription());
        //
        // There should be a metadata section
        //
        this.fillMetadataSection(newPolicyType, toscaPolicy.getMetadata());
        //
        // There should be properties metadata section
        //
        if (toscaPolicy.getProperties() == null) {
            throw new ToscaPolicyConversionException("no properties specified on guard policy: " + policyName);
        }
        //
        // Generate the TargetType - add true if not blacklist
        //
        newPolicyType.setTarget(this.generateTargetType(toscaPolicy.getProperties(),
            !POLICYTYPE_BLACKLIST.equals(toscaPolicy.getType())));
        //
        // Add specific's per guard policy type
        //
        if (POLICYTYPE_FREQUENCY.equals(toscaPolicy.getType())) {
            newPolicyType.setRuleCombiningAlgId(XACML3.ID_RULE_DENY_UNLESS_PERMIT.stringValue());
            generateFrequencyRules(toscaPolicy, policyName, newPolicyType);
        } else if (POLICYTYPE_MINMAX.equals(toscaPolicy.getType())) {
            newPolicyType.setRuleCombiningAlgId(XACML3.ID_RULE_DENY_UNLESS_PERMIT.stringValue());
            generateMinMaxRules(toscaPolicy, policyName, newPolicyType);
        } else if (POLICYTYPE_BLACKLIST.equals(toscaPolicy.getType())) {
            newPolicyType.setRuleCombiningAlgId(XACML3.ID_RULE_PERMIT_UNLESS_DENY.stringValue());
            generateBlacklistRules(toscaPolicy, policyName, newPolicyType);
        } else if (POLICYTYPE_FILTER.equals(toscaPolicy.getType())) {
            newPolicyType.setRuleCombiningAlgId(XACML3.ID_RULE_PERMIT_UNLESS_DENY.stringValue());
            generateFilterRules(toscaPolicy, policyName, newPolicyType);
        } else {
            throw new ToscaPolicyConversionException("Unknown guard policy type " + toscaPolicy.getType());
        }
        //
        // Add in our variable definition
        //
        VariableReferenceType variable = this.createTimeRangeVariable(toscaPolicy.getProperties(), newPolicyType);
        if (variable != null) {
            //
            // Update all the rules to have conditions for this variable
            //
            this.addVariableToConditionTypes(variable, newPolicyType);
        }
        return newPolicyType;
    }

    /**
     * This method iterates through all the existing rules, adding in a conditionType that will test
     * whether the Variable is true or false. Any existing ConditionType will be updated to AND with the
     * Variable.
     *
     * @param variable      VariableDefinitionType to add
     * @param newPolicyType PolicyType that will be updated
     */
    protected void addVariableToConditionTypes(VariableReferenceType variable,
                                               PolicyType newPolicyType) {
        //
        // Iterate through the rules
        //
        for (Object objectType : newPolicyType.getCombinerParametersOrRuleCombinerParametersOrVariableDefinition()) {
            if (objectType instanceof RuleType rule) {
                if (rule.getCondition() == null) {
                    //
                    // No condition already, just create and add a new one
                    //
                    var condition = new ConditionType();
                    condition.setExpression(new ObjectFactory().createVariableReference(variable));
                    rule.setCondition(condition);
                } else {
                    //
                    // Need to create a new ConditionType that treats all the expressions as an AND
                    // with the Variable.
                    //
                    rule.setCondition(ToscaPolicyTranslatorUtils.addVariableToCondition(rule.getCondition(), variable,
                        XACML3.ID_FUNCTION_AND));
                }
            }
        }
    }

    /**
     * Convert Request.
     */
    @Override
    public Request convertRequest(DecisionRequest request) throws ToscaPolicyConversionException {
        LOGGER.info("Converting Request {}", request);
        try {
            return RequestParser.parseRequest(GuardPolicyRequest.createInstance(request));
        } catch (IllegalArgumentException | IllegalAccessException | DataTypeException e) {
            throw new ToscaPolicyConversionException("Failed to convert DecisionRequest", e);
        }
    }

    /**
     * Convert response.
     */
    @Override
    public DecisionResponse convertResponse(Response xacmlResponse) {
        LOGGER.info("Converting Response {}", xacmlResponse);
        var decisionResponse = new DecisionResponse();
        //
        // Iterate through all the results
        //
        for (Result xacmlResult : xacmlResponse.getResults()) {
            //
            // Check the result
            //
            if (xacmlResult.getDecision() == Decision.PERMIT) {
                //
                // Just simply return a Permit response
                //
                decisionResponse.setStatus(Decision.PERMIT.toString());
            } else if (xacmlResult.getDecision() == Decision.DENY) {
                //
                // Just simply return a Deny response
                //
                decisionResponse.setStatus(Decision.DENY.toString());
            } else {
                //
                // There is no guard policy, so we return a permit
                //
                decisionResponse.setStatus(Decision.PERMIT.toString());
            }
        }

        return decisionResponse;
    }

    /**
     * From the TOSCA metadata section, pull in values that are needed into the XACML policy.
     *
     * @param policy Policy Object to store the metadata
     * @param map    The Metadata TOSCA Map
     * @return Same Policy Object
     */
    protected PolicyType fillMetadataSection(PolicyType policy, Map<String, Object> map) {
        //
        // NOTE: The models code ensures the metadata section ALWAYS exists
        //
        //
        // Add in the Policy Version
        //
        policy.setVersion(String.valueOf(map.get("policy-version")));
        return policy;
    }

    /**
     * Generate the targetType for the policy. Optional to add MatchType for the target. e.g. the
     * blacklist policy type uses the target in a different manner.
     *
     * @param properties TOSCA properties object
     * @param addTargets true to go ahead and add target to the match list.
     * @return TargetType object
     * @throws ToscaPolicyConversionException if there is a missing property
     */
    protected TargetType generateTargetType(Map<String, Object> properties, boolean addTargets)
        throws ToscaPolicyConversionException {
        //
        // Decode the definition from the policy's properties
        //
        TargetTypeDefinition targetTypeDef =
            ToscaPolicyTranslatorUtils.decodeProperties(properties, TargetTypeDefinition.class);
        //
        // Go through potential properties
        //
        var allOf = new AllOfType();
        if (targetTypeDef.getActor() != null) {
            addMatch(allOf, targetTypeDef.getActor(), ToscaDictionary.ID_RESOURCE_GUARD_ACTOR);
        }
        if (targetTypeDef.getOperation() != null) {
            addMatch(allOf, targetTypeDef.getOperation(), ToscaDictionary.ID_RESOURCE_GUARD_RECIPE);
        }
        if (addTargets && targetTypeDef.getTarget() != null) {
            addMatch(allOf, targetTypeDef.getTarget(), ToscaDictionary.ID_RESOURCE_GUARD_TARGETID);
        }
        if (targetTypeDef.getId() != null) {
            addMatch(allOf, targetTypeDef.getId(), ToscaDictionary.ID_RESOURCE_GUARD_CLNAME);
        }
        //
        // Create target
        //
        var target = new TargetType();
        var anyOf = new AnyOfType();
        anyOf.getAllOf().add(allOf);
        target.getAnyOf().add(anyOf);
        return target;
    }

    protected AllOfType addMatch(AllOfType allOf, Object value, Identifier attributeId) {
        if (value instanceof String) {
            if (".*".equals(value.toString())) {
                //
                // There's no point to even have a match
                //
                return allOf;
            } else {
                //
                // Exact match
                //
                var match = ToscaPolicyTranslatorUtils.buildMatchTypeDesignator(
                    XACML3.ID_FUNCTION_STRING_EQUAL,
                    value,
                    XACML3.ID_DATATYPE_STRING,
                    attributeId,
                    XACML3.ID_ATTRIBUTE_CATEGORY_RESOURCE);

                allOf.getMatch().add(match);
            }
            return allOf;
        }
        if (value instanceof Collection<?> collection) {
            collection.forEach(val -> {
                var match = ToscaPolicyTranslatorUtils.buildMatchTypeDesignator(
                    XACML3.ID_FUNCTION_STRING_EQUAL,
                    val,
                    XACML3.ID_DATATYPE_STRING,
                    attributeId,
                    XACML3.ID_ATTRIBUTE_CATEGORY_RESOURCE);

                allOf.getMatch().add(match);
            });
        }
        return allOf;
    }

    protected VariableReferenceType createTimeRangeVariable(Map<String, Object> properties, PolicyType newPolicyType)
        throws ToscaPolicyConversionException {
        //
        // Decode the definition from the policy's properties
        //
        TimeRangeDefinition timeRangeDef =
            ToscaPolicyTranslatorUtils.decodeProperties(properties, TimeRangeDefinition.class);
        TimeRange timeRange = timeRangeDef.getTimeRange();
        if (timeRange == null) {
            return null;
        }
        //
        // Should also be parseable as an ISO8601 timestamp
        //
        var startTimeObject = parseTimestamp(timeRange.getStartTime());
        var endTimeObject = parseTimestamp(timeRange.getEndTime());
        //
        // They should be the same object types. We cannot establish a range
        // between an OffsetDateTime and an OffsetTime
        //
        if (!startTimeObject.getClass().equals(endTimeObject.getClass())) {
            throw new ToscaPolicyConversionException("start_time and end_time class types do not match");
        }
        //
        // Create the inner timeInRange ApplyType
        //
        ApplyType timeInRange = ToscaPolicyTranslatorUtils.generateTimeInRange(timeRange.getStartTime(),
            timeRange.getEndTime(), true);
        var variable = new VariableDefinitionType();
        variable.setVariableId(VARIABLE_TIMEINRANGE);
        variable.setExpression(new ObjectFactory().createApply(timeInRange));
        //
        // Add it to the policy
        //
        newPolicyType.getCombinerParametersOrRuleCombinerParametersOrVariableDefinition().add(variable);
        //
        // Create and return the reference to the variable
        //
        var reference = new VariableReferenceType();
        reference.setVariableId(variable.getVariableId());
        return reference;
    }

    protected Object parseTimestamp(String string) throws ToscaPolicyConversionException {
        //
        // First see if it is a full datetime object
        //
        try {
            return OffsetDateTime.parse(string);
        } catch (Exception e) {
            LOGGER.warn("timestamp {} could not be parsed. This may not be an error.", string, e);
        }
        //
        // May only be a time object
        //
        try {
            return OffsetTime.parse(string);
        } catch (Exception e) {
            throw new ToscaPolicyConversionException("timestamp " + string + " could not be parsed ", e);
        }
    }

    protected void generateFrequencyRules(ToscaPolicy toscaPolicy, String policyName, PolicyType newPolicyType)
        throws ToscaPolicyConversionException {
        //
        // Decode the definition from the policy's properties
        //
        FrequencyDefinition frequencyDef = ToscaPolicyTranslatorUtils.decodeProperties(toscaPolicy.getProperties(),
            FrequencyDefinition.class);
        //
        // See if it's possible to generate a count
        //
        String timeWindow = null;
        if (frequencyDef.getTimeWindow() != null) {
            timeWindow = frequencyDef.getTimeWindow().toString();
        }
        //
        // Generate a count
        //
        final ApplyType countCheck =
            generateCountCheck(frequencyDef.getLimit(), timeWindow, frequencyDef.getTimeUnits());
        //
        // Create our condition
        //
        final var condition = new ConditionType();
        condition.setExpression(new ObjectFactory().createApply(countCheck));

        //
        // Now we can create our rule
        //
        var frequencyRule = new RuleType();
        frequencyRule.setDescription("Frequency limit permit rule");
        frequencyRule.setRuleId(policyName + ":frequency");
        frequencyRule.setEffect(EffectType.PERMIT);
        frequencyRule.setTarget(new TargetType());
        //
        // Add the condition
        //
        frequencyRule.setCondition(condition);
        //
        // Add the rule to the policy
        //
        newPolicyType.getCombinerParametersOrRuleCombinerParametersOrVariableDefinition().add(frequencyRule);
    }

    protected ApplyType generateCountCheck(Integer limit, String timeWindow, String timeUnits) {
        var designator = new AttributeDesignatorType();
        designator.setAttributeId(ToscaDictionary.ID_RESOURCE_GUARD_OPERATIONCOUNT.stringValue());
        designator.setCategory(XACML3.ID_ATTRIBUTE_CATEGORY_RESOURCE.stringValue());
        designator.setDataType(XACML3.ID_DATATYPE_INTEGER.stringValue());
        //
        // Setup issuer - used by the operations PIP to determine
        // how to do the database query.
        //
        String issuer = ToscaDictionary.GUARD_ISSUER_PREFIX
            + CountRecentOperationsPip.ISSUER_NAME
            + ":tw:" + timeWindow + ":" + timeUnits;
        designator.setIssuer(issuer);

        var valueLimit = new AttributeValueType();
        valueLimit.setDataType(XACML3.ID_DATATYPE_INTEGER.stringValue());
        //
        // Yes really use toString(), the marshaller will
        // throw an exception if this is an integer object
        // and not a string.
        //
        valueLimit.getContent().add(limit.toString());

        var factory = new ObjectFactory();

        var applyOneAndOnly = new ApplyType();
        applyOneAndOnly.setDescription("Unbag the limit");
        applyOneAndOnly.setFunctionId(XACML3.ID_FUNCTION_INTEGER_ONE_AND_ONLY.stringValue());
        applyOneAndOnly.getExpression().add(factory.createAttributeDesignator(designator));

        var applyLessThan = new ApplyType();
        applyLessThan.setDescription("return true if current count is less than.");
        applyLessThan.setFunctionId(XACML3.ID_FUNCTION_INTEGER_LESS_THAN.stringValue());
        applyLessThan.getExpression().add(factory.createApply(applyOneAndOnly));
        applyLessThan.getExpression().add(factory.createAttributeValue(valueLimit));

        return applyLessThan;
    }

    protected void generateMinMaxRules(ToscaPolicy toscaPolicy, String policyName, PolicyType newPolicyType)
        throws ToscaPolicyConversionException {
        //
        // Decode the definition from the policy's properties
        //
        MinMaxDefinition minMaxDef = ToscaPolicyTranslatorUtils.decodeProperties(toscaPolicy.getProperties(),
            MinMaxDefinition.class);
        //
        // Add the target
        //
        var matchTarget = ToscaPolicyTranslatorUtils.buildMatchTypeDesignator(
            XACML3.ID_FUNCTION_STRING_EQUAL,
            minMaxDef.getTarget(),
            XACML3.ID_DATATYPE_STRING,
            ToscaDictionary.ID_RESOURCE_GUARD_TARGETID,
            XACML3.ID_ATTRIBUTE_CATEGORY_RESOURCE);
        //
        // For the min, if the # of instances is less than the minimum
        // then allow the scale.
        //
        if (minMaxDef.getMin() != null) {
            var matchMin = ToscaPolicyTranslatorUtils.buildMatchTypeDesignator(
                XACML3.ID_FUNCTION_INTEGER_GREATER_THAN,
                minMaxDef.getMin().toString(),
                XACML3.ID_DATATYPE_INTEGER,
                ToscaDictionary.ID_RESOURCE_GUARD_VFCOUNT,
                XACML3.ID_ATTRIBUTE_CATEGORY_RESOURCE);

            newPolicyType.getCombinerParametersOrRuleCombinerParametersOrVariableDefinition().add(
                generateMinMaxRule(matchTarget, matchMin, policyName + ":minrule", "check minimum"));
        }
        if (minMaxDef.getMax() != null) {
            var matchMax = ToscaPolicyTranslatorUtils.buildMatchTypeDesignator(
                XACML3.ID_FUNCTION_INTEGER_GREATER_THAN,
                minMaxDef.getMax().toString(),
                XACML3.ID_DATATYPE_INTEGER,
                ToscaDictionary.ID_RESOURCE_GUARD_VFCOUNT,
                XACML3.ID_ATTRIBUTE_CATEGORY_RESOURCE);

            newPolicyType.getCombinerParametersOrRuleCombinerParametersOrVariableDefinition().add(
                generateMinMaxRule(matchTarget, matchMax, policyName + ":maxrule", "check maximum"));
        }
        //
        // Do we have at least a min or max?
        //
        if (minMaxDef.getMin() == null && minMaxDef.getMax() == null) {
            throw new ToscaPolicyConversionException("Missing min or max field in minmax policy");
        }
    }

    protected RuleType generateMinMaxRule(MatchType matchTarget, MatchType matchMinOrMax, String ruleId, String desc) {
        var allOf = new AllOfType();
        allOf.getMatch().add(matchTarget);
        allOf.getMatch().add(matchMinOrMax);
        var anyOf = new AnyOfType();
        anyOf.getAllOf().add(allOf);
        var target = new TargetType();
        target.getAnyOf().add(anyOf);
        var minMaxRule = new RuleType();
        minMaxRule.setEffect(EffectType.PERMIT);
        minMaxRule.setDescription(desc);
        minMaxRule.setRuleId(ruleId);
        minMaxRule.setTarget(target);
        return minMaxRule;
    }

    protected void generateBlacklistRules(ToscaPolicy toscaPolicy, String policyName, PolicyType newPolicyType)
        throws ToscaPolicyConversionException {
        //
        // Decode the definition from the policy's properties
        //
        BlacklistDefinition blacklistDef = ToscaPolicyTranslatorUtils.decodeProperties(toscaPolicy.getProperties(),
            BlacklistDefinition.class);
        //
        // Iterate the entries and create individual AnyOf so each entry is
        // treated as an OR.
        //
        var target = new TargetType();
        var anyOf = new AnyOfType();
        for (Object blacklisted : blacklistDef.blacklist) {
            var allOf = new AllOfType();
            this.addMatch(allOf, blacklisted, ToscaDictionary.ID_RESOURCE_GUARD_TARGETID);
            anyOf.getAllOf().add(allOf);
        }
        target.getAnyOf().add(anyOf);
        //
        // Create our rule and add the target
        //
        var blacklistRule = new RuleType();
        blacklistRule.setEffect(EffectType.DENY);
        blacklistRule.setDescription("blacklist the entities");
        blacklistRule.setRuleId(policyName + ":blacklist");
        blacklistRule.setTarget(target);
        //
        // Add the rule to the policy
        //
        newPolicyType.getCombinerParametersOrRuleCombinerParametersOrVariableDefinition().add(blacklistRule);
    }

    protected void generateFilterRules(ToscaPolicy toscaPolicy, String policyName, PolicyType newPolicyType)
        throws ToscaPolicyConversionException {
        //
        // Decode the definition from the policy's properties
        //
        FilterDefinition filterDef = ToscaPolicyTranslatorUtils.decodeProperties(toscaPolicy.getProperties(),
            FilterDefinition.class);
        //
        // Set the combining algorithm
        //
        switch (filterDef.getAlgorithm()) {
            case "whitelist-overrides":
                newPolicyType.setRuleCombiningAlgId(XACML3.ID_RULE_PERMIT_OVERRIDES.stringValue());
                break;
            case "blacklist-overrides":
                newPolicyType.setRuleCombiningAlgId(XACML3.ID_RULE_DENY_OVERRIDES.stringValue());
                break;
            default:
                throw new ToscaPolicyConversionException(
                    "Unexpected value for algorithm, should be whitelist-overrides or blacklist-overrides");
        }
        //
        // Iterate the filters
        //
        var ruleId = 1;
        for (FilterAttribute filterAttributes : filterDef.filters) {
            //
            // Check fields requiring extra validation
            //
            String field = validateFilterPropertyField(filterAttributes.getField());
            Identifier function = validateFilterPropertyFunction(filterAttributes.getFunction());
            //
            // Create our filter rule
            //
            RuleType filterRule = createFilterRule(policyName + ":rule" + ruleId++, field, filterAttributes.getFilter(),
                function, filterAttributes.getBlacklist());
            //
            // Add the rule to the policy
            //
            newPolicyType.getCombinerParametersOrRuleCombinerParametersOrVariableDefinition().add(filterRule);
        }
    }

    protected String validateFilterPropertyField(String field)
        throws ToscaPolicyConversionException {
        String fieldLowerCase = field.toLowerCase();
        return switch (fieldLowerCase) {
            case "generic-vnf.vnf-name", "generic-vnf.vnf-id", "generic-vnf.vnf-type", "generic-vnf.nf-naming-code",
                "vserver.vserver-id", "cloud-region.cloud-region-id" -> fieldLowerCase;
            default -> throw new ToscaPolicyConversionException("Unexpected value for field in filter");
        };
    }

    protected Identifier validateFilterPropertyFunction(String function)
        throws ToscaPolicyConversionException {
        return switch (function.toLowerCase()) {
            case "string-equal" -> XACML3.ID_FUNCTION_STRING_EQUAL;
            case "string-equal-ignore-case" -> XACML3.ID_FUNCTION_STRING_EQUAL_IGNORE_CASE;
            case "string-regexp-match" -> XACML3.ID_FUNCTION_STRING_REGEXP_MATCH;
            case "string-contains" -> XACML3.ID_FUNCTION_STRING_CONTAINS;
            case "string-greater-than" -> XACML3.ID_FUNCTION_STRING_GREATER_THAN;
            case "string-greater-than-or-equal" -> XACML3.ID_FUNCTION_STRING_GREATER_THAN_OR_EQUAL;
            case "string-less-than" -> XACML3.ID_FUNCTION_STRING_LESS_THAN;
            case "string-less-than-or-equal" -> XACML3.ID_FUNCTION_STRING_LESS_THAN_OR_EQUAL;
            case "string-starts-with" -> XACML3.ID_FUNCTION_STRING_STARTS_WITH;
            case "string-ends-with" -> XACML3.ID_FUNCTION_STRING_ENDS_WITH;
            default -> throw new ToscaPolicyConversionException("Unexpected value for function in filter");
        };
    }

    protected RuleType createFilterRule(String ruleId, String field, String filter, Identifier function,
                                        boolean isBlacklisted) {
        var rule = new RuleType();
        rule.setRuleId(ruleId);

        //
        // Create the Match
        //
        var matchFilter = ToscaPolicyTranslatorUtils.buildMatchTypeDesignator(
            function,
            filter,
            XACML3.ID_DATATYPE_STRING,
            new IdentifierImpl(GuardPolicyRequest.PREFIX_RESOURCE_ATTRIBUTE_ID + field),
            XACML3.ID_ATTRIBUTE_CATEGORY_RESOURCE
        );
        var allOf = new AllOfType();
        allOf.getMatch().add(matchFilter);
        var anyOf = new AnyOfType();
        anyOf.getAllOf().add(allOf);
        var target = new TargetType();
        target.getAnyOf().add(anyOf);

        rule.setTarget(target);

        if (isBlacklisted) {
            rule.setEffect(EffectType.DENY);
        } else {
            rule.setEffect(EffectType.PERMIT);
        }
        return rule;
    }

    @Getter
    public static class TimeRangeDefinition {
        private @Valid TimeRange timeRange;
    }

    @Getter
    public static class TargetTypeDefinition {
        private String actor;
        private String operation;
        private String target;
        private String id;
    }

    @Getter
    @NotNull
    @NotBlank
    public static class TimeRange {
        @SerializedName("start_time")
        private String startTime;

        @SerializedName("end_time")
        private String endTime;
    }

    @Getter
    public static class FrequencyDefinition {
        @NotNull
        private Integer limit;
        private Integer timeWindow;
        private String timeUnits;
    }

    @Getter
    public static class MinMaxDefinition {
        @NotNull
        private String target;
        private Integer min;
        private Integer max;
    }

    @Getter
    @NotNull
    public static class BlacklistDefinition {
        private List<@NotNull Object> blacklist;
    }

    @Getter
    @NotNull
    public static class FilterDefinition {
        private String algorithm;
        private List<@NotNull @Valid FilterAttribute> filters;
    }

    @Getter
    @NotNull
    public static class FilterAttribute {
        private String field;
        private String filter;
        private String function;
        private Boolean blacklist;
    }
}
