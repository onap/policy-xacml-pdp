/*-
 * ============LICENSE_START=======================================================
 * Copyright (C) 2020-2021 AT&T Intellectual Property. All rights reserved.
 * Modifications Copyright (C) 2024 Nordix Foundation.
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
 * ============LICENSE_END=========================================================
 */

package org.onap.policy.tutorial.tutorial;

import com.att.research.xacml.api.Advice;
import com.att.research.xacml.api.DataTypeException;
import com.att.research.xacml.api.Decision;
import com.att.research.xacml.api.Identifier;
import com.att.research.xacml.api.Obligation;
import com.att.research.xacml.api.Request;
import com.att.research.xacml.api.Response;
import com.att.research.xacml.api.Result;
import com.att.research.xacml.api.XACML3;
import com.att.research.xacml.std.IdentifierImpl;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import oasis.names.tc.xacml._3_0.core.schema.wd_17.AnyOfType;
import oasis.names.tc.xacml._3_0.core.schema.wd_17.EffectType;
import oasis.names.tc.xacml._3_0.core.schema.wd_17.PolicyType;
import oasis.names.tc.xacml._3_0.core.schema.wd_17.RuleType;
import oasis.names.tc.xacml._3_0.core.schema.wd_17.TargetType;
import org.onap.policy.models.decisions.concepts.DecisionRequest;
import org.onap.policy.models.decisions.concepts.DecisionResponse;
import org.onap.policy.models.tosca.authorative.concepts.ToscaPolicy;
import org.onap.policy.pdp.xacml.application.common.ToscaDictionary;
import org.onap.policy.pdp.xacml.application.common.ToscaPolicyConversionException;
import org.onap.policy.pdp.xacml.application.common.ToscaPolicyTranslatorUtils;
import org.onap.policy.pdp.xacml.application.common.std.StdBaseTranslator;

public class TutorialTranslator extends StdBaseTranslator {

    private static final Identifier ID_TUTORIAL_USER = new IdentifierImpl(ToscaDictionary.ID_URN_ONAP, "tutorial-user");
    private static final Identifier ID_TUTORIAL_ENTITY =
            new IdentifierImpl(ToscaDictionary.ID_URN_ONAP, "tutorial-entity");
    private static final Identifier ID_TUTORIAL_PERM =
            new IdentifierImpl(ToscaDictionary.ID_URN_ONAP, "tutorial-permission");

    /**
     * Constructor will set up some defaults.
     */
    public TutorialTranslator() {
        //
        // For demonstration purposes, this tutorial will have
        // the original attributes returned in the request.
        //
        this.booleanReturnAttributes = true;
        this.booleanReturnSingleValueAttributesAsCollection = false;
    }

    /**
     * Convert Policy from TOSCA to XACML.
     */
    @SuppressWarnings("unchecked")
    @Override
    public PolicyType convertPolicy(ToscaPolicy toscaPolicy) {
        //
        // Here is our policy with a version and default combining algo
        //
        var newPolicyType = new PolicyType();
        newPolicyType.setPolicyId(String.valueOf(toscaPolicy.getMetadata().get("policy-id")));
        newPolicyType.setVersion(String.valueOf(toscaPolicy.getMetadata().get("policy-version")));
        //
        // When choosing the rule combining algorithm, be sure to be mindful of the
        // setting xacml.att.policyFinderFactory.combineRootPolicies in the
        // xacml.properties file. As that choice for ALL the policies together may have
        // an impact on the decision rendered from each individual policy.
        //
        // In this case, we will only produce XACML rules for permissions. If no permission
        // combo exists, then the default is to deny.
        //
        newPolicyType.setRuleCombiningAlgId(XACML3.ID_RULE_DENY_UNLESS_PERMIT.stringValue());
        //
        // Create the target for the Policy.
        //
        // For simplicity, let's just match on the action "authorize" and the user
        //
        var matchAction =
                ToscaPolicyTranslatorUtils.buildMatchTypeDesignator(XACML3.ID_FUNCTION_STRING_EQUAL, "authorize",
                        XACML3.ID_DATATYPE_STRING, XACML3.ID_ACTION_ACTION_ID, XACML3.ID_ATTRIBUTE_CATEGORY_ACTION);
        Map<String, Object> props = toscaPolicy.getProperties();
        var user = props.get("user").toString();
        var matchUser = ToscaPolicyTranslatorUtils.buildMatchTypeDesignator(XACML3.ID_FUNCTION_STRING_EQUAL, user,
                XACML3.ID_DATATYPE_STRING, ID_TUTORIAL_USER, XACML3.ID_ATTRIBUTE_CATEGORY_RESOURCE);
        var anyOf = new AnyOfType();
        //
        // Create AllOf (AND) of just Policy ID
        //
        anyOf.getAllOf().add(ToscaPolicyTranslatorUtils.buildAllOf(matchAction, matchUser));
        var target = new TargetType();
        target.getAnyOf().add(anyOf);
        newPolicyType.setTarget(target);
        //
        // Now add the rule for each permission
        //
        var ruleNumber = 0;
        List<Object> permissions = (List<Object>) props.get("permissions");
        for (Object permission : permissions) {

            var matchEntity = ToscaPolicyTranslatorUtils.buildMatchTypeDesignator(XACML3.ID_FUNCTION_STRING_EQUAL,
                    ((Map<String, String>) permission).get("entity"), XACML3.ID_DATATYPE_STRING, ID_TUTORIAL_ENTITY,
                    XACML3.ID_ATTRIBUTE_CATEGORY_RESOURCE);

            var matchPermission = ToscaPolicyTranslatorUtils.buildMatchTypeDesignator(XACML3.ID_FUNCTION_STRING_EQUAL,
                    ((Map<String, String>) permission).get("permission"), XACML3.ID_DATATYPE_STRING, ID_TUTORIAL_PERM,
                    XACML3.ID_ATTRIBUTE_CATEGORY_RESOURCE);
            anyOf = new AnyOfType();
            anyOf.getAllOf().add(ToscaPolicyTranslatorUtils.buildAllOf(matchEntity, matchPermission));
            target = new TargetType();
            target.getAnyOf().add(anyOf);

            var rule = new RuleType();
            rule.setDescription("Default is to PERMIT if the policy matches.");
            rule.setRuleId(newPolicyType.getPolicyId() + ":rule" + ruleNumber);

            rule.setEffect(EffectType.PERMIT);
            rule.setTarget(target);

            newPolicyType.getCombinerParametersOrRuleCombinerParametersOrVariableDefinition().add(rule);

            ruleNumber++;
        }
        return newPolicyType;
    }

    /**
     * Convert ONAP DecisionRequest to XACML Request.
     */
    @Override
    public Request convertRequest(DecisionRequest request) {
        try {
            return TutorialRequest.createRequest(request);
        } catch (IllegalArgumentException | IllegalAccessException | DataTypeException e) {
            // Empty
        }
        return null;
    }

    @Override
    public DecisionResponse convertResponse(Response xacmlResponse) {
        //
        // Single or Multi?
        //
        if (xacmlResponse.getResults().size() > 1) {
            return convertMultiResponse(xacmlResponse);
        } else {
            return convertSingleResponse(xacmlResponse.getResults().iterator().next());
        }
    }

    protected DecisionResponse convertSingleResponse(Result xacmlResult) {
        var decisionResponse = new DecisionResponse();
        //
        // Setup policies
        //
        decisionResponse.setPolicies(new HashMap<>());
        //
        // Check the result
        //
        if (xacmlResult.getDecision() == Decision.PERMIT) {
            //
            // This tutorial will simply set the status to Permit
            //
            decisionResponse.setStatus(Decision.PERMIT.toString());
        } else {
            //
            // This tutorial will simply set the status to Deny
            //
            decisionResponse.setStatus(Decision.DENY.toString());
        }
        //
        // Add attributes use the default scanAttributes. Note that one
        // could override that method and return the structure as desired.
        // The attributes returned by default method are in the format
        // of XACML syntax. It may be more desirable to map them back to
        // the original request name-value.
        //
        if (booleanReturnAttributes) {
            scanAttributes(xacmlResult.getAttributes(), decisionResponse);
        }
        return decisionResponse;
    }

    protected DecisionResponse convertMultiResponse(Response xacmlResponse) {
        TutorialResponse decisionResponse = new TutorialResponse();
        //
        // Setup policies
        //
        decisionResponse.setPolicies(new HashMap<>());
        decisionResponse.setStatus("multi");
        List<TutorialResponsePermission> permissions = new ArrayList<>();
        for (Result xacmlResult : xacmlResponse.getResults()) {
            TutorialResponsePermission permission = new TutorialResponsePermission();
            //
            // Check the result
            //
            if (xacmlResult.getDecision() == Decision.PERMIT) {
                //
                // This tutorial will simply set the status to Permit
                //
                permission.setStatus(Decision.PERMIT.toString());
            } else {
                //
                // This tutorial will simply set the status to Deny
                //
                permission.setStatus(Decision.DENY.toString());
            }
            //
            // Add attributes use the default scanAttributes. Note that one
            // could override that method and return the structure as desired.
            // The attributes returned by default method are in the format
            // of XACML syntax. It may be more desirable to map them back to
            // the original request name-value.
            //
            if (booleanReturnAttributes) {
                //
                // Call existing method
                //
                scanAttributes(xacmlResult.getAttributes(), decisionResponse);
                //
                // Move from overall response to the individual permission
                //
                permission.setAttributes(decisionResponse.getAttributes());
                decisionResponse.setAttributes(null);
            }
            //
            // Add it
            //
            permissions.add(permission);
        }
        decisionResponse.setPermissions(permissions);
        return decisionResponse;
    }

    @Override
    protected void scanObligations(Collection<Obligation> obligations, DecisionResponse decisionResponse) {
        //
        // No obligations in this tutorial yet.
        //
    }

    @Override
    protected void scanAdvice(Collection<Advice> advice, DecisionResponse decisionResponse) {
        //
        // No advice in this tutorial yet.
        //
    }

}
