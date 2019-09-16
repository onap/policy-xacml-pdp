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
import com.att.research.xacml.std.IdentifierImpl;
import com.att.research.xacml.util.XACMLPolicyWriter;
import com.google.gson.Gson;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import lombok.Setter;
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
import org.onap.policy.common.endpoints.parameters.RestServerParameters;
import org.onap.policy.common.utils.coder.CoderException;
import org.onap.policy.common.utils.coder.StandardCoder;
import org.onap.policy.models.decisions.concepts.DecisionRequest;
import org.onap.policy.models.decisions.concepts.DecisionResponse;
import org.onap.policy.models.tosca.authorative.concepts.ToscaPolicy;
import org.onap.policy.models.tosca.authorative.concepts.ToscaPolicyType;
import org.onap.policy.models.tosca.authorative.concepts.ToscaPolicyTypeIdentifier;
import org.onap.policy.models.tosca.authorative.concepts.ToscaProperty;
import org.onap.policy.models.tosca.authorative.concepts.ToscaServiceTemplate;
import org.onap.policy.models.tosca.simple.concepts.JpaToscaServiceTemplate;
import org.onap.policy.pdp.xacml.application.common.PolicyApiCaller;
import org.onap.policy.pdp.xacml.application.common.PolicyApiException;
import org.onap.policy.pdp.xacml.application.common.ToscaDictionary;
import org.onap.policy.pdp.xacml.application.common.ToscaPolicyConversionException;
import org.onap.policy.pdp.xacml.application.common.ToscaPolicyTranslator;
import org.onap.policy.pdp.xacml.application.common.ToscaPolicyTranslatorUtils;
import org.onap.policy.pdp.xacml.application.common.XacmlApplicationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This standard matchable translator uses Policy Types that contain "matchable" field in order
 * to translate policies.
 *
 * @author pameladragosh
 *
 */
public class StdMatchableTranslator implements ToscaPolicyTranslator {

    private static final Logger LOGGER = LoggerFactory.getLogger(StdMatchableTranslator.class);
    private static final String POLICY_ID = "policy-id";
    private static final StandardCoder standardCoder = new StandardCoder();

    private final Map<ToscaPolicyTypeIdentifier, ToscaPolicyType> matchablePolicyTypes = new HashMap<>();
    @Setter
    private RestServerParameters apiRestParameters;
    @Setter
    private Path pathForData;

    public StdMatchableTranslator() {
        super();
    }

    @Override
    public Request convertRequest(DecisionRequest request) {
        LOGGER.info("Converting Request {}", request);
        try {
            return StdMatchablePolicyRequest.createInstance(request);
        } catch (XacmlApplicationException e) {
            LOGGER.error("Failed to convert DecisionRequest: {}", e);
        }
        //
        // TODO throw exception
        //
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
            LOGGER.info("Obligation: {}", obligation);
            for (AttributeAssignment assignment : obligation.getAttributeAssignments()) {
                LOGGER.info("Attribute Assignment: {}", assignment);
                //
                // We care about the content attribute
                //
                if (! ToscaDictionary.ID_OBLIGATION_POLICY_MONITORING_CONTENTS
                        .equals(assignment.getAttributeId())) {
                    //
                    // If its not there, move on
                    //
                    continue;
                }
                //
                // The contents are in Json form
                //
                Object stringContents = assignment.getAttributeValue().getValue();
                if (LOGGER.isInfoEnabled()) {
                    LOGGER.info("Policy contents: {}{}", System.lineSeparator(), stringContents);
                }
                //
                // Let's parse it into a map using Gson
                //
                Gson gson = new Gson();
                @SuppressWarnings("unchecked")
                Map<String, Object> result = gson.fromJson(stringContents.toString() ,Map.class);
                //
                // Find the metadata section
                //
                @SuppressWarnings("unchecked")
                Map<String, Object> metadata = (Map<String, Object>) result.get("metadata");
                if (metadata != null) {
                    decisionResponse.getPolicies().put(metadata.get(POLICY_ID).toString(), result);
                } else {
                    LOGGER.error("Missing metadata section in policy contained in obligation.");
                }
            }
        }

    }

    @Override
    public PolicyType convertPolicy(ToscaPolicy toscaPolicy) throws ToscaPolicyConversionException {
        //
        // Get the TOSCA Policy Type for this policy
        //
        Collection<ToscaPolicyType> policyTypes = this.getPolicyTypes(toscaPolicy.getTypeIdentifier());
        //
        // If we don't have any policy types, then we cannot know
        // which properties are matchable.
        //
        if (policyTypes.isEmpty()) {
            throw new ToscaPolicyConversionException(
                    "Cannot retrieve Policy Type definition for policy " + toscaPolicy.getName());
        }
        //
        // Policy name should be at the root
        //
        String policyName = toscaPolicy.getMetadata().get(POLICY_ID);
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
        // Set the combining rule
        //
        newPolicyType.setRuleCombiningAlgId(XACML3.ID_RULE_FIRST_APPLICABLE.stringValue());
        //
        // Generate the TargetType
        //
        newPolicyType.setTarget(generateTargetType(toscaPolicy.getProperties(), policyTypes));
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
        StandardCoder coder = new StandardCoder();
        String jsonPolicy;
        try {
            jsonPolicy = coder.encode(toscaPolicy);
        } catch (CoderException e) {
            throw new ToscaPolicyConversionException("Failed to encode policy to json", e);
        }
        addObligation(rule, jsonPolicy);
        //
        // Add the rule to the policy
        //
        newPolicyType.getCombinerParametersOrRuleCombinerParametersOrVariableDefinition().add(rule);
        //
        // Return our new policy
        //
        try (ByteArrayOutputStream os = new ByteArrayOutputStream()) {
            XACMLPolicyWriter.writePolicyFile(os, newPolicyType);
            LOGGER.info("{}", os);
        } catch (IOException e) {
            LOGGER.error("Failed to create byte array stream", e);
        }
        return newPolicyType;
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
        if (! map.containsKey(POLICY_ID)) {
            throw new ToscaPolicyConversionException(policy.getPolicyId() + " missing metadata policy-id");
        } else {
            //
            // Do nothing here - the XACML PolicyId is used from TOSCA Policy Name field
            //
        }
        if (! map.containsKey("policy-version")) {
            throw new ToscaPolicyConversionException(policy.getPolicyId() + " missing metadata policy-version");
        } else {
            //
            // Add in the Policy Version
            //
            policy.setVersion(map.get("policy-version"));
        }
        return policy;
    }

    /**
     * For generating target type, we are scan for matchable properties
     * and use those to build the policy.
     *
     * @param properties Properties section of policy
     * @param policyTypes Collection of policy Type to find matchable metadata
     * @return TargetType object
     */
    @SuppressWarnings("unchecked")
    protected TargetType generateTargetType(Map<String, Object> properties, Collection<ToscaPolicyType> policyTypes) {
        TargetType targetType = new TargetType();
        //
        // Iterate the properties
        //
        for (Entry<String, Object> entrySet : properties.entrySet()) {
            //
            // Find matchable properties
            //
            if (isMatchable(entrySet.getKey(), policyTypes)) {
                LOGGER.info("Found matchable property {}", entrySet.getValue());
                if (entrySet.getValue() instanceof Collection) {
                    AnyOfType anyOf = generateMatches((Collection<Object>) entrySet.getValue(),
                            new IdentifierImpl(ToscaDictionary.ID_RESOURCE_MATCHABLE + entrySet.getKey()));
                    if (! anyOf.getAllOf().isEmpty()) {
                        targetType.getAnyOf().add(anyOf);
                    }
                } else {
                    AnyOfType anyOf = generateMatches(Arrays.asList(entrySet.getValue()),
                            new IdentifierImpl(ToscaDictionary.ID_RESOURCE_MATCHABLE + entrySet.getKey()));
                    if (! anyOf.getAllOf().isEmpty()) {
                        targetType.getAnyOf().add(anyOf);
                    }
                }
            }
        }

        return targetType;
    }

    protected boolean isMatchable(String propertyName, Collection<ToscaPolicyType> policyTypes) {
        for (ToscaPolicyType policyType : policyTypes) {
            for (Entry<String, ToscaProperty> propertiesEntry : policyType.getProperties().entrySet()) {
                if (! propertiesEntry.getKey().equals(propertyName)
                        || propertiesEntry.getValue().getMetadata() == null) {
                    continue;
                }
                for (Entry<String, String> entrySet : propertiesEntry.getValue().getMetadata().entrySet()) {
                    if ("matchable".equals(entrySet.getKey()) && "true".equals(entrySet.getValue())) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    protected AnyOfType generateMatches(Collection<Object> matchables, Identifier attributeId) {
        //
        // This is our outer AnyOf - which is an OR
        //
        AnyOfType anyOf = new AnyOfType();
        for (Object matchable : matchables) {
            //
            // Default to string
            //
            Identifier idFunction = XACML3.ID_FUNCTION_STRING_EQUAL;
            Identifier idDatatype = XACML3.ID_DATATYPE_STRING;
            //
            // See if we are another datatype
            //
            // TODO We should add datetime support. But to do that we need
            // probably more metadata to describe how that would be translated.
            //
            if (matchable instanceof Integer) {
                idFunction = XACML3.ID_FUNCTION_INTEGER_EQUAL;
                idDatatype = XACML3.ID_DATATYPE_INTEGER;
            } else if (matchable instanceof Double) {
                idFunction = XACML3.ID_FUNCTION_DOUBLE_EQUAL;
                idDatatype = XACML3.ID_DATATYPE_DOUBLE;
            } else if (matchable instanceof Boolean) {
                idFunction = XACML3.ID_FUNCTION_BOOLEAN_EQUAL;
                idDatatype = XACML3.ID_DATATYPE_BOOLEAN;
            }
            //
            // Create a match for this
            //
            MatchType match = ToscaPolicyTranslatorUtils.buildMatchTypeDesignator(
                    idFunction,
                    matchable.toString(),
                    idDatatype,
                    attributeId,
                    XACML3.ID_ATTRIBUTE_CATEGORY_RESOURCE);
            //
            // Now create an anyOf (OR)
            //
            anyOf.getAllOf().add(ToscaPolicyTranslatorUtils.buildAllOf(match));
        }
        return anyOf;
    }

    protected RuleType addObligation(RuleType rule, String jsonPolicy) {
        //
        // Convert the YAML Policy to JSON Object
        //
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info("JSON Optimization Policy {}{}", System.lineSeparator(), jsonPolicy);
        }
        //
        // Create an AttributeValue for it
        //
        AttributeValueType value = new AttributeValueType();
        value.setDataType(ToscaDictionary.ID_OBLIGATION_POLICY_MONITORING_DATATYPE.stringValue());
        value.getContent().add(jsonPolicy);
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


    /**
     * Get Policy Type definitions. This could be previously loaded, or could be
     * stored in application path, or may need to be pulled from the API.
     *
     *
     * @param policyTypeId Policy Type Id
     * @return A list of PolicyTypes
     */
    private List<ToscaPolicyType> getPolicyTypes(ToscaPolicyTypeIdentifier policyTypeId) {
        //
        // Create identifier from the policy
        //
        ToscaPolicyTypeIdentifier typeId = new ToscaPolicyTypeIdentifier(policyTypeId);
        //
        // Find the Policy Type
        //
        ToscaPolicyType policyType = findPolicyType(typeId);
        if (policyType == null)  {
            return Collections.emptyList();
        }
        //
        // Create our return object
        //
        List<ToscaPolicyType> listTypes = new ArrayList<>();
        listTypes.add(policyType);
        //
        // Look for parent policy types that could also contain matchable properties
        //
        ToscaPolicyType childPolicyType = policyType;
        while (! childPolicyType.getDerivedFrom().startsWith("tosca.policies.Root")) {
            //
            // Create parent policy type id.
            //
            // We will have to assume the same version between child and the
            // parent policy type it derives from.
            //
            // Or do we assume 1.0.0?
            //
            ToscaPolicyTypeIdentifier parentId = new ToscaPolicyTypeIdentifier(childPolicyType.getDerivedFrom(),
                    "1.0.0");
            //
            // Find the policy type
            //
            ToscaPolicyType parentPolicyType = findPolicyType(parentId);
            if (parentPolicyType == null) {
                //
                // Probably would be best to throw an exception and
                // return nothing back.
                //
                // But instead we will log a warning
                //
                LOGGER.warn("Missing parent policy type - proceeding anyway {}", parentId);
                //
                // Break the loop
                //
                break;
            }
            //
            // Great save it
            //
            listTypes.add(parentPolicyType);
            //
            // Move to the next parent
            //
            childPolicyType = parentPolicyType;
        }


        return listTypes;
    }

    private ToscaPolicyType findPolicyType(ToscaPolicyTypeIdentifier policyTypeId) {
        //
        // Is it loaded in memory?
        //
        ToscaPolicyType policyType = this.matchablePolicyTypes.get(policyTypeId);
        if (policyType == null)  {
            //
            // Load the policy
            //
            policyType = this.loadPolicyType(policyTypeId);
        }
        //
        // Yep return it
        //
        return policyType;
    }

    private ToscaPolicyType loadPolicyType(ToscaPolicyTypeIdentifier policyTypeId) {
        //
        // Construct what the file name should be
        //
        Path policyTypePath = this.constructLocalFilePath(policyTypeId);
        //
        // See if it exists
        //
        byte[] bytes;
        try {
            //
            // If it exists locally, read the bytes in
            //
            bytes = Files.readAllBytes(policyTypePath);
        } catch (IOException e) {
            //
            // Does not exist locally, so let's GET it from the policy api
            //
            LOGGER.error("PolicyType not found in data area yet {}", policyTypePath, e);
            //
            // So let's pull it from API REST call and save it locally
            //
            return this.pullPolicyType(policyTypeId, policyTypePath);
        }
        LOGGER.info("Read in local policy type {}", policyTypePath.toAbsolutePath());
        try {
            ToscaServiceTemplate serviceTemplate = standardCoder.decode(new String(bytes, StandardCharsets.UTF_8),
                    ToscaServiceTemplate.class);
            JpaToscaServiceTemplate jtst = new JpaToscaServiceTemplate();
            jtst.fromAuthorative(serviceTemplate);
            ToscaServiceTemplate completedJtst = jtst.toAuthorative();
            //
            // Search for our Policy Type, there really only should be one but
            // this is returned as a map.
            //
            for ( Entry<String, ToscaPolicyType> entrySet : completedJtst.getPolicyTypes().entrySet()) {
                ToscaPolicyType entryPolicyType = entrySet.getValue();
                if (policyTypeId.getName().equals(entryPolicyType.getName())
                        && policyTypeId.getVersion().equals(entryPolicyType.getVersion())) {
                    LOGGER.info("Found existing local policy type {} {}", entryPolicyType.getName(),
                            entryPolicyType.getVersion());
                    //
                    // Just simply return the policy type right here
                    //
                    return entryPolicyType;
                } else {
                    LOGGER.warn("local policy type contains different name version {} {}", entryPolicyType.getName(),
                            entryPolicyType.getVersion());
                }
            }
            //
            // This would be an error, if the file stored does not match what its supposed to be
            //
            LOGGER.error("Existing policy type file does not contain right name and version");
        } catch (CoderException e) {
            LOGGER.error("Failed to decode tosca template for {}", policyTypePath, e);
        }
        //
        // Hopefully we never get here
        //
        LOGGER.error("Failed to find/load policy type {}", policyTypeId);
        return null;
    }

    private synchronized ToscaPolicyType pullPolicyType(ToscaPolicyTypeIdentifier policyTypeId, Path policyTypePath) {
        //
        // This is what we return
        //
        ToscaPolicyType policyType = null;
        try {
            PolicyApiCaller api = new PolicyApiCaller(this.apiRestParameters);

            policyType = api.getPolicyType(policyTypeId);
        } catch (PolicyApiException e) {
            LOGGER.error("Failed to make API call", e);
            LOGGER.error("parameters: {} ", this.apiRestParameters);
            return null;
        }
        //
        // Store it locally
        //
        try {
            standardCoder.encode(policyTypePath.toFile(), policyType);
        } catch (CoderException e) {
            LOGGER.error("Failed to store {} locally to {}", policyTypeId, policyTypePath, e);
        }
        //
        // Done return the policy type
        //
        return policyType;
    }

    private Path constructLocalFilePath(ToscaPolicyTypeIdentifier policyTypeId) {
        return Paths.get(this.pathForData.toAbsolutePath().toString(), policyTypeId.getName() + "-"
                + policyTypeId.getVersion() + ".json");
    }
}
