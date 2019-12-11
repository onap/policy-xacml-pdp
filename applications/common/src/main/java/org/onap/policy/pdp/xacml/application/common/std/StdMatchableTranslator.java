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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
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
import org.onap.policy.models.tosca.authorative.concepts.ToscaPolicy;
import org.onap.policy.models.tosca.authorative.concepts.ToscaPolicyType;
import org.onap.policy.models.tosca.authorative.concepts.ToscaPolicyTypeIdentifier;
import org.onap.policy.models.tosca.authorative.concepts.ToscaProperty;
import org.onap.policy.models.tosca.authorative.concepts.ToscaServiceTemplate;
import org.onap.policy.models.tosca.simple.concepts.JpaToscaServiceTemplate;
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
        Collection<ToscaPolicyType> toscaPolicyTypes = this.getPolicyTypes(toscaPolicy.getTypeIdentifier());
        //
        // If we don't have any TOSCA policy types, then we cannot know
        // which properties are matchable.
        //
        if (toscaPolicyTypes.isEmpty()) {
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
        Pair<TargetType, Integer> pairGenerated = generateTargetType(toscaPolicy.getProperties(), toscaPolicyTypes);
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
    protected Pair<TargetType, Integer> generateTargetType(Map<String, Object> properties,
            Collection<ToscaPolicyType> policyTypes) {
        TargetType targetType = new TargetType();
        //
        // Iterate the properties
        //
        int totalWeight = findMatchableFromMap(properties, policyTypes, targetType);
        LOGGER.info("Total weight is {}", totalWeight);
        return Pair.of(targetType, totalWeight);
    }

    @SuppressWarnings("unchecked")
    protected int findMatchableFromList(List<Object> listProperties, Collection<ToscaPolicyType> policyTypes,
            TargetType targetType) {
        LOGGER.info("find matchable in list {}", listProperties);
        int totalWeight = 0;
        for (Object property : listProperties) {
            if (property instanceof List) {
                totalWeight += findMatchableFromList((List<Object>) property, policyTypes, targetType);
            } else if (property instanceof Map) {
                totalWeight += findMatchableFromMap((Map<String, Object>) property, policyTypes, targetType);
            }
        }
        return totalWeight;
    }

    protected int findMatchableFromMap(Map<String, Object> properties, Collection<ToscaPolicyType> policyTypes,
            TargetType targetType) {
        LOGGER.info("find matchable in map {}", properties);
        int totalWeight = 0;
        for (Entry<String, Object> entrySet : properties.entrySet()) {
            //
            // Is this a matchable property?
            //
            if (isMatchable(entrySet.getKey(), policyTypes)) {
                LOGGER.info("Found matchable property {}", entrySet.getKey());
                int weight = generateMatchable(targetType, entrySet.getKey(), entrySet.getValue());
                LOGGER.info("Weight is {}", weight);
                totalWeight += weight;
            } else {
                //
                // Check if we need to search deeper
                //
                totalWeight += checkDeeperForMatchable(entrySet.getValue(), policyTypes, targetType);
            }
        }
        return totalWeight;
    }

    @SuppressWarnings("unchecked")
    protected int checkDeeperForMatchable(Object property, Collection<ToscaPolicyType> policyTypes,
            TargetType targetType) {
        if (property instanceof List) {
            return findMatchableFromList((List<Object>) property, policyTypes, targetType);
        } else if (property instanceof Map) {
            return findMatchableFromMap((Map<String, Object>) property, policyTypes,
                    targetType);
        }
        return 0;
    }

    /**
     * isMatchable - Iterates through available TOSCA Policy Types to determine if a property
     * should be treated as matchable.
     *
     * @param propertyName Name of property
     * @param policyTypes Collection of TOSCA Policy Types to scan
     * @return true if matchable
     */
    protected boolean isMatchable(String propertyName, Collection<ToscaPolicyType> policyTypes) {
        for (ToscaPolicyType policyType : policyTypes) {
            for (Entry<String, ToscaProperty> propertiesEntry : policyType.getProperties().entrySet()) {
                if (checkIsMatchableProperty(propertyName, propertiesEntry)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * checkIsMatchableProperty - checks the policy contents for matchable field. If the metadata doesn't exist,
     * then definitely not. If the property doesn't exist, then definitely not. Otherwise need to have a metadata
     * section with the matchable property set to true.
     *
     * @param propertyName String value of property
     * @param propertiesEntry Section of the TOSCA Policy Type where properties and metadata sections are held
     * @return true if matchable
     */
    protected boolean checkIsMatchableProperty(String propertyName, Entry<String, ToscaProperty> propertiesEntry) {
        if (! propertiesEntry.getKey().equals(propertyName)
                || propertiesEntry.getValue().getMetadata() == null) {
            return false;
        }
        for (Entry<String, String> entrySet : propertiesEntry.getValue().getMetadata().entrySet()) {
            if ("matchable".equals(entrySet.getKey()) && "true".equals(entrySet.getValue())) {
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
     * @return int Weight of the match.
     */
    @SuppressWarnings("unchecked")
    protected int generateMatchable(TargetType targetType, String key, Object value) {
        int weight = 0;
        if (value instanceof Collection) {
            AnyOfType anyOf = generateMatches((Collection<Object>) value,
                    new IdentifierImpl(ToscaDictionary.ID_RESOURCE_MATCHABLE + key));
            if (! anyOf.getAllOf().isEmpty()) {
                targetType.getAnyOf().add(anyOf);
                weight = 1;
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

    /**
     * findPolicyType - given the ToscaPolicyTypeIdentifier, finds it in memory, or
     * then tries to find it either locally on disk or pull it from the Policy
     * Lifecycle API the given TOSCA Policy Type.
     *
     * @param policyTypeId ToscaPolicyTypeIdentifier to find
     * @return ToscaPolicyType object. Can be null if failure.
     */
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

    /**
     * loadPolicyType - Tries to load the given ToscaPolicyTypeIdentifier from local
     * storage. If it does not exist, will then attempt to pull from Policy Lifecycle
     * API.
     *
     * @param policyTypeId ToscaPolicyTypeIdentifier input
     * @return ToscaPolicyType object. Null if failure.
     */
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
        //
        // Success - we have read locally the policy type. Now bring it into our
        // return object.
        //
        LOGGER.info("Read in local policy type {}", policyTypePath.toAbsolutePath());
        try {
            ToscaServiceTemplate serviceTemplate = standardYamlCoder.decode(new String(bytes, StandardCharsets.UTF_8),
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

    /**
     * pullPolicyType - pulls the given ToscaPolicyTypeIdentifier from the Policy Lifecycle API.
     * If successful, will store it locally given the policyTypePath.
     *
     * @param policyTypeId ToscaPolicyTypeIdentifier
     * @param policyTypePath Path object to store locally
     * @return ToscaPolicyType object. Null if failure.
     */
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
            standardYamlCoder.encode(policyTypePath.toFile(), policyType);
        } catch (CoderException e) {
            LOGGER.error("Failed to store {} locally to {}", policyTypeId, policyTypePath, e);
        }
        //
        // Done return the policy type
        //
        return policyType;
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
