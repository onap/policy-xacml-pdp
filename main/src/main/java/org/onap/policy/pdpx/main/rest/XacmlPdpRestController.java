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

package org.onap.policy.pdpx.main.rest;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import io.swagger.annotations.Authorization;
import io.swagger.annotations.BasicAuthDefinition;
import io.swagger.annotations.Extension;
import io.swagger.annotations.ExtensionProperty;
import io.swagger.annotations.Info;
import io.swagger.annotations.ResponseHeader;
import io.swagger.annotations.SecurityDefinition;
import io.swagger.annotations.SwaggerDefinition;
import java.util.UUID;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;
import org.onap.policy.common.endpoints.report.HealthCheckReport;
import org.onap.policy.pdpx.main.rest.model.Decision;
import org.onap.policy.pdpx.main.rest.model.StatisticsReport;
import org.onap.policy.pdpx.main.rest.provider.DecisionProvider;
import org.onap.policy.pdpx.main.rest.provider.HealthCheckProvider;
import org.onap.policy.pdpx.main.rest.provider.StatisticsProvider;

/**
 * Class to provide xacml pdp REST services.
 *
 */
@Path("/policy/pdpx/v1")
@Api
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@SwaggerDefinition(info = @Info(description = "Policy Xacml PDP Service", version = "1.0.0", title = "Policy Xacml PDP",
        extensions = {@Extension(properties = {@ExtensionProperty(name = "planned-retirement-date", value = "tbd"),
                @ExtensionProperty(name = "component", value = "Policy Framework")})}),
        schemes = {SwaggerDefinition.Scheme.HTTP, SwaggerDefinition.Scheme.HTTPS},
        securityDefinition = @SecurityDefinition(basicAuthDefinitions = {@BasicAuthDefinition(key = "basicAuth")}))
public class XacmlPdpRestController {

    @GET
    @Path("/healthcheck")
    @ApiOperation(value = "Perform a system healthcheck",
            notes = "Provides healthy status of the Policy Xacml PDP component", response = HealthCheckReport.class,
            responseHeaders = {
                    @ResponseHeader(name = "X-MinorVersion",
                            description = "Used to request or communicate a MINOR version back from the client"
                                    + " to the server, and from the server back to the client",
                            response = String.class),
                    @ResponseHeader(name = "X-PatchVersion",
                            description = "Used only to communicate a PATCH version in a response for"
                                    + " troubleshooting purposes only, and will not be provided by"
                                    + " the client on request",
                            response = String.class),
                    @ResponseHeader(name = "X-LatestVersion",
                            description = "Used only to communicate an API's latest version", response = String.class),
                    @ResponseHeader(name = "X-ONAP-RequestID",
                            description = "Used to track REST transactions for logging purpose",
                            response = UUID.class)},
            authorizations = @Authorization(value = "basicAuth"), tags = {"HealthCheck",},
            extensions = {@Extension(name = "interface info",
                    properties = {@ExtensionProperty(name = "pdpx-version", value = "1.0.0"),
                            @ExtensionProperty(name = "last-mod-release", value = "Dublin")})})
    @ApiResponses(value = {@ApiResponse(code = 401, message = "Authentication Error"),
            @ApiResponse(code = 403, message = "Authorization Error"),
            @ApiResponse(code = 500, message = "Internal Server Error")})
    public Response healthcheck(
            @HeaderParam("X-ONAP-RequestID") @ApiParam("RequestID for http transaction") UUID requestId) {
        return addLoggingHeaders(addVersionControlHeaders(Response.status(Response.Status.OK)), requestId)
                .entity(new HealthCheckProvider().performHealthCheck()).build();
    }

    @GET
    @Path("/statistics")
    @ApiOperation(value = "Fetch current statistics",
            notes = "Provides current statistics of the Policy Xacml PDP component", response = StatisticsReport.class,
            responseHeaders = {
                    @ResponseHeader(name = "X-MinorVersion",
                            description = "Used to request or communicate a MINOR version back from the client"
                                    + " to the server, and from the server back to the client",
                            response = String.class),
                    @ResponseHeader(name = "X-PatchVersion",
                            description = "Used only to communicate a PATCH version in a response for"
                                    + " troubleshooting purposes only, and will not be provided by"
                                    + " the client on request",
                            response = String.class),
                    @ResponseHeader(name = "X-LatestVersion",
                            description = "Used only to communicate an API's latest version", response = String.class),
                    @ResponseHeader(name = "X-ONAP-RequestID",
                            description = "Used to track REST transactions for logging purpose",
                            response = UUID.class)},
            authorizations = @Authorization(value = "basicAuth"), tags = {"Statistics",},
            extensions = {@Extension(name = "interface info",
                    properties = {@ExtensionProperty(name = "pdpx-version", value = "1.0.0"),
                            @ExtensionProperty(name = "last-mod-release", value = "Dublin")})})
    @ApiResponses(value = {@ApiResponse(code = 401, message = "Authentication Error"),
            @ApiResponse(code = 403, message = "Authorization Error"),
            @ApiResponse(code = 500, message = "Internal Server Error")})
    public Response statistics(
            @HeaderParam("X-ONAP-RequestID") @ApiParam("RequestID for http transaction") UUID requestId) {
        return addLoggingHeaders(addVersionControlHeaders(Response.status(Response.Status.OK)), requestId)
                .entity(new StatisticsProvider().fetchCurrentStatistics()).build();
    }

    @POST
    @Path("/decision")
    @ApiOperation(value = "Fetch the decision using specified decision parameters",
            notes = "Returns the policy decision from Policy Xacml PDP", response = Decision.class,
            responseHeaders = {
                    @ResponseHeader(name = "X-MinorVersion",
                            description = "Used to request or communicate a MINOR version back from the client"
                                    + " to the server, and from the server back to the client",
                            response = String.class),
                    @ResponseHeader(name = "X-PatchVersion",
                            description = "Used only to communicate a PATCH version in a response for"
                                    + " troubleshooting purposes only, and will not be provided by"
                                    + " the client on request",
                            response = String.class),
                    @ResponseHeader(name = "X-LatestVersion",
                            description = "Used only to communicate an API's latest version", response = String.class),
                    @ResponseHeader(name = "X-ONAP-RequestID",
                            description = "Used to track REST transactions for logging purpose",
                            response = UUID.class)},
            authorizations = @Authorization(value = "basicAuth"), tags = {"Decision",},
            extensions = {@Extension(name = "interface info",
                    properties = {@ExtensionProperty(name = "pdpx-version", value = "1.0.0"),
                            @ExtensionProperty(name = "last-mod-release", value = "Dublin")})})
    @ApiResponses(value = {@ApiResponse(code = 400, message = "Bad Request"),
            @ApiResponse(code = 401, message = "Authentication Error"),
            @ApiResponse(code = 403, message = "Authorization Error"),
            @ApiResponse(code = 500, message = "Internal Server Error")})
    public Response decision(Decision body,
            @HeaderParam("X-ONAP-RequestID") @ApiParam("RequestID for http transaction") UUID requestId) {
        return addLoggingHeaders(addVersionControlHeaders(Response.status(Response.Status.OK)), requestId)
                .entity(new DecisionProvider().fetchDecision()).build();
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
