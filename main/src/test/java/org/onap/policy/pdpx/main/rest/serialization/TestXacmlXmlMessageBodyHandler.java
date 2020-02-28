/*-
 * ============LICENSE_START=======================================================
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

package org.onap.policy.pdpx.main.rest.serialization;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.att.research.xacml.api.Request;
import com.att.research.xacml.api.RequestAttributes;
import com.att.research.xacml.api.Response;
import com.att.research.xacml.std.dom.DOMResponse;
import com.att.research.xacml.std.dom.DOMStructureException;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Iterator;
import org.junit.Before;
import org.junit.Test;
import org.onap.policy.common.utils.resources.ResourceUtils;

public class TestXacmlXmlMessageBodyHandler {

    private static final String PRIMARY_TYPE = "application";
    private static final String SUB_TYPE = "xacml+xml";

    @SuppressWarnings("rawtypes")
    private static final Class REQUEST_CLASS = Request.class;
    @SuppressWarnings("rawtypes")
    private static final Class RESPONSE_CLASS = Response.class;

    private XacmlXmlMessageBodyHandler hdlr;

    @Before
    public void setUp() {
        hdlr = new XacmlXmlMessageBodyHandler();
    }

    @Test
    public void testIsWriteable() {
        CommonSerialization.testIsWritableOrReadable(PRIMARY_TYPE, SUB_TYPE, hdlr::isWriteable);
    }

    @Test
    public void testWriteTo() throws IOException, DOMStructureException {
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        Response resp = DOMResponse.load(ResourceUtils.getResourceAsString(
                "src/test/resources/decisions/decision.native.response.xml"));
        hdlr.writeTo(resp, RESPONSE_CLASS, RESPONSE_CLASS, null, null, null, stream);
        assertEquals(resp, DOMResponse.load(new ByteArrayInputStream(stream.toByteArray())));
    }

    @Test
    public void testIsReadable() {
        CommonSerialization.testIsWritableOrReadable(PRIMARY_TYPE, SUB_TYPE, hdlr::isReadable);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testReadFrom() throws IOException {
        Request req = hdlr.readFrom(REQUEST_CLASS, REQUEST_CLASS, null, null, null, ResourceUtils.getResourceAsStream(
                "src/test/resources/decisions/decision.native.request.xml"));
        assertFalse(req.getCombinedDecision());
        assertFalse(req.getReturnPolicyIdList());
        assertTrue(req.getRequestAttributes().size() == 4);
        Iterator<RequestAttributes> iter = req.getRequestAttributes().iterator();

        RequestAttributes firstRequestAttributes = iter.next();
        assertTrue(firstRequestAttributes.getAttributes().size() == 1);
        assertEquals("Julius Hibbert", firstRequestAttributes.getAttributes().iterator().next()
                .getValues().iterator().next().getValue().toString());

        RequestAttributes secondRequestAttributes = iter.next();
        assertTrue(secondRequestAttributes.getAttributes().size() == 1);
        assertEquals("http://medico.com/record/patient/BartSimpson", secondRequestAttributes.getAttributes()
                .iterator().next().getValues().iterator().next().getValue().toString());

        RequestAttributes thirdRequestAttributes = iter.next();
        assertTrue(thirdRequestAttributes.getAttributes().size() == 1);
        assertEquals("read", thirdRequestAttributes.getAttributes().iterator().next()
                .getValues().iterator().next().getValue().toString());
    }
}