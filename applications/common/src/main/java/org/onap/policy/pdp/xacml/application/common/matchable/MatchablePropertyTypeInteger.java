/*-
 * ============LICENSE_START=======================================================
 * ONAP
 * ================================================================================
 * Copyright (C) 2020 AT&T Intellectual Property. All rights reserved.
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
import oasis.names.tc.xacml._3_0.core.schema.wd_17.AllOfType;
import oasis.names.tc.xacml._3_0.core.schema.wd_17.MatchType;
import oasis.names.tc.xacml._3_0.core.schema.wd_17.TargetType;
import org.onap.policy.models.tosca.authorative.concepts.ToscaEntrySchema;
import org.onap.policy.models.tosca.authorative.concepts.ToscaProperty;
import org.onap.policy.pdp.xacml.application.common.ToscaPolicyConversionException;
import org.onap.policy.pdp.xacml.application.common.ToscaPolicyTranslatorUtils;

public class MatchablePropertyTypeInteger extends MatchablePropertyTypeBase<Integer> {

    public MatchablePropertyTypeInteger(ToscaProperty inProperty) {
        super(inProperty);
    }

    public MatchablePropertyTypeInteger(ToscaEntrySchema toscaSchema) {
        super(toscaSchema);
    }

    @Override
    public Integer validate(Object value) throws ToscaPolicyConversionException {
        if (value instanceof Integer) {
            return (Integer) value;
        }
        try {
            return Integer.parseInt(value.toString());
        } catch (NumberFormatException e) {
            throw new ToscaPolicyConversionException("Bad float value" + value.toString(), e);
        }
    }

    @Override
    public void generate(TargetType target, Object value, Identifier attributeId)
            throws ToscaPolicyConversionException {
        MatchType match = ToscaPolicyTranslatorUtils.buildMatchTypeDesignator(
                XACML3.ID_FUNCTION_INTEGER_EQUAL,
                validate(value).toString(),
                XACML3.ID_DATATYPE_INTEGER,
                attributeId,
                XACML3.ID_ATTRIBUTE_CATEGORY_RESOURCE);
        AllOfType allOf = new AllOfType();
        allOf.getMatch().add(match);
        //
        // Add to the first AllOf - in the future we will use other metadata and/or
        // constraints to dictate how the TargetType should be setup.
        //
        target.getAnyOf().get(0).getAllOf().add(allOf);
    }

}
