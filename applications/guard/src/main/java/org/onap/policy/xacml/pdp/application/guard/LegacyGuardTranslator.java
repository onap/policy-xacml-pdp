/*-
 * ============LICENSE_START=======================================================
 * ONAP
 * ================================================================================
 * Copyright (C) 2019 AT&T Intellectual Property. All rights reserved.
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
import com.att.research.xacml.std.annotations.RequestParser;
import com.att.research.xacml.util.XACMLPolicyWriter;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import oasis.names.tc.xacml._3_0.core.schema.wd_17.AdviceExpressionType;
import oasis.names.tc.xacml._3_0.core.schema.wd_17.AdviceExpressionsType;
import oasis.names.tc.xacml._3_0.core.schema.wd_17.AllOfType;
import oasis.names.tc.xacml._3_0.core.schema.wd_17.AnyOfType;
import oasis.names.tc.xacml._3_0.core.schema.wd_17.ApplyType;
import oasis.names.tc.xacml._3_0.core.schema.wd_17.AttributeAssignmentExpressionType;
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
import org.onap.policy.pdp.xacml.application.common.ToscaDictionary;
import org.onap.policy.pdp.xacml.application.common.ToscaPolicyConversionException;
import org.onap.policy.pdp.xacml.application.common.ToscaPolicyTranslator;
import org.onap.policy.pdp.xacml.application.common.ToscaPolicyTranslatorUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LegacyGuardTranslator implements ToscaPolicyTranslator {

    private static final Logger LOGGER = LoggerFactory.getLogger(LegacyGuardTranslator.class);

    private static final String FIELD_POLICIES = "policies";
    private static final String FIELD_TOPOLOGY_TEMPLATE = "topology_template";

    public LegacyGuardTranslator() {
        super();
    }

    @SuppressWarnings("unchecked")
    @Override
    public List<PolicyType> scanAndConvertPolicies(Map<String, Object> toscaObject)
            throws ToscaPolicyConversionException {
        //
        // Our return object
        //
        List<PolicyType> scannedPolicies = new ArrayList<>();
        //
        // Find the Policies
        //
        List<Object> policies;

        if (toscaObject.containsKey(FIELD_POLICIES)) {
            policies = (List<Object>) toscaObject.get(FIELD_POLICIES);
        } else if (toscaObject.containsKey(FIELD_TOPOLOGY_TEMPLATE)) {
            Map<String, Object> topologyTemplate = (Map<String, Object>) toscaObject.get(FIELD_TOPOLOGY_TEMPLATE);
            if (topologyTemplate.containsKey(FIELD_POLICIES)) {
                policies = (List<Object>) topologyTemplate.get(FIELD_POLICIES);
            } else {
                LOGGER.warn("topologyTemplate does not contain policies");
                return scannedPolicies;
            }
        } else {
            LOGGER.warn("Failed to find policies or topologyTemplate");
            return scannedPolicies;
        }
        //
        // Iterate each of the Policies
        //
        for (Object policyObject : policies) {
            //
            // Get the contents
            //
            LOGGER.debug("Found policy {}", policyObject.getClass());
            Map<String, Object> policyContents = (Map<String, Object>) policyObject;
            for (Entry<String, Object> entrySet : policyContents.entrySet()) {
                LOGGER.debug("Entry set {}", entrySet);
                //
                // Convert this policy
                //
                PolicyType policy = this.convertPolicy(entrySet);
                try (ByteArrayOutputStream os = new ByteArrayOutputStream()) {
                    XACMLPolicyWriter.writePolicyFile(os, policy);
                    LOGGER.debug("{}", os);
                } catch (IOException e) {
                    LOGGER.error("Failed to convert {}", e);
                }
                //
                // Convert and add in the new policy
                //
                scannedPolicies.add(policy);
            }
        }

        return scannedPolicies;
    }

    @Override
    public Request convertRequest(DecisionRequest request) {
        LOGGER.debug("Converting Request {}", request);
        try {
            return RequestParser.parseRequest(LegacyGuardPolicyRequest.createInstance(request));
        } catch (IllegalArgumentException | IllegalAccessException | DataTypeException e) {
            LOGGER.error("Failed to convert DecisionRequest: {}", e);
        }
        //
        // TODO throw exception
        //
        return null;
    }


    @Override
    public DecisionResponse convertResponse(Response xacmlResponse) {
        LOGGER.debug("Converting Response {}", xacmlResponse);
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
            }
            if (xacmlResult.getDecision() == Decision.DENY) {
                //
                // Just simply return a Deny response
                //
                decisionResponse.setStatus(Decision.DENY.toString());
            }
            if (xacmlResult.getDecision() == Decision.NOTAPPLICABLE) {
                //
                // There is no guard policy, so we return a permit
                //
                decisionResponse.setStatus(Decision.PERMIT.toString());
            }
        }

        return decisionResponse;
    }

    @SuppressWarnings("unchecked")
    private PolicyType convertPolicy(Entry<String, Object> entrySet) throws ToscaPolicyConversionException {
        //
        // Policy name should be at the root
        //
        String policyName = entrySet.getKey();
        Map<String, Object> policyDefinition = (Map<String, Object>) entrySet.getValue();
        //
        // Set it as the policy ID
        //
        PolicyType newPolicyType = new PolicyType();
        newPolicyType.setPolicyId(policyName);
        //
        // Optional description
        //
        if (policyDefinition.containsKey("description")) {
            newPolicyType.setDescription(policyDefinition.get("description").toString());
        }
        //
        // There should be a metadata section
        //
        if (! policyDefinition.containsKey("metadata")) {
            throw new ToscaPolicyConversionException(policyName + " missing metadata section");
        }
        this.fillMetadataSection(newPolicyType,
                (Map<String, Object>) policyDefinition.get("metadata"));
        //
        // Set the combining rule
        //
        newPolicyType.setRuleCombiningAlgId(XACML3.ID_RULE_DENY_UNLESS_PERMIT.stringValue());
        //
        // Generate the TargetType
        //
        if (! policyDefinition.containsKey("properties")) {
            throw new ToscaPolicyConversionException(policyName + " missing properties section");
        }
        newPolicyType.setTarget(this.generateTargetType((Map<String, Object>) policyDefinition.get("properties")));
        //
        // Now create the Permit Rule
        //
        RuleType rule = generatePermitRule(policyName, policyDefinition.get("type").toString(),
                (Map<String, Object>) policyDefinition.get("properties"));
        //
        // Add the rule to the policy
        //
        newPolicyType.getCombinerParametersOrRuleCombinerParametersOrVariableDefinition().add(rule);
        //
        // Return our new policy
        //
        return newPolicyType;
    }

    /**
     * From the TOSCA metadata section, pull in values that are needed into the XACML policy.
     *
     * @param policy Policy Object to store the metadata
     * @param metadata The Metadata TOSCA Map
     * @return Same Policy Object
     * @throws ToscaPolicyConversionException If there is something missing from the metadata
     */
    protected PolicyType fillMetadataSection(PolicyType policy,
            Map<String, Object> metadata) throws ToscaPolicyConversionException {
        if (! metadata.containsKey("policy-id")) {
            throw new ToscaPolicyConversionException(policy.getPolicyId() + " missing metadata policy-id");
        } else {
            //
            // Do nothing here - the XACML PolicyId is used from TOSCA Policy Name field
            //
        }
        if (! metadata.containsKey("policy-version")) {
            throw new ToscaPolicyConversionException(policy.getPolicyId() + " missing metadata policy-version");
        } else {
            //
            // Add in the Policy Version
            //
            policy.setVersion(metadata.get("policy-version").toString());
        }
        return policy;
    }

    protected TargetType generateTargetType(Map<String, Object> properties) {
        //
        // Go through potential properties
        //
        AllOfType allOf = new AllOfType();
        if (properties.containsKey("actor")) {
            addMatch(allOf, properties.get("actor"), ToscaDictionary.ID_RESOURCE_GUARD_ACTOR);
        }
        if (properties.containsKey("recipe")) {
            addMatch(allOf, properties.get("recipe"), ToscaDictionary.ID_RESOURCE_GUARD_RECIPE);
        }
        if (properties.containsKey("targets")) {
            addMatch(allOf, properties.get("targets"), ToscaDictionary.ID_RESOURCE_GUARD_TARGETID);
        }
        if (properties.containsKey("clname")) {
            addMatch(allOf, properties.get("clname"), ToscaDictionary.ID_RESOURCE_GUARD_CLNAME);
        }
        if (properties.containsKey("targets")) {
            addMatch(allOf, properties.get("targets"), ToscaDictionary.ID_RESOURCE_GUARD_TARGETID);
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

    private static AllOfType addMatch(AllOfType allOf, Object value, Identifier attributeId) {
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
            //
            // TODO support a collection of that attribute
            //
        }
        return allOf;
    }

    private static RuleType generatePermitRule(String policyName, String policyType, Map<String, Object> properties) {
        //
        // Now determine which policy type we are generating
        //
        if ("onap.policies.controlloop.guard.FrequencyLimiter".equals(policyType)) {
            return generateFrequencyPermit(policyName, policyType, properties);
        } else if ("onap.policies.controlloop.guard.MinMax".equals(policyType)) {
            return generateMinMaxPermit(policyName, policyType, properties);
        }
        return null;
    }

    private static RuleType generateFrequencyPermit(String policyName, String policyType,
            Map<String, Object> properties) {
        //
        // Get the properties that are common among guards
        //
        String timeWindow = null;
        if (properties.containsKey("timeWindow")) {
            timeWindow = properties.get("timeWindow").toString();
        }
        String timeUnits = null;
        if (properties.containsKey("timeUnits")) {
            timeUnits = properties.get("timeUnits").toString();
        }
        String guardActiveStart = null;
        if (properties.containsKey("guardActiveStart")) {
            guardActiveStart = properties.get("guardActiveStart").toString();
        }
        String guardActiveEnd = null;
        if (properties.containsKey("guardActiveEnd")) {
            guardActiveEnd = properties.get("guardActiveEnd").toString();
        }
        //
        // Generate the time in range
        //
        final ApplyType timeRange = generateTimeInRange(guardActiveStart, guardActiveEnd);
        //
        // See if its possible to generate a count
        //
        Integer limit = null;
        if (properties.containsKey("limit")) {
            limit = Integer.decode(properties.get("limit").toString());
        }
        final ApplyType countCheck = generateCountCheck(limit, timeWindow, timeUnits);
        //
        // Now combine into an And
        //
        ApplyType applyAnd = new ApplyType();
        applyAnd.setDescription("return true if all the apply's are true.");
        applyAnd.setFunctionId(XACML3.ID_FUNCTION_AND.stringValue());
        applyAnd.getExpression().add(new ObjectFactory().createApply(timeRange));
        applyAnd.getExpression().add(new ObjectFactory().createApply(countCheck));
        //
        // And create an outer negation of the And
        //
        ApplyType applyNot = new ApplyType();
        applyNot.setDescription("Negate the and");
        applyNot.setFunctionId(XACML3.ID_FUNCTION_NOT.stringValue());
        applyNot.getExpression().add(new ObjectFactory().createApply(applyAnd));

        //
        // Create our condition
        //
        final ConditionType condition = new ConditionType();
        condition.setExpression(new ObjectFactory().createApply(applyNot));

        //
        // Now we can create our rule
        //
        RuleType permit = new RuleType();
        permit.setDescription("Default is to PERMIT if the policy matches.");
        permit.setRuleId(policyName + ":rule");
        permit.setEffect(EffectType.PERMIT);
        permit.setTarget(new TargetType());
        //
        // Add the condition
        //
        permit.setCondition(condition);
        //
        // TODO Add the advice - Is the request id needed to be returned?
        //
        // permit.setAdviceExpressions(adviceExpressions);
        //
        // Done
        //
        return permit;
    }

    private static RuleType generateMinMaxPermit(String policyName, String policyType, Map<String, Object> properties) {
        //
        // Get the properties that are common among guards
        //
        String guardActiveStart = null;
        if (properties.containsKey("guardActiveStart")) {
            guardActiveStart = properties.get("guardActiveStart").toString();
        }
        String guardActiveEnd = null;
        if (properties.containsKey("guardActiveEnd")) {
            guardActiveEnd = properties.get("guardActiveEnd").toString();
        }
        //
        // Generate the time in range
        //
        final ApplyType timeRange = generateTimeInRange(guardActiveStart, guardActiveEnd);
        //
        // See if its possible to generate a count
        //
        Integer min = null;
        if (properties.containsKey("min")) {
            min = Integer.decode(properties.get("min").toString());
        }
        Integer max = null;
        if (properties.containsKey("max")) {
            max = Integer.decode(properties.get("max").toString());
        }
        final ApplyType minApply = generateMinCheck(min);
        final ApplyType maxApply = generateMaxCheck(max);

        //
        // Now combine into an And
        //
        ApplyType applyAnd = new ApplyType();
        applyAnd.setDescription("return true if all the apply's are true.");
        applyAnd.setFunctionId(XACML3.ID_FUNCTION_AND.stringValue());
        applyAnd.getExpression().add(new ObjectFactory().createApply(timeRange));
        applyAnd.getExpression().add(new ObjectFactory().createApply(minApply));
        applyAnd.getExpression().add(new ObjectFactory().createApply(maxApply));
        //
        // Create our condition
        //
        final ConditionType condition = new ConditionType();
        condition.setExpression(new ObjectFactory().createApply(applyAnd));
        //
        // Now we can create our rule
        //
        RuleType permit = new RuleType();
        permit.setDescription("Default is to PERMIT if the policy matches.");
        permit.setRuleId(policyName + ":rule");
        permit.setEffect(EffectType.PERMIT);
        permit.setTarget(new TargetType());
        //
        // Add the condition
        //
        permit.setCondition(condition);
        //
        // TODO Add the advice - Is the request id needed to be returned?
        //
        // permit.setAdviceExpressions(adviceExpressions);
        //
        // Done
        //
        return permit;
    }

    private static ApplyType generateTimeInRange(String start, String end) {
        if (start == null || end == null) {
            LOGGER.warn("Missing time range start {} end {}", start, end);
            return null;
        }
        if (start.isEmpty() || end.isEmpty()) {
            LOGGER.warn("Empty time range start {} end {}", start, end);
            return null;
        }

        AttributeDesignatorType designator = new AttributeDesignatorType();
        designator.setAttributeId(XACML3.ID_ENVIRONMENT_CURRENT_TIME.stringValue());
        designator.setCategory(XACML3.ID_ATTRIBUTE_CATEGORY_ENVIRONMENT.stringValue());
        designator.setDataType(XACML3.ID_DATATYPE_TIME.stringValue());

        AttributeValueType valueStart = new AttributeValueType();
        valueStart.setDataType(XACML3.ID_DATATYPE_TIME.stringValue());
        valueStart.getContent().add(start);

        AttributeValueType valueEnd = new AttributeValueType();
        valueEnd.setDataType(XACML3.ID_DATATYPE_TIME.stringValue());
        valueEnd.getContent().add(end);

        ObjectFactory factory = new ObjectFactory();

        ApplyType applyOneAndOnly = new ApplyType();
        applyOneAndOnly.setDescription("Unbag the current time");
        applyOneAndOnly.setFunctionId(XACML3.ID_FUNCTION_TIME_ONE_AND_ONLY.stringValue());
        applyOneAndOnly.getExpression().add(factory.createAttributeDesignator(designator));

        ApplyType applyTimeInRange = new ApplyType();
        applyTimeInRange.setDescription("return true if current time is in range.");
        applyTimeInRange.setFunctionId(XACML3.ID_FUNCTION_TIME_IN_RANGE.stringValue());
        applyTimeInRange.getExpression().add(factory.createApply(applyOneAndOnly));
        applyTimeInRange.getExpression().add(factory.createAttributeValue(valueStart));
        applyTimeInRange.getExpression().add(factory.createAttributeValue(valueEnd));

        return applyTimeInRange;
    }

    private static ApplyType generateCountCheck(Integer limit, String timeWindow, String timeUnits) {
        AttributeDesignatorType designator = new AttributeDesignatorType();
        designator.setAttributeId(ToscaDictionary.ID_RESOURCE_GUARD_OPERATIONCOUNT.stringValue());
        designator.setCategory(XACML3.ID_ATTRIBUTE_CATEGORY_RESOURCE.stringValue());
        designator.setDataType(XACML3.ID_DATATYPE_INTEGER.stringValue());
        //
        // TODO Add this back in when the operational database PIP is configured.
        // The issuer indicates that the PIP will be providing this attribute during
        // the decision making.
        //
        // Right now I am faking the count value by re-using the request-id field
        //
        //String issuer = "org:onap:xacml:guard:historydb:tw:" + timeWindow + ":" + timeUnits;
        //designator.setIssuer(issuer);

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

        ApplyType applyGreaterThanEqual = new ApplyType();
        applyGreaterThanEqual.setDescription("return true if current count is greater than or equal.");
        applyGreaterThanEqual.setFunctionId(XACML3.ID_FUNCTION_INTEGER_GREATER_THAN_OR_EQUAL.stringValue());
        applyGreaterThanEqual.getExpression().add(factory.createApply(applyOneAndOnly));
        applyGreaterThanEqual.getExpression().add(factory.createAttributeValue(valueLimit));

        return applyGreaterThanEqual;
    }

    private static ApplyType generateMinCheck(Integer min) {
        AttributeDesignatorType designator = new AttributeDesignatorType();
        designator.setAttributeId(ToscaDictionary.ID_RESOURCE_GUARD_VFCOUNT.stringValue());
        designator.setCategory(XACML3.ID_ATTRIBUTE_CATEGORY_RESOURCE.stringValue());
        designator.setDataType(XACML3.ID_DATATYPE_INTEGER.stringValue());
        //
        //
        //
        AttributeValueType valueLimit = new AttributeValueType();
        valueLimit.setDataType(XACML3.ID_DATATYPE_INTEGER.stringValue());
        //
        // Yes really use toString(), the marshaller will
        // throw an exception if this is an integer object
        // and not a string.
        //
        valueLimit.getContent().add(min.toString());
        ObjectFactory factory = new ObjectFactory();

        ApplyType applyOneAndOnly = new ApplyType();
        applyOneAndOnly.setDescription("Unbag the min");
        applyOneAndOnly.setFunctionId(XACML3.ID_FUNCTION_INTEGER_ONE_AND_ONLY.stringValue());
        applyOneAndOnly.getExpression().add(factory.createAttributeDesignator(designator));

        ApplyType applyGreaterThanEqual = new ApplyType();
        applyGreaterThanEqual.setDescription("return true if current count is greater than or equal.");
        applyGreaterThanEqual.setFunctionId(XACML3.ID_FUNCTION_INTEGER_GREATER_THAN_OR_EQUAL.stringValue());
        applyGreaterThanEqual.getExpression().add(factory.createApply(applyOneAndOnly));
        applyGreaterThanEqual.getExpression().add(factory.createAttributeValue(valueLimit));

        return applyGreaterThanEqual;
    }

    private static ApplyType generateMaxCheck(Integer max) {
        AttributeDesignatorType designator = new AttributeDesignatorType();
        designator.setAttributeId(ToscaDictionary.ID_RESOURCE_GUARD_VFCOUNT.stringValue());
        designator.setCategory(XACML3.ID_ATTRIBUTE_CATEGORY_RESOURCE.stringValue());
        designator.setDataType(XACML3.ID_DATATYPE_INTEGER.stringValue());
        //
        //
        //
        AttributeValueType valueLimit = new AttributeValueType();
        valueLimit.setDataType(XACML3.ID_DATATYPE_INTEGER.stringValue());
        //
        // Yes really use toString(), the marshaller will
        // throw an exception if this is an integer object
        // and not a string.
        //
        valueLimit.getContent().add(max.toString());
        ObjectFactory factory = new ObjectFactory();

        ApplyType applyOneAndOnly = new ApplyType();
        applyOneAndOnly.setDescription("Unbag the min");
        applyOneAndOnly.setFunctionId(XACML3.ID_FUNCTION_INTEGER_ONE_AND_ONLY.stringValue());
        applyOneAndOnly.getExpression().add(factory.createAttributeDesignator(designator));

        ApplyType applyGreaterThanEqual = new ApplyType();
        applyGreaterThanEqual.setDescription("return true if current count is less than or equal.");
        applyGreaterThanEqual.setFunctionId(XACML3.ID_FUNCTION_INTEGER_LESS_THAN_OR_EQUAL.stringValue());
        applyGreaterThanEqual.getExpression().add(factory.createApply(applyOneAndOnly));
        applyGreaterThanEqual.getExpression().add(factory.createAttributeValue(valueLimit));

        return applyGreaterThanEqual;
    }

    private static AdviceExpressionsType generateRequestIdAdvice() {
        AdviceExpressionType adviceExpression = new AdviceExpressionType();
        adviceExpression.setAppliesTo(EffectType.PERMIT);
        adviceExpression.setAdviceId(ToscaDictionary.ID_ADVICE_GUARD.stringValue());

        AttributeDesignatorType designator = new AttributeDesignatorType();
        designator.setAttributeId(ToscaDictionary.ID_SUBJECT_GUARD_REQUESTID.stringValue());
        designator.setCategory(XACML3.ID_SUBJECT_CATEGORY_ACCESS_SUBJECT.stringValue());
        designator.setDataType(XACML3.ID_DATATYPE_STRING.stringValue());

        AttributeAssignmentExpressionType assignment = new AttributeAssignmentExpressionType();
        assignment.setAttributeId(ToscaDictionary.ID_ADVICE_GUARD_REQUESTID.stringValue());
        assignment.setCategory(XACML3.ID_SUBJECT_CATEGORY_ACCESS_SUBJECT.stringValue());
        assignment.setExpression(new ObjectFactory().createAttributeDesignator(designator));

        adviceExpression.getAttributeAssignmentExpression().add(assignment);

        AdviceExpressionsType adviceExpressions = new AdviceExpressionsType();
        adviceExpressions.getAdviceExpression().add(adviceExpression);

        return adviceExpressions;
    }
}
