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
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import oasis.names.tc.xacml._3_0.core.schema.wd_17.AllOfType;
import oasis.names.tc.xacml._3_0.core.schema.wd_17.AnyOfType;
import oasis.names.tc.xacml._3_0.core.schema.wd_17.MatchType;
import org.onap.policy.models.tosca.authorative.concepts.ToscaEntrySchema;
import org.onap.policy.models.tosca.authorative.concepts.ToscaProperty;
import org.onap.policy.pdp.xacml.application.common.ToscaPolicyConversionException;

public class MatchablePropertyTypeList extends MatchablePropertyTypeBase<List<MatchablePropertyType<?>>> {
    MatchableProperty primitiveProperty;

    /**
     * constructor.
     *
     * @param toscaProperty ToscaProperty object
     */
    public MatchablePropertyTypeList(ToscaProperty toscaProperty) {
        super(toscaProperty);

        ToscaEntrySchema schema = toscaProperty.getEntrySchema();
        this.primitiveProperty = MatchablePolicyType.handlePrimitive(schema.getType(), schema);
    }

    @SuppressWarnings("unchecked")
    @Override
    public List<MatchablePropertyType<?>> validate(Object value) throws ToscaPolicyConversionException {
        if (value instanceof Collection) {
            return (List<MatchablePropertyType<?>>) value;
        }
        return Collections.emptyList();
    }

    @Override
    public Object generate(Object value, Identifier attributeId)
            throws ToscaPolicyConversionException {
        AnyOfType anyOf = null;
        for (Object val : this.validate(value)) {
            Object match = primitiveProperty.getType().generate(val, attributeId);
            if (match instanceof MatchType) {
                AllOfType allOf = new AllOfType();
                allOf.getMatch().add((MatchType) match);
                if (anyOf == null) {
                    anyOf = new AnyOfType();
                }
                anyOf.getAllOf().add(allOf);
            } else if (match instanceof AllOfType) {
                if (anyOf == null) {
                    anyOf = new AnyOfType();
                }
                anyOf.getAllOf().add((AllOfType) match);
            }
        }
        return anyOf;
    }

}
