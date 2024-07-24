/*-
 * ============LICENSE_START=======================================================
 * Copyright (C) 2020 AT&T Intellectual Property. All rights reserved.
 * Modifications Copyright (C) 2023-2024 Nordix Foundation.
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

package org.onap.policy.pdpx.main.rest.serialization;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.att.research.xacml.api.Request;
import com.att.research.xacml.api.Response;
import jakarta.ws.rs.core.MediaType;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

public class CommonSerialization {

    @SuppressWarnings("rawtypes")
    private static final Class REQUEST_CLASS = Request.class;
    @SuppressWarnings("rawtypes")
    private static final Class RESPONSE_CLASS = Response.class;
    @SuppressWarnings("rawtypes")
    private static final Class GENERAL_CLASS = MyObject.class;

    public static void testIsWritableOrReadable(String primaryType, String subType,
            MultiArgsFunction<Class<?>, Type, Annotation[], MediaType, Boolean> getter) {

        // null media type
        assertFalse(getter.apply(null, null, null, null));

        // valid media type and class type
        assertTrue(getter.apply(REQUEST_CLASS, null, null, new MediaType(primaryType, subType)));
        assertTrue(getter.apply(RESPONSE_CLASS, null, null, new MediaType(primaryType, subType)));

        // valid media type but invalid class type
        assertFalse(getter.apply(GENERAL_CLASS, null, null, new MediaType(primaryType, subType)));

        // null subtype or invalid media type
        assertFalse(getter.apply(null, null, null, new MediaType(primaryType, null)));
        assertFalse(getter.apply(null, null, null, MediaType.APPLICATION_JSON_TYPE));
    }

    public static class MyObject {
        private int id;

        public MyObject() {
            super();
        }

        public MyObject(int id) {
            this.id = id;
        }

        @Override
        public String toString() {
            return "MyObject [id=" + id + "]";
        }
    }

    @FunctionalInterface
    public interface MultiArgsFunction<T, U, V, W, R> {
        public R apply(T value1, U value2, V value3, W value4);
    }
}