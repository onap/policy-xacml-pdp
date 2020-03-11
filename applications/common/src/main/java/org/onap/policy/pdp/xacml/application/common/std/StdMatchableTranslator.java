/*-
 * ============LICENSE_START=======================================================
 * ONAP
 * ================================================================================
 * Copyright (C) 2019-2020 AT&T Intellectual Property. All rights reserved.
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

import com.att.research.xacml.api.Advice;
import com.att.research.xacml.api.Identifier;
import com.att.research.xacml.api.Obligation;
import com.att.research.xacml.api.Request;
import com.att.research.xacml.api.XACML3;
import com.att.research.xacml.std.IdentifierImpl;
import com.att.research.xacml.util.XACMLPolicyWriter;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import lombok.Setter;
import oasis.names.tc.xacml._3_0.core.schema.wd_17.AnyOfType;
import oasis.names.tc.xacml._3_0.core.schema.wd_17.EffectType;
import oasis.names.tc.xacml._3_0.core.schema.wd_17.MatchType;
import oasis.names.tc.xacml._3_0.core.schema.wd_17.PolicyType;
import oasis.names.tc.xacml._3_0.core.schema.wd_17.RuleType;
import oasis.names.tc.xacml._3_0.core.schema.wd_17.TargetType;
import org.apache.commons.lang3.tuple.Pair;
import org.onap.policy.common.endpoints.parameters.RestServerParameters;
import org.onap.policy.common.utils.coder.CoderException;
import org.onap.policy.common.utils.coder.StandardCoder;
import org.onap.policy.common.utils.coder.StandardYamlCoder;
import org.onap.policy.models.decisions.concepts.DecisionRequest;
import org.onap.policy.models.decisions.concepts.DecisionResponse;
import org.onap.policy.models.tosca.authorative.concepts.ToscaDataType;
import org.onap.policy.models.tosca.authorative.concepts.ToscaPolicy;
import org.onap.policy.models.tosca.authorative.concepts.ToscaPolicyType;
import org.onap.policy.models.tosca.authorative.concepts.ToscaPolicyTypeIdentifier;
import org.onap.policy.models.tosca.authorative.concepts.ToscaProperty;
import org.onap.policy.models.tosca.authorative.concepts.ToscaServiceTemplate;
import org.onap.policy.pdp.xacml.application.common.OnapObligation;
import org.onap.policy.pdp.xacml.application.common.PolicyApiCaller;
import org.onap.policy.pdp.xacml.application.common.PolicyApiException;
import org.onap.policy.pdp.xacml.application.common.ToscaDictionary;
import org.onap.policy.pdp.xacml.application.common.ToscaPolicyConversionException;
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
public class StdMatchableTranslator  extends StdBaseTranslator {

    private static final Logger LOGGER = LoggerFactory.getLogger(StdMatchableTranslator.class);
    private static final StandardYamlCoder standardYamlCoder = new StandardYamlCoder();

    private static final String MSG_WEIGHT = "Weight is {}";
    private static final String MSG_WEIGHT_LIST = "Weight list is {}";
    private static final String MSG_WEIGHT_MAP = "Weight map is {}";

    private final Map<ToscaPolicyTypeIdentifier, ToscaServiceTemplate> matchablePolicyTypes = new HashMap<>();
    @Setter
    private RestServerParameters apiRestParameters;
    @Setter
    private Path pathForData;

    public StdMatchableTranslator() {
        super();
    }

    @Override
    public Request convertRequest(DecisionRequest request) throws ToscaPolicyConversionException {
        LOGGER.info("Converting Request {}", request);
        try {
            return StdMatchablePolicyRequest.createInstance(request);
        } catch (XacmlApplicationException e) {
            throw new ToscaPolicyConversionException("Failed to convert DecisionRequest", e);
        }
    }

    /**
     * scanObligations - scans the list of obligations and make appropriate method calls to process
     * obligations.
     *
     * @param obligations Collection of obligation objects
     * @param decisionResponse DecisionResponse object used to store any results from obligations.
     */
    @Override
    protected void scanObligations(Collection<Obligation> obligations, DecisionResponse decisionResponse) {
        //
        // Implementing a crude "closest match" on the results, which means we will strip out
        // any policies that has the lower weight than any of the others.
        //
        // Most likely these are "default" policies with a weight of zero, but not always.
        //
        // It is possible to have multiple policies with an equal weight, that is desired.
        //
        // So we need to track each policy type separately and the weights for each policy.
        //
        // policy-type -> weight -> List({policy-id, policy-content}, {policy-id, policy-content})
        //
        Map<String, Map<Integer, List<Pair<String, Map<String, Object>>>>> closestMatches = new LinkedHashMap<>();
        //
        // Now scan the list of obligations
        //
        for (Obligation obligation : obligations) {
            Identifier obligationId = obligation.getId();
            LOGGER.info("Obligation: {}", obligationId);
            if (ToscaDictionary.ID_OBLIGATION_REST_BODY.equals(obligationId)) {
                scanClosestMatchObligation(closestMatches, obligation);
            } else {
                LOGGER.warn("Unsupported Obligation Id {}", obligation.getId());
            }
        }
        //
        // Now add all the policies to the DecisionResponse
        //
        closestMatches.forEach((thePolicyType, weightMap) ->
            weightMap.forEach((weight, policies) ->
                policies.forEach(policy -> {
                    LOGGER.info("Policy {}", policy);
                    decisionResponse.getPolicies().put(policy.getLeft(), policy.getRight());
                })
            )
        );
    }

    protected void scanAdvice(Collection<Advice> advice, DecisionResponse decisionResponse) {
        LOGGER.warn("scanAdvice not supported by {}", this.getClass());
    }

    /**
     * scanClosestMatchObligation - scans for the obligation specifically holding policy
     * contents and their details.
     *
     * @param closestMatches Map holding the current set of highest weight policy types
     * @param Obligation Obligation object
     */
    protected void scanClosestMatchObligation(
            Map<String, Map<Integer, List<Pair<String, Map<String, Object>>>>> closestMatches, Obligation obligation) {
        //
        // Create our OnapObligation object
        //
        OnapObligation onapObligation = new OnapObligation(obligation);
        //
        // All 4 *should* be there
        //
        if (onapObligation.getPolicyId() == null || onapObligation.getPolicyContent() == null
                || onapObligation.getPolicyType() == null || onapObligation.getWeight() == null) {
            LOGGER.error("Missing an expected attribute in obligation.");
            return;
        }
        //
        // Save the values
        //
        String policyId = onapObligation.getPolicyId();
        String policyType = onapObligation.getPolicyType();
        Map<String, Object> policyContent = onapObligation.getPolicyContentAsMap();
        int policyWeight = onapObligation.getWeight();
        //
        // If the Policy Type exists, get the weight map.
        //
        Map<Integer, List<Pair<String, Map<String, Object>>>> weightMap = closestMatches.get(policyType);
        if (weightMap != null) {
            //
            // Only need to check first one - as we will ensure there is only one weight
            //
            Entry<Integer, List<Pair<String, Map<String, Object>>>> firstEntry =
                    weightMap.entrySet().iterator().next();
            if (policyWeight < firstEntry.getKey()) {
                //
                // Existing policies have a greater weight, so we will not add it
                //
                LOGGER.info("{} is lesser weight {} than current policies, will not return it", policyWeight,
                        firstEntry.getKey());
            } else if (firstEntry.getKey().equals(policyWeight)) {
                //
                // Same weight - we will add it
                //
                LOGGER.info("Same weight {}, adding policy", policyWeight);
                firstEntry.getValue().add(Pair.of(policyId, policyContent));
            } else {
                //
                // The weight is greater, so we need to remove the other policies
                // and point to this one.
                //
                LOGGER.info("New policy has greater weight {}, replacing {}", policyWeight, firstEntry.getKey());
                List<Pair<String, Map<String, Object>>> listPolicies = new LinkedList<>();
                listPolicies.add(Pair.of(policyId, policyContent));
                weightMap.clear();
                weightMap.put(policyWeight, listPolicies);
            }
        } else {
            //
            // Create a new entry
            //
            LOGGER.info("New entry {} weight {}", policyType, policyWeight);
            List<Pair<String, Map<String, Object>>> listPolicies = new LinkedList<>();
            listPolicies.add(Pair.of(policyId, policyContent));
            Map<Integer, List<Pair<String, Map<String, Object>>>> newWeightMap = new LinkedHashMap<>();
            newWeightMap.put(policyWeight, listPolicies);
            closestMatches.put(policyType, newWeightMap);
        }
    }

    @Override
    public PolicyType convertPolicy(ToscaPolicy toscaPolicy) throws ToscaPolicyConversionException {
        //
        // Get the TOSCA Policy Type for this policy
        //
        ToscaServiceTemplate toscaPolicyTypeTemplate = this.findPolicyType(toscaPolicy.getTypeIdentifier());
        //
        // If we don't have any TOSCA policy types, then we cannot know
        // which properties are matchable.
        //
        if (toscaPolicyTypeTemplate == null) {
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
        fillMetadataSection(newPolicyType, toscaPolicy.getMetadata());
        //
        // Set the combining rule
        //
        newPolicyType.setRuleCombiningAlgId(XACML3.ID_RULE_FIRST_APPLICABLE.stringValue());
        //
        // Generate the TargetType - the policy should not be evaluated
        // unless all the matchable properties it cares about are matched.
        //
        Pair<TargetType, Integer> pairGenerated = generateTargetType(toscaPolicy, toscaPolicyTypeTemplate);
        newPolicyType.setTarget(pairGenerated.getLeft());
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
        //
        // Add it as an obligation
        //
        addObligation(newPolicyType, policyName, jsonPolicy, pairGenerated.getRight(), toscaPolicy.getType());
        //
        // Now create the Permit Rule.
        //
        RuleType rule = new RuleType();
        rule.setDescription("Default is to PERMIT if the policy matches.");
        rule.setRuleId(policyName + ":rule");
        rule.setEffect(EffectType.PERMIT);
        rule.setTarget(new TargetType());
        //
        // The rule contains the Condition which adds logic for
        // optional policy-type filtering.
        //
        rule.setCondition(generateConditionForPolicyType(toscaPolicy.getType()));
        //
        // Add the rule to the policy
        //
        newPolicyType.getCombinerParametersOrRuleCombinerParametersOrVariableDefinition().add(rule);
        //
        // Log output of the policy
        //
        try (ByteArrayOutputStream os = new ByteArrayOutputStream()) {
            XACMLPolicyWriter.writePolicyFile(os, newPolicyType);
            LOGGER.info("{}", os);
        } catch (IOException e) {
            LOGGER.error("Failed to create byte array stream", e);
        }
        //
        // Done
        //
        return newPolicyType;
    }

    /**
     * For generating target type, we scan for matchable properties
     * and use those to build the policy.
     *
     * @param properties Properties section of policy
     * @param policyTypes Collection of policy Type to find matchable metadata
     * @return {@code Pair<TargetType, Integer>} Returns a TargetType and a Total Weight of matchables.
     */
    protected Pair<TargetType, Integer> generateTargetType(ToscaPolicy policyType,
            ToscaServiceTemplate policyTemplate) {
        //
        // Our return object
        //
        TargetType targetType = new TargetType();
        //
        // Top-level list of properties
        //
        Map<String, Object> properties = policyType.getProperties();
        //
        // To start, we know these properties are for this specific Policy Type ID/Version
        //
        ToscaPolicyTypeIdentifier propertiesPolicyId = policyType.getTypeIdentifier();
        //
        // Scan the property map for matchables
        //
        int totalWeight = findMatchablesInProperties(properties, propertiesPolicyId, policyTemplate, targetType);
        LOGGER.info("Total weight is {}", totalWeight);
        return Pair.of(targetType, totalWeight);
    }

    protected int findMatchablesInProperties(Map<String, Object> properties,
            ToscaPolicyTypeIdentifier propertiesPolicyId,
            ToscaServiceTemplate policyTemplate,
            TargetType targetType) {
        LOGGER.info("findMatchablesInProperties from policy Type {} {}", propertiesPolicyId, properties);
        //
        // We better have the policy type definition available from the template
        //
        ToscaPolicyType policyType = getToscaPolicyTypeFromTemplate(propertiesPolicyId, policyTemplate);
        if (policyType == null) {
            LOGGER.error("Failed to find policy type in template {}", propertiesPolicyId);
            return 0;
        }
        //
        // Our total weight to return
        //
        int totalWeight = 0;
        for (Entry<String, Object> entrySet : properties.entrySet()) {
            //
            // Find the property details
            //
            Pair<ToscaProperty, ToscaServiceTemplate> property = findProperty(entrySet.getKey(),
                    policyType, propertiesPolicyId, policyTemplate);
            if (property == null) {
                continue;
            }
            ToscaProperty toscaProperty = property.getLeft();
            LOGGER.info("Found property {} with type {} schema {}", entrySet.getKey(), toscaProperty.getType(),
                    (toscaProperty.getEntrySchema() == null ? "null" : toscaProperty.getEntrySchema().getType()));
            //
            // Is it matchable?
            //
            if (checkIsMatchableProperty(toscaProperty)) {
                //
                // This will generate the matchables for the property
                //
                int weight = generateMatchable(targetType, entrySet.getKey(), entrySet.getValue(),
                        property.getLeft(), property.getRight());
                LOGGER.info(MSG_WEIGHT, weight);
                totalWeight += weight;
            } else {
                //
                // Not matchable, but we need to check if this contains list or map of datatypes.
                // Those will need to be searched for matchables.
                //
                if ("list".equals(toscaProperty.getType())) {
                    int weight = findMatchablesInList(entrySet.getKey(), entrySet.getValue(), toscaProperty,
                            policyTemplate, targetType);
                    LOGGER.info(MSG_WEIGHT_LIST, weight);
                    totalWeight += weight;
                } else if ("map".equals(toscaProperty.getType())) {
                    int weight = findMatchablesInMap(entrySet.getKey(), entrySet.getValue(), toscaProperty,
                            policyTemplate, targetType);
                    LOGGER.info(MSG_WEIGHT_MAP, weight);
                    totalWeight += weight;
                }
            }
        }
        return totalWeight;
    }

    @SuppressWarnings("unchecked")
    protected int findMatchablesInList(String listPropertyName, Object listValue, ToscaProperty listProperty,
            ToscaServiceTemplate listTemplate, TargetType targetType) {
        //
        // Don't bother if there is no schema (which should be a problem) or
        // its a list of primitives
        //
        if (listProperty.getEntrySchema() == null) {
            LOGGER.error("No entry schema for list property {}", listPropertyName);
            return 0;
        }
        //
        // If they are primitives, then no need to go through them. ??
        //
        if (isYamlType(listProperty.getEntrySchema().getType())) {
            LOGGER.info("list of primitives");
            return 0;
        }
        //
        // Find the datatype
        //
        ToscaDataType listDataType = listTemplate.getDataTypes().get(listProperty.getEntrySchema().getType());
        if (listDataType == null) {
            LOGGER.error("Unable to find datatype {}", listProperty.getEntrySchema().getType());
            return 0;
        }

        int totalWeight = 0;
        for (Object datatypeValue : ((Collection<Object>)listValue)) {
            //
            // This should be a map - because this is a list of datatypes.
            //
            if (! (datatypeValue instanceof Map)) {
                LOGGER.error("datatype {} value is not a map {}", listDataType.getName(), datatypeValue.getClass());
                continue;
            }
            for (Entry<String, Object> entrySet : ((Map<String, Object>)datatypeValue).entrySet()) {
                ToscaProperty toscaProperty = listDataType.getProperties().get(entrySet.getKey());
                if (toscaProperty == null) {
                    LOGGER.error("Failed to find datatype {} property {}", listDataType.getName(), entrySet.getKey());
                    continue;
                }
                LOGGER.info("Found list property {} with type {} schema {}", entrySet.getKey(), toscaProperty.getType(),
                        (toscaProperty.getEntrySchema() == null ? "null" : toscaProperty.getEntrySchema().getType()));
                //
                // Is it matchable?
                //
                if (checkIsMatchableProperty(toscaProperty)) {
                    //
                    // This will generate the matchables for the property
                    //
                    int weight = generateMatchable(targetType, entrySet.getKey(), entrySet.getValue(),
                            toscaProperty, listTemplate);
                    LOGGER.info(MSG_WEIGHT, weight);
                    totalWeight += weight;
                } else {
                    //
                    // Not matchable, but we need to check if this contains list or map of datatypes.
                    // Those will need to be searched for matchables.
                    //
                    if ("list".equals(toscaProperty.getType())) {
                        int weight = findMatchablesInList(entrySet.getKey(), entrySet.getValue(), toscaProperty,
                                listTemplate, targetType);
                        LOGGER.info(MSG_WEIGHT_LIST, weight);
                        totalWeight += weight;
                    } else if ("map".equals(toscaProperty.getType())) {
                        int weight = findMatchablesInMap(entrySet.getKey(), entrySet.getValue(), toscaProperty,
                                listTemplate, targetType);
                        LOGGER.info(MSG_WEIGHT_MAP, weight);
                        totalWeight += weight;
                    }
                }
            }
        }

        return totalWeight;
    }

    @SuppressWarnings("unchecked")
    protected int findMatchablesInMap(String mapPropertyName, Object mapValue, ToscaProperty mapProperty,
            ToscaServiceTemplate mapTemplate, TargetType targetType) {
        //
        // There needs to be a schema.
        //
        if (mapProperty.getEntrySchema() == null) {
            LOGGER.error("No entry schema for map property {}", mapPropertyName);
            return 0;
        }
        //
        // If they are primitives, then no need to go through them. ??
        //
        if (isYamlType(mapProperty.getEntrySchema().getType())) {
            LOGGER.debug("map property {} is primitives", mapPropertyName);
            return 0;
        }
        //
        // Find the datatype
        //
        ToscaDataType mapDataType = mapTemplate.getDataTypes().get(mapProperty.getEntrySchema().getType());
        if (mapDataType == null) {
            LOGGER.error("Unable to find datatype {}", mapProperty.getEntrySchema().getType());
            return 0;
        }

        int totalWeight = 0;
        for (Entry<String, Object> entrySet : ((Map<String, Object>)mapValue).entrySet()) {
            ToscaProperty toscaProperty = mapDataType.getProperties().get(entrySet.getKey());
            if (toscaProperty == null) {
                LOGGER.error("Failed to find datatype {} property {}", mapDataType.getName(), entrySet.getKey());
                continue;
            }
            LOGGER.info("Found map property {} with type {} schema {}", entrySet.getKey(), toscaProperty.getType(),
                    (toscaProperty.getEntrySchema() == null ? "null" : toscaProperty.getEntrySchema().getType()));
            //
            // Is it matchable?
            //
            if (checkIsMatchableProperty(toscaProperty)) {
                //
                // This will generate the matchables for the property
                //
                int weight = generateMatchable(targetType, entrySet.getKey(), entrySet.getValue(),
                        toscaProperty, mapTemplate);
                LOGGER.info(MSG_WEIGHT, weight);
                totalWeight += weight;
            } else {
                //
                // Not matchable, but we need to check if this contains list or map of datatypes.
                // Those will need to be searched for matchables.
                //
                if ("list".equals(toscaProperty.getType())) {
                    int weight = findMatchablesInList(entrySet.getKey(), entrySet.getValue(), toscaProperty,
                            mapTemplate, targetType);
                    LOGGER.info(MSG_WEIGHT_LIST, weight);
                    totalWeight += weight;
                } else if ("map".equals(toscaProperty.getType())) {
                    int weight = findMatchablesInMap(entrySet.getKey(), entrySet.getValue(), toscaProperty,
                            mapTemplate, targetType);
                    LOGGER.info(MSG_WEIGHT_MAP, weight);
                    totalWeight += weight;
                }
            }
        }

        return totalWeight;
    }

    /**
     * findMatchableProperty - Iterates through available TOSCA Policy Types and return the
     * ToscaProperty and template for the property.
     *
     * @param propertyName Name of property
     * @param policyTypes Collection of TOSCA Policy Types to scan
     * @return ToscaProperty and ToscaServiceTemplate if matchable
     */
    protected Pair<ToscaProperty, ToscaServiceTemplate> findProperty(String propertyName,
            ToscaPolicyType policyType, ToscaPolicyTypeIdentifier propertiesPolicyId,
            ToscaServiceTemplate policyTemplate) {
        //
        // See if the property is defined by the policy template
        //
        ToscaProperty toscaProperty = policyType.getProperties().get(propertyName);
        if (toscaProperty != null) {
            //
            // Does it contain the matchable property and if so its set to true?
            //
            return Pair.of(toscaProperty, policyTemplate);
        }
        LOGGER.debug("property {} is not in policy type {}", propertyName, propertiesPolicyId);
        //
        // Check its parent policy types
        //
        ToscaPolicyTypeIdentifier parentId = getParentDerivedFrom(propertiesPolicyId, policyTemplate);
        while (parentId != null) {
            LOGGER.debug("searching parent policy type {}", parentId);
            //
            // Search the existing template (should be there during runtime)
            //
            ToscaPolicyType parentPolicyType = getParentPolicyType(parentId, policyTemplate);
            if (parentPolicyType != null) {
                toscaProperty = parentPolicyType.getProperties().get(propertyName);
                if (toscaProperty != null) {
                    return Pair.of(toscaProperty, policyTemplate);
                }
                //
                // Move to the next parent
                //
                parentId = getParentDerivedFrom(parentId, policyTemplate);
            } else {
                LOGGER.warn("Parent policy type is not found {}", parentId);
                //
                // Find the parent policy type. During JUnit this may be in a separate
                // file. We hope that during runtime the template is complete.
                //
                ToscaServiceTemplate parentTemplate = findPolicyType(parentId);
                if (parentTemplate != null) {
                    parentPolicyType = getParentPolicyType(parentId, parentTemplate);
                    if (parentPolicyType != null) {
                        toscaProperty = parentPolicyType.getProperties().get(propertyName);
                        if (toscaProperty != null) {
                            return Pair.of(toscaProperty, parentTemplate);
                        }
                    }
                    //
                    // Move to the next parent
                    //
                    parentId = getParentDerivedFrom(parentId, parentTemplate);
                } else {
                    LOGGER.error("Unable to find/pull parent policy type {}", parentId);
                    parentId = null;
                }
            }
        }
        LOGGER.warn("Property {} is NOT found in any template", propertyName);
        return null;
    }

    private ToscaPolicyType getToscaPolicyTypeFromTemplate(ToscaPolicyTypeIdentifier propertiesPolicyId,
            ToscaServiceTemplate policyTemplate) {
        for (Entry<String, ToscaPolicyType> entry : policyTemplate.getPolicyTypes().entrySet()) {
            if (propertiesPolicyId.getName().equals(entry.getKey())
                    && propertiesPolicyId.getVersion().equals(entry.getValue().getVersion())) {
                return entry.getValue();
            }
        }
        return null;
    }

    private boolean isYamlType(String type) {
        return "string".equalsIgnoreCase(type) || "integer".equalsIgnoreCase(type) || "float".equalsIgnoreCase(type)
                || "boolean".equalsIgnoreCase(type) || "timestamp".equalsIgnoreCase(type);
    }

    /**
     * checkIsMatchableProperty - checks the property metadata to see if matchable exists.
     *
     * @param toscaProperty ToscaProperty
     * @return true if matchable
     */
    protected boolean checkIsMatchableProperty(ToscaProperty toscaProperty) {
        if (toscaProperty.getMetadata() == null) {
            return false;
        }
        for (Entry<String, String> entrySet : toscaProperty.getMetadata().entrySet()) {
            if ("matchable".equals(entrySet.getKey()) && "true".equals(entrySet.getValue())) {
                LOGGER.debug("found matchable of type {}", toscaProperty.getType());
                return true;
            }
        }
        return false;
    }

    /**
     * generateMatchable - Given the object, generates list of MatchType objects and add them
     * to the TargetType object. Returns a weight which is the number of AnyOf's generated. The
     * weight can be used to further filter the results for "closest match".
     *
     * @param targetType TargetType object to add matches to
     * @param key Property key
     * @param value Object is the value - which can be a Collection or single Object
     * @param toscaProperty The property that was found
     * @param toscaServiceTemplate The template from which the property was found
     * @return int Weight of the match.
     */
    protected int generateMatchable(TargetType targetType, String key, Object value, ToscaProperty toscaProperty,
            ToscaServiceTemplate toscaServiceTemplate) {
        int weight = 0;
        if (value instanceof Collection) {
            //
            // Further determine how we treat this collection. We will need the schema
            // if it is not available then we have to bail.
            //
            if (toscaProperty.getEntrySchema() == null) {
                LOGGER.error("No schema for property {} of type {}", key, toscaProperty.getType());
            }
            if ("list".equals(toscaProperty.getType())) {
                return generateMatchableList(targetType, key, value, toscaProperty, toscaServiceTemplate);
            }
            if ("map".equals(toscaProperty.getType())) {
                return generateMatchableMap(targetType, key, value, toscaProperty, toscaServiceTemplate);
            }
        } else {
            AnyOfType anyOf = generateMatches(Arrays.asList(value),
                    new IdentifierImpl(ToscaDictionary.ID_RESOURCE_MATCHABLE + key));
            if (! anyOf.getAllOf().isEmpty()) {
                targetType.getAnyOf().add(anyOf);
                weight = 1;
            }
        }
        return weight;
    }

    @SuppressWarnings("unchecked")
    protected int generateMatchableList(TargetType targetType, String key, Object value, ToscaProperty toscaProperty,
            ToscaServiceTemplate toscaServiceTemplate) {
        int weight = 0;
        if (isYamlType(toscaProperty.getEntrySchema().getType())) {
            AnyOfType anyOf = generateMatches((Collection<Object>) value,
                    new IdentifierImpl(ToscaDictionary.ID_RESOURCE_MATCHABLE + key));
            if (! anyOf.getAllOf().isEmpty()) {
                targetType.getAnyOf().add(anyOf);
                weight = 1;
            }
        } else {
            LOGGER.debug("PLD use datatype for list?");
        }
        return weight;
    }

    @SuppressWarnings("unchecked")
    protected int generateMatchableMap(TargetType targetType, String key, Object value, ToscaProperty toscaProperty,
            ToscaServiceTemplate toscaServiceTemplate) {
        int weight = 0;
        if (isYamlType(toscaProperty.getEntrySchema().getType())) {
            //
            // PLD TODO - this won't work. Right now there are no maps being used to match.
            // need to investigate whether we really can support that situation.
            //
            AnyOfType anyOf = generateMatches((Collection<Object>) value,
                    new IdentifierImpl(ToscaDictionary.ID_RESOURCE_MATCHABLE + key));
            if (! anyOf.getAllOf().isEmpty()) {
                targetType.getAnyOf().add(anyOf);
                weight = 1;
            }
        } else {
            LOGGER.debug("PLD use datatype for map?");
        }
        return weight;
    }

    /**
     * generateMatches - Goes through the collection of objects, creates a MatchType object
     * for each object and associates it with the given attribute Id. Returns the AnyOfType
     * object that contains all the generated MatchType objects.
     *
     * @param matchables Collection of object to generate MatchType from
     * @param attributeId Given attribute Id for each MatchType
     * @return AnyOfType object
     */
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
            // We should add datetime support. But to do that we need
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

    private ToscaPolicyTypeIdentifier getParentDerivedFrom(ToscaPolicyTypeIdentifier policyTypeId,
            ToscaServiceTemplate template) {
        for (Entry<String, ToscaPolicyType> entrySet : template.getPolicyTypes().entrySet()) {
            ToscaPolicyType policyType = entrySet.getValue();
            if (entrySet.getKey().equals(policyTypeId.getName())
                    && policyType.getVersion().equals(policyTypeId.getVersion())
                    && ! "tosca.policies.Root".equals(policyType.getDerivedFrom())) {
                return new ToscaPolicyTypeIdentifier(policyType.getDerivedFrom(), "1.0.0");
            }
        }

        return null;
    }

    private ToscaPolicyType getParentPolicyType(ToscaPolicyTypeIdentifier policyTypeId, ToscaServiceTemplate template) {
        for (Entry<String, ToscaPolicyType> entrySet : template.getPolicyTypes().entrySet()) {
            ToscaPolicyType policyType = entrySet.getValue();
            if (entrySet.getKey().equals(policyTypeId.getName())
                    && policyType.getVersion().equals(policyTypeId.getVersion())) {
                return policyType;
            }
        }
        return null;
    }

    /**
     * findPolicyType - given the ToscaPolicyTypeIdentifier, finds it in memory, or
     * then tries to find it either locally on disk or pull it from the Policy
     * Lifecycle API the given TOSCA Policy Type.
     *
     * @param policyTypeId ToscaPolicyTypeIdentifier to find
     * @return ToscaPolicyType object. Can be null if failure.
     */
    private ToscaServiceTemplate findPolicyType(ToscaPolicyTypeIdentifier policyTypeId) {
        //
        // Is it loaded in memory?
        //
        ToscaServiceTemplate policyTemplate = this.matchablePolicyTypes.get(policyTypeId);
        if (policyTemplate == null)  {
            //
            // Load the policy
            //
            policyTemplate = this.loadPolicyType(policyTypeId);
            //
            // Save it
            //
            if (policyTemplate != null) {
                this.matchablePolicyTypes.put(policyTypeId, policyTemplate);
            }
        }
        //
        // Yep return it
        //
        return policyTemplate;
    }

    /**
     * loadPolicyType - Tries to load the given ToscaPolicyTypeIdentifier from local
     * storage. If it does not exist, will then attempt to pull from Policy Lifecycle
     * API.
     *
     * @param policyTypeId ToscaPolicyTypeIdentifier input
     * @return ToscaPolicyType object. Null if failure.
     */
    private ToscaServiceTemplate loadPolicyType(ToscaPolicyTypeIdentifier policyTypeId) {
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
        //
        // Success - we have read locally the policy type. Now bring it into our
        // return object.
        //
        LOGGER.info("Read in local policy type {}", policyTypePath.toAbsolutePath());
        try {
            return standardYamlCoder.decode(new String(bytes, StandardCharsets.UTF_8),
                    ToscaServiceTemplate.class);
        } catch (CoderException e) {
            LOGGER.error("Failed to decode tosca template for {}", policyTypePath, e);
        }
        //
        // Hopefully we never get here
        //
        LOGGER.error("Failed to find/load policy type {}", policyTypeId);
        return null;
    }

    /**
     * pullPolicyType - pulls the given ToscaPolicyTypeIdentifier from the Policy Lifecycle API.
     * If successful, will store it locally given the policyTypePath.
     *
     * @param policyTypeId ToscaPolicyTypeIdentifier
     * @param policyTypePath Path object to store locally
     * @return ToscaPolicyType object. Null if failure.
     */
    private synchronized ToscaServiceTemplate pullPolicyType(ToscaPolicyTypeIdentifier policyTypeId,
            Path policyTypePath) {
        //
        // This is what we return
        //
        ToscaServiceTemplate policyTemplate = null;
        try {
            PolicyApiCaller api = new PolicyApiCaller(this.apiRestParameters);

            policyTemplate = api.getPolicyType(policyTypeId);
        } catch (PolicyApiException e) {
            LOGGER.error("Failed to make API call", e);
            LOGGER.error("parameters: {} ", this.apiRestParameters);
            return null;
        }
        LOGGER.info("Successfully pulled {}", policyTypeId);
        //
        // Store it locally
        //
        try {
            standardYamlCoder.encode(policyTypePath.toFile(), policyTemplate);
        } catch (CoderException e) {
            LOGGER.error("Failed to store {} locally to {}", policyTypeId, policyTypePath, e);
        }
        //
        // Done return the policy type
        //
        return policyTemplate;
    }

    /**
     * constructLocalFilePath - common method to ensure the name of the local file for the
     * policy type is the same.
     *
     * @param policyTypeId ToscaPolicyTypeIdentifier
     * @return Path object
     */
    private Path constructLocalFilePath(ToscaPolicyTypeIdentifier policyTypeId) {
        return Paths.get(this.pathForData.toAbsolutePath().toString(), policyTypeId.getName() + "-"
                + policyTypeId.getVersion() + ".yaml");
    }
}
