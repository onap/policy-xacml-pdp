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
import com.att.research.xacml.std.IdentifierImpl;
import java.util.Collections;
import java.util.Map;
import java.util.Map.Entry;
import lombok.NonNull;
import oasis.names.tc.xacml._3_0.core.schema.wd_17.TargetType;
import org.onap.policy.models.tosca.authorative.concepts.ToscaEntrySchema;
import org.onap.policy.models.tosca.authorative.concepts.ToscaProperty;
import org.onap.policy.pdp.xacml.application.common.ToscaPolicyConversionException;

public class MatchablePropertyTypeMap extends MatchablePropertyTypeBase<Map<String, MatchablePropertyType<?>>> {
    MatchableProperty primitiveProperty;

    /**
     * constructor.
     *
     * @param toscaProperty ToscaProperty object
     */
    public MatchablePropertyTypeMap(@NonNull ToscaProperty toscaProperty) {
        super(toscaProperty);

        ToscaEntrySchema schema = toscaProperty.getEntrySchema();
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
    public void generate(TargetType target, Object value, Identifier attributeId)
            throws ToscaPolicyConversionException {
        for (Entry<String, MatchablePropertyType<?>> entrySet : this.validate(value).entrySet()) {
            final String id = entrySet.getKey();
            final Object val = entrySet.getValue();
            Identifier newId = new IdentifierImpl(attributeId + ":" + id);
            primitiveProperty.getType().generate(target, val, newId);
        }
    }

}
