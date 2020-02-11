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
 * ============LICENSE_END=========================================================
 */

package org.onap.policy.pdpx.main.rest.serialization;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import javax.ws.rs.core.Response;
import org.junit.Before;
import org.junit.Test;
import org.onap.policy.common.utils.coder.CoderException;
import org.onap.policy.common.utils.coder.StandardCoder;

public class TestXacmlJsonExceptionMapper {
    private XacmlJsonExceptionMapper mapper;

    @Before
    public void setUp() {
        mapper = new XacmlJsonExceptionMapper();
    }

    @Test
    public void testToResponse() throws CoderException {
        IOException writeToEx = new IOException("failed to convert a json response to a string");
        IOException readFromEx = new IOException("failed to decode incoming request string to a json request");
        IOException unexpectedEx = new IOException("unexpected exception");
        Response writeToResp = mapper.toResponse(writeToEx);
        Response readFromResp = mapper.toResponse(readFromEx);
        Response unexpectedResp = mapper.toResponse(unexpectedEx);

        assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), writeToResp.getStatus());
        assertEquals("{'errorDetails':'invalid JSON xacml response'}".replace('\'', '"'),
                        new StandardCoder().encode(writeToResp.getEntity()));
        assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), readFromResp.getStatus());
        assertEquals("{'errorDetails':'invalid JSON xacml request'}".replace('\'', '"'),
                        new StandardCoder().encode(readFromResp.getEntity()));
        assertEquals(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), unexpectedResp.getStatus());
    }
}