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
import com.att.research.xacml.api.Identifier;
import com.att.research.xacml.api.Obligation;
import com.att.research.xacml.api.Request;
import com.att.research.xacml.api.XACML3;
import com.att.research.xacml.std.annotations.RequestParser;
import java.util.Collection;
import java.util.Map;
import oasis.names.tc.xacml._3_0.core.schema.wd_17.AnyOfType;
import oasis.names.tc.xacml._3_0.core.schema.wd_17.EffectType;
import oasis.names.tc.xacml._3_0.core.schema.wd_17.MatchType;
import oasis.names.tc.xacml._3_0.core.schema.wd_17.PolicyType;
import oasis.names.tc.xacml._3_0.core.schema.wd_17.RuleType;
import oasis.names.tc.xacml._3_0.core.schema.wd_17.TargetType;
import org.apache.commons.lang3.tuple.Pair;
import org.onap.policy.common.utils.coder.CoderException;
import org.onap.policy.common.utils.coder.StandardCoder;
import org.onap.policy.models.decisions.concepts.DecisionRequest;
import org.onap.policy.models.decisions.concepts.DecisionResponse;
import org.onap.policy.models.tosca.authorative.concepts.ToscaPolicy;
import org.onap.policy.pdp.xacml.application.common.ToscaDictionary;
import org.onap.policy.pdp.xacml.application.common.ToscaPolicyConversionException;
import org.onap.policy.pdp.xacml.application.common.ToscaPolicyTranslatorUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class StdCombinedPolicyResultsTranslator extends StdBaseTranslator {

    private static final Logger LOGGER = LoggerFactory.getLogger(StdCombinedPolicyResultsTranslator.class);

    public StdCombinedPolicyResultsTranslator() {
        super();
    }

    @Override
    public PolicyType convertPolicy(ToscaPolicy toscaPolicy) throws ToscaPolicyConversionException {
        //
        // Set it as the policy ID
        //
        PolicyType newPolicyType = new PolicyType();
        newPolicyType.setPolicyId(toscaPolicy.getMetadata().get(POLICY_ID));
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
        TargetType target = this.generateTargetType(toscaPolicy.getMetadata().get(POLICY_ID),
                toscaPolicy.getType(), toscaPolicy.getVersion());
        newPolicyType.setTarget(target);
        //
        // Now create the Permit Rule
        // No target since the policy has a target
        // With obligations.
        //
        RuleType rule = new RuleType();
        rule.setDescription("Default is to PERMIT if the policy matches.");
        rule.setRuleId(toscaPolicy.getMetadata().get(POLICY_ID) + ":rule");
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
            throw new ToscaPolicyConversionException(e);
        }
        addObligation(rule, jsonPolicy, null, toscaPolicy.getType());
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
    public Request convertRequest(DecisionRequest request) {
        LOGGER.info("Converting Request {}", request);
        try {
            return RequestParser.parseRequest(StdCombinedPolicyRequest.createInstance(request));
        } catch (IllegalArgumentException | IllegalAccessException | DataTypeException e) {
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
        for (Obligation obligation : obligations) {
            LOGGER.info("Obligation: {}", obligation);
            for (AttributeAssignment assignment : obligation.getAttributeAssignments()) {
                LOGGER.info("Attribute Assignment: {}", assignment);
                Pair<Identifier, Object> pair = scanObligationAttribute(assignment);
                if (pair.getLeft().equals(ToscaDictionary.ID_OBLIGATION_POLICY_MONITORING_CONTENTS)) {
                    Pair<String, Map<String, Object>> policyInfo = (Pair<String, Map<String, Object>>) pair.getRight();
                    decisionResponse.getPolicies().put(policyInfo.getKey(), policyInfo.getValue());
                }
            }
        }
    }

    protected TargetType generateTargetType(String policyId, String policyType, String policyTypeVersion) {
        //
        // Create all the match's that are possible
        //
        // This is for the Policy Id
        //
        MatchType matchPolicyId = ToscaPolicyTranslatorUtils.buildMatchTypeDesignator(
                XACML3.ID_FUNCTION_STRING_EQUAL,
                policyId,
                XACML3.ID_DATATYPE_STRING,
                ToscaDictionary.ID_RESOURCE_POLICY_ID,
                XACML3.ID_ATTRIBUTE_CATEGORY_RESOURCE);
        //
        // This is for the Policy Type
        //
        MatchType matchPolicyType = ToscaPolicyTranslatorUtils.buildMatchTypeDesignator(
                XACML3.ID_FUNCTION_STRING_EQUAL,
                policyType,
                XACML3.ID_DATATYPE_STRING,
                ToscaDictionary.ID_RESOURCE_POLICY_TYPE,
                XACML3.ID_ATTRIBUTE_CATEGORY_RESOURCE);
        //
        // This is for the Policy Type version
        //
        MatchType matchPolicyTypeVersion = ToscaPolicyTranslatorUtils.buildMatchTypeDesignator(
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
        TargetType target = new TargetType();
        target.getAnyOf().add(anyOf);
        return target;
    }

}
