/*-
 * ============LICENSE_START=======================================================
 * ONAP
 * ================================================================================
 * Copyright (C) 2020 AT&T Intellectual Property. All rights reserved.
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
import java.util.Collection;
import java.util.Map;
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

public class GuardTranslator implements ToscaPolicyTranslator {
    private static final Logger LOGGER = LoggerFactory.getLogger(GuardTranslator.class);

    //
    // common guard property fields
    //
    public static final String FIELD_ACTOR = "actor";
    public static final String FIELD_OPERATION = "operation";
    public static final String FIELD_CONTROLLOOP = "id";
    public static final String FIELD_TIMERANGE = "timeRange";

    //
    // frequency property fields
    //
    public static final String FIELD_TIMEWINDOW = "timeWindow";
    public static final String FIELD_TIMEUNITS = "timeUnits";
    public static final String FIELD_LIMIT = "limit";

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

    //
    // filter property fields
    //
    public static final String FIELD_FILTER_WHITELIST = "whitelist";
    public static final String FIELD_FILTER_ALGORITHM = "algorithm";
    public static final String FIELD_FILTER_FILTERS = "filters";
    public static final String FIELD_FILTER_FIELD = "field";
    public static final String FIELD_FILTER_FUNCTION = "function";
    public static final String FIELD_FILTER_FILTER = "filter";
    public static final String FIELD_FILTER_BLACKLIST = "blacklist";

    public static final String POLICYTYPE_FREQUENCY = "onap.policies.controlloop.guard.common.FrequencyLimiter";
    public static final String POLICYTYPE_MINMAX = "onap.policies.controlloop.guard.common.MinMax";
    public static final String POLICYTYPE_BLACKLIST = "onap.policies.controlloop.guard.common.Blacklist";
    public static final String POLICYTYPE_FILTER = "onap.policies.controlloop.guard.common.Filter";

    public GuardTranslator() {
        super();
    }

    /**
     * Convert the policy.
     */
    @Override
    public Object convertPolicy(ToscaPolicy toscaPolicy) throws ToscaPolicyConversionException {
        //
        // Policy name should be at the root
        //
        String policyName = toscaPolicy.getMetadata().get("policy-id");
        //
        // Set it as the policy ID
        //
        PolicyType newPolicyType = new PolicyType();
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
                ! POLICYTYPE_BLACKLIST.equals(toscaPolicy.getType())));
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
        return newPolicyType;
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
        DecisionResponse decisionResponse = new DecisionResponse();
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
     * @param map The Metadata TOSCA Map
     * @return Same Policy Object
     */
    protected PolicyType fillMetadataSection(PolicyType policy, Map<String, String> map) {
        //
        // NOTE: The models code ensures the metadata section ALWAYS exists
        //
        //
        // Add in the Policy Version
        //
        policy.setVersion(map.get("policy-version"));
        return policy;
    }

    /**
     * Generate the targettype for the policy. Optional to add MatchType for the target. eg. the
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
        // Go through potential properties
        //
        AllOfType allOf = new AllOfType();
        if (properties.containsKey(FIELD_ACTOR)) {
            addMatch(allOf, properties.get(FIELD_ACTOR), ToscaDictionary.ID_RESOURCE_GUARD_ACTOR);
        }
        if (properties.containsKey(FIELD_OPERATION)) {
            addMatch(allOf, properties.get(FIELD_OPERATION), ToscaDictionary.ID_RESOURCE_GUARD_RECIPE);
        }
        if (addTargets && properties.containsKey(FIELD_TARGET)) {
            addMatch(allOf, properties.get(FIELD_TARGET), ToscaDictionary.ID_RESOURCE_GUARD_TARGETID);
        }
        if (properties.containsKey(FIELD_CONTROLLOOP)) {
            addMatch(allOf, properties.get(FIELD_CONTROLLOOP), ToscaDictionary.ID_RESOURCE_GUARD_CLNAME);
        }
        if (properties.containsKey(FIELD_TIMERANGE)) {
            addTimeRangeMatch(allOf, properties.get(FIELD_TIMERANGE));
        }
        //
        // Create target
        //
        TargetType target = new TargetType();
        AnyOfType anyOf = new AnyOfType();
        anyOf.getAllOf().add(allOf);
        target.getAnyOf().add(anyOf);
        return target;
    }

    @SuppressWarnings("unchecked")
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
                MatchType match = ToscaPolicyTranslatorUtils.buildMatchTypeDesignator(
                    XACML3.ID_FUNCTION_STRING_EQUAL,
                    value,
                    XACML3.ID_DATATYPE_STRING,
                    attributeId,
                    XACML3.ID_ATTRIBUTE_CATEGORY_RESOURCE);

                allOf.getMatch().add(match);
            }
            return allOf;
        }
        if (value instanceof Collection) {
            ((Collection<String>) value).forEach(val -> {
                MatchType match = ToscaPolicyTranslatorUtils.buildMatchTypeDesignator(
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

    @SuppressWarnings("rawtypes")
    protected void addTimeRangeMatch(AllOfType allOf, Object timeRange)
            throws ToscaPolicyConversionException {
        if (! (timeRange instanceof Map)) {
            throw new ToscaPolicyConversionException("timeRange is not a map object " + timeRange.getClass());
        }

        MatchType matchStart = ToscaPolicyTranslatorUtils.buildMatchTypeDesignator(
                XACML3.ID_FUNCTION_TIME_GREATER_THAN_OR_EQUAL,
                ((Map) timeRange).get("start_time").toString(),
                XACML3.ID_DATATYPE_TIME,
                XACML3.ID_ENVIRONMENT_CURRENT_TIME,
                XACML3.ID_ATTRIBUTE_CATEGORY_RESOURCE);

        allOf.getMatch().add(matchStart);

        MatchType matchEnd = ToscaPolicyTranslatorUtils.buildMatchTypeDesignator(
                XACML3.ID_FUNCTION_TIME_LESS_THAN_OR_EQUAL,
                ((Map) timeRange).get("end_time").toString(),
                XACML3.ID_DATATYPE_TIME,
                XACML3.ID_ENVIRONMENT_CURRENT_TIME,
                XACML3.ID_ATTRIBUTE_CATEGORY_ENVIRONMENT);

        allOf.getMatch().add(matchEnd);
    }

    protected void generateFrequencyRules(ToscaPolicy toscaPolicy, String policyName, PolicyType newPolicyType)
            throws ToscaPolicyConversionException {
        //
        // We must have the limit
        //
        if (! toscaPolicy.getProperties().containsKey(FIELD_LIMIT)) {
            throw new ToscaPolicyConversionException("Missing property limit");
        }
        //
        // See if its possible to generate a count
        //
        Integer limit = ToscaPolicyTranslatorUtils.parseInteger(
                toscaPolicy.getProperties().get(FIELD_LIMIT).toString());
        if (limit == null) {
            throw new ToscaPolicyConversionException("Missing limit value");
        }
        String timeWindow = null;
        if (toscaPolicy.getProperties().containsKey(FIELD_TIMEWINDOW)) {
            Integer intTimeWindow = ToscaPolicyTranslatorUtils.parseInteger(
                    toscaPolicy.getProperties().get(FIELD_TIMEWINDOW).toString());
            if (intTimeWindow == null) {
                throw new ToscaPolicyConversionException("timeWindow is not an integer");
            }
            timeWindow = intTimeWindow.toString();
        }
        String timeUnits = null;
        if (toscaPolicy.getProperties().containsKey(FIELD_TIMEUNITS)) {
            timeUnits = toscaPolicy.getProperties().get(FIELD_TIMEUNITS).toString();
        }
        //
        // Generate a count
        //
        final ApplyType countCheck = generateCountCheck(limit, timeWindow, timeUnits);
        //
        // Create our condition
        //
        final ConditionType condition = new ConditionType();
        condition.setExpression(new ObjectFactory().createApply(countCheck));

        //
        // Now we can create our rule
        //
        RuleType frequencyRule = new RuleType();
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
        AttributeDesignatorType designator = new AttributeDesignatorType();
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

        AttributeValueType valueLimit = new AttributeValueType();
        valueLimit.setDataType(XACML3.ID_DATATYPE_INTEGER.stringValue());
        //
        // Yes really use toString(), the marshaller will
        // throw an exception if this is an integer object
        // and not a string.
        //
        valueLimit.getContent().add(limit.toString());

        ObjectFactory factory = new ObjectFactory();

        ApplyType applyOneAndOnly = new ApplyType();
        applyOneAndOnly.setDescription("Unbag the limit");
        applyOneAndOnly.setFunctionId(XACML3.ID_FUNCTION_INTEGER_ONE_AND_ONLY.stringValue());
        applyOneAndOnly.getExpression().add(factory.createAttributeDesignator(designator));

        ApplyType applyLessThan = new ApplyType();
        applyLessThan.setDescription("return true if current count is less than.");
        applyLessThan.setFunctionId(XACML3.ID_FUNCTION_INTEGER_LESS_THAN.stringValue());
        applyLessThan.getExpression().add(factory.createApply(applyOneAndOnly));
        applyLessThan.getExpression().add(factory.createAttributeValue(valueLimit));

        return applyLessThan;
    }

    protected void generateMinMaxRules(ToscaPolicy toscaPolicy, String policyName, PolicyType newPolicyType)
            throws ToscaPolicyConversionException {
        //
        // Add the target
        //
        if (! toscaPolicy.getProperties().containsKey(FIELD_TARGET)) {
            throw new ToscaPolicyConversionException("Missing target field in minmax policy");
        }
        MatchType matchTarget = ToscaPolicyTranslatorUtils.buildMatchTypeDesignator(
                XACML3.ID_FUNCTION_STRING_EQUAL,
                toscaPolicy.getProperties().get(FIELD_TARGET).toString(),
                XACML3.ID_DATATYPE_STRING,
                ToscaDictionary.ID_RESOURCE_GUARD_TARGETID,
                XACML3.ID_ATTRIBUTE_CATEGORY_RESOURCE);
        //
        // For the min, if the # of instances is less than the minimum
        // then allow the scale.
        //
        Integer min = null;
        if (toscaPolicy.getProperties().containsKey(FIELD_MIN)) {
            min = ToscaPolicyTranslatorUtils.parseInteger(toscaPolicy.getProperties().get(FIELD_MIN).toString());
            MatchType matchMin = ToscaPolicyTranslatorUtils.buildMatchTypeDesignator(
                    XACML3.ID_FUNCTION_INTEGER_GREATER_THAN,
                    min.toString(),
                    XACML3.ID_DATATYPE_INTEGER,
                    ToscaDictionary.ID_RESOURCE_GUARD_VFCOUNT,
                    XACML3.ID_ATTRIBUTE_CATEGORY_RESOURCE);

            newPolicyType.getCombinerParametersOrRuleCombinerParametersOrVariableDefinition().add(
                    generateMinMaxRule(matchTarget, matchMin, policyName + ":minrule", "check minimum"));
        }
        Integer max = null;
        if (toscaPolicy.getProperties().containsKey(FIELD_MAX)) {
            max = ToscaPolicyTranslatorUtils.parseInteger(toscaPolicy.getProperties().get(FIELD_MAX).toString());
            MatchType matchMax = ToscaPolicyTranslatorUtils.buildMatchTypeDesignator(
                    XACML3.ID_FUNCTION_INTEGER_GREATER_THAN,
                    max.toString(),
                    XACML3.ID_DATATYPE_INTEGER,
                    ToscaDictionary.ID_RESOURCE_GUARD_VFCOUNT,
                    XACML3.ID_ATTRIBUTE_CATEGORY_RESOURCE);

            newPolicyType.getCombinerParametersOrRuleCombinerParametersOrVariableDefinition().add(
                    generateMinMaxRule(matchTarget, matchMax, policyName + ":maxrule", "check maximum"));
        }
        //
        // Do we have at least a min or max?
        //
        if (min == null && max == null) {
            throw new ToscaPolicyConversionException("Missing min or max field in minmax policy");
        }
    }

    protected RuleType generateMinMaxRule(MatchType matchTarget, MatchType matchMinOrMax, String ruleId, String desc) {
        AllOfType allOf = new AllOfType();
        allOf.getMatch().add(matchTarget);
        allOf.getMatch().add(matchMinOrMax);
        AnyOfType anyOf = new AnyOfType();
        anyOf.getAllOf().add(allOf);
        TargetType target = new TargetType();
        target.getAnyOf().add(anyOf);
        RuleType minMaxRule = new RuleType();
        minMaxRule.setEffect(EffectType.PERMIT);
        minMaxRule.setDescription(desc);
        minMaxRule.setRuleId(ruleId);
        minMaxRule.setTarget(target);
        return minMaxRule;
    }

    protected void generateBlacklistRules(ToscaPolicy toscaPolicy, String policyName, PolicyType newPolicyType)
            throws ToscaPolicyConversionException {
        //
        // Validate the blacklist exists
        //
        if (! toscaPolicy.getProperties().containsKey(FIELD_BLACKLIST)) {
            throw new ToscaPolicyConversionException("Missing blacklist field");
        }
        //
        // Get the blacklist, which should be an array or collection.
        //
        Object arrayBlacklisted = toscaPolicy.getProperties().get(FIELD_BLACKLIST);
        if (!(arrayBlacklisted instanceof Collection)) {
            throw new ToscaPolicyConversionException("Blacklist is not a collection");
        }
        //
        // Iterate the entries and create individual AnyOf so each entry is
        // treated as an OR.
        //
        TargetType target = new TargetType();
        AnyOfType anyOf = new AnyOfType();
        for (Object blacklisted : ((Collection<?>) arrayBlacklisted)) {
            AllOfType allOf = new AllOfType();
            this.addMatch(allOf, blacklisted, ToscaDictionary.ID_RESOURCE_GUARD_TARGETID);
            anyOf.getAllOf().add(allOf);
        }
        target.getAnyOf().add(anyOf);
        //
        // Create our rule and add the target
        //
        RuleType blacklistRule = new RuleType();
        blacklistRule.setEffect(EffectType.DENY);
        blacklistRule.setDescription("blacklist the entities");
        blacklistRule.setRuleId(policyName + ":blacklist");
        blacklistRule.setTarget(target);
        //
        // Add the rule to the policy
        //
        newPolicyType.getCombinerParametersOrRuleCombinerParametersOrVariableDefinition().add(blacklistRule);
    }

    @SuppressWarnings("unchecked")
    protected void generateFilterRules(ToscaPolicy toscaPolicy, String policyName, PolicyType newPolicyType)
            throws ToscaPolicyConversionException {
        //
        // Validate the algorithm
        //
        if (! toscaPolicy.getProperties().containsKey(FIELD_FILTER_ALGORITHM)) {
            throw new ToscaPolicyConversionException("Missing algorithm");
        }
        Object algorithm = toscaPolicy.getProperties().get(FIELD_FILTER_ALGORITHM);
        if ("whitelist-overrides".equals(algorithm.toString())) {
            newPolicyType.setRuleCombiningAlgId(XACML3.ID_RULE_PERMIT_OVERRIDES.stringValue());
        } else if ("blacklist-overrides".equals(algorithm.toString())) {
            newPolicyType.setRuleCombiningAlgId(XACML3.ID_RULE_DENY_OVERRIDES.stringValue());
        } else {
            throw new ToscaPolicyConversionException(
                    "Unexpected value for algorithm, should be whitelist-overrides or blacklist-overrides");
        }
        //
        // Validate the filters exist and have the right properties
        //
        if (! toscaPolicy.getProperties().containsKey(FIELD_FILTER_FILTERS)) {
            throw new ToscaPolicyConversionException("Missing filters");
        }
        //
        // Get the filters, which should be an array or collection.
        //
        Object arrayFilters = toscaPolicy.getProperties().get(FIELD_FILTER_FILTERS);
        if (!(arrayFilters instanceof Collection)) {
            throw new ToscaPolicyConversionException("Filters is not a collection");
        }
        //
        // Iterate the filters
        //
        int ruleId = 1;
        for (Object filterAttributes : ((Collection<?>) arrayFilters)) {
            if (!(filterAttributes instanceof Map)) {
                throw new ToscaPolicyConversionException("Filter should be a map");
            }
            //
            // All fields must be there
            //
            String field = validateFilterPropertyField((Map<String, Object>) filterAttributes);
            String filter = validateFilterPropertyFilter((Map<String, Object>) filterAttributes);
            Identifier function = validateFilterPropertyFunction((Map<String, Object>) filterAttributes);
            boolean isBlacklisted = validateFilterPropertyBlacklist((Map<String, Object>) filterAttributes);
            //
            // Create our filter rule
            //
            RuleType filterRule = createFilterRule(policyName + ":rule" + ruleId++, field, filter,
                    function, isBlacklisted);
            //
            // Add the rule to the policy
            //
            newPolicyType.getCombinerParametersOrRuleCombinerParametersOrVariableDefinition().add(filterRule);
        }
    }

    private String validateFilterPropertyField(Map<String, Object> filterAttributes)
            throws ToscaPolicyConversionException {
        Object field = filterAttributes.get(FIELD_FILTER_FIELD);
        if (field != null) {
            switch (field.toString().toLowerCase()) {
                case "generic-vnf.vnf-name":
                case "generic-vnf.vnf-id":
                case "generic-vnf.vnf-type":
                case "generic-vnf.nf-naming-code":
                case "vserver.vserver-id":
                case "cloud-region.cloud-region-id":
                    return field.toString();
                default:
                    throw new ToscaPolicyConversionException("Unexpected value for field in filter");
            }
        }
        throw new ToscaPolicyConversionException("Missing \'field\' from filter");
    }

    private String validateFilterPropertyFilter(Map<String, Object> filterAttributes)
            throws ToscaPolicyConversionException {
        Object filter = filterAttributes.get(FIELD_FILTER_FILTER);
        if (filter != null) {
            return filter.toString();
        }
        throw new ToscaPolicyConversionException("Missing \'filter\' from filter");
    }

    private Identifier validateFilterPropertyFunction(Map<String, Object> filterAttributes)
            throws ToscaPolicyConversionException {
        Object function = filterAttributes.get(FIELD_FILTER_FUNCTION);
        if (function != null) {
            switch (function.toString().toLowerCase()) {
                case "string-equal":
                    return XACML3.ID_FUNCTION_STRING_EQUAL;
                case "string-equal-ignore-case":
                    return XACML3.ID_FUNCTION_STRING_EQUAL_IGNORE_CASE;
                case "string-regexp-match":
                    return XACML3.ID_FUNCTION_STRING_REGEXP_MATCH;
                case "string-contains":
                    return XACML3.ID_FUNCTION_STRING_CONTAINS;
                case "string-greater-than":
                    return XACML3.ID_FUNCTION_STRING_GREATER_THAN;
                case "string-greater-than-or-equal":
                    return XACML3.ID_FUNCTION_STRING_GREATER_THAN_OR_EQUAL;
                case "string-less-than":
                    return XACML3.ID_FUNCTION_STRING_LESS_THAN;
                case "string-less-than-or-equal":
                    return XACML3.ID_FUNCTION_STRING_LESS_THAN_OR_EQUAL;
                case "string-starts-with":
                    return XACML3.ID_FUNCTION_STRING_STARTS_WITH;
                case "string-ends-with":
                    return XACML3.ID_FUNCTION_STRING_ENDS_WITH;
                default:
                    throw new ToscaPolicyConversionException("Unexpected value for function in filter");
            }
        }
        throw new ToscaPolicyConversionException("Missing \'function\' from filter");
    }

    private boolean validateFilterPropertyBlacklist(Map<String, Object> filterAttributes)
            throws ToscaPolicyConversionException {
        Object filter = filterAttributes.get(FIELD_FILTER_BLACKLIST);
        if (filter != null) {
            if ("true".equalsIgnoreCase(filter.toString())) {
                return true;
            }
            if ("false".equalsIgnoreCase(filter.toString())) {
                return false;
            }
            throw new ToscaPolicyConversionException("Unexpected value for blacklist in filter");
        }
        throw new ToscaPolicyConversionException("Missing \'blacklist\' from filter");
    }

    private RuleType createFilterRule(String ruleId, String field, String filter, Identifier function,
            boolean isBlacklisted) {
        RuleType rule = new RuleType();
        rule.setRuleId(ruleId);

        //
        // Create the Match
        //
        MatchType matchFilter = ToscaPolicyTranslatorUtils.buildMatchTypeDesignator(
                function,
                filter,
                XACML3.ID_DATATYPE_STRING,
                new IdentifierImpl(GuardPolicyRequest.PREFIX_RESOURCE_ATTRIBUTE_ID + field),
                XACML3.ID_ATTRIBUTE_CATEGORY_RESOURCE
                );
        AllOfType allOf = new AllOfType();
        allOf.getMatch().add(matchFilter);
        AnyOfType anyOf = new AnyOfType();
        anyOf.getAllOf().add(allOf);
        TargetType target = new TargetType();
        target.getAnyOf().add(anyOf);

        rule.setTarget(target);

        if (isBlacklisted) {
            rule.setEffect(EffectType.DENY);
        } else {
            rule.setEffect(EffectType.PERMIT);
        }
        return rule;
    }

}
