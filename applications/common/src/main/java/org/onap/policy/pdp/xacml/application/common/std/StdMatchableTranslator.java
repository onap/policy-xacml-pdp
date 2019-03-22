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
import com.att.research.xacml.api.DataTypeException;
import com.att.research.xacml.api.Decision;
import com.att.research.xacml.api.Identifier;
import com.att.research.xacml.api.Obligation;
import com.att.research.xacml.api.Request;
import com.att.research.xacml.api.Response;
import com.att.research.xacml.api.Result;
import com.att.research.xacml.api.XACML3;
import com.att.research.xacml.std.annotations.RequestParser;
import com.att.research.xacml.util.XACMLPolicyWriter;
import com.google.gson.Gson;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import oasis.names.tc.xacml._3_0.core.schema.wd_17.AnyOfType;
import oasis.names.tc.xacml._3_0.core.schema.wd_17.AttributeAssignmentExpressionType;
import oasis.names.tc.xacml._3_0.core.schema.wd_17.AttributeValueType;
import oasis.names.tc.xacml._3_0.core.schema.wd_17.EffectType;
import oasis.names.tc.xacml._3_0.core.schema.wd_17.MatchType;
import oasis.names.tc.xacml._3_0.core.schema.wd_17.ObjectFactory;
import oasis.names.tc.xacml._3_0.core.schema.wd_17.ObligationExpressionType;
import oasis.names.tc.xacml._3_0.core.schema.wd_17.ObligationExpressionsType;
import oasis.names.tc.xacml._3_0.core.schema.wd_17.PolicyType;
import oasis.names.tc.xacml._3_0.core.schema.wd_17.RuleType;
import oasis.names.tc.xacml._3_0.core.schema.wd_17.TargetType;

import org.json.JSONObject;
import org.onap.policy.models.decisions.concepts.DecisionRequest;
import org.onap.policy.models.decisions.concepts.DecisionResponse;
import org.onap.policy.pdp.xacml.application.common.ToscaDictionary;
import org.onap.policy.pdp.xacml.application.common.ToscaPolicyConversionException;
import org.onap.policy.pdp.xacml.application.common.ToscaPolicyTranslator;
import org.onap.policy.pdp.xacml.application.common.ToscaPolicyTranslatorUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class StdMatchableTranslator implements ToscaPolicyTranslator {

    private static final Logger LOGGER = LoggerFactory.getLogger(StdMatchableTranslator.class);

    public StdMatchableTranslator() {
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
        // Iterate each of the Policies
        //
        List<Object> policies = (List<Object>) toscaObject.get("policies");
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
            return RequestParser.parseRequest(StdMatchablePolicyRequest.createInstance(request));
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
                // Setup policies
                //
                decisionResponse.setPolicies(new ArrayList<>());
                //
                // Go through obligations
                //
                scanObligations(xacmlResult.getObligations(), decisionResponse);
            }
            if (xacmlResult.getDecision() == Decision.NOTAPPLICABLE) {
                //
                // There is no policy
                //
                decisionResponse.setPolicies(new ArrayList<>());
            }
            if (xacmlResult.getDecision() == Decision.DENY
                    || xacmlResult.getDecision() == Decision.INDETERMINATE) {
                //
                // TODO we have to return an ErrorResponse object instead
                //
                decisionResponse.setStatus("A better error message");
            }
        }

        return decisionResponse;
    }

    protected void scanObligations(Collection<Obligation> obligations, DecisionResponse decisionResponse) {
        for (Obligation obligation : obligations) {
            LOGGER.debug("Obligation: {}", obligation);
            for (AttributeAssignment assignment : obligation.getAttributeAssignments()) {
                LOGGER.debug("Attribute Assignment: {}", assignment);
                //
                // We care about the content attribute
                //
                if (ToscaDictionary.ID_OBLIGATION_POLICY_MONITORING_CONTENTS
                        .equals(assignment.getAttributeId())) {
                    //
                    // The contents are in Json form
                    //
                    Object stringContents = assignment.getAttributeValue().getValue();
                    if (LOGGER.isDebugEnabled()) {
                        LOGGER.debug("DCAE contents: {}{}", System.lineSeparator(), stringContents);
                    }
                    //
                    // Let's parse it into a map using Gson
                    //
                    Gson gson = new Gson();
                    @SuppressWarnings("unchecked")
                    Map<String, Object> result = gson.fromJson(stringContents.toString() ,Map.class);
                    decisionResponse.getPolicies().add(result);
                }
            }
        }

    }

    @SuppressWarnings("unchecked")
    protected PolicyType convertPolicy(Entry<String, Object> entrySet) throws ToscaPolicyConversionException {
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
        newPolicyType.setRuleCombiningAlgId(XACML3.ID_RULE_FIRST_APPLICABLE.stringValue());
        //
        // Generate the TargetType
        //
        if (! policyDefinition.containsKey("properties")) {
            throw new ToscaPolicyConversionException(policyName + " missing properties section");
        }
        policyDefinition.get("properties");
        newPolicyType.setTarget(generateTargetType((Map<String, Object>) policyDefinition.get("properties")));
        //
        // Now create the Permit Rule
        // No target since the policy has a target
        // With obligations.
        //
        RuleType rule = new RuleType();
        rule.setDescription("Default is to PERMIT if the policy matches.");
        rule.setRuleId(policyName + ":rule");
        rule.setEffect(EffectType.PERMIT);
        rule.setTarget(new TargetType());
        //
        // Now represent the policy as Json
        //
        JSONObject jsonObligation = new JSONObject();
        jsonObligation.put(policyName, policyDefinition);
        addObligation(rule, jsonObligation);
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

    /**
     * For generating target type, we are making an assumption that the
     * policyScope and policyType are the fields that OOF wants to match on.
     *
     * <P>In the future, we would need to receive the Policy Type specification
     * from the PAP so we can dynamically see which fields are matchable.
     *
     * <P>Note: I am making an assumption that the matchable fields are what
     * the OOF wants to query a policy on.
     *
     * @param properties Properties section of policy
     * @return TargetType object
     */
    @SuppressWarnings("unchecked")
    protected TargetType generateTargetType(Map<String, Object> properties) {
        TargetType targetType = new TargetType();
        //
        // Iterate the properties
        //
        for (Entry<String, Object> entrySet : properties.entrySet()) {
            //
            // Find policyScope and policyType
            //
            if (entrySet.getKey().equals("policyScope")) {
                LOGGER.debug("Found policyScope: {}", entrySet.getValue());
                if (entrySet.getValue() instanceof Collection) {
                    targetType.getAnyOf().add(generateMatches((Collection<Object>) entrySet.getValue(),
                            ToscaDictionary.ID_RESOURCE_POLICY_SCOPE_PROPERTY));
                } else if (entrySet.getValue() instanceof String) {
                    targetType.getAnyOf().add(generateMatches(Arrays.asList(entrySet.getValue()),
                            ToscaDictionary.ID_RESOURCE_POLICY_SCOPE_PROPERTY));
                }
            }
            if (entrySet.getKey().equals("policyType")) {
                LOGGER.debug("Found policyType: {}", entrySet.getValue());
                if (entrySet.getValue() instanceof Collection) {
                    targetType.getAnyOf().add(generateMatches((Collection<Object>) entrySet.getValue(),
                            ToscaDictionary.ID_RESOURCE_POLICY_TYPE_PROPERTY));
                } else if (entrySet.getValue() instanceof String) {
                    targetType.getAnyOf().add(generateMatches(Arrays.asList(entrySet.getValue()),
                            ToscaDictionary.ID_RESOURCE_POLICY_TYPE_PROPERTY));
                }
            }
        }

        return targetType;
    }

    protected AnyOfType generateMatches(Collection<Object> matchables, Identifier attributeId) {
        //
        // This is our outer AnyOf - which is an OR
        //
        AnyOfType anyOf = new AnyOfType();
        for (Object matchable : matchables) {
            //
            // Create a match for this
            //
            MatchType match = ToscaPolicyTranslatorUtils.buildMatchTypeDesignator(
                    XACML3.ID_FUNCTION_STRING_EQUAL,
                    matchable.toString(),
                    XACML3.ID_DATATYPE_STRING,
                    attributeId,
                    XACML3.ID_ATTRIBUTE_CATEGORY_RESOURCE);
            //
            // Now create an anyOf (OR)
            //
            anyOf.getAllOf().add(ToscaPolicyTranslatorUtils.buildAllOf(match));
        }
        return anyOf;
    }

    protected RuleType addObligation(RuleType rule, JSONObject jsonPolicy) {
        //
        // Convert the YAML Policy to JSON Object
        //
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("JSON Optimization Policy {}{}", System.lineSeparator(), jsonPolicy);
        }
        //
        // Create an AttributeValue for it
        //
        AttributeValueType value = new AttributeValueType();
        value.setDataType(ToscaDictionary.ID_OBLIGATION_POLICY_MONITORING_DATATYPE.stringValue());
        value.getContent().add(jsonPolicy.toString());
        //
        // Create our AttributeAssignmentExpression where we will
        // store the contents of the policy in JSON format.
        //
        AttributeAssignmentExpressionType expressionType = new AttributeAssignmentExpressionType();
        expressionType.setAttributeId(ToscaDictionary.ID_OBLIGATION_POLICY_MONITORING_CONTENTS.stringValue());
        ObjectFactory factory = new ObjectFactory();
        expressionType.setExpression(factory.createAttributeValue(value));
        //
        // Create an ObligationExpression for it
        //
        ObligationExpressionType obligation = new ObligationExpressionType();
        obligation.setFulfillOn(EffectType.PERMIT);
        obligation.setObligationId(ToscaDictionary.ID_OBLIGATION_REST_BODY.stringValue());
        obligation.getAttributeAssignmentExpression().add(expressionType);
        //
        // Now we can add it into the rule
        //
        ObligationExpressionsType obligations = new ObligationExpressionsType();
        obligations.getObligationExpression().add(obligation);
        rule.setObligationExpressions(obligations);
        return rule;
    }

}
