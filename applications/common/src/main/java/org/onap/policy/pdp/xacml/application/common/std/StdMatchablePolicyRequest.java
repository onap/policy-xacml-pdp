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

package org.onap.policy.pdp.xacml.application.common.std;

import com.att.research.xacml.api.AttributeValue;
import com.att.research.xacml.api.DataType;
import com.att.research.xacml.api.DataTypeException;
import com.att.research.xacml.api.DataTypeFactory;
import com.att.research.xacml.api.Request;
import com.att.research.xacml.api.XACML3;
import com.att.research.xacml.std.IdentifierImpl;
import com.att.research.xacml.std.StdMutableAttribute;
import com.att.research.xacml.std.StdMutableRequest;
import com.att.research.xacml.std.StdMutableRequestAttributes;
import com.att.research.xacml.std.annotations.RequestParser;
import com.att.research.xacml.std.annotations.XACMLAction;
import com.att.research.xacml.std.annotations.XACMLRequest;
import com.att.research.xacml.std.annotations.XACMLSubject;
import com.att.research.xacml.util.FactoryException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Map.Entry;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.onap.policy.models.decisions.concepts.DecisionRequest;
import org.onap.policy.pdp.xacml.application.common.ToscaDictionary;
import org.onap.policy.pdp.xacml.application.common.XacmlApplicationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Getter
@Setter
@ToString
@NoArgsConstructor
@XACMLRequest(ReturnPolicyIdList = true)
public class StdMatchablePolicyRequest {

    private static final Logger LOGGER = LoggerFactory.getLogger(StdMatchablePolicyRequest.class);

    public static final String POLICY_TYPE_KEY = "policy-type";

    @XACMLSubject(includeInResults = true)
    private String onapName;

    @XACMLSubject(attributeId = "urn:org:onap:onap-component", includeInResults = true)
    private String onapComponent;

    @XACMLSubject(attributeId = "urn:org:onap:onap-instance", includeInResults = true)
    private String onapInstance;

    @XACMLAction()
    private String action;

    protected static DataTypeFactory dataTypeFactory = null;

    protected static synchronized DataTypeFactory getDataTypeFactory() {
        try {
            if (dataTypeFactory != null) {
                return dataTypeFactory;
            }
            dataTypeFactory = DataTypeFactory.newInstance();
        } catch (FactoryException e) {
            LOGGER.error("Can't get Data type Factory", e);
        }
        return dataTypeFactory;
    }

    /**
     * Parses the DecisionRequest into a XAML request.
     *
     * @param decisionRequest Input DecisionRequest
     * @return Request XACML Request object
     * @throws XacmlApplicationException Exception occurred parsing or creating request
     */
    @SuppressWarnings({"rawtypes", "unchecked"})
    public static Request createInstance(DecisionRequest decisionRequest) throws XacmlApplicationException {
        //
        // Create our request object
        //
        var request = new StdMatchablePolicyRequest();
        //
        // Add the subject attributes
        //
        request.onapName = decisionRequest.getOnapName();
        request.onapComponent = decisionRequest.getOnapComponent();
        request.onapInstance = decisionRequest.getOnapInstance();
        //
        // Add the action attribute
        //
        request.action = decisionRequest.getAction();
        //
        // Parse the request - we use the annotations to create a
        // basic XACML request.
        //
        Request xacmlRequest;
        try {
            xacmlRequest = RequestParser.parseRequest(request);
        } catch (IllegalAccessException | DataTypeException e) {
            throw new XacmlApplicationException("Could not parse request ", e);
        }
        //
        // Create an object we can add to
        //
        var mutableRequest = new StdMutableRequest(xacmlRequest);
        var resourceAttributes = new StdMutableRequestAttributes();
        resourceAttributes.setCategory(XACML3.ID_ATTRIBUTE_CATEGORY_RESOURCE);
        //
        // Add the resource attributes
        //
        Map<String, Object> resources = decisionRequest.getResource();
        for (Entry<String, Object> entrySet : resources.entrySet()) {
            //
            // Check for special policy-type
            //
            String attributeId;
            if (POLICY_TYPE_KEY.equals(entrySet.getKey())) {
                attributeId = ToscaDictionary.ID_RESOURCE_POLICY_TYPE.stringValue();
            } else {
                attributeId = ToscaDictionary.ID_RESOURCE_MATCHABLE + entrySet.getKey();
            }
            //
            // Making an assumption that these fields are matchable.
            // Its possible we may have to load the policy type model
            // and use that to validate the fields that are matchable.
            //
            try {
                if (entrySet.getValue() instanceof Collection collection) {
                    addResources(resourceAttributes, collection, attributeId);
                } else {
                    addResources(resourceAttributes,
                        Collections.singletonList(entrySet.getValue().toString()), attributeId);
                }
            } catch (DataTypeException e) {
                throw new XacmlApplicationException("Failed to add resource ", e);
            }
        }
        mutableRequest.add(resourceAttributes);
        return mutableRequest;
    }

    protected static StdMutableRequestAttributes addResources(StdMutableRequestAttributes attributes,
                                                              Collection<Object> values, String id)
        throws DataTypeException {

        var factory = getDataTypeFactory();
        if (factory == null) {
            return null;
        }
        for (Object value : values) {
            var mutableAttribute = new StdMutableAttribute();
            mutableAttribute.setCategory(XACML3.ID_ATTRIBUTE_CATEGORY_RESOURCE);
            mutableAttribute.setAttributeId(new IdentifierImpl(id));
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
