/*-
 * ============LICENSE_START=======================================================
 * ONAP
 * ================================================================================
 * Copyright (C) 2020 AT&T Intellectual Property. All rights reserved.
 * Modifications Copyright (C) 2020, 2024 Nordix Foundation.
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

package org.onap.policy.pdp.xacml.application.common.matchable;

import com.att.research.xacml.api.Identifier;
import com.att.research.xacml.api.XACML3;
import org.onap.policy.models.tosca.authorative.concepts.ToscaProperty;
import org.onap.policy.models.tosca.authorative.concepts.ToscaSchemaDefinition;
import org.onap.policy.pdp.xacml.application.common.ToscaPolicyConversionException;
import org.onap.policy.pdp.xacml.application.common.ToscaPolicyTranslatorUtils;

public class MatchablePropertyTypeFloat extends MatchablePropertyTypeBase<Float> {

    public MatchablePropertyTypeFloat(ToscaProperty inProperty) {
        super(inProperty);
    }

    public MatchablePropertyTypeFloat(ToscaSchemaDefinition toscaSchema) {
        super(toscaSchema);
    }

    @Override
    public Float validate(Object value) throws ToscaPolicyConversionException {
        //
        // Most likely it isn't because Gson does not recognize floats
        //
        if (value instanceof Float floatValue) {
            return floatValue;
        }
        try {
            return Float.parseFloat(value.toString());
        } catch (NumberFormatException e) {
            throw new ToscaPolicyConversionException("Bad float value" + value, e);
        }
    }

    @Override
    public Object generate(Object value, Identifier attributeId)
            throws ToscaPolicyConversionException {
        return ToscaPolicyTranslatorUtils.buildMatchTypeDesignator(
                XACML3.ID_FUNCTION_INTEGER_EQUAL,
                validate(value).toString(),
                XACML3.ID_DATATYPE_INTEGER,
                attributeId,
                XACML3.ID_ATTRIBUTE_CATEGORY_RESOURCE);
    }

}
