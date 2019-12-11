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

package org.onap.policy.xacml.pdp.application.optimization;

import com.att.research.xacml.api.XACML3;
import com.att.research.xacml.util.XACMLPolicyWriter;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import oasis.names.tc.xacml._3_0.core.schema.wd_17.AdviceExpressionType;
import oasis.names.tc.xacml._3_0.core.schema.wd_17.AdviceExpressionsType;
import oasis.names.tc.xacml._3_0.core.schema.wd_17.AnyOfType;
import oasis.names.tc.xacml._3_0.core.schema.wd_17.AttributeAssignmentExpressionType;
import oasis.names.tc.xacml._3_0.core.schema.wd_17.AttributeValueType;
import oasis.names.tc.xacml._3_0.core.schema.wd_17.EffectType;
import oasis.names.tc.xacml._3_0.core.schema.wd_17.MatchType;
import oasis.names.tc.xacml._3_0.core.schema.wd_17.ObjectFactory;
import oasis.names.tc.xacml._3_0.core.schema.wd_17.PolicyType;
import org.apache.commons.lang3.StringUtils;
import org.onap.policy.models.tosca.authorative.concepts.ToscaPolicy;
import org.onap.policy.pdp.xacml.application.common.ToscaDictionary;
import org.onap.policy.pdp.xacml.application.common.ToscaPolicyConversionException;
import org.onap.policy.pdp.xacml.application.common.ToscaPolicyTranslatorUtils;
import org.onap.policy.pdp.xacml.application.common.std.StdMatchableTranslator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OptimizationPdpApplicationTranslator extends StdMatchableTranslator {
    private static final Logger LOGGER = LoggerFactory.getLogger(OptimizationPdpApplicationTranslator.class);

    private static final String OPTIMIZATION_POLICYTYPE_SUBSCRIBER =
            "onap.policies.optimization.service.SubscriberPolicy";

    @SuppressWarnings("unchecked")
    @Override
    public PolicyType convertPolicy(ToscaPolicy toscaPolicy) throws ToscaPolicyConversionException {
        //
        // Have our superclass do the work
        //
        PolicyType policy = super.convertPolicy(toscaPolicy);
        //
        // Check if this is the subscriber policy
        //
        if (OPTIMIZATION_POLICYTYPE_SUBSCRIBER.equals(toscaPolicy.getType())) {
            //
            // Ensure the policy has the subscriber properties
            //
            Map<String, Object> subscriberProperties = (Map<String, Object>) toscaPolicy.getProperties()
                    .get("subscriberProperties");
            if (subscriberProperties == null) {
                throw new ToscaPolicyConversionException("Missing subscriberProperties from subscriber policy");
            }
            //
            // Add subscriber name to the target so the policy
            // only matches for the given subscriberName.
            //
            addSubscriberNameIntoTarget(policy, subscriberProperties);
            //
            // Add subscriber advice
            //
            policy.setAdviceExpressions(generateSubscriberAdvice(toscaPolicy, subscriberProperties));
            //
            // Dump our revised policy out
            //
            try (ByteArrayOutputStream os = new ByteArrayOutputStream()) {
                XACMLPolicyWriter.writePolicyFile(os, policy);
                LOGGER.info("{}", os);
            } catch (IOException e) {
                LOGGER.error("Failed to create byte array stream", e);
            }
        }
        return policy;
    }

    @SuppressWarnings("unchecked")
    private static PolicyType addSubscriberNameIntoTarget(PolicyType policy,
            Map<String, Object> subscriberProperties) throws ToscaPolicyConversionException {
        //
        // Find the subscriber names
        //
        Object subscriberNames = subscriberProperties.get("subscriberName");
        if (subscriberNames == null) {
            throw new ToscaPolicyConversionException("Missing subscriberName property");
        }
        //
        // Iterate through all the subscriber names
        //
        AnyOfType anyOf = new AnyOfType();
        for (Object subscriberName : subscriberNames instanceof Collection ? (List<Object>) subscriberNames :
            Arrays.asList(subscriberNames)) {

            MatchType match = ToscaPolicyTranslatorUtils.buildMatchTypeDesignator(
                    XACML3.ID_FUNCTION_STRING_EQUAL,
                    subscriberName,
                    XACML3.ID_DATATYPE_STRING,
                    ToscaDictionary.ID_SUBJECT_OPTIMIZATION_SUBSCRIBER_NAME,
                    XACML3.ID_SUBJECT_CATEGORY_ACCESS_SUBJECT);

            anyOf.getAllOf().add(ToscaPolicyTranslatorUtils.buildAllOf(match));
        }
        //
        // Add to the target
        //
        policy.getTarget().getAnyOf().add(anyOf);
        //
        // Return for convenience
        //
        return policy;
    }

    @SuppressWarnings("unchecked")
    private static AdviceExpressionsType generateSubscriberAdvice(ToscaPolicy toscaPolicy,
            Map<String, Object> subscriberProperties) throws ToscaPolicyConversionException {
        //
        // Get the subscriber role
        //
        Object role = subscriberProperties.get("subscriberRole");
        if (role == null || StringUtils.isBlank(role.toString())) {
            throw new ToscaPolicyConversionException("Missing subscriberRole");
        }
        //
        // Get the provision status
        // TODO
        //
        // Our subscriber Advice expression holds all the attribute assignments
        //
        AdviceExpressionType adviceExpression = new AdviceExpressionType();
        adviceExpression.setAppliesTo(EffectType.PERMIT);
        adviceExpression.setAdviceId(ToscaDictionary.ID_ADVICE_OPTIMIZATION_SUBSCRIBER.stringValue());
        //
        // Add in subscriber role
        //
        generateSubscriberRoleAdvice(adviceExpression, role instanceof Collection ? (List<Object>) role :
            Arrays.asList(role));

        AdviceExpressionsType adviceExpressions = new AdviceExpressionsType();
        adviceExpressions.getAdviceExpression().add(adviceExpression);

        return adviceExpressions;
    }

    private static AdviceExpressionType generateSubscriberRoleAdvice(AdviceExpressionType adviceExpression,
            Collection<Object> subscriberRoles) {
        for (Object subscriberRole : subscriberRoles) {
            AttributeValueType value = new AttributeValueType();
            value.setDataType(XACML3.ID_DATATYPE_STRING.stringValue());
            value.getContent().add(subscriberRole.toString());

            AttributeAssignmentExpressionType assignment = new AttributeAssignmentExpressionType();
            assignment.setAttributeId(ToscaDictionary.ID_ADVICE_OPTIMIZATION_SUBSCRIBER_ROLE.stringValue());
            assignment.setCategory(XACML3.ID_SUBJECT_CATEGORY_ACCESS_SUBJECT.stringValue());
            assignment.setExpression(new ObjectFactory().createAttributeValue(value));

            adviceExpression.getAttributeAssignmentExpression().add(assignment);

        }
        //
        // Return for convenience
        //
        return adviceExpression;
    }
}
