/*-
 * ============LICENSE_START=======================================================
 * Copyright (C) 2019-2020 AT&T Intellectual Property. All rights reserved.
 * Modifications Copyright (C) 2021-2023 Nordix Foundation.
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

package org.onap.policy.pdpx.main.rest;

import com.att.research.xacml.api.Request;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.ResponseBuilder;
import java.util.UUID;
import org.onap.policy.models.decisions.concepts.DecisionException;
import org.onap.policy.models.decisions.concepts.DecisionRequest;
import org.onap.policy.pdpx.main.rest.provider.DecisionProvider;
import org.onap.policy.pdpx.main.rest.provider.HealthCheckProvider;
import org.onap.policy.pdpx.main.rest.provider.StatisticsProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Class to provide xacml pdp REST services.
 *
 */
@Path("/policy/pdpx/v1")
@Produces({MediaType.APPLICATION_JSON, XacmlPdpRestController.APPLICATION_YAML})
@Consumes({MediaType.APPLICATION_JSON, XacmlPdpRestController.APPLICATION_YAML})
public class XacmlPdpRestController implements HealthcheckApi, StatisticsApi, DecisionApi, XacmlApi {
    private static final Logger LOGGER = LoggerFactory.getLogger(XacmlPdpRestController.class);
    public static final String APPLICATION_YAML = "application/yaml";
    public static final String APPLICATION_XACML_JSON = "application/xacml+json";
    public static final String APPLICATION_XACML_XML = "application/xacml+xml";
    @Context private HttpServletRequest request;

    @GET
    @Path("/healthcheck")
    @Override
    public Response healthcheck(
            @HeaderParam("X-ONAP-RequestID") UUID requestId) {
        return addLoggingHeaders(addVersionControlHeaders(Response.status(Response.Status.OK)), requestId)
                .entity(new HealthCheckProvider().performHealthCheck()).build();
    }

    @GET
    @Path("/statistics")
    @Override
    public Response statistics(
            @HeaderParam("X-ONAP-RequestID") UUID requestId) {
        return addLoggingHeaders(addVersionControlHeaders(Response.status(Response.Status.OK)), requestId)
                .entity(new StatisticsProvider().fetchCurrentStatistics()).build();
    }

    @POST
    @Path("/decision")
    @Override
    public Response decision(DecisionRequest body, @HeaderParam("X-ONAP-RequestID") UUID requestId) {
        return decision(body, requestId, request);
    }

    /**
     * Our decision entry point.
     *
     * @param body Should be a DecisionRequest object
     * @param requestId Unique request id
     * @return DecisionResponse or ErrorResponse object
     */
    private Response decision(DecisionRequest body,
            @HeaderParam("X-ONAP-RequestID") UUID requestId,
            @Context HttpServletRequest request) {
        try {
            return addLoggingHeaders(addVersionControlHeaders(Response.status(Response.Status.OK)), requestId)
                    .entity(new DecisionProvider().fetchDecision(body, request.getParameterMap())).build();
        } catch (DecisionException e) {
            LOGGER.error("Decision exception", e);
            XacmlPdpStatisticsManager.getCurrent().updateErrorCount();
            return addLoggingHeaders(
                    addVersionControlHeaders(Response.status((e.getErrorResponse().getResponseCode()))), requestId)
                    .entity(e.getErrorResponse()).build();
        }
    }

    /**
     * Our native decision entry point.
     *
     * @param body Should be an Xacml Request object
     * @param requestId Unique request id
     * @return Xacml Response or ErrorResponse object
     */
    @POST
    @Path("/xacml")
    @Produces({XacmlPdpRestController.APPLICATION_XACML_JSON, XacmlPdpRestController.APPLICATION_XACML_XML})
    @Consumes({XacmlPdpRestController.APPLICATION_XACML_JSON, XacmlPdpRestController.APPLICATION_XACML_XML})
    @Override
    public Response xacml(Request body,
            @HeaderParam("X-ONAP-RequestID") UUID requestId) {
        try {
            return addLoggingHeaders(addVersionControlHeaders(Response.status(Response.Status.OK)), requestId)
                    .entity(new DecisionProvider().fetchNativeDecision(body)).build();
        } catch (DecisionException e) {
            LOGGER.error("Decision exception", e);
            XacmlPdpStatisticsManager.getCurrent().updateErrorCount();
            return addLoggingHeaders(
                    addVersionControlHeaders(Response.status((e.getErrorResponse().getResponseCode()))), requestId)
                    .entity(e.getErrorResponse()).build();
        }
    }

    private ResponseBuilder addVersionControlHeaders(ResponseBuilder rb) {
        return rb.header("X-MinorVersion", "0").header("X-PatchVersion", "0").header("X-LatestVersion", "1.0.0");
    }

    private ResponseBuilder addLoggingHeaders(ResponseBuilder rb, UUID requestId) {
        if (requestId == null) {
            // Generate a random uuid if client does not embed requestId in rest request
            return rb.header("X-ONAP-RequestID", UUID.randomUUID());
        }
        return rb.header("X-ONAP-RequestID", requestId);
    }


}