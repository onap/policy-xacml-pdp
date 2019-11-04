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

package org.onap.policy.pdp.xacml.application.common.std;

import com.att.research.xacml.api.AttributeAssignment;
import com.att.research.xacml.api.Decision;
import com.att.research.xacml.api.Identifier;
import com.att.research.xacml.api.Obligation;
import com.att.research.xacml.api.Request;
import com.att.research.xacml.api.Response;
import com.att.research.xacml.api.Result;
import com.att.research.xacml.api.XACML3;
import com.google.gson.Gson;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import oasis.names.tc.xacml._3_0.core.schema.wd_17.AnyOfType;
import oasis.names.tc.xacml._3_0.core.schema.wd_17.ApplyType;
import oasis.names.tc.xacml._3_0.core.schema.wd_17.AttributeAssignmentExpressionType;
import oasis.names.tc.xacml._3_0.core.schema.wd_17.AttributeDesignatorType;
import oasis.names.tc.xacml._3_0.core.schema.wd_17.AttributeValueType;
import oasis.names.tc.xacml._3_0.core.schema.wd_17.ConditionType;
import oasis.names.tc.xacml._3_0.core.schema.wd_17.EffectType;
import oasis.names.tc.xacml._3_0.core.schema.wd_17.MatchType;
import oasis.names.tc.xacml._3_0.core.schema.wd_17.ObjectFactory;
import oasis.names.tc.xacml._3_0.core.schema.wd_17.ObligationExpressionType;
import oasis.names.tc.xacml._3_0.core.schema.wd_17.ObligationExpressionsType;
import oasis.names.tc.xacml._3_0.core.schema.wd_17.PolicySetType;
import oasis.names.tc.xacml._3_0.core.schema.wd_17.PolicyType;
import oasis.names.tc.xacml._3_0.core.schema.wd_17.RuleType;
import org.apache.commons.lang3.tuple.Pair;
import org.onap.policy.models.decisions.concepts.DecisionRequest;
import org.onap.policy.models.decisions.concepts.DecisionResponse;
import org.onap.policy.models.tosca.authorative.concepts.ToscaPolicy;
import org.onap.policy.pdp.xacml.application.common.ToscaDictionary;
import org.onap.policy.pdp.xacml.application.common.ToscaPolicyConversionException;
import org.onap.policy.pdp.xacml.application.common.ToscaPolicyTranslator;
import org.onap.policy.pdp.xacml.application.common.ToscaPolicyTranslatorUtils;
import org.onap.policy.pdp.xacml.application.common.XacmlPolicyUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class StdBaseTranslator implements ToscaPolicyTranslator {
    private static final Logger LOGGER = LoggerFactory.getLogger(StdBaseTranslator.class);
    private static Gson gson = new Gson();
    private final ObjectFactory factory = new ObjectFactory();

    public static final String POLICY_ID = "policy-id";
    public static final String POLICY_VERSION = "policy-version";

    @Override
    public PolicyType convertPolicy(ToscaPolicy toscaPolicy) throws ToscaPolicyConversionException {
        throw new ToscaPolicyConversionException("Please override convertPolicy");
    }

    @Override
    public Request convertRequest(DecisionRequest request) {
        return null;
    }

    @Override
    public DecisionResponse convertResponse(Response xacmlResponse) {
        LOGGER.info("Converting Response {}", xacmlResponse);
        DecisionResponse decisionResponse = new DecisionResponse();
        //
        // Setup policies
        //
        decisionResponse.setPolicies(new HashMap<>());
        //
        // Iterate through all the results
        //
        for (Result xacmlResult : xacmlResponse.getResults()) {
            //
            // Check the result
            //
            if (xacmlResult.getDecision() == Decision.PERMIT) {
                //
                // Go through obligations
                //
                scanObligations(xacmlResult.getObligations(), decisionResponse);
            } else {
                //
                // TODO we have to return an ErrorResponse object instead
                //
                decisionResponse.setStatus("A better error message");
            }
        }

        return decisionResponse;
    }

    /**
     * scanObligations - scans the list of obligations and make appropriate method calls to process
     * obligations. This method must be overridden and be implemented for the specific application.
     *
     * @param obligations Collection of obligation objects
     * @param decisionResponse DecisionResponse object used to store any results from obligations.
     */
    protected abstract void scanObligations(Collection<Obligation> obligations, DecisionResponse decisionResponse);

    /**
     * scanObligationAttribute - processes an individual obligation attribute assignment object.
     *
     * @param assignment AttributeAssignment object
     * @return {@code Pair<Identifier, Object>} The XACML Id of the obligation and its value object.
     */
    protected Pair<Identifier, Object> scanObligationAttribute(AttributeAssignment assignment) {
        //
        // We care about the id attribute
        //
        if (ToscaDictionary.ID_OBLIGATION_POLICY_ID.equals(assignment.getAttributeId())) {
            return Pair.of(ToscaDictionary.ID_OBLIGATION_POLICY_ID,
                    scanPolicyIdObligationAttribute(assignment));
        }
        //
        // We care about the content attribute
        //
        if (ToscaDictionary.ID_OBLIGATION_POLICY_CONTENT.equals(assignment.getAttributeId())) {
            return Pair.of(ToscaDictionary.ID_OBLIGATION_POLICY_CONTENT,
                    scanPolicyContentObligationAttribute(assignment));
        }
        //
        // We care about the weight attribute
        //
        if (ToscaDictionary.ID_OBLIGATION_POLICY_WEIGHT.equals(assignment.getAttributeId())) {
            return Pair.of(ToscaDictionary.ID_OBLIGATION_POLICY_WEIGHT,
                    scanWeightObligationAttribute(assignment));
        }
        //
        // We care about the policy type attribute
        //
        if (ToscaDictionary.ID_OBLIGATION_POLICY_TYPE.equals(assignment.getAttributeId())) {
            return Pair.of(ToscaDictionary.ID_OBLIGATION_POLICY_TYPE,
                    scanPolicyTypeObligationAttribute(assignment));
        }
        LOGGER.warn("Scanning for an unknown obligation attribute {}", assignment.getAttributeId());
        return null;
    }

    /**
     * scanPolicyIdObligationAttribute - processes an individual obligation attribute assignment object.
     *
     * @param assignment AttributeAssignment object
     * @return String policy-id as String
     */
    protected String scanPolicyIdObligationAttribute(AttributeAssignment assignment) {
        //
        // Get the policy type value
        //
        Object policyId = assignment.getAttributeValue().getValue();
        LOGGER.info("Policy Id: {}", policyId);

        return policyId.toString();
    }

    /**
     * scanPolicyContentObligationAttribute - processes an individual obligation attribute assignment object.
     *
     * @param assignment AttributeAssignment object
     * @return {@code Pair<String, Map<String, Object>>} policy-id as String and policy content as JSON map.
     */
    @SuppressWarnings("unchecked")
    protected Map<String, Object> scanPolicyContentObligationAttribute(AttributeAssignment assignment) {
        //
        // The contents are in Json form
        //
        Object stringContents = assignment.getAttributeValue().getValue();
        LOGGER.info("Policy contents: {}{}", XacmlPolicyUtils.LINE_SEPARATOR, stringContents);
        //
        // Let's parse it into a map using Gson
        //
        return gson.fromJson(stringContents.toString(), Map.class);
    }

    /**
     * scanWeightObligationAttribute - processes an individual obligation attribute assignment object.
     *
     * @param assignment AttributeAssignment object
     * @return Integer weight
     */
    protected Integer scanWeightObligationAttribute(AttributeAssignment assignment) {
        //
        // Get the weight value
        //
        Object weight = assignment.getAttributeValue().getValue();
        LOGGER.info("Weight: {}", weight);
        //
        // Comes back as a BigInteger according to XACML
        // easier to just decode it.
        //
        return Integer.decode(weight.toString());
    }

    /**
     * scanPolicyTypeObligationAttribute - processes an individual obligation attribute assignment object.
     *
     * @param assignment AttributeAssignment object
     * @return String the policy type
     */
    protected String scanPolicyTypeObligationAttribute(AttributeAssignment assignment) {
        //
        // Get the policy type value
        //
        Object policyType = assignment.getAttributeValue().getValue();
        LOGGER.info("Policy Type: {}", policyType);

        return policyType.toString();
    }

    /**
     * From the TOSCA metadata section, pull in values that are needed into the XACML policy.
     *
     * @param policy Policy Object to store the metadata
     * @param map The Metadata TOSCA Map
     * @return Same Policy Object
     * @throws ToscaPolicyConversionException If there is something missing from the metadata
     */
    protected PolicyType fillMetadataSection(PolicyType policy,
            Map<String, String> map) throws ToscaPolicyConversionException {
        //
        // Ensure the policy-id exists - we don't use it here. It
        // is saved in the TOSCA Policy Name field.
        //
        if (! map.containsKey(POLICY_ID)) {
            throw new ToscaPolicyConversionException(policy.getPolicyId() + " missing metadata " + POLICY_ID);
        }
        //
        // Ensure the policy-version exists
        //
        if (! map.containsKey(POLICY_VERSION)) {
            throw new ToscaPolicyConversionException(policy.getPolicyId() + " missing metadata "
                    + POLICY_VERSION);
        }
        //
        // Add in the Policy Version
        //
        policy.setVersion(map.get(POLICY_VERSION));
        return policy;
    }

    /**
     * addObligation - general code to add a json policy as an obligation. Probably could just
     * return the obligation only instead of adding it directly to a rule/policy/policyset.
     * But this is fine for now.
     *
     * @param <T> RuleType, PolicyType, PolicySetType object
     * @Param policyId The policy-id
     * @param ruleOrPolicy Incoming RuleType, PolicyType, PolicySetType object
     * @param jsonPolicy JSON String representation of policy.
     * @param weight Weighting for the policy (optional)
     * @return Return the Incoming RuleType, PolicyType, PolicySetType object for convenience.
     */
    protected <T> T addObligation(T ruleOrPolicy, String policyId, String jsonPolicy, Integer weight,
            String policyType) {
        //
        // Creating obligation for returning policy
        //
        LOGGER.info("Obligation Policy {}{}{}{}{}", policyId, policyType, weight,
                XacmlPolicyUtils.LINE_SEPARATOR, jsonPolicy);
        //
        // Create an ObligationExpression
        //
        ObligationExpressionType obligation = new ObligationExpressionType();
        obligation.setFulfillOn(EffectType.PERMIT);
        obligation.setObligationId(ToscaDictionary.ID_OBLIGATION_REST_BODY.stringValue());
        //
        // Add policy-id
        //
        addOptionalAttributeToObligation(obligation, ToscaDictionary.ID_OBLIGATION_POLICY_ID,
                ToscaDictionary.ID_OBLIGATION_POLICY_ID_DATATYPE,
                ToscaDictionary.ID_OBLIGATION_POLICY_ID_CATEGORY,
                policyId);
        //
        // Add policy contents
        //
        addOptionalAttributeToObligation(obligation, ToscaDictionary.ID_OBLIGATION_POLICY_CONTENT,
                ToscaDictionary.ID_OBLIGATION_POLICY_CONTENT_DATATYPE,
                ToscaDictionary.ID_OBLIGATION_POLICY_CONTENT_CATEGORY,
                jsonPolicy);
        //
        // Add the weight
        //
        addOptionalAttributeToObligation(obligation, ToscaDictionary.ID_OBLIGATION_POLICY_WEIGHT,
                ToscaDictionary.ID_OBLIGATION_POLICY_WEIGHT_DATATYPE,
                ToscaDictionary.ID_OBLIGATION_POLICY_WEIGHT_CATEGORY,
                weight);
        //
        // Add the policy type
        //
        addOptionalAttributeToObligation(obligation, ToscaDictionary.ID_OBLIGATION_POLICY_TYPE,
                ToscaDictionary.ID_OBLIGATION_POLICY_TYPE_DATATYPE,
                ToscaDictionary.ID_OBLIGATION_POLICY_TYPE_CATEGORY,
                policyType);
        //
        // Sanity check
        //
        if (obligation.getAttributeAssignmentExpression().isEmpty()) {
            LOGGER.warn("Creating an empty obligation");
        }
        //
        // Now we can add it into the rule/policy/policyset
        //
        ObligationExpressionsType obligations = new ObligationExpressionsType();
        obligations.getObligationExpression().add(obligation);
        if (ruleOrPolicy instanceof RuleType) {
            ((RuleType) ruleOrPolicy).setObligationExpressions(obligations);
        } else if (ruleOrPolicy instanceof PolicyType) {
            ((PolicyType) ruleOrPolicy).setObligationExpressions(obligations);
        } else if (ruleOrPolicy instanceof PolicySetType) {
            ((PolicySetType) ruleOrPolicy).setObligationExpressions(obligations);
        } else {
            LOGGER.error("Unsupported class for adding obligation {}", ruleOrPolicy.getClass());
        }
        //
        // Return as a convenience
        //
        return ruleOrPolicy;
    }

    /**
     * Creates the necessary objects to insert into the obligation, if the value object is not null.
     *
     * @param obligation Incoming Obligation
     * @param id Attribute Id
     * @param datatype Attribute's Data type
     * @param category Attributes Category
     * @param theValue Attribute value
     * @return obligation Incoming obligation
     */
    protected ObligationExpressionType addOptionalAttributeToObligation(ObligationExpressionType obligation,
            Identifier id, Identifier datatype, Identifier category, Object theValue) {
        //
        // Simple check for null
        //
        if (theValue == null) {
            return obligation;
        }
        //
        // Create an AttributeValue for it
        //
        AttributeValueType value = new AttributeValueType();
        value.setDataType(datatype.stringValue());
        value.getContent().add(theValue.toString());
        //
        // Create our AttributeAssignmentExpression where we will
        // store the contents of the policy id.
        //
        AttributeAssignmentExpressionType expressionType = new AttributeAssignmentExpressionType();
        expressionType.setAttributeId(id.stringValue());
        expressionType.setCategory(category.stringValue());
        expressionType.setExpression(factory.createAttributeValue(value));
        //
        // Add it to the obligation
        //
        obligation.getAttributeAssignmentExpression().add(expressionType);
        //
        // Return as convenience
        //
        return obligation;
    }

    /**
     * generateAnyOfForPolicyType - Creates a specific AnyOfType that includes the check
     * to match on a specific TOSCA Policy Type.
     *
     * @param type String represenatation of TOSCA Policy Type (eg. "onap.policies.Foo")
     * @return AnyOfType object
     */
    protected AnyOfType generateAnyOfForPolicyType(String type) {
        //
        // Create the match for the policy type
        //
        MatchType match = ToscaPolicyTranslatorUtils.buildMatchTypeDesignator(
                XACML3.ID_FUNCTION_STRING_EQUAL,
                type,
                XACML3.ID_DATATYPE_STRING,
                ToscaDictionary.ID_RESOURCE_POLICY_TYPE,
                XACML3.ID_ATTRIBUTE_CATEGORY_RESOURCE);
        //
        // Add it to an AnyOfType object
        //
        AnyOfType anyOf = new AnyOfType();
        anyOf.getAllOf().add(ToscaPolicyTranslatorUtils.buildAllOf(match));
        //
        // Return new AnyOfType
        //
        return anyOf;
    }

    /**
     * generateConditionForPolicyType - create a ConditionType XACML object
     * that is able to determine if a request specifies a specific policy type
     * that the policy is created from, only then is the rule applied. If the
     * request doesn't even care about the policy type (eg it is missing) then
     * return the rule should not apply.
     *
     * @param type PolicyType (eg. onap.policies.Foo
     * @return ConditionType object
     */
    protected ConditionType generateConditionForPolicyType(String type) {
        //
        // Create an ApplyType that checks if the request contains the
        // policy-type attribute
        //
        AttributeDesignatorType designator = new AttributeDesignatorType();
        designator.setAttributeId(ToscaDictionary.ID_RESOURCE_POLICY_TYPE.stringValue());
        designator.setCategory(XACML3.ID_ATTRIBUTE_CATEGORY_RESOURCE.stringValue());
        designator.setDataType(XACML3.ID_DATATYPE_STRING.stringValue());

        ApplyType applyBagSize = new ApplyType();
        applyBagSize.setDescription("Get the size of policy-type attributes");
        applyBagSize.setFunctionId(XACML3.ID_FUNCTION_STRING_BAG_SIZE.stringValue());

        AttributeValueType valueZero = new AttributeValueType();
        valueZero.setDataType(XACML3.ID_DATATYPE_INTEGER.stringValue());
        valueZero.getContent().add("0");    // Yes really - represent as a string

        applyBagSize.getExpression().add(factory.createAttributeDesignator(designator));

        ApplyType applyGreaterThan = new ApplyType();
        applyGreaterThan.setDescription("Does the policy-type attribute exist?");
        applyGreaterThan.setFunctionId(XACML3.ID_FUNCTION_INTEGER_EQUAL.stringValue());

        applyGreaterThan.getExpression().add(factory.createApply(applyBagSize));
        applyGreaterThan.getExpression().add(factory.createAttributeValue(valueZero));

        //
        // Create an apply type that checks the actual value
        //
        AttributeValueType value = new AttributeValueType();
        value.setDataType(XACML3.ID_DATATYPE_STRING.stringValue());
        value.getContent().add(type);

        //
        // Create string-is-in apply - which determines if the policy-type
        // is in the request bag of resources for policy-type
        //
        ApplyType applyIsIn = new ApplyType();
        applyIsIn.setDescription("Is this policy-type in the list?");
        applyIsIn.setFunctionId(XACML3.ID_FUNCTION_STRING_IS_IN.stringValue());
        applyIsIn.getExpression().add(factory.createAttributeValue(value));
        applyIsIn.getExpression().add(factory.createAttributeDesignator(designator));

        //
        // Create our outer apply
        //
        ApplyType applyOr = new ApplyType();
        applyOr.setDescription("IF exists and is equal");
        applyOr.setFunctionId(XACML3.ID_FUNCTION_OR.stringValue());

        applyOr.getExpression().add(factory.createApply(applyGreaterThan));
        applyOr.getExpression().add(factory.createApply(applyIsIn));

        //
        // Finally create the condition
        //
        ConditionType condition = new ConditionType();

        condition.setExpression(factory.createApply(applyOr));

        return condition;
    }

}
