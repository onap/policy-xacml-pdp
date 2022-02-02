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

package org.onap.policy.pdp.xacml.application.common;

import com.att.research.xacml.api.AttributeAssignment;
import com.att.research.xacml.api.Identifier;
import com.att.research.xacml.api.Obligation;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.util.Collections;
import java.util.Map;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.ToString;
import oasis.names.tc.xacml._3_0.core.schema.wd_17.AttributeAssignmentExpressionType;
import oasis.names.tc.xacml._3_0.core.schema.wd_17.AttributeValueType;
import oasis.names.tc.xacml._3_0.core.schema.wd_17.EffectType;
import oasis.names.tc.xacml._3_0.core.schema.wd_17.ObjectFactory;
import oasis.names.tc.xacml._3_0.core.schema.wd_17.ObligationExpressionType;
import org.onap.policy.common.gson.MapDoubleAdapterFactory;

@Getter
@ToString
public class OnapObligation {

    @Getter(AccessLevel.NONE)
    private static final ObjectFactory factory = new ObjectFactory();

    @Getter(AccessLevel.NONE)
    private static final Gson gson =
            new GsonBuilder().registerTypeAdapterFactory(new MapDoubleAdapterFactory()).create();

    private String policyId;
    private String policyType;
    private String policyContent;
    private Integer weight;

    /**
     * Constructor from an obligation.
     *
     * @param obligation Obligation object
     */
    public OnapObligation(Obligation obligation) {
        //
        // Scan through the obligations for them
        //
        for (AttributeAssignment assignment : obligation.getAttributeAssignments()) {
            scanAttribute(assignment);
        }
    }

    /**
     * Constructor for just the policy details.
     *
     * @param policyId String
     * @param policyContent String
     */
    public OnapObligation(String policyId, String policyContent) {
        this.policyId = policyId;
        this.policyContent = policyContent;
    }


    /**
     * Constructor for policy details, type and weight.
     *
     * @param policyId String
     * @param policyContent String
     * @param policyType String
     * @param weight int
     */
    public OnapObligation(String policyId, String policyContent, String policyType, Integer weight) {
        this.policyId = policyId;
        this.policyContent = policyContent;
        this.policyType = policyType;
        this.weight = weight;
    }

    /**
     * getPolicyContentAsMap returns the policy as a map for convience.
     *
     * @return {@code Map<String, Object>}
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> getPolicyContentAsMap() {
        if (this.policyContent == null) {
            return Collections.emptyMap();
        }
        return gson.fromJson(this.policyContent, Map.class);
    }

    /**
     * Generates default obligation using default Permit and Obligation Id.
     *
     * @return ObligationExpressionType object
     */
    public ObligationExpressionType generateObligation() {
        return this.generateObligation(EffectType.PERMIT, ToscaDictionary.ID_OBLIGATION_REST_BODY);
    }

    /**
     * generateObligation - generates an obligation object with the attributes that exist. Note:
     * any null values will result in NO attribute assignment for that attribute.
     *
     * @param effectType EffectType object
     * @param obligationId Id for the obligation
     * @return ObligationExpressionType object
     */
    public ObligationExpressionType generateObligation(EffectType effectType, Identifier obligationId) {
        //
        // Create an ObligationExpression
        //
        var obligation = new ObligationExpressionType();
        obligation.setFulfillOn(effectType);
        obligation.setObligationId(obligationId.stringValue());
        //
        // Update the obligation
        //
        updateObligation(obligation);
        //
        // Convenience return
        //
        return obligation;
    }

    /**
     * Updates an existing Obligation object with the attributes that exist. Note: any null values
     * will result in NO attribute assignment for that attribute.
     *
     * @param obligation ObligationExpressionType object
     * @return ObligationExpressionType object
     */
    public ObligationExpressionType updateObligation(ObligationExpressionType obligation) {
        //
        // Add policy-id
        //
        addOptionalAttributeToObligation(obligation, ToscaDictionary.ID_OBLIGATION_POLICY_ID,
                ToscaDictionary.ID_OBLIGATION_POLICY_ID_DATATYPE,
                ToscaDictionary.ID_OBLIGATION_POLICY_ID_CATEGORY,
                policyId);
        //
        // Add policy contents
        //
        addOptionalAttributeToObligation(obligation, ToscaDictionary.ID_OBLIGATION_POLICY_CONTENT,
                ToscaDictionary.ID_OBLIGATION_POLICY_CONTENT_DATATYPE,
                ToscaDictionary.ID_OBLIGATION_POLICY_CONTENT_CATEGORY,
                policyContent);
        //
        // Add the weight
        //
        addOptionalAttributeToObligation(obligation, ToscaDictionary.ID_OBLIGATION_POLICY_WEIGHT,
                ToscaDictionary.ID_OBLIGATION_POLICY_WEIGHT_DATATYPE,
                ToscaDictionary.ID_OBLIGATION_POLICY_WEIGHT_CATEGORY,
                weight);
        //
        // Add the policy type
        //
        addOptionalAttributeToObligation(obligation, ToscaDictionary.ID_OBLIGATION_POLICY_TYPE,
                ToscaDictionary.ID_OBLIGATION_POLICY_TYPE_DATATYPE,
                ToscaDictionary.ID_OBLIGATION_POLICY_TYPE_CATEGORY,
                policyType);
        //
        // Return as a convenience
        //
        return obligation;
    }

    /**
     * scanAttribute - scans the assignment for a supported obligation assignment. Applications
     * can override this class and provide their own custom attribute assignments if desired.
     *
     * @param assignment AttributeAssignment object
     * @return true if found an ONAP supported attribute
     */
    protected boolean scanAttribute(AttributeAssignment assignment) {
        //
        // Check for our supported attributes. Note: Cannot use a switch
        // as Identifier isn't a constant.
        //
        if (ToscaDictionary.ID_OBLIGATION_POLICY_ID.equals(assignment.getAttributeId())) {
            policyId = assignment.getAttributeValue().getValue().toString();
            return true;
        } else if (ToscaDictionary.ID_OBLIGATION_POLICY_TYPE.equals(assignment.getAttributeId())) {
            policyType = assignment.getAttributeValue().getValue().toString();
            return true;
        } else if (ToscaDictionary.ID_OBLIGATION_POLICY_CONTENT.equals(assignment.getAttributeId())) {
            policyContent = assignment.getAttributeValue().getValue().toString();
            return true;
        } else if (ToscaDictionary.ID_OBLIGATION_POLICY_WEIGHT.equals(assignment.getAttributeId())) {
            weight = Integer.decode(assignment.getAttributeValue().getValue().toString());
            return true;
        }
        //
        // By returning true, we indicate this isn't an attribute
        // supported in the this class. Targeted for applications
        // that derive from this class in order to extend it.
        //
        return false;
    }

    /**
     * Creates the necessary objects to insert into the obligation, if the value object is not null.
     *
     * @param obligation Incoming Obligation
     * @param id Attribute Id
     * @param datatype Attribute's Data type
     * @param category Attributes Category
     * @param theValue Attribute value
     * @return obligation Incoming obligation
     */
    protected ObligationExpressionType addOptionalAttributeToObligation(ObligationExpressionType obligation,
            Identifier id, Identifier datatype, Identifier category, Object theValue) {
        //
        // Simple check for null
        //
        if (theValue == null) {
            return obligation;
        }
        //
        // Create an AttributeValue for it
        //
        var value = new AttributeValueType();
        value.setDataType(datatype.stringValue());
        value.getContent().add(theValue.toString());
        //
        // Create our AttributeAssignmentExpression where we will
        // store the contents of the policy id.
        //
        var expressionType = new AttributeAssignmentExpressionType();
        expressionType.setAttributeId(id.stringValue());
        expressionType.setCategory(category.stringValue());
        expressionType.setExpression(factory.createAttributeValue(value));
        //
        // Add it to the obligation
        //
        obligation.getAttributeAssignmentExpression().add(expressionType);
        //
        // Return as convenience
        //
        return obligation;
    }

}
