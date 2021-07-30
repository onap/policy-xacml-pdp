/*-
 * ============LICENSE_START=======================================================
 * Copyright (C) 2020-2021 AT&T Intellectual Property. All rights reserved.
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

import com.att.research.xacml.api.DataTypeException;
import com.att.research.xacml.api.Identifier;
import com.att.research.xacml.api.Request;
import com.att.research.xacml.api.XACML3;
import com.att.research.xacml.std.IdentifierImpl;
import com.att.research.xacml.std.StdAttributeValue;
import com.att.research.xacml.std.StdMutableAttribute;
import com.att.research.xacml.std.StdMutableRequest;
import com.att.research.xacml.std.StdMutableRequestAttributes;
import com.att.research.xacml.std.StdMutableRequestReference;
import com.att.research.xacml.std.StdRequestAttributesReference;
import com.att.research.xacml.std.annotations.RequestParser;
import com.att.research.xacml.std.annotations.XACMLAction;
import com.att.research.xacml.std.annotations.XACMLRequest;
import com.att.research.xacml.std.annotations.XACMLResource;
import com.att.research.xacml.std.annotations.XACMLSubject;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.onap.policy.models.decisions.concepts.DecisionRequest;

@Getter
@Setter
@ToString
@XACMLRequest(ReturnPolicyIdList = true)
public class TutorialRequest {
    public static final Identifier ATTRIBUTE_ID_MULTIID = new IdentifierImpl("urn:org:onap:tutorial-multi-id");
    public static final Identifier ATTRIBUTE_ID_USER = new IdentifierImpl("urn:org:onap:tutorial-user");
    public static final Identifier ATTRIBUTE_ID_ENTITY = new IdentifierImpl("urn:org:onap:tutorial-entity");
    public static final Identifier ATTRIBUTE_ID_PERMISSION = new IdentifierImpl("urn:org:onap:tutorial-permission");
    //
    // For use with a multi request
    //
    private static final StdRequestAttributesReference refSubjects = new StdRequestAttributesReference("subjects1");
    private static final StdRequestAttributesReference refActions = new StdRequestAttributesReference("actions1");
    //
    // Excluding from results to demonstrate control as to which attributes can be returned.
    //
    @XACMLSubject(includeInResults = false)
    private String onapName;

    @XACMLSubject(attributeId = "urn:org:onap:onap-component", includeInResults = false)
    private String onapComponent;

    @XACMLSubject(attributeId = "urn:org:onap:onap-instance", includeInResults = false)
    private String onapInstance;

    @XACMLAction
    private String action;

    //
    // Including in results to demonstrate control as to which attributes can be returned.
    //
    @XACMLResource(attributeId = "urn:org:onap:tutorial-user", includeInResults = true)
    private String user;

    @XACMLResource(attributeId = "urn:org:onap:tutorial-entity", includeInResults = true)
    private String entity;

    @XACMLResource(attributeId = "urn:org:onap:tutorial-permission", includeInResults = true)
    private String permission;

    /**
     * createRequest.
     *
     * @param decisionRequest Incoming
     * @return Request XACML Request object
     * @throws DataTypeException DataTypeException
     * @throws IllegalAccessException IllegalAccessException
     */
    @SuppressWarnings("unchecked")
    public static Request createRequest(DecisionRequest decisionRequest)
            throws IllegalAccessException, DataTypeException {
        //
        // Create our object
        //
        var request = new TutorialRequest();
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
        // Add the resource attributes
        //
        Map<String, Object> resources = decisionRequest.getResource();
        //
        // Check if this is a multi-request
        //
        if (resources.containsKey("users")) {
            //
            // Setup the multi-request
            //
            return buildMultiRequest(request, (List<Object>) resources.get("users"));
        }
        //
        // Continue as a single request
        //
        for (Entry<String, Object> entrySet : resources.entrySet()) {
            if ("user".equals(entrySet.getKey())) {
                request.user = entrySet.getValue().toString();
            }
            if ("entity".equals(entrySet.getKey())) {
                request.entity = entrySet.getValue().toString();
            }
            if ("permission".equals(entrySet.getKey())) {
                request.permission = entrySet.getValue().toString();
            }
        }

        return RequestParser.parseRequest(request);
    }

    @SuppressWarnings("unchecked")
    protected static Request buildMultiRequest(TutorialRequest existingRequest, List<Object> users)
            throws IllegalAccessException, DataTypeException {
        //
        // Create a single request absorbing the existing attributes, we will copy
        // them over and assign them an ID.
        //
        StdMutableRequest singleRequest = new StdMutableRequest(RequestParser.parseRequest(existingRequest));
        //
        // Copy the attributes and assign ID's
        //
        StdMutableRequest multiRequest = addMultiRequestIds(singleRequest);
        //
        // Iterate and add in the requests
        //
        users.forEach(user -> addUser(multiRequest, (Map<String, Object>) user));
        //
        // Done
        //
        return multiRequest;
    }

    protected static StdMutableRequest addMultiRequestIds(StdMutableRequest singleRequest) {
        StdMutableRequest multiRequest = new StdMutableRequest();
        singleRequest.getRequestAttributes().forEach(attributes -> {
            StdMutableRequestAttributes newAttributes = new StdMutableRequestAttributes();
            if (attributes.getCategory().equals(XACML3.ID_SUBJECT_CATEGORY_ACCESS_SUBJECT)) {
                newAttributes.setCategory(XACML3.ID_SUBJECT_CATEGORY_ACCESS_SUBJECT);
                newAttributes.setXmlId(refSubjects.getReferenceId());
            } else if (attributes.getCategory().equals(XACML3.ID_ATTRIBUTE_CATEGORY_ACTION)) {
                newAttributes.setCategory(XACML3.ID_ATTRIBUTE_CATEGORY_ACTION);
                newAttributes.setXmlId(refActions.getReferenceId());
            }
            attributes.getAttributes().forEach(newAttributes::add);
            multiRequest.add(newAttributes);
        });
        return multiRequest;
    }

    protected static void addUser(StdMutableRequest multiRequest, Map<String, Object> user) {
        StdMutableRequestAttributes attributes = new StdMutableRequestAttributes();
        attributes.setCategory(XACML3.ID_ATTRIBUTE_CATEGORY_RESOURCE);
        for (Entry<String, Object> entrySet : user.entrySet()) {
            StdAttributeValue<String> value =
                    new StdAttributeValue<>(XACML3.ID_DATATYPE_STRING, entrySet.getValue().toString());
            StdMutableAttribute attribute = new StdMutableAttribute();
            attribute.setCategory(XACML3.ID_ATTRIBUTE_CATEGORY_RESOURCE);
            attribute.setIncludeInResults(true);
            attribute.addValue(value);
            if ("multiId".equals(entrySet.getKey())) {
                attributes.setXmlId(entrySet.getValue().toString());
                attribute.setAttributeId(ATTRIBUTE_ID_MULTIID);
            } else if ("user".equals(entrySet.getKey())) {
                attribute.setAttributeId(ATTRIBUTE_ID_USER);
            } else if ("entity".equals(entrySet.getKey())) {
                attribute.setAttributeId(ATTRIBUTE_ID_ENTITY);
            } else if ("permission".equals(entrySet.getKey())) {
                attribute.setAttributeId(ATTRIBUTE_ID_PERMISSION);
            } else {
                throw new IllegalArgumentException("Unknown request attribute given");
            }
            attributes.add(attribute);
        }
        //
        // Add the attributes to the Multi-Request
        //
        multiRequest.add(attributes);
        //
        // Create the references
        //
        StdRequestAttributesReference attributesReference = new StdRequestAttributesReference(attributes.getXmlId());
        StdMutableRequestReference reference = new StdMutableRequestReference();
        reference.add(refSubjects);
        reference.add(refActions);
        reference.add(attributesReference);
        //
        // Add the reference to this request
        //
        multiRequest.add(reference);
    }
}
