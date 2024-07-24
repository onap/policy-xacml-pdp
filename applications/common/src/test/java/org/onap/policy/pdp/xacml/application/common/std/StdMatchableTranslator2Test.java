/*-
 * ============LICENSE_START=======================================================
 * Copyright (C) 2024 Nordix Foundation.
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

package org.onap.policy.pdp.xacml.application.common.std;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.att.research.xacml.api.Obligation;
import com.att.research.xacml.api.Request;
import com.att.research.xacml.std.IdentifierImpl;
import java.util.HashMap;
import java.util.HashSet;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.onap.policy.models.decisions.concepts.DecisionRequest;
import org.onap.policy.models.tosca.authorative.concepts.ToscaPolicy;
import org.onap.policy.models.tosca.authorative.concepts.ToscaPolicyType;
import org.onap.policy.models.tosca.authorative.concepts.ToscaServiceTemplate;
import org.onap.policy.pdp.xacml.application.common.ToscaPolicyConversionException;
import org.onap.policy.pdp.xacml.application.common.XacmlApplicationException;

class StdMatchableTranslator2Test {

    @Test
    void convertRequest() throws ToscaPolicyConversionException {
        var translator = new StdMatchableTranslator();
        var returnRequest = Mockito.mock(Request.class);
        var decisionRequest = Mockito.mock(DecisionRequest.class);

        try (MockedStatic<StdMatchablePolicyRequest> utilities = Mockito.mockStatic(StdMatchablePolicyRequest.class)) {
            utilities.when(() -> StdMatchablePolicyRequest.createInstance(decisionRequest))
                .thenReturn(returnRequest);

            assertEquals(returnRequest, translator.convertRequest(decisionRequest));
        }
    }

    @Test
    void convertRequest_Exception() {
        var translator = new StdMatchableTranslator();
        var decisionRequest = Mockito.mock(DecisionRequest.class);

        try (MockedStatic<StdMatchablePolicyRequest> utilities = Mockito.mockStatic(StdMatchablePolicyRequest.class)) {
            utilities.when(() -> StdMatchablePolicyRequest.createInstance(decisionRequest))
                .thenThrow(new XacmlApplicationException("throwing an exception"));

            assertThrows(ToscaPolicyConversionException.class, () -> translator.convertRequest(decisionRequest));
        }
    }

    @Test
    void scanClosestMatchObligation() {
        var translator = new StdMatchableTranslator();
        var obligation = Mockito.mock(Obligation.class);
        when(obligation.getId()).thenReturn(new IdentifierImpl("id"));
        when(obligation.getAttributeAssignments()).thenReturn(new HashSet<>());

        assertDoesNotThrow(() -> translator.scanClosestMatchObligation(new HashMap<>(), obligation));
    }

    @Test
    void convertPolicy() throws ToscaPolicyConversionException {
        var translator = mock(StdMatchableTranslator.class);
        var toscaPolicy = Mockito.mock(ToscaPolicy.class);
        when(translator.convertPolicy(toscaPolicy)).thenCallRealMethod();
        when(translator.findPolicyType(toscaPolicy.getTypeIdentifier())).thenReturn(null);

        assertThrows(ToscaPolicyConversionException.class, () -> translator.convertPolicy(toscaPolicy));
    }

    @Test
    void retrievePolicyType() {
        var hashMap = new HashMap<String, ToscaPolicyType>();
        var toscaPolicyType = new ToscaPolicyType();
        hashMap.put("someId", toscaPolicyType);

        var toscaTemplate = mock(ToscaServiceTemplate.class);
        when(toscaTemplate.getPolicyTypes()).thenReturn(hashMap);

        var translator = mock(StdMatchableTranslator.class);
        when(translator.findPolicyType(any())).thenReturn(toscaTemplate);
        when(translator.retrievePolicyType("someId")).thenCallRealMethod();

        assertEquals(toscaPolicyType, translator.retrievePolicyType("someId"));
    }

    @Test
    void retrievePolicyType_Exception() {
        var translator = mock(StdMatchableTranslator.class);
        when(translator.findPolicyType(any())).thenReturn(null);
        when(translator.retrievePolicyType("someId")).thenCallRealMethod();

        assertNull(translator.retrievePolicyType("someId"));
    }

    @Test
    void retrieveDataType() {
        assertNull(new StdMatchableTranslator().retrieveDataType("dataType"));
    }
}