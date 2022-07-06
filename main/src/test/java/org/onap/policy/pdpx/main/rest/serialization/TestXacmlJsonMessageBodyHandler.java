/*-
 * ============LICENSE_START=======================================================
 * Copyright (C) 2020, 2022 AT&T Intellectual Property. All rights reserved.
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import com.att.research.xacml.api.Request;
import com.att.research.xacml.api.RequestAttributes;
import com.att.research.xacml.api.Response;
import com.att.research.xacml.std.StdMutableRequest;
import com.att.research.xacml.std.StdMutableResponse;
import com.att.research.xacml.std.StdRequest;
import com.att.research.xacml.std.StdResponse;
import com.att.research.xacml.std.dom.DOMResponse;
import com.att.research.xacml.std.dom.DOMStructureException;
import com.att.research.xacml.std.json.JSONStructureException;
import com.att.research.xacml.std.json.JsonResponseTranslator;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Iterator;
import org.junit.Before;
import org.junit.Test;
import org.onap.policy.common.utils.resources.ResourceUtils;

public class TestXacmlJsonMessageBodyHandler {

    private static final String PRIMARY_TYPE = "application";
    private static final String SUB_TYPE = "xacml+json";

    @SuppressWarnings("rawtypes")
    private static final Class REQUEST_CLASS = Request.class;
    @SuppressWarnings("rawtypes")
    private static final Class RESPONSE_CLASS = Response.class;

    private XacmlJsonMessageBodyHandler hdlr;

    @Before
    public void setUp() {
        hdlr = new XacmlJsonMessageBodyHandler();
    }

    @Test
    public void testIsWriteable() {
        CommonSerialization.testIsWritableOrReadable(PRIMARY_TYPE, SUB_TYPE, hdlr::isWriteable);
    }

    @Test
    public void testWriteTo() throws IOException, DOMStructureException, JSONStructureException {
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        Response resp = DOMResponse.load(ResourceUtils.getResourceAsString(
                "src/test/resources/decisions/decision.native.response.xml"));
        hdlr.writeTo(resp, RESPONSE_CLASS, RESPONSE_CLASS, null, null, null, stream);
        assertEquals(resp, JsonResponseTranslator.load(new ByteArrayInputStream(stream.toByteArray())));
    }

    @Test
    public void testIsReadable() {
        CommonSerialization.testIsWritableOrReadable(PRIMARY_TYPE, SUB_TYPE, hdlr::isReadable);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testReadFrom() throws IOException {
        Request req = hdlr.readFrom(REQUEST_CLASS, REQUEST_CLASS, null, null, null, ResourceUtils.getResourceAsStream(
                "src/test/resources/decisions/decision.native.request.json"));
        assertFalse(req.getCombinedDecision());
        assertFalse(req.getReturnPolicyIdList());
        assertEquals(3, req.getRequestAttributes().size());
        Iterator<RequestAttributes> iter = req.getRequestAttributes().iterator();

        RequestAttributes firstRequestAttributes = iter.next();
        assertEquals(1, firstRequestAttributes.getAttributes().size());
        assertEquals("Julius Hibbert", firstRequestAttributes.getAttributes().iterator().next()
                .getValues().iterator().next().getValue().toString());

        RequestAttributes secondRequestAttributes = iter.next();
        assertEquals(1, secondRequestAttributes.getAttributes().size());
        assertEquals("http://medico.com/record/patient/BartSimpson", secondRequestAttributes.getAttributes()
                .iterator().next().getValues().iterator().next().getValue().toString());

        RequestAttributes thirdRequestAttributes = iter.next();
        assertEquals(1, thirdRequestAttributes.getAttributes().size());
        assertEquals("read", thirdRequestAttributes.getAttributes().iterator().next()
                .getValues().iterator().next().getValue().toString());
    }

    @Test
    public void testIsRequestType() {
        assertThat(hdlr.isRequestType(Request.class)).isTrue();
        assertThat(hdlr.isRequestType(StdMutableRequest.class)).isTrue();
        assertThat(hdlr.isRequestType(StdRequest.class)).isTrue();
        assertThat(hdlr.isRequestType(Response.class)).isFalse();
    }

    @Test
    public void testIsResponseType() {
        assertThat(hdlr.isResponseType(Response.class)).isTrue();
        assertThat(hdlr.isResponseType(StdMutableResponse.class)).isTrue();
        assertThat(hdlr.isResponseType(StdResponse.class)).isTrue();
        assertThat(hdlr.isResponseType(Request.class)).isFalse();
    }
}
