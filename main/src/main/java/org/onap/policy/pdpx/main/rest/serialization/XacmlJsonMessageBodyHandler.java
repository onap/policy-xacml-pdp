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

import com.att.research.xacml.api.Request;
import com.att.research.xacml.api.Response;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import javax.ws.rs.Consumes;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.MessageBodyReader;
import javax.ws.rs.ext.MessageBodyWriter;
import javax.ws.rs.ext.Provider;

/**
 * Provider that serializes and de-serializes xacml request/response json.
 *
 * @author Chenfei Gao (cgao@research.att.com)
 */
@Provider
@Consumes(XacmlJsonMessageBodyHandler.APPLICATION_XACML_JSON)
@Produces(XacmlJsonMessageBodyHandler.APPLICATION_XACML_JSON)
public class XacmlJsonMessageBodyHandler implements MessageBodyReader<Request>, MessageBodyWriter<Response> {

    public static final String APPLICATION_XACML_JSON = "application/xacml+json";

    @Override
    public boolean isWriteable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
        return canHandle(mediaType, type);
    }

    @Override
    public void writeTo(Response response, Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType,
            MultivaluedMap<String, Object> httpHeaders, OutputStream entityStream) throws IOException {

        try (OutputStreamWriter writer = new OutputStreamWriter(entityStream, StandardCharsets.UTF_8)) {
            writer.write(JsonResponseTranslator.toString(response, true));
        } catch (Exception exc) {
            throw new IOException("failed to convert a json response to a string");
        }
    }

    @Override
    public boolean isReadable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
        return canHandle(mediaType, type);
    }

    @Override
    public Request readFrom(Class<Request> type, Type genericType, Annotation[] annotations, MediaType mediaType,
            MultivaluedMap<String, String> httpHeaders, InputStream entityStream) throws IOException {

        Request jsonRequest;
        try {
            jsonRequest = JsonResponseTranslator.load(entityStream);
        } catch (Exception exc) {
            throw new IOException("failed to decode incoming request string to a json request");
        }
        return jsonRequest;
    }

    /**
     * Determines if this provider can handle the given media type.
     * @param mediaType the media type of interest
     * @param type the class type of the object to read/write
     * @return {@code true} if this provider handles the given media type and class type
     *         {@code false} otherwise
     */
    private boolean canHandle(MediaType mediaType, Class<?> type) {
        return ("xacml+json".equals(mediaType.getSubtype()))
                && (type == Request.class || type == Response.class);
    }
}
