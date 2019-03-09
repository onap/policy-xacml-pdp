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

package org.onap.policy.pdp.xacml.application.common;

import com.att.research.xacml.api.Identifier;
import com.att.research.xacml.api.XACML3;
import oasis.names.tc.xacml._3_0.core.schema.wd_17.AllOfType;
import oasis.names.tc.xacml._3_0.core.schema.wd_17.AttributeDesignatorType;
import oasis.names.tc.xacml._3_0.core.schema.wd_17.AttributeValueType;
import oasis.names.tc.xacml._3_0.core.schema.wd_17.MatchType;

/**
 * This class contains static methods of helper classes to convert TOSCA policies
 * into XACML policies.
 *
 * @author pameladragosh
 *
 */
public final class ToscaPolicyConverterUtils {

    private ToscaPolicyConverterUtils() {
        super();
    }

    /**
     * This method builds a MatchType for TargetType object for AttributeValue and AttributeDesignator
     * combination.
     *
     * @param <T> Incoming value could be any object
     * @param function Function for the Match
     * @param value Attribute value used
     * @param datatype Datatype for attribute value and AttributeDesignator
     * @param designatorId ID for the AttributeDesignator
     * @param designatorCategory Category ID for the AttributeDesignator
     * @return The MatchType object
     */
    public static <T> MatchType buildMatchTypeDesignator(Identifier function,
            T value,
            Identifier datatype,
            Identifier designatorId,
            Identifier designatorCategory) {
        //
        // Create the MatchType object and set its function
        //
        MatchType match = new MatchType();
        match.setMatchId(XACML3.ID_FUNCTION_STRING_EQUAL.toString());
        //
        // Add in the AttributeValue object
        //
        AttributeValueType valueType = new AttributeValueType();
        valueType.setDataType(datatype.stringValue());
        valueType.getContent().add(value);

        match.setAttributeValue(valueType);
        //
        // Add in the AttributeDesignator object
        //
        AttributeDesignatorType designator = new AttributeDesignatorType();
        designator.setAttributeId(designatorId.stringValue());
        designator.setCategory(designatorCategory.stringValue());
        designator.setDataType(datatype.stringValue());

        match.setAttributeDesignator(designator);
        //
        // Done
        //
        return match;
    }

    /**
     * Builds an AllOfType (AND) with one or more MatchType objects.
     *
     * @param matches A list of one or more MatchType
     * @return The AllOf object
     */
    public static AllOfType buildAllOf(MatchType... matches) {
        AllOfType allOf = new AllOfType();
        for (MatchType match : matches) {
            allOf.getMatch().add(match);
        }
        return allOf;
    }
}
