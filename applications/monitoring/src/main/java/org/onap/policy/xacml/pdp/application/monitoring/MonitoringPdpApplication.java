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

package org.onap.policy.xacml.pdp.application.monitoring;

import com.att.research.xacml.api.AttributeAssignment;
import com.att.research.xacml.api.DataTypeException;
import com.att.research.xacml.api.Decision;
import com.att.research.xacml.api.Obligation;
import com.att.research.xacml.api.Request;
import com.att.research.xacml.api.Response;
import com.att.research.xacml.api.Result;
import com.att.research.xacml.api.XACML3;
import com.att.research.xacml.std.annotations.RequestParser;
import com.att.research.xacml.util.XACMLPolicyScanner;
import com.att.research.xacml.util.XACMLPolicyWriter;
import com.att.research.xacml.util.XACMLProperties;
import com.google.common.collect.Lists;
import com.google.gson.Gson;

import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;

import oasis.names.tc.xacml._3_0.core.schema.wd_17.AnyOfType;
import oasis.names.tc.xacml._3_0.core.schema.wd_17.AttributeAssignmentExpressionType;
import oasis.names.tc.xacml._3_0.core.schema.wd_17.AttributeValueType;
import oasis.names.tc.xacml._3_0.core.schema.wd_17.EffectType;
import oasis.names.tc.xacml._3_0.core.schema.wd_17.MatchType;
import oasis.names.tc.xacml._3_0.core.schema.wd_17.ObjectFactory;
import oasis.names.tc.xacml._3_0.core.schema.wd_17.ObligationExpressionType;
import oasis.names.tc.xacml._3_0.core.schema.wd_17.ObligationExpressionsType;
import oasis.names.tc.xacml._3_0.core.schema.wd_17.PolicySetType;
import oasis.names.tc.xacml._3_0.core.schema.wd_17.PolicyType;
import oasis.names.tc.xacml._3_0.core.schema.wd_17.RuleType;
import oasis.names.tc.xacml._3_0.core.schema.wd_17.TargetType;

import org.json.JSONObject;
import org.onap.policy.models.decisions.concepts.DecisionRequest;
import org.onap.policy.models.decisions.concepts.DecisionResponse;
import org.onap.policy.pdp.xacml.application.common.ToscaDictionary;
import org.onap.policy.pdp.xacml.application.common.ToscaPolicyConversionException;
import org.onap.policy.pdp.xacml.application.common.ToscaPolicyConverterUtils;
import org.onap.policy.pdp.xacml.application.common.ToscaPolicyTranslator;
import org.onap.policy.pdp.xacml.application.common.XacmlPolicyUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This is the engine class that manages the instance of the XACML PDP engine.
 *
 * <p>It is responsible for initializing it and shutting it down properly in a thread-safe manner.
 *
 *
 * @author pameladragosh
 *
 */
public class MonitoringPdpApplication extends ToscaPolicyTranslator {

    private static final Logger LOGGER = LoggerFactory.getLogger(MonitoringPdpApplication.class);
    private static final String ONAP_MONITORING_BASE_POLICY_TYPE = "onap.Monitoring";
    private static final String ONAP_MONITORING_DERIVED_POLICY_TYPE = "onap.policies.monitoring";

    private Map<String, String> supportedPolicyTypes = new HashMap<>();

    /**
     * Constructor.
     */
    public MonitoringPdpApplication() {
        //
        // By default this supports just Monitoring policy types
        //
        supportedPolicyTypes.put(ONAP_MONITORING_BASE_POLICY_TYPE, "1.0.0");
    }

    @Override
    public String applicationName() {
        return "Monitoring Application";
    }

    @Override
    public List<String> actionDecisionsSupported() {
        return Arrays.asList("configure");
    }

    @Override
    public synchronized List<String> supportedPolicyTypes() {
        return Lists.newArrayList(supportedPolicyTypes.keySet());
    }

    @Override
    public boolean canSupportPolicyType(String policyType, String policyTypeVersion) {
        //
        // For Monitoring, we will attempt to support all versions
        // of the policy type. Since we are only packaging a decision
        // back with a JSON payload of the property contents.
        //
        return (policyType.equals(ONAP_MONITORING_BASE_POLICY_TYPE)
                || policyType.startsWith(ONAP_MONITORING_DERIVED_POLICY_TYPE));
    }

    @Override
    public synchronized void loadPolicies(Map<String, Object> toscaPolicies) {
        try {
            //
            // Convert the policies first
            //
            List<PolicyType> listPolicies = this.convertPolicies(toscaPolicies);
            if (listPolicies.isEmpty()) {
                throw new ToscaPolicyConversionException("Converted 0 policies");
            }
            //
            // Get our properties because we are going to update
            //
            Properties currentProperties = this.getProperties();
            //
            // Read in our Root Policy
            //
            Set<String> roots = XACMLProperties.getRootPolicyIDs(currentProperties);
            if (roots.isEmpty()) {
                throw new ToscaPolicyConversionException("There are NO root policies defined");
            }
            //
            // Really only should be one
            //
            String rootFile = currentProperties.getProperty(roots.iterator().next() + ".file");
            try (InputStream is = new FileInputStream(rootFile)) {
                //
                // Read the Root Policy into memory
                //
                Object policyData = XACMLPolicyScanner.readPolicy(is);
                //
                // Should be a PolicySet
                //
                if (policyData instanceof PolicySetType) {
                    //
                    // Add the referenced policies into a new Root Policy
                    //
                    PolicyType[] newPolicies = listPolicies.toArray(new PolicyType[listPolicies.size()]);
                    PolicySetType newRootPolicy = XacmlPolicyUtils.addPoliciesToXacmlRootPolicy(
                            (PolicySetType) policyData, newPolicies);
                    LOGGER.debug("New ROOT Policy");
                    try (ByteArrayOutputStream os = new ByteArrayOutputStream()) {
                        XACMLPolicyWriter.writePolicyFile(os, newRootPolicy);
                        LOGGER.debug("{}", os);
                    } catch (IOException e) {
                        LOGGER.error("Failed to convert {}", e);
                    }
                    //
                    // Save the new Policies to disk
                    //
                    for (PolicyType policy : newPolicies) {
                        //
                        // Construct the filename
                        //
                        Path refPath = XacmlPolicyUtils.constructUniquePolicyFilename(policy, this.getDataPath());
                        //
                        // Write the policy to disk
                        // Maybe check for an error
                        //
                        XACMLPolicyWriter.writePolicyFile(refPath, policy);
                        //
                        // Save it off
                        //
                        XacmlPolicyUtils.addReferencedPolicy(currentProperties, refPath);
                    }
                    //
                    // Save the root policy to disk
                    //
                    XACMLPolicyWriter.writePolicyFile(Paths.get(rootFile), newRootPolicy);
                    //
                    // Write the policies to disk
                    //
                    XacmlPolicyUtils.storeXacmlProperties(currentProperties,
                            XacmlPolicyUtils.getPropertiesPath(this.getDataPath()));
                    //
                    // Reload the engine
                    //
                    this.createEngine(currentProperties);
                } else {
                    throw new ToscaPolicyConversionException("Root policy isn't a PolicySet");
                }
            }
        } catch (IOException | ToscaPolicyConversionException e) {
            LOGGER.error("Failed to loadPolicies {}", e);
        }
    }

    @Override
    public synchronized DecisionResponse makeDecision(DecisionRequest request) {
        //
        // Convert to a XacmlRequest
        //
        Request xacmlRequest = this.convertRequest(request);
        //
        // Now get a decision
        //
        Response xacmlResponse = this.xacmlDecision(xacmlRequest);
        //
        // Convert to a DecisionResponse
        //
        return this.convertResponse(xacmlResponse);
    }

    @Override
    @SuppressWarnings("unchecked")
    protected List<PolicyType> scanAndConvertPolicies(Map<String, Object> toscaObject)
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
        newPolicyType.setRuleCombiningAlgId(XACML3.ID_RULE_FIRST_APPLICABLE.stringValue());
        //
        // Generate the TargetType
        //
        //
        // There should be a metadata section
        //
        if (! policyDefinition.containsKey("type")) {
            throw new ToscaPolicyConversionException(policyName + " missing type value");
        }
        if (! policyDefinition.containsKey("version")) {
            throw new ToscaPolicyConversionException(policyName + " missing version value");
        }
        TargetType target = this.generateTargetType(policyName,
                policyDefinition.get("type").toString(),
                policyDefinition.get("version").toString());
        newPolicyType.setTarget(target);
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
    private PolicyType fillMetadataSection(PolicyType policy,
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

    private TargetType generateTargetType(String policyId, String policyType, String policyTypeVersion) {
        //
        // Create all the match's that are possible
        //
        // This is for the Policy Id
        //
        MatchType matchPolicyId = ToscaPolicyConverterUtils.buildMatchTypeDesignator(
                XACML3.ID_FUNCTION_STRING_EQUAL,
                policyId,
                XACML3.ID_DATATYPE_STRING,
                ToscaDictionary.ID_RESOURCE_POLICY_ID,
                XACML3.ID_ATTRIBUTE_CATEGORY_RESOURCE);
        //
        // This is for the Policy Type
        //
        MatchType matchPolicyType = ToscaPolicyConverterUtils.buildMatchTypeDesignator(
                XACML3.ID_FUNCTION_STRING_EQUAL,
                policyType,
                XACML3.ID_DATATYPE_STRING,
                ToscaDictionary.ID_RESOURCE_POLICY_TYPE,
                XACML3.ID_ATTRIBUTE_CATEGORY_RESOURCE);
        //
        // This is for the Policy Type version
        //
        MatchType matchPolicyTypeVersion = ToscaPolicyConverterUtils.buildMatchTypeDesignator(
                XACML3.ID_FUNCTION_STRING_EQUAL,
                policyTypeVersion,
                XACML3.ID_DATATYPE_STRING,
                ToscaDictionary.ID_RESOURCE_POLICY_TYPE_VERSION,
                XACML3.ID_ATTRIBUTE_CATEGORY_RESOURCE);
        //
        // This is our outer AnyOf - which is an OR
        //
        AnyOfType anyOf = new AnyOfType();
        //
        // Create AllOf (AND) of just Policy Id
        //
        anyOf.getAllOf().add(ToscaPolicyConverterUtils.buildAllOf(matchPolicyId));
        //
        // Create AllOf (AND) of just Policy Type
        //
        anyOf.getAllOf().add(ToscaPolicyConverterUtils.buildAllOf(matchPolicyType));
        //
        // Create AllOf (AND) of Policy Type and Policy Type Version
        //
        anyOf.getAllOf().add(ToscaPolicyConverterUtils.buildAllOf(matchPolicyType, matchPolicyTypeVersion));
        //
        // Now we can create the TargetType, add the top-level anyOf (OR),
        // and return the value.
        //
        TargetType target = new TargetType();
        target.getAnyOf().add(anyOf);
        return target;
    }

    private RuleType addObligation(RuleType rule, JSONObject jsonPolicy) {
        //
        // Convert the YAML Policy to JSON Object
        //
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("JSON DCAE Policy {}{}", System.lineSeparator(), jsonPolicy);
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

    @Override
    public Request convertRequest(DecisionRequest request) {
        LOGGER.debug("Converting Request {}", request);
        try {
            return RequestParser.parseRequest(MonitoringRequest.createInstance(request));
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
                for (Obligation obligation : xacmlResult.getObligations()) {
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
            } else {
                decisionResponse.setErrorMessage("A better error message");
            }
        }

        return decisionResponse;
    }
}
