/*-
 * ============LICENSE_START=======================================================
 * ONAP
 * ================================================================================
 * Copyright (C) 2019-2021 AT&T Intellectual Property. All rights reserved.
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

import com.att.research.xacml.api.Advice;
import com.att.research.xacml.api.AttributeAssignment;
import com.att.research.xacml.api.Identifier;
import com.att.research.xacml.api.XACML3;
import com.att.research.xacml.util.XACMLPolicyWriter;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import oasis.names.tc.xacml._3_0.core.schema.wd_17.AdviceExpressionType;
import oasis.names.tc.xacml._3_0.core.schema.wd_17.AdviceExpressionsType;
import oasis.names.tc.xacml._3_0.core.schema.wd_17.AnyOfType;
import oasis.names.tc.xacml._3_0.core.schema.wd_17.AttributeAssignmentExpressionType;
import oasis.names.tc.xacml._3_0.core.schema.wd_17.AttributeValueType;
import oasis.names.tc.xacml._3_0.core.schema.wd_17.EffectType;
import oasis.names.tc.xacml._3_0.core.schema.wd_17.ObjectFactory;
import oasis.names.tc.xacml._3_0.core.schema.wd_17.PolicyType;
import org.apache.commons.lang3.StringUtils;
import org.onap.policy.models.decisions.concepts.DecisionResponse;
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

    private static final String FIELD_SUBSCRIBER_ROLE = "subscriberRole";
    private static final String FIELD_PROV_STATUS = "provStatus";

    @SuppressWarnings("unchecked")
    @Override
    public Object convertPolicy(ToscaPolicy toscaPolicy) throws ToscaPolicyConversionException {
        //
        // Have our superclass do the work - NOTE we are assuming
        // that we are getting a PolicyType converted.
        //
        PolicyType policy = (PolicyType) super.convertPolicy(toscaPolicy);
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
            policy.setAdviceExpressions(generateSubscriberAdvice(subscriberProperties));
            //
            // Dump our revised policy out
            //
            try (var os = new ByteArrayOutputStream()) {
                XACMLPolicyWriter.writePolicyFile(os, policy);
                LOGGER.info("{}", os);
            } catch (IOException e) {
                LOGGER.error("Failed to create byte array stream", e);
            }
        }
        return policy;
    }

    @Override
    protected void scanAdvice(Collection<Advice> advice, DecisionResponse decisionResponse) {
        for (Advice adv : advice) {
            if (! ToscaDictionary.ID_ADVICE_OPTIMIZATION_SUBSCRIBER.equals(adv.getId())) {
                LOGGER.warn("Unknown advice id {}", adv.getId());
                continue;
            }
            //
            // Get the existing advice if any, we are appending to it.
            //
            Map<String, Object> mapAdvice = decisionResponse.getAdvice();
            //
            // If there's nothing, create a map
            //
            if (mapAdvice == null) {
                mapAdvice = new HashMap<>();
            }
            for (AttributeAssignment assignment : adv.getAttributeAssignments()) {
                if (ToscaDictionary.ID_ADVICE_OPTIMIZATION_SUBSCRIBER_ROLE.equals(assignment.getAttributeId())) {
                    addValuesToMap(assignment.getAttributeValue().getValue(), FIELD_SUBSCRIBER_ROLE, mapAdvice);
                } else if (ToscaDictionary.ID_ADVICE_OPTIMIZATION_SUBSCRIBER_STATUS.equals(
                        assignment.getAttributeId())) {
                    addValuesToMap(assignment.getAttributeValue().getValue(), FIELD_PROV_STATUS, mapAdvice);
                }
            }
            if (! mapAdvice.isEmpty()) {
                decisionResponse.setAdvice(mapAdvice);
            }
        }
    }

    @SuppressWarnings("unchecked")
    protected static void addValuesToMap(Object values, String key, Map<String, Object> mapAdvice) {
        if (values instanceof Collection) {
            List<String> valueList = new ArrayList<>();
            ((Collection<Object>) values).forEach(val -> valueList.add(val.toString()));
            mapAdvice.put(key, valueList);
        } else {
            mapAdvice.put(key, values.toString());
        }

    }

    protected static PolicyType addSubscriberNameIntoTarget(PolicyType policy,
            Map<String, Object> subscriberProperties) throws ToscaPolicyConversionException {
        //
        // Iterate through all the subscriber names
        //
        var anyOf = new AnyOfType();
        for (Object subscriberName : getPropAsList(subscriberProperties, "subscriberName")) {

            var match = ToscaPolicyTranslatorUtils.buildMatchTypeDesignator(
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

    protected static AdviceExpressionsType generateSubscriberAdvice(Map<String, Object> subscriberProperties)
            throws ToscaPolicyConversionException {
        //
        // Create our subscriber advice expression
        //
        var adviceExpression = new AdviceExpressionType();
        adviceExpression.setAppliesTo(EffectType.PERMIT);
        adviceExpression.setAdviceId(ToscaDictionary.ID_ADVICE_OPTIMIZATION_SUBSCRIBER.stringValue());
        //
        // Add in subscriber role advice attributes
        //
        generateSubscriberAdviceAttributes(
                adviceExpression,
                ToscaDictionary.ID_ADVICE_OPTIMIZATION_SUBSCRIBER_ROLE,
                getPropAsList(subscriberProperties, FIELD_SUBSCRIBER_ROLE));
        //
        // Get the provision status
        //
        generateSubscriberAdviceAttributes(
                adviceExpression,
                ToscaDictionary.ID_ADVICE_OPTIMIZATION_SUBSCRIBER_STATUS,
                getPropAsList(subscriberProperties, FIELD_PROV_STATUS));
        //
        // Add it to the overall expressions
        //
        var adviceExpressions = new AdviceExpressionsType();
        adviceExpressions.getAdviceExpression().add(adviceExpression);
        //
        // Done return our advice expressions
        //
        return adviceExpressions;
    }

    protected static void generateSubscriberAdviceAttributes(AdviceExpressionType adviceExpression,
            Identifier attributeId, Collection<Object> adviceAttribute) {
        for (Object attribute : adviceAttribute) {
            var value = new AttributeValueType();
            value.setDataType(XACML3.ID_DATATYPE_STRING.stringValue());
            value.getContent().add(attribute.toString());

            var assignment = new AttributeAssignmentExpressionType();
            assignment.setAttributeId(attributeId.stringValue());
            assignment.setCategory(XACML3.ID_SUBJECT_CATEGORY_ACCESS_SUBJECT.stringValue());
            assignment.setExpression(new ObjectFactory().createAttributeValue(value));

            adviceExpression.getAttributeAssignmentExpression().add(assignment);
        }
    }

    @SuppressWarnings("unchecked")
    private static List<Object> getPropAsList(Map<String, Object> properties, String fieldName)
                    throws ToscaPolicyConversionException {

        Object raw = properties.get(fieldName);
        if (raw == null || StringUtils.isBlank(raw.toString())) {
            throw new ToscaPolicyConversionException("Missing " + fieldName);
        }

        return raw instanceof Collection ? (List<Object>) raw : Arrays.asList(raw);
    }
}
