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

import com.att.research.xacml.std.json.JSONStructureException;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;
import lombok.Getter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Catches JSONStructureException when decoding a REST xacml request and converts them from an HTTP 500
 * error code to an HTTP 400 error code.
 *
 * @author Chenfei Gao (cgao@research.att.com)
 */
@Provider
@Produces(XacmlJsonMessageBodyHandler.APPLICATION_XACML_JSON)
public class XacmlJsonExceptionMapper implements ExceptionMapper<JSONStructureException> {

    private static final Logger LOGGER = LoggerFactory.getLogger(XacmlJsonExceptionMapper.class);
    private static final String INVALID_REQUEST = "invalid JSON xacml request";

    @Override
    public Response toResponse(JSONStructureException exception) {
        LOGGER.warn(INVALID_REQUEST, exception);
        return Response.status(Response.Status.BAD_REQUEST).entity(new SimpleResponse(INVALID_REQUEST)).build();
    }

    @Getter
    private static class SimpleResponse {
        private String errorDetails;

        public SimpleResponse(String errorDetails) {
            this.errorDetails = errorDetails;
        }
    }
}