/*-
 * ============LICENSE_START=======================================================
 * Copyright (C) 2020 AT&T Intellectual Property. All rights reserved.
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

import jakarta.ws.rs.Produces;
import jakarta.ws.rs.ext.Provider;

/**
 * Catches IOException when decoding/encoding a REST xacml request/response and converts them from an HTTP 500
 * error code to an HTTP 400 error code.
 *
 * @author Chenfei Gao (cgao@research.att.com)
 */
@Provider
@Produces(XacmlJsonMessageBodyHandler.APPLICATION_XACML_JSON)
public class XacmlJsonExceptionMapper  extends XacmlExceptionMapper {

    public XacmlJsonExceptionMapper() {
        this.invalidRequest = "invalid JSON xacml request";
        this.invalidResponse = "invalid JSON xacml response";
    }

    @Override
    public boolean isInvalidRequest(String message) {
        return message.contains("json request");
    }

    @Override
    public boolean isInvalidResponse(String message) {
        return message.contains("json response");
    }
}