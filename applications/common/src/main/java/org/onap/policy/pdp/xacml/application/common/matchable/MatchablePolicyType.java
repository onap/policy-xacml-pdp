/*-
 * ============LICENSE_START=======================================================
 * ONAP
 * ================================================================================
 * Copyright (C) 2020-2021 AT&T Intellectual Property. All rights reserved.
 * Modifications Copyright (C) 2020-2021, 2024 Nordix Foundation.
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

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Function;
import lombok.Getter;
import lombok.NonNull;
import org.onap.policy.models.tosca.authorative.concepts.ToscaConceptIdentifier;
import org.onap.policy.models.tosca.authorative.concepts.ToscaDataType;
import org.onap.policy.models.tosca.authorative.concepts.ToscaPolicyType;
import org.onap.policy.models.tosca.authorative.concepts.ToscaProperty;
import org.onap.policy.models.tosca.authorative.concepts.ToscaSchemaDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Getter
public class MatchablePolicyType {
    private static final Logger LOGGER = LoggerFactory.getLogger(MatchablePolicyType.class);

    public static final String TOSCA_PRIMITIVE_STRING = "string";
    public static final String TOSCA_PRIMITIVE_INTEGER = "integer";
    public static final String TOSCA_PRIMITIVE_FLOAT = "float";
    public static final String TOSCA_PRIMITIVE_BOOLEAN = "boolean";
    public static final String TOSCA_PRIMITIVE_TIMESTAMP = "timestamp";
    public static final String TOSCA_TYPE_LIST = "list";
    public static final String TOSCA_TYPE_MAP = "map";

    //@formatter:off
    private static final Map<String, Function<ToscaProperty, MatchablePropertyTypeBase<?>>>
        mapPrimitivesProperty = Map.of(
            TOSCA_PRIMITIVE_STRING, MatchablePropertyTypeString::new,
            TOSCA_PRIMITIVE_INTEGER, MatchablePropertyTypeInteger::new,
            TOSCA_PRIMITIVE_FLOAT, MatchablePropertyTypeFloat::new,
            TOSCA_PRIMITIVE_BOOLEAN, MatchablePropertyTypeBoolean::new,
            TOSCA_PRIMITIVE_TIMESTAMP, MatchablePropertyTypeTimestamp::new
            );

    private static final Map<String, Function<ToscaSchemaDefinition, MatchablePropertyTypeBase<?>>>
        mapPrimitivesSchema = Map.of(
            TOSCA_PRIMITIVE_STRING, MatchablePropertyTypeString::new,
            TOSCA_PRIMITIVE_INTEGER, MatchablePropertyTypeInteger::new,
            TOSCA_PRIMITIVE_FLOAT, MatchablePropertyTypeFloat::new,
            TOSCA_PRIMITIVE_BOOLEAN, MatchablePropertyTypeBoolean::new,
            TOSCA_PRIMITIVE_TIMESTAMP, MatchablePropertyTypeTimestamp::new
            );
    //@formatter:on

    private final ToscaConceptIdentifier policyId;
    private final Map<String, MatchableProperty> matchables = new HashMap<>();

    public MatchablePolicyType(@NonNull ToscaPolicyType policyType, @NonNull MatchableCallback callback) {
        this.policyId = new ToscaConceptIdentifier(policyType.getName(), policyType.getVersion());
        scanPolicyType(policyType, callback);
    }

    public MatchableProperty get(@NonNull String property) {
        return this.matchables.get(property);
    }

    protected void scanPolicyType(@NonNull ToscaPolicyType inPolicyType, @NonNull MatchableCallback callback) {
        ToscaPolicyType policyType = inPolicyType;
        while (policyType != null) {
            LOGGER.info("Scanning PolicyType {}:{}", policyType.getName(), policyType.getVersion());
            //
            // Scan for all the matchable properties
            //
            scanProperties(policyType.getProperties(), matchables, callback);
            //
            // Does this PolicyType derive from another Policy Type?
            //
            if ("tosca.policies.Root".equals(policyType.getDerivedFrom())) {
                //
                // No we are finished
                //
                LOGGER.info("Found root - done scanning");
                break;
            }
            //
            // Move to the parent policy and scan it for matchables.
            //
            policyType = callback.retrievePolicyType(policyType.getDerivedFrom());
        }
    }

    /**
     * Scans properties for matchables.
     *
     * @param properties Map of ToscaProperties to scan
     * @param matchables Found matchables will be put into this list
     * @param callback   Callback routine for finding Policy Types and Data Types
     */
    public static void scanProperties(Map<String, ToscaProperty> properties, Map<String, MatchableProperty> matchables,
            MatchableCallback callback) {
        for (Entry<String, ToscaProperty> entrySet : properties.entrySet()) {
            final String property = entrySet.getKey();
            final var toscaProperty = entrySet.getValue();
            //
            // Most likely case is it's a primitive
            //
            if (isPrimitive(toscaProperty.getType())) {
                MatchableProperty primitiveProperty = handlePrimitive(property, toscaProperty);
                if (primitiveProperty != null) {
                    matchables.put(property, primitiveProperty);
                }
            } else if (TOSCA_TYPE_LIST.equals(toscaProperty.getType())) {
                MatchableProperty listProperty = handleList(property, toscaProperty, matchables, callback);
                if (listProperty != null) {
                    matchables.put(property, listProperty);
                }
            } else if (TOSCA_TYPE_MAP.equals(toscaProperty.getType())) {
                MatchableProperty mapProperty = handleMap(property, toscaProperty, matchables, callback);
                if (mapProperty != null) {
                    matchables.put(property, mapProperty);
                }
            } else {
                scanDatatype(toscaProperty, matchables, callback);
            }
        }
    }

    /**
     * handlePrimitive - handles a primitive type only if its matchable.
     *
     * @param property      String containing property name
     * @param toscaProperty ToscaProperty object
     * @return MatchableProperty object
     */
    public static MatchableProperty handlePrimitive(String property, ToscaProperty toscaProperty) {
        if (!isMatchable(toscaProperty)) {
            return null;
        }
        Function<ToscaProperty, MatchablePropertyTypeBase<?>> function =
                mapPrimitivesProperty.get(toscaProperty.getType());
        if (function != null) {
            return new MatchableProperty(property, function.apply(toscaProperty));
        }
        throw new IllegalArgumentException("Not a primitive " + toscaProperty.getType());
    }

    /**
     * handlePrimitive from a schema. Note that a ToscaEntrySchema does NOT have a metadata section
     * so you cannot check if its matchable.
     *
     * @param property    String containing property name
     * @param toscaSchema ToscaSchema
     * @return MatchableProperty object
     */
    public static MatchableProperty handlePrimitive(String property, ToscaSchemaDefinition toscaSchema) {
        Function<ToscaSchemaDefinition, MatchablePropertyTypeBase<?>> function =
            mapPrimitivesSchema.get(toscaSchema.getType());
        if (function != null) {
            return new MatchableProperty(property, function.apply(toscaSchema));
        }
        throw new IllegalArgumentException("Not a primitive " + toscaSchema.getType());
    }

    /**
     * handleList - iterates a list looking for matchables.
     *
     * @param property      String containing property name
     * @param toscaProperty ToscaProperty object
     * @param matchables    list of matchables to add to
     * @param callback      MatchableCallback to retrieve Data Types
     * @return MatchableProperty object
     */
    public static MatchableProperty handleList(@NonNull String property, @NonNull ToscaProperty toscaProperty,
                                               Map<String, MatchableProperty> matchables,
                                               @NonNull MatchableCallback callback) {
        if (!TOSCA_TYPE_LIST.equals(toscaProperty.getType())) {
            throw new IllegalArgumentException("must be a list");
        }
        //
        // Is it a simple or complex list?
        //
        if (isPrimitive(toscaProperty.getEntrySchema().getType())) {
            //
            // Only if the list is matchable
            //
            if (!isMatchable(toscaProperty)) {
                return null;
            }
            //
            // Simple list of primitives
            //
            return new MatchableProperty(property, new MatchablePropertyTypeList(toscaProperty));
        }
        //
        // Scan the datatype for matchables
        //
        scanDatatype(toscaProperty.getEntrySchema(), matchables, callback);
        //
        // Return nothing - scanning the datatype should find the matchables
        //
        return null;
    }

    /**
     * handleMap - iterates a map looking for matchables.
     *
     * @param property      String containing property name
     * @param toscaProperty ToscaProperty object
     * @param matchables    list of matchables to add to
     * @param callback      MatchableCallback to retrieve Data Types
     * @return MatchableProperty object
     */
    public static MatchableProperty handleMap(@NonNull String property, @NonNull ToscaProperty toscaProperty,
                                              Map<String, MatchableProperty> matchables,
                                              @NonNull MatchableCallback callback) {
        if (!TOSCA_TYPE_MAP.equals(toscaProperty.getType())) {
            throw new IllegalArgumentException("must be a map");
        }
        //
        // Is it a simple or complex map?
        //
        if (isPrimitive(toscaProperty.getEntrySchema().getType())) {
            //
            // Only if the map is matchable
            //
            if (!isMatchable(toscaProperty)) {
                return null;
            }
            //
            // Simple map of primitives
            //
            return new MatchableProperty(property, new MatchablePropertyTypeMap(toscaProperty));
        }
        //
        // Scan the datatype for matchables
        //
        scanDatatype(toscaProperty.getEntrySchema(), matchables, callback);
        //
        // Return nothing - scanning the datatype should find the matchables
        //
        return null;
    }

    /**
     * scanDatatype - scans a datatypes schema properties for matchables.
     *
     * @param toscaProperty ToscaProperty object
     * @param matchables    list of matchables to add to
     * @param callback      MatchableCallback to retrieve Data Types
     */
    public static void scanDatatype(@NonNull ToscaProperty toscaProperty, Map<String, MatchableProperty> matchables,
                                    @NonNull MatchableCallback callback) {
        //
        // Don't check for matchable property, as that does not make sense to support right now. But we need to
        // scan the datatype for matchable properties.
        //
        LOGGER.info("Retrieving datatype {}", toscaProperty.getType());
        //
        // Try to retrieve the datatype
        //
        ToscaDataType dataType = callback.retrieveDataType(toscaProperty.getType());
        if (dataType == null) {
            LOGGER.error("Failed to retrieve datatype {}", toscaProperty.getType());
            return;
        }
        //
        // Now scan the properties in the datatype
        //
        scanProperties(dataType.getProperties(), matchables, callback);
    }

    /**
     * scanDatatypes - scans a datatype schema for matchables.
     *
     * @param toscaSchema ToscaEntrySchema
     * @param matchables  list of matchables to add to
     * @param callback    MatchableCallback object for retrieving other data types
     */
    public static void scanDatatype(@NonNull ToscaSchemaDefinition toscaSchema,
                                    Map<String, MatchableProperty> matchables,
                                    @NonNull MatchableCallback callback) {
        //
        // Don't check for matchable property, as that does not make sense to support right now. But we need to
        // scan the datatype for matchable properties.
        //
        LOGGER.info("Retrieving datatype {}", toscaSchema.getType());
        //
        // Try to retrieve the datatype
        //
        ToscaDataType dataType = callback.retrieveDataType(toscaSchema.getType());
        if (dataType == null) {
            LOGGER.error("Failed to retrieve datatype {}", toscaSchema.getType());
            return;
        }
        //
        // Now scan the properties in the datatype
        //
        scanProperties(dataType.getProperties(), matchables, callback);
    }

    /**
     * isPrimitive.
     *
     * @param type String containing type
     * @return true if it is a primitive
     */
    public static boolean isPrimitive(@NonNull String type) {
        return mapPrimitivesProperty.containsKey(type);
    }

    /**
     * isMatchable - scans metadata for matchable field set to true.
     *
     * @param property ToscaProperty object
     * @return true if matchable
     */
    public static boolean isMatchable(@NonNull ToscaProperty property) {
        if (property.getMetadata() == null) {
            return false;
        }
        String isMatchable = property.getMetadata().get("matchable");
        if (isMatchable == null) {
            return false;
        }
        return "true".equalsIgnoreCase(isMatchable);
    }

}
