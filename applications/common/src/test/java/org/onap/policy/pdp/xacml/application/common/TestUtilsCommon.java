/*-
 * ============LICENSE_START=======================================================
 * ONAP
 * ================================================================================
 * Copyright (C) 2019 AT&T Intellectual Property. All rights reserved.
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

import com.att.research.xacml.api.AttributeAssignment;
import com.att.research.xacml.api.Decision;
import com.att.research.xacml.api.IdReference;
import com.att.research.xacml.api.Obligation;
import com.att.research.xacml.api.Response;
import com.att.research.xacml.api.StatusCode;
import com.att.research.xacml.api.XACML3;
import com.att.research.xacml.std.IdentifierImpl;
import com.att.research.xacml.std.StdAttributeAssignment;
import com.att.research.xacml.std.StdAttributeValue;
import com.att.research.xacml.std.StdIdReference;
import com.att.research.xacml.std.StdMutableObligation;
import com.att.research.xacml.std.StdMutableResponse;
import com.att.research.xacml.std.StdMutableResult;
import com.att.research.xacml.std.StdStatus;
import com.att.research.xacml.std.StdVersion;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

public class TestUtilsCommon {

    /**
     * createAttributeAssignment.
     *
     * @param <T>      Object can be String, Integer, Double, Boolean
     * @param id       String attribute id
     * @param category String for the attribute category
     * @param value    Object containing a value
     * @return AttributeAssignment object
     */
    public static <T> AttributeAssignment createAttributeAssignment(String id, String category, T value) {
        StdAttributeValue<T> attributeValue = null;
        if (value instanceof String) {
            attributeValue = new StdAttributeValue<>(XACML3.ID_DATATYPE_STRING, value);
        } else if (value instanceof Integer) {
            attributeValue = new StdAttributeValue<>(XACML3.ID_DATATYPE_INTEGER, value);
        } else if (value instanceof Double) {
            attributeValue = new StdAttributeValue<>(XACML3.ID_DATATYPE_DOUBLE, value);
        } else if (value instanceof Boolean) {
            attributeValue = new StdAttributeValue<>(XACML3.ID_DATATYPE_BOOLEAN, value);
        } else {
            throw new IllegalArgumentException("Unsupported value object " + value.getClass());
        }

        return new StdAttributeAssignment(new IdentifierImpl(category),
            new IdentifierImpl(id), "", attributeValue);
    }

    /**
     * createXacmlObligation.
     *
     * @param id                   String obligation id
     * @param attributeAssignments Collection of AttributeAssignment objects
     * @return Obligation object
     */
    public static Obligation createXacmlObligation(String id, Collection<AttributeAssignment> attributeAssignments) {
        return new StdMutableObligation(new IdentifierImpl(id), attributeAssignments);
    }

    /**
     * createPolicyIdList.
     *
     * @param ids Map of policy Ids
     * @return {@code Collection<IdReference>} objects
     * @throws ParseException ParseException
     */
    public static Collection<IdReference> createPolicyIdList(Map<String, String> ids) throws ParseException {
        List<IdReference> policyIds = new ArrayList<>();

        for (Entry<String, String> entrySet : ids.entrySet()) {
            policyIds.add(new StdIdReference(new IdentifierImpl(entrySet.getKey()),
                StdVersion.newInstance(entrySet.getValue())));
        }

        return policyIds;
    }

    /**
     * createXacmlResponse.
     *
     * @param code        StatusCode
     * @param decision    Decision
     * @param obligations Collection of Obligation objects
     * @param policyIds   Collection of IdReference objects
     * @return Response object
     */
    public static Response createXacmlResponse(StatusCode code, String message, Decision decision,
                                               Collection<Obligation> obligations,
                                               Collection<IdReference> policyIds) {

        StdStatus status = new StdStatus(code, message);

        StdMutableResult result = new StdMutableResult(decision, status);
        result.addObligations(obligations);
        result.addPolicyIdentifiers(policyIds);

        return new StdMutableResponse(result);
    }

}
