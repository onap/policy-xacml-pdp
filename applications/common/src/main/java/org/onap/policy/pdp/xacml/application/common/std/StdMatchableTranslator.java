/*-
 * ============LICENSE_START=======================================================
 * ONAP
 * ================================================================================
 * Copyright (C) 2019-2021 AT&T Intellectual Property. All rights reserved.
 * Modifications Copyright (C) 2021, 2024 Nordix Foundation.
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
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import lombok.NoArgsConstructor;
import lombok.Setter;
import oasis.names.tc.xacml._3_0.core.schema.wd_17.AllOfType;
import oasis.names.tc.xacml._3_0.core.schema.wd_17.AnyOfType;
import oasis.names.tc.xacml._3_0.core.schema.wd_17.EffectType;
import oasis.names.tc.xacml._3_0.core.schema.wd_17.PolicyType;
import oasis.names.tc.xacml._3_0.core.schema.wd_17.RuleType;
import oasis.names.tc.xacml._3_0.core.schema.wd_17.TargetType;
import org.apache.commons.lang3.tuple.Pair;
import org.onap.policy.common.endpoints.http.client.HttpClient;
import org.onap.policy.common.utils.coder.CoderException;
import org.onap.policy.common.utils.coder.StandardCoder;
import org.onap.policy.common.utils.coder.StandardYamlCoder;
import org.onap.policy.models.decisions.concepts.DecisionRequest;
import org.onap.policy.models.decisions.concepts.DecisionResponse;
import org.onap.policy.models.tosca.authorative.concepts.ToscaConceptIdentifier;
import org.onap.policy.models.tosca.authorative.concepts.ToscaDataType;
import org.onap.policy.models.tosca.authorative.concepts.ToscaPolicy;
import org.onap.policy.models.tosca.authorative.concepts.ToscaPolicyType;
import org.onap.policy.models.tosca.authorative.concepts.ToscaServiceTemplate;
import org.onap.policy.models.tosca.simple.concepts.JpaToscaServiceTemplate;
import org.onap.policy.pdp.xacml.application.common.OnapObligation;
import org.onap.policy.pdp.xacml.application.common.PolicyApiCaller;
import org.onap.policy.pdp.xacml.application.common.PolicyApiException;
import org.onap.policy.pdp.xacml.application.common.ToscaDictionary;
import org.onap.policy.pdp.xacml.application.common.ToscaPolicyConversionException;
import org.onap.policy.pdp.xacml.application.common.ToscaPolicyTranslatorUtils;
import org.onap.policy.pdp.xacml.application.common.XacmlApplicationException;
import org.onap.policy.pdp.xacml.application.common.matchable.MatchableCallback;
import org.onap.policy.pdp.xacml.application.common.matchable.MatchablePolicyType;
import org.onap.policy.pdp.xacml.application.common.matchable.MatchableProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This standard matchable translator uses Policy Types that contain "matchable" field in order
 * to translate policies.
 *
 * @author pameladragosh
 */
@NoArgsConstructor
public class StdMatchableTranslator extends StdBaseTranslator implements MatchableCallback {

    private static final Logger LOGGER = LoggerFactory.getLogger(StdMatchableTranslator.class);
    private static final StandardYamlCoder standardYamlCoder = new StandardYamlCoder();

    private final Map<ToscaConceptIdentifier, ToscaServiceTemplate> matchablePolicyTypes = new HashMap<>();
    private final Map<ToscaConceptIdentifier, MatchablePolicyType> matchableCache = new HashMap<>();

    @Setter
    private HttpClient apiClient;
    @Setter
    private Path pathForData;

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
     * @param obligations      Collection of obligation objects
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

    @Override
    protected void scanAdvice(Collection<Advice> advice, DecisionResponse decisionResponse) {
        LOGGER.warn("scanAdvice not supported by {}", this.getClass());
    }

    /**
     * scanClosestMatchObligation - scans for the obligation specifically holding policy
     * contents and their details.
     *
     * @param closestMatches Map holding the current set of highest weight policy types
     * @param obligation     Obligation object
     */
    protected void scanClosestMatchObligation(
        Map<String, Map<Integer, List<Pair<String, Map<String, Object>>>>> closestMatches, Obligation obligation) {
        //
        // Create our OnapObligation object
        //
        var onapObligation = new OnapObligation(obligation);
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
    public Object convertPolicy(ToscaPolicy toscaPolicy) throws ToscaPolicyConversionException {
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
        String policyName = String.valueOf(toscaPolicy.getMetadata().get(POLICY_ID));
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
        var coder = new StandardCoder();
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
        var rule = new RuleType();
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
        try (var os = new ByteArrayOutputStream()) {
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

    @Override
    public ToscaPolicyType retrievePolicyType(String derivedFrom) {
        ToscaServiceTemplate template = this.findPolicyType(new ToscaConceptIdentifier(derivedFrom, "1.0.0"));
        if (template == null) {
            LOGGER.error("Could not retrieve Policy Type {}", derivedFrom);
            return null;
        }
        return template.getPolicyTypes().get(derivedFrom);
    }

    @Override
    public ToscaDataType retrieveDataType(String datatype) {
        //
        // Our outer class is not storing the current template being scanned
        //
        LOGGER.error("this retrieveDataType should not be called.");
        return null;
    }

    private record MyMatchableCallback(StdMatchableTranslator translator, ToscaServiceTemplate template)
        implements MatchableCallback {

        @Override
        public ToscaPolicyType retrievePolicyType(String derivedFrom) {
            ToscaPolicyType policyType = this.template.getPolicyTypes().get(derivedFrom);
            if (policyType != null) {
                return policyType;
            }
            return translator.retrievePolicyType(derivedFrom);
        }

        @Override
        public ToscaDataType retrieveDataType(String datatype) {
            return this.template.getDataTypes().get(datatype);
        }

    }

    /**
     * For generating target type, we scan for matchable properties
     * and use those to build the policy.
     *
     * @param policy   the policy
     * @param template template containing the policy
     * @return {@code Pair<TargetType, Integer>} Returns a TargetType and a Total Weight of matchables.
     */
    protected Pair<TargetType, Integer> generateTargetType(ToscaPolicy policy, ToscaServiceTemplate template) {
        //
        // Our return object
        //
        var target = new TargetType();
        //
        // See if we have a matchable in the cache already
        //
        var matchablePolicyType = matchableCache.get(policy.getTypeIdentifier());
        //
        // If not found, create one
        //
        if (matchablePolicyType == null) {
            //
            // Our callback
            //
            var myCallback = new MyMatchableCallback(this, template);
            //
            // Create the matchable
            //
            matchablePolicyType = new MatchablePolicyType(
                template.getPolicyTypes().get(policy.getType()), myCallback);
            //
            // Cache it
            //
            matchableCache.put(policy.getTypeIdentifier(), matchablePolicyType);
        }
        //
        // Fill in the target type with potential matchables
        //
        try {
            fillTargetTypeWithMatchables(target, matchablePolicyType, policy.getProperties());
        } catch (ToscaPolicyConversionException e) {
            LOGGER.error("Could not generate target type", e);
        }
        //
        // There may be a case for default policies there is no weight - need to clean
        // up the target then else PDP will report bad policy missing AnyOf
        //
        int weight = calculateWeight(target);
        LOGGER.debug("Weight is {} for policy {}", weight, policy.getName());
        //
        // Assume the number of AllOf's is the weight for now
        //
        return Pair.of(target, weight);
    }

    @SuppressWarnings("unchecked")
    protected void fillTargetTypeWithMatchables(TargetType target, MatchablePolicyType matchablePolicyType,
                                                Map<String, Object> properties) throws ToscaPolicyConversionException {
        for (Entry<String, Object> entrySet : properties.entrySet()) {
            String propertyName = entrySet.getKey();
            Object propertyValue = entrySet.getValue();
            MatchableProperty matchable = matchablePolicyType.get(propertyName);
            if (matchable != null) {
                //
                // Construct attribute id
                //
                Identifier id = new IdentifierImpl(ToscaDictionary.ID_RESOURCE_MATCHABLE + propertyName);
                //
                // Depending on what type it is, add it into the target
                //
                ToscaPolicyTranslatorUtils.buildAndAppendTarget(target,
                    matchable.getType().generate(propertyValue, id));

                continue;
            }
            //
            // Here is the special case where we look for a Collection of values that may
            // contain potential matchables
            //
            if (propertyValue instanceof List) {
                for (Object listValue : ((List<?>) propertyValue)) {
                    if (listValue instanceof Map) {
                        fillTargetTypeWithMatchables(target, matchablePolicyType, (Map<String, Object>) listValue);
                    }
                }
            } else if (propertyValue instanceof Map) {
                fillTargetTypeWithMatchables(target, matchablePolicyType, (Map<String, Object>) propertyValue);
            }
        }
    }

    protected int calculateWeight(TargetType target) {
        var weight = 0;
        for (AnyOfType anyOf : target.getAnyOf()) {
            for (AllOfType allOf : anyOf.getAllOf()) {
                weight += allOf.getMatch().size();
            }
        }

        return weight;
    }

    /**
     * findPolicyType - given the ToscaConceptIdentifier, finds it in memory, or
     * then tries to find it either locally on disk or pull it from the Policy
     * Lifecycle API the given TOSCA Policy Type.
     *
     * @param policyTypeId ToscaConceptIdentifier to find
     * @return ToscaPolicyType object. Can be null if failure.
     */
    protected ToscaServiceTemplate findPolicyType(ToscaConceptIdentifier policyTypeId) {
        //
        // Is it loaded in memory?
        //
        ToscaServiceTemplate policyTemplate = this.matchablePolicyTypes.get(policyTypeId);
        if (policyTemplate == null) {
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
     * loadPolicyType - Tries to load the given ToscaConceptIdentifier from local
     * storage. If it does not exist, will then attempt to pull from Policy Lifecycle
     * API.
     *
     * @param policyTypeId ToscaConceptIdentifier input
     * @return ToscaPolicyType object. Null if failure.
     */
    protected ToscaServiceTemplate loadPolicyType(ToscaConceptIdentifier policyTypeId) {
        //
        // Construct what the file name should be
        //
        var policyTypePath = this.constructLocalFilePath(policyTypeId);
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
            //
            // Decode the template
            //
            ToscaServiceTemplate template = standardYamlCoder.decode(new String(bytes, StandardCharsets.UTF_8),
                ToscaServiceTemplate.class);
            //
            // Ensure all the fields are setup correctly
            //
            var jtst = new JpaToscaServiceTemplate();
            jtst.fromAuthorative(template);
            return jtst.toAuthorative();
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
     * pullPolicyType - pulls the given ToscaConceptIdentifier from the Policy Lifecycle API.
     * If successful, will store it locally given the policyTypePath.
     *
     * @param policyTypeId   ToscaConceptIdentifier
     * @param policyTypePath Path object to store locally
     * @return ToscaPolicyType object. Null if failure.
     */
    protected synchronized ToscaServiceTemplate pullPolicyType(ToscaConceptIdentifier policyTypeId,
                                                               Path policyTypePath) {
        //
        // This is what we return
        //
        ToscaServiceTemplate policyTemplate = null;
        try {
            var api = new PolicyApiCaller(this.apiClient);

            policyTemplate = api.getPolicyType(policyTypeId);
        } catch (PolicyApiException e) {
            LOGGER.error("Failed to make API call", e);
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
     * @param policyTypeId ToscaConceptIdentifier
     * @return Path object
     */
    protected Path constructLocalFilePath(ToscaConceptIdentifier policyTypeId) {
        return Paths.get(this.pathForData.toAbsolutePath().toString(), policyTypeId.getName() + "-"
            + policyTypeId.getVersion() + ".yaml");
    }
}
