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

import com.att.research.xacml.api.Decision;
import com.att.research.xacml.api.Obligation;
import com.att.research.xacml.api.Request;
import com.att.research.xacml.api.Response;
import com.att.research.xacml.api.Result;
import com.att.research.xacml.api.XACML3;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import oasis.names.tc.xacml._3_0.core.schema.wd_17.AnyOfType;
import oasis.names.tc.xacml._3_0.core.schema.wd_17.ApplyType;
import oasis.names.tc.xacml._3_0.core.schema.wd_17.AttributeDesignatorType;
import oasis.names.tc.xacml._3_0.core.schema.wd_17.AttributeValueType;
import oasis.names.tc.xacml._3_0.core.schema.wd_17.ConditionType;
import oasis.names.tc.xacml._3_0.core.schema.wd_17.MatchType;
import oasis.names.tc.xacml._3_0.core.schema.wd_17.ObjectFactory;
import oasis.names.tc.xacml._3_0.core.schema.wd_17.ObligationExpressionType;
import oasis.names.tc.xacml._3_0.core.schema.wd_17.ObligationExpressionsType;
import oasis.names.tc.xacml._3_0.core.schema.wd_17.PolicySetType;
import oasis.names.tc.xacml._3_0.core.schema.wd_17.PolicyType;
import oasis.names.tc.xacml._3_0.core.schema.wd_17.RuleType;
import org.onap.policy.models.decisions.concepts.DecisionRequest;
import org.onap.policy.models.decisions.concepts.DecisionResponse;
import org.onap.policy.models.tosca.authorative.concepts.ToscaPolicy;
import org.onap.policy.pdp.xacml.application.common.OnapObligation;
import org.onap.policy.pdp.xacml.application.common.ToscaDictionary;
import org.onap.policy.pdp.xacml.application.common.ToscaPolicyConversionException;
import org.onap.policy.pdp.xacml.application.common.ToscaPolicyTranslator;
import org.onap.policy.pdp.xacml.application.common.ToscaPolicyTranslatorUtils;
import org.onap.policy.pdp.xacml.application.common.XacmlPolicyUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class StdBaseTranslator implements ToscaPolicyTranslator {
    private static final Logger LOGGER = LoggerFactory.getLogger(StdBaseTranslator.class);
    private static final ObjectFactory factory = new ObjectFactory();

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
     * obligations. This method must be overridden and be implemented for the specific application as
     * obligations may have different expected attributes per application.
     *
     * @param obligations Collection of obligation objects
     * @param decisionResponse DecisionResponse object used to store any results from obligations.
     */
    protected abstract void scanObligations(Collection<Obligation> obligations, DecisionResponse decisionResponse);

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
        LOGGER.info("Obligation Policy id: {} type: {} weight: {} policy:{}{}", policyId, policyType, weight,
                XacmlPolicyUtils.LINE_SEPARATOR, jsonPolicy);
        //
        // Create our OnapObligation
        //
        OnapObligation onapObligation = new OnapObligation(policyId, jsonPolicy, policyType, weight);
        //
        // Generate the obligation
        //
        ObligationExpressionType obligation = onapObligation.generateObligation();
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
