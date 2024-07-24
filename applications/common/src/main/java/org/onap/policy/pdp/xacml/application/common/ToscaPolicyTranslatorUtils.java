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

package org.onap.policy.pdp.xacml.application.common;

import com.att.research.xacml.api.Identifier;
import com.att.research.xacml.api.XACML3;
import java.util.Map;
import oasis.names.tc.xacml._3_0.core.schema.wd_17.AllOfType;
import oasis.names.tc.xacml._3_0.core.schema.wd_17.AnyOfType;
import oasis.names.tc.xacml._3_0.core.schema.wd_17.ApplyType;
import oasis.names.tc.xacml._3_0.core.schema.wd_17.AttributeDesignatorType;
import oasis.names.tc.xacml._3_0.core.schema.wd_17.AttributeValueType;
import oasis.names.tc.xacml._3_0.core.schema.wd_17.ConditionType;
import oasis.names.tc.xacml._3_0.core.schema.wd_17.MatchType;
import oasis.names.tc.xacml._3_0.core.schema.wd_17.ObjectFactory;
import oasis.names.tc.xacml._3_0.core.schema.wd_17.TargetType;
import oasis.names.tc.xacml._3_0.core.schema.wd_17.VariableReferenceType;
import org.apache.commons.lang3.StringUtils;
import org.onap.policy.common.parameters.BeanValidationResult;
import org.onap.policy.common.parameters.BeanValidator;
import org.onap.policy.common.utils.coder.CoderException;
import org.onap.policy.common.utils.coder.StandardCoder;

/**
 * This class contains static methods of helper classes to convert TOSCA policies
 * into XACML policies.
 *
 * @author pameladragosh
 *
 */
public final class ToscaPolicyTranslatorUtils {
    private static final ObjectFactory factory = new ObjectFactory();
    private static final StandardCoder CODER = new StandardCoder();

    private ToscaPolicyTranslatorUtils() {
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
        var match = new MatchType();
        match.setMatchId(function.stringValue());
        //
        // Add in the AttributeValue object
        //
        var valueType = new AttributeValueType();
        valueType.setDataType(datatype.stringValue());
        valueType.getContent().add(value);

        match.setAttributeValue(valueType);
        //
        // Add in the AttributeDesignator object
        //
        var designator = new AttributeDesignatorType();
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
        var allOf = new AllOfType();
        for (MatchType match : matches) {
            allOf.getMatch().add(match);
        }
        return allOf;
    }

    /**
     * Takes start and end time interval and generates an ApplyType for it.
     *
     * @param start ISO8601 timestamp
     * @param end ISO8601 timestamp
     * @return ApplyType
     */
    public static ApplyType generateTimeInRange(String start, String end, boolean useRecurringFunction) {
        if (StringUtils.isBlank(start) || StringUtils.isBlank(end)) {
            return null;
        }

        var designator = new AttributeDesignatorType();
        designator.setAttributeId(XACML3.ID_ENVIRONMENT_CURRENT_TIME.stringValue());
        designator.setCategory(XACML3.ID_ATTRIBUTE_CATEGORY_ENVIRONMENT.stringValue());
        designator.setDataType(XACML3.ID_DATATYPE_TIME.stringValue());

        var valueStart = new AttributeValueType();
        valueStart.setDataType(XACML3.ID_DATATYPE_TIME.stringValue());
        valueStart.getContent().add(start);

        var valueEnd = new AttributeValueType();
        valueEnd.setDataType(XACML3.ID_DATATYPE_TIME.stringValue());
        valueEnd.getContent().add(end);


        var applyOneAndOnly = new ApplyType();
        applyOneAndOnly.setDescription("Unbag the current time");
        applyOneAndOnly.setFunctionId(XACML3.ID_FUNCTION_TIME_ONE_AND_ONLY.stringValue());
        applyOneAndOnly.getExpression().add(factory.createAttributeDesignator(designator));

        var applyTimeInRange = new ApplyType();
        applyTimeInRange.setDescription("return true if current time is in range.");
        if (useRecurringFunction) {
            applyTimeInRange.setFunctionId(XACML3.ID_FUNCTION_TIME_IN_RECURRING_RANGE.stringValue());
        } else {
            applyTimeInRange.setFunctionId(XACML3.ID_FUNCTION_TIME_IN_RANGE.stringValue());
        }
        applyTimeInRange.getExpression().add(factory.createApply(applyOneAndOnly));
        applyTimeInRange.getExpression().add(factory.createAttributeValue(valueStart));
        applyTimeInRange.getExpression().add(factory.createAttributeValue(valueEnd));

        return applyTimeInRange;
    }

    /**
     * Parses an integer value from the string.
     *
     * @param strInteger String representation of integer
     * @return Integer object
     */
    public static Integer parseInteger(String strInteger) {
        Integer theInt = null;
        try {
            theInt = Integer.parseInt(strInteger);
        } catch (NumberFormatException e) {
            try {
                double dblLimit = Double.parseDouble(strInteger);
                theInt = (int) dblLimit;
            } catch (NumberFormatException e1) {
                return null;
            }
        }
        return theInt;
    }

    /**
     * For a given MatchType or AnyOfType, builds it and appends it into the
     * AnyOfType.
     *
     * @param anyOf AnyOfType - will create if null
     * @param type MatchType or AnyOfType
     * @return returns the given anyOf or new AnyTypeOf if null
     */
    public static AnyOfType buildAndAppendAllof(AnyOfType anyOf, Object type) {
        if (type instanceof MatchType matchType) {
            var allOf = new AllOfType();
            allOf.getMatch().add(matchType);
            if (anyOf == null) {
                anyOf = new AnyOfType();
            }
            anyOf.getAllOf().add(allOf);
        } else if (type instanceof AllOfType allOfType) {
            if (anyOf == null) {
                anyOf = new AnyOfType();
            }
            anyOf.getAllOf().add(allOfType);
        }

        return anyOf;
    }

    /**
     * buildAndAppendTarget - adds in the potential object into TargetType.
     *
     * @param target TargetType - must exist
     * @param object AnyOfType or MatchType
     * @return TargetType
     */
    public static TargetType buildAndAppendTarget(TargetType target, Object object) {
        if (object instanceof AnyOfType anyOfType) {
            target.getAnyOf().add(anyOfType);
        } else if (object instanceof MatchType matchType) {
            var allOf = new AllOfType();
            allOf.getMatch().add(matchType);
            var anyOf = new AnyOfType();
            anyOf.getAllOf().add(allOf);
            target.getAnyOf().add(anyOf);
        }
        return target;
    }

    /**
     * For an existing ConditionType, this method adds in a check for a variable. You must specify
     * the function that compares the existing ConditionType's expression against the Variable.
     *
     * @param condition Existing ConditionType to use
     * @param variable VariableReferenceType to use
     * @param functionId XACML 3.0 identifier for the function
     * @return a new ConditionType
     */
    public static ConditionType addVariableToCondition(ConditionType condition, VariableReferenceType variable,
            Identifier functionId) {
        var applyFunction = new ApplyType();
        applyFunction.setFunctionId(functionId.stringValue());
        applyFunction.getExpression().add(condition.getExpression());
        applyFunction.getExpression().add(factory.createVariableReference(variable));
        var newCondition = new ConditionType();
        newCondition.setExpression(factory.createApply(applyFunction));
        return newCondition;
    }

    /**
     * Decodes TOSCA Policy properties into a particular type and validates the result.
     *
     * @param <T> desired type
     * @param properties properties to be decoded
     * @param clazz desired class
     * @return the decoded properties
     * @throws ToscaPolicyConversionException if the properties cannot be decoded or are
     *         invalid
     */
    public static <T> T decodeProperties(Map<String, Object> properties, Class<T> clazz)
                    throws ToscaPolicyConversionException {

        if (properties == null) {
            throw new ToscaPolicyConversionException(
                            "Cannot decode " + clazz.getSimpleName() + " from null properties");
        }

        try {
            var data = CODER.convert(properties, clazz);
            BeanValidationResult result = new BeanValidator().validateTop("properties", data);
            if (!result.isValid()) {
                throw new ToscaPolicyConversionException(result.getResult());
            }
            return data;
        } catch (CoderException e) {
            throw new ToscaPolicyConversionException("Cannot decode " + clazz.getSimpleName() + " from properties", e);
        }
    }
}
