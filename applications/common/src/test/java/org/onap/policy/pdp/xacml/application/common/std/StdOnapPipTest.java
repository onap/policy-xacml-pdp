/*-
 * ============LICENSE_START=======================================================
 * Copyright (C) 2019 AT&T Intellectual Property. All rights reserved.
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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.att.research.xacml.api.Attribute;
import com.att.research.xacml.api.AttributeValue;
import com.att.research.xacml.api.DataTypeException;
import com.att.research.xacml.api.Identifier;
import com.att.research.xacml.api.Status;
import com.att.research.xacml.api.XACML3;
import com.att.research.xacml.api.pip.PIPException;
import com.att.research.xacml.api.pip.PIPFinder;
import com.att.research.xacml.api.pip.PIPRequest;
import com.att.research.xacml.api.pip.PIPResponse;
import com.att.research.xacml.std.datatypes.DataTypes;
import com.att.research.xacml.std.pip.StdMutablePIPResponse;
import java.math.BigInteger;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.Properties;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.onap.policy.pdp.xacml.application.common.ToscaDictionary;

public class StdOnapPipTest {
    private static final String EXPECTED_EXCEPTION = "expected exception";
    private static final String MY_ID = "my-id";
    private static final String ISSUER = "my-issuer";
    private static final String STRING_VALUE = "my-value";

    private static final int INT_VALUE = 100;
    private static final long LONG_VALUE = 200L;

    private static final Identifier CATEGORY = XACML3.ID_ATTRIBUTE_CATEGORY_RESOURCE;
    private static final Identifier ATTRIBUTE_ID = ToscaDictionary.ID_RESOURCE_GUARD_ACTOR;

    @Mock
    private PIPRequest request;

    @Mock
    private PIPFinder finder;

    private StdMutablePIPResponse resp;

    private StdOnapPip pip;

    /**
     * Initializes objects, including the PIP.
     *
     * @throws PIPException if an error occurs
     */
    @Before
    public void setUp() throws PIPException {
        MockitoAnnotations.initMocks(this);

        resp = new StdMutablePIPResponse();

        when(request.getIssuer()).thenReturn(ISSUER);
        when(request.getAttributeId()).thenReturn(ATTRIBUTE_ID);

        pip = new MyPip();

        when(finder.getMatchingAttributes(request, pip)).thenReturn(resp);
    }

    @Test
    public void testAttributesProvided() {
        assertTrue(pip.attributesProvided().isEmpty());
    }

    @Test
    public void testConfigureStringProperties() throws PIPException {
        Properties props = new Properties();
        pip.configure(MY_ID, props);

        assertEquals(MY_ID, pip.getName());
        assertSame(props, pip.properties);
    }

    @Test
    public void testGetAttributePipFinderPipRequest_NullResponse() {
        assertNull(pip.getAttribute(finder, request));
    }

    @Test
    public void testGetAttributePipFinderPipRequest() {
        pip.addStringAttribute(resp, CATEGORY, CATEGORY, STRING_VALUE, request);

        assertEquals(STRING_VALUE, pip.getAttribute(finder, request));
    }

    @Test
    public void testGetAttributePipRequestPipFinder_NoStatus() {
        resp.setStatus(null);
        pip.addStringAttribute(resp, CATEGORY, CATEGORY, STRING_VALUE, request);

        assertSame(resp, pip.getAttribute(request, finder));
    }

    @Test
    public void testGetAttributePipRequestPipFinder_StatusNotOk() {
        Status status = mock(Status.class);
        when(status.isOk()).thenReturn(false);
        resp.setStatus(status);

        pip.addStringAttribute(resp, CATEGORY, CATEGORY, STRING_VALUE, request);

        assertNull(pip.getAttribute(request, finder));
    }

    @Test
    public void testGetAttributePipRequestPipFinder_StatusOk() {
        Status status = mock(Status.class);
        when(status.isOk()).thenReturn(true);
        resp.setStatus(status);

        pip.addStringAttribute(resp, CATEGORY, CATEGORY, STRING_VALUE, request);

        assertSame(resp, pip.getAttribute(request, finder));
    }

    @Test
    public void testGetAttributePipRequestPipFinder_NoAttributes() {
        assertNull(pip.getAttribute(request, finder));
    }

    @Test
    public void testGetAttributePipRequestPipFinder_Ex() throws PIPException {
        when(finder.getMatchingAttributes(request, pip)).thenThrow(new PIPException(EXPECTED_EXCEPTION));

        pip.addStringAttribute(resp, CATEGORY, CATEGORY, STRING_VALUE, request);

        assertNull(pip.getAttribute(request, finder));
    }

    @Test
    public void testFindFirstAttributeValue_NoAttributes() {
        assertNull(pip.findFirstAttributeValue(resp));
    }

    @Test
    public void testFindFirstAttributeValue_NullAttributeValue() {
        pip.addIntegerAttribute(resp, CATEGORY, ATTRIBUTE_ID, INT_VALUE, request);

        assertNull(pip.findFirstAttributeValue(resp));
    }

    @Test
    public void testFindFirstAttributeValue_NullValues() {
        pip.addStringAttribute(resp, CATEGORY, ATTRIBUTE_ID, null, request);
        pip.addStringAttribute(resp, CATEGORY, ATTRIBUTE_ID, STRING_VALUE, request);
        pip.addStringAttribute(resp, CATEGORY, ATTRIBUTE_ID, null, request);

        assertEquals(STRING_VALUE, pip.findFirstAttributeValue(resp));
    }

    @Test
    public void testAddIntegerAttribute() {
        pip.addIntegerAttribute(resp, CATEGORY, ATTRIBUTE_ID, INT_VALUE, request);
        assertEquals(1, resp.getAttributes().size());

        Attribute attr = resp.getAttributes().iterator().next();
        assertEquals(ISSUER, attr.getIssuer());
        assertEquals(CATEGORY, attr.getCategory());
        assertEquals(ATTRIBUTE_ID, attr.getAttributeId());

        Iterator<AttributeValue<BigInteger>> attrValues = attr.findValues(DataTypes.DT_INTEGER);
        assertTrue(attrValues.hasNext());
        assertEquals(INT_VALUE, attrValues.next().getValue().intValue());
    }

    @Test
    public void testAddIntegerAttribute_Ex() {
        pip = new MyPip() {
            @Override
            protected AttributeValue<BigInteger> makeInteger(int value) throws DataTypeException {
                throw new RuntimeException(EXPECTED_EXCEPTION);
            }
        };
        pip.addIntegerAttribute(resp, CATEGORY, ATTRIBUTE_ID, INT_VALUE, request);
        assertEquals(0, resp.getAttributes().size());
    }

    @Test
    public void testAddIntegerAttribute_Null() {
        pip = new MyPip() {
            @Override
            protected AttributeValue<BigInteger> makeInteger(int value) throws DataTypeException {
                return null;
            }
        };
        pip.addIntegerAttribute(resp, CATEGORY, ATTRIBUTE_ID, INT_VALUE, request);
        assertEquals(0, resp.getAttributes().size());
    }

    @Test
    public void testAddLongAttribute() {
        pip.addLongAttribute(resp, CATEGORY, ATTRIBUTE_ID, LONG_VALUE, request);
        assertEquals(1, resp.getAttributes().size());

        Attribute attr = resp.getAttributes().iterator().next();
        assertEquals(ISSUER, attr.getIssuer());
        assertEquals(CATEGORY, attr.getCategory());
        assertEquals(ATTRIBUTE_ID, attr.getAttributeId());

        Iterator<AttributeValue<BigInteger>> attrValues = attr.findValues(DataTypes.DT_INTEGER);
        assertTrue(attrValues.hasNext());
        assertEquals(LONG_VALUE, attrValues.next().getValue().longValue());
    }

    @Test
    public void testAddLongAttribute_Ex() {
        pip = new MyPip() {
            @Override
            protected AttributeValue<BigInteger> makeLong(long value) throws DataTypeException {
                throw new RuntimeException(EXPECTED_EXCEPTION);
            }
        };
        pip.addLongAttribute(resp, CATEGORY, ATTRIBUTE_ID, LONG_VALUE, request);
        assertEquals(0, resp.getAttributes().size());
    }

    @Test
    public void testAddLongAttribute_NullAttrValue() {
        pip = new MyPip() {
            @Override
            protected AttributeValue<BigInteger> makeLong(long value) throws DataTypeException {
                return null;
            }
        };
        pip.addLongAttribute(resp, CATEGORY, ATTRIBUTE_ID, LONG_VALUE, request);
        assertEquals(0, resp.getAttributes().size());
    }

    @Test
    public void testAddStringAttribute() {
        pip.addStringAttribute(resp, CATEGORY, ATTRIBUTE_ID, STRING_VALUE, request);
        assertEquals(1, resp.getAttributes().size());

        Attribute attr = resp.getAttributes().iterator().next();
        assertEquals(ISSUER, attr.getIssuer());
        assertEquals(CATEGORY, attr.getCategory());
        assertEquals(ATTRIBUTE_ID, attr.getAttributeId());

        Iterator<AttributeValue<String>> attrValues = attr.findValues(DataTypes.DT_STRING);
        assertTrue(attrValues.hasNext());
        assertEquals(STRING_VALUE, attrValues.next().getValue());
    }

    @Test
    public void testAddStringAttribute_Ex() {
        pip = new MyPip() {
            @Override
            protected AttributeValue<String> makeString(String value) throws DataTypeException {
                throw new RuntimeException(EXPECTED_EXCEPTION);
            }
        };
        pip.addStringAttribute(resp, CATEGORY, ATTRIBUTE_ID, STRING_VALUE, request);
        assertEquals(0, resp.getAttributes().size());
    }

    @Test
    public void testAddStringAttribute_NullAttrValue() {
        pip = new MyPip() {
            @Override
            protected AttributeValue<String> makeString(String value) throws DataTypeException {
                return null;
            }
        };
        pip.addStringAttribute(resp, CATEGORY, ATTRIBUTE_ID, STRING_VALUE, request);
        assertEquals(0, resp.getAttributes().size());
    }

    private class MyPip extends StdOnapPip {

        @Override
        public Collection<PIPRequest> attributesRequired() {
            return Collections.emptyList();
        }

        @Override
        public PIPResponse getAttributes(PIPRequest pipRequest, PIPFinder pipFinder) throws PIPException {
            return null;
        }
    }
}
