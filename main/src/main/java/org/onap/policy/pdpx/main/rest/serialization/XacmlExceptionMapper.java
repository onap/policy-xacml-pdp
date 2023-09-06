/*-
 * ============LICENSE_START=======================================================
 * Copyright (C) 2020-2021 AT&T Intellectual Property. All rights reserved.
 * Modifications Copyright (C) 2023 Nordix Foundation.
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

import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import java.io.IOException;
import lombok.Getter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Catches IOException when decoding/encoding a REST xacml request/response and converts them from an HTTP 500
 * error code to an HTTP 400 error code.
 *
 */
public abstract class XacmlExceptionMapper implements ExceptionMapper<IOException> {
    private static final Logger LOGGER = LoggerFactory.getLogger(XacmlExceptionMapper.class);

    @Getter
    protected String invalidRequest;
    @Getter
    protected String invalidResponse;

    public abstract boolean isInvalidRequest(String message);

    public abstract boolean isInvalidResponse(String message);

    @Override
    public Response toResponse(IOException exc) {
        if (isInvalidRequest(exc.getMessage())) {
            LOGGER.warn(invalidRequest, exc);
            return Response.status(Response.Status.BAD_REQUEST).entity(new SimpleResponse(invalidRequest)).build();
        } else if (isInvalidResponse(exc.getMessage())) {
            LOGGER.warn(invalidResponse, exc);
            return Response.status(Response.Status.BAD_REQUEST).entity(new SimpleResponse(invalidResponse)).build();
        } else {
            // Unexpected 500
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        }
    }

    @Getter
    private static class SimpleResponse {
        private String errorDetails;

        public SimpleResponse(String errorDetails) {
            this.errorDetails = errorDetails;
        }
    }
}
