/*-
 * ============LICENSE_START=======================================================
 * ONAP
 * ================================================================================
 * Copyright (C) 2019-2021 AT&T Intellectual Property. All rights reserved.
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
 *
 * SPDX-License-Identifier: Apache-2.0
 * ============LICENSE_END=========================================================
 */

package org.onap.policy.xacml.pdp.application.optimization;

import com.att.research.xacml.api.AttributeValue;
import com.att.research.xacml.api.DataType;
import com.att.research.xacml.api.DataTypeException;
import com.att.research.xacml.api.Identifier;
import com.att.research.xacml.api.Request;
import com.att.research.xacml.api.XACML3;
import com.att.research.xacml.std.StdMutableAttribute;
import com.att.research.xacml.std.StdMutableRequest;
import com.att.research.xacml.std.StdMutableRequestAttributes;
import com.att.research.xacml.std.annotations.XACMLSubject;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import org.onap.policy.models.decisions.concepts.DecisionRequest;
import org.onap.policy.pdp.xacml.application.common.ToscaDictionary;
import org.onap.policy.pdp.xacml.application.common.XacmlApplicationException;
import org.onap.policy.pdp.xacml.application.common.std.StdMatchablePolicyRequest;

public class OptimizationSubscriberRequest extends StdMatchablePolicyRequest {

    @XACMLSubject(attributeId = "urn:org:onap:optimization:subscriber:name", includeInResults = true)
    private List<String> subscriberRoles;

    /**
     * Create an instance of xacml request.
     *
     * @param decisionRequest Incoming DecisionRequest object
     * @return XACML request
     * @throws XacmlApplicationException XacmlApplicationException
     */
    @SuppressWarnings({"rawtypes", "unchecked"})
    public static Request createInstance(DecisionRequest decisionRequest) throws XacmlApplicationException {
        var request = StdMatchablePolicyRequest.createInstance(decisionRequest);

        //
        // Add in the context attributes
        //
        var mutableRequest = new StdMutableRequest(request);
        var contextAttributes = new StdMutableRequestAttributes();
        contextAttributes.setCategory(XACML3.ID_SUBJECT_CATEGORY_ACCESS_SUBJECT);
        //
        // Add the context attributes
        //
        Map<String, Object> contexts = decisionRequest.getContext();
        for (Entry<String, Object> entrySet : contexts.entrySet()) {
            try {
                //
                // Should always be a collection, but in case someone changes
                // the class without checking this repo.
                //
                if (entrySet.getValue() instanceof Collection collection) {
                    addSubject(contextAttributes, collection,
                        ToscaDictionary.ID_SUBJECT_OPTIMIZATION_SUBSCRIBER_NAME);
                } else {
                    addSubject(contextAttributes, Collections.singletonList(entrySet.getValue().toString()),
                        ToscaDictionary.ID_SUBJECT_OPTIMIZATION_SUBSCRIBER_NAME);
                }
            } catch (DataTypeException e) {
                throw new XacmlApplicationException("Failed to add resource ", e);
            }
        }
        mutableRequest.add(contextAttributes);
        return mutableRequest;
    }

    protected static StdMutableRequestAttributes addSubject(StdMutableRequestAttributes attributes,
                                                            Collection<Object> values, Identifier id)
        throws DataTypeException {

        var factory = getDataTypeFactory();
        if (factory == null) {
            return null;
        }
        for (Object value : values) {
            var mutableAttribute = new StdMutableAttribute();
            mutableAttribute.setCategory(XACML3.ID_SUBJECT_CATEGORY_ACCESS_SUBJECT);
            mutableAttribute.setAttributeId(id);
            mutableAttribute.setIncludeInResults(true);

            DataType<?> dataTypeExtended = factory.getDataType(XACML3.ID_DATATYPE_STRING);
            AttributeValue<?> attributeValue = dataTypeExtended.createAttributeValue(value);
            Collection<AttributeValue<?>> attributeValues = new ArrayList<>();
            attributeValues.add(attributeValue);
            mutableAttribute.setValues(attributeValues);

            attributes.add(mutableAttribute);
        }
        return attributes;
    }
}
