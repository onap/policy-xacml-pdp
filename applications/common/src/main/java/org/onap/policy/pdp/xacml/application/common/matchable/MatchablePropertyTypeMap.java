/*-
 * ============LICENSE_START=======================================================
 * ONAP
 * ================================================================================
 * Copyright (C) 2020-2021 AT&T Intellectual Property. All rights reserved.
 * Modifications Copyright (C) 2020 Nordix Foundation.
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
import com.att.research.xacml.std.IdentifierImpl;
import java.util.Collections;
import java.util.Map;
import java.util.Map.Entry;
import lombok.NonNull;
import oasis.names.tc.xacml._3_0.core.schema.wd_17.AnyOfType;
import org.onap.policy.models.tosca.authorative.concepts.ToscaProperty;
import org.onap.policy.models.tosca.authorative.concepts.ToscaSchemaDefinition;
import org.onap.policy.pdp.xacml.application.common.ToscaPolicyConversionException;
import org.onap.policy.pdp.xacml.application.common.ToscaPolicyTranslatorUtils;

public class MatchablePropertyTypeMap extends MatchablePropertyTypeBase<Map<String, MatchablePropertyType<?>>> {
    private MatchableProperty primitiveProperty;

    /**
     * constructor.
     *
     * @param toscaProperty ToscaProperty object
     */
    public MatchablePropertyTypeMap(@NonNull ToscaProperty toscaProperty) {
        super(toscaProperty);

        ToscaSchemaDefinition schema = toscaProperty.getEntrySchema();
        this.primitiveProperty = MatchablePolicyType.handlePrimitive(schema.getType(), schema);
    }

    @SuppressWarnings("unchecked")
    @Override
    public Map<String, MatchablePropertyType<?>> validate(Object value) throws ToscaPolicyConversionException {
        if (value instanceof Map) {
            return (Map<String, MatchablePropertyType<?>>) value;
        }
        return Collections.emptyMap();
    }

    @Override
    public Object generate(Object value, Identifier attributeId) throws ToscaPolicyConversionException {
        var anyOf = new AnyOfType();
        for (Entry<String, MatchablePropertyType<?>> entrySet : this.validate(value).entrySet()) {
            final String id = entrySet.getKey();
            final Object val = entrySet.getValue();
            //
            // For map we use the LHS as part of the attribute id
            //
            Identifier newId = new IdentifierImpl(attributeId + ":" + id);
            //
            // Build the AnyOfType
            //
            anyOf = ToscaPolicyTranslatorUtils.buildAndAppendAllof(anyOf,
                    primitiveProperty.getType().generate(val, newId));
        }
        return anyOf;
    }

}
