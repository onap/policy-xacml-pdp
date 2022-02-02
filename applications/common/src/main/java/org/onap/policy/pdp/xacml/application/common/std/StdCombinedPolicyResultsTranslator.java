/*-
 * ============LICENSE_START=======================================================
 * ONAP
 * ================================================================================
 * Copyright (C) 2019-2022 AT&T Intellectual Property. All rights reserved.
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
import com.att.research.xacml.api.DataTypeException;
import com.att.research.xacml.api.Identifier;
import com.att.research.xacml.api.Obligation;
import com.att.research.xacml.api.Request;
import com.att.research.xacml.api.XACML3;
import com.att.research.xacml.std.annotations.RequestParser;
import com.google.common.base.Strings;
import java.util.Collection;
import java.util.Map;
import lombok.NoArgsConstructor;
import oasis.names.tc.xacml._3_0.core.schema.wd_17.AnyOfType;
import oasis.names.tc.xacml._3_0.core.schema.wd_17.EffectType;
import oasis.names.tc.xacml._3_0.core.schema.wd_17.PolicyType;
import oasis.names.tc.xacml._3_0.core.schema.wd_17.RuleType;
import oasis.names.tc.xacml._3_0.core.schema.wd_17.TargetType;
import org.onap.policy.common.utils.coder.CoderException;
import org.onap.policy.common.utils.coder.StandardCoder;
import org.onap.policy.models.decisions.concepts.DecisionRequest;
import org.onap.policy.models.decisions.concepts.DecisionResponse;
import org.onap.policy.models.tosca.authorative.concepts.ToscaPolicy;
import org.onap.policy.pdp.xacml.application.common.OnapObligation;
import org.onap.policy.pdp.xacml.application.common.ToscaDictionary;
import org.onap.policy.pdp.xacml.application.common.ToscaPolicyConversionException;
import org.onap.policy.pdp.xacml.application.common.ToscaPolicyTranslatorUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@NoArgsConstructor
public class StdCombinedPolicyResultsTranslator extends StdBaseTranslator {

    private static final Logger LOGGER = LoggerFactory.getLogger(StdCombinedPolicyResultsTranslator.class);

    @Override
    public Object convertPolicy(ToscaPolicy toscaPolicy) throws ToscaPolicyConversionException {
        //
        // Sanity checks
        //
        if (toscaPolicy == null) {
            throw new ToscaPolicyConversionException("Cannot convert a NULL policy");
        }
        if (toscaPolicy.getMetadata() == null) {
            throw new ToscaPolicyConversionException("Cannot convert a policy with missing metadata section");
        }
        //
        // Get the policy Id
        //
        String policyId = toscaPolicy.getMetadata().get(POLICY_ID);
        //
        // Set it as the policy ID
        //
        var newPolicyType = new PolicyType();
        newPolicyType.setPolicyId(policyId);
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
        var target = this.generateTargetType(policyId, toscaPolicy.getType(), toscaPolicy.getVersion());
        newPolicyType.setTarget(target);
        //
        // Now create the Permit Rule
        // No target since the policy has a target
        // With obligations.
        //
        var rule = new RuleType();
        rule.setDescription("Default is to PERMIT if the policy matches.");
        rule.setRuleId(policyId + ":rule");
        rule.setEffect(EffectType.PERMIT);
        rule.setTarget(new TargetType());
        //
        // Now represent the policy as Json
        //
        var coder = new StandardCoder();
        String jsonPolicy;
        try {
            jsonPolicy = coder.encode(toscaPolicy);
        } catch (CoderException e) {
            throw new ToscaPolicyConversionException(e);
        }
        addObligation(rule, policyId, jsonPolicy, null, toscaPolicy.getType());
        //
        // Add the rule to the policy
        //
        newPolicyType.getCombinerParametersOrRuleCombinerParametersOrVariableDefinition().add(rule);
        //
        // Return our new policy
        //
        return newPolicyType;
    }

    @Override
    public Request convertRequest(DecisionRequest request) throws ToscaPolicyConversionException {
        LOGGER.info("Converting Request {}", request);
        try {
            return RequestParser.parseRequest(StdCombinedPolicyRequest.createInstance(request));
        } catch (IllegalArgumentException | IllegalAccessException | DataTypeException e) {
            throw new ToscaPolicyConversionException("Failed to parse request", e);
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
        for (Obligation obligation : obligations) {
            Identifier obligationId = obligation.getId();
            LOGGER.info("Obligation: {}", obligationId);
            if (ToscaDictionary.ID_OBLIGATION_REST_BODY.equals(obligationId)) {
                scanContentObligation(obligation, decisionResponse);
            }
        }
    }

    /**
     * scanAdvice - not implemented in this class.
     *
     * @param advice Collection of advice objects
     * @param decisionResponse DecisionResponse object
     */
    @Override
    protected void scanAdvice(Collection<Advice> advice, DecisionResponse decisionResponse) {
        //
        // By default there are no advice supported in this object. Please override and provide
        // any needed functionality.
        //
        LOGGER.warn("Advice found - not supported in this class {}", this.getClass());
    }

    /**
     * scanContentObligation - scans the specific obligation for policy-id and policy-content.
     *
     * @param obligation Obligation incoming obligation object
     * @param decisionResponse DecisionResponse object
     */
    protected void scanContentObligation(Obligation obligation, DecisionResponse decisionResponse) {
        //
        // Create our OnapObligation which will scan for attributes
        //
        var onapObligation = new OnapObligation(obligation);
        //
        // Get the attributes we care about
        //
        String policyId = onapObligation.getPolicyId();
        Map<String, Object> policyContent = onapObligation.getPolicyContentAsMap();
        //
        // Sanity check that we got the attributes we care about. NOTE: This translator
        // ensures that these are set when convertPolicy is called.
        //
        if (! Strings.isNullOrEmpty(policyId) && !policyContent.isEmpty()) {
            decisionResponse.getPolicies().put(policyId, policyContent);
        } else {
            LOGGER.error("Missing obligation policyId {} or policyContent {}", policyId,
                    policyContent == null ? "null" : policyContent.size());
        }
    }

    /**
     * generateTargetType - Generates a TargetType object for the policy-id and policy-type.
     *
     * @param policyId String policy-id
     * @param policyType String policy type
     * @param policyTypeVersion String policy type version
     * @return TargetType object
     */
    protected TargetType generateTargetType(String policyId, String policyType, String policyTypeVersion) {
        //
        // Create all the match's that are possible
        //
        // This is for the Policy Id
        //
        var matchPolicyId = ToscaPolicyTranslatorUtils.buildMatchTypeDesignator(
                XACML3.ID_FUNCTION_STRING_EQUAL,
                policyId,
                XACML3.ID_DATATYPE_STRING,
                ToscaDictionary.ID_RESOURCE_POLICY_ID,
                XACML3.ID_ATTRIBUTE_CATEGORY_RESOURCE);
        //
        // This is for the Policy Type
        //
        var matchPolicyType = ToscaPolicyTranslatorUtils.buildMatchTypeDesignator(
                XACML3.ID_FUNCTION_STRING_EQUAL,
                policyType,
                XACML3.ID_DATATYPE_STRING,
                ToscaDictionary.ID_RESOURCE_POLICY_TYPE,
                XACML3.ID_ATTRIBUTE_CATEGORY_RESOURCE);
        //
        // This is for the Policy Type version
        //
        var matchPolicyTypeVersion = ToscaPolicyTranslatorUtils.buildMatchTypeDesignator(
                XACML3.ID_FUNCTION_STRING_EQUAL,
                policyTypeVersion,
                XACML3.ID_DATATYPE_STRING,
                ToscaDictionary.ID_RESOURCE_POLICY_TYPE_VERSION,
                XACML3.ID_ATTRIBUTE_CATEGORY_RESOURCE);
        //
        // This is our outer AnyOf - which is an OR
        //
        var anyOf = new AnyOfType();
        //
        // Create AllOf (AND) of just Policy Id
        //
        anyOf.getAllOf().add(ToscaPolicyTranslatorUtils.buildAllOf(matchPolicyId));
        //
        // Create AllOf (AND) of just Policy Type
        //
        anyOf.getAllOf().add(ToscaPolicyTranslatorUtils.buildAllOf(matchPolicyType));
        //
        // Create AllOf (AND) of Policy Type and Policy Type Version
        //
        anyOf.getAllOf().add(ToscaPolicyTranslatorUtils.buildAllOf(matchPolicyType, matchPolicyTypeVersion));
        //
        // Now we can create the TargetType, add the top-level anyOf (OR),
        // and return the value.
        //
        var target = new TargetType();
        target.getAnyOf().add(anyOf);
        return target;
    }

}
