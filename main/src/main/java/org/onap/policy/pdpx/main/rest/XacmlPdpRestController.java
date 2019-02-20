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
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import io.swagger.annotations.Authorization;
import io.swagger.annotations.BasicAuthDefinition;
import io.swagger.annotations.Info;
import io.swagger.annotations.SecurityDefinition;
import io.swagger.annotations.SwaggerDefinition;
import io.swagger.annotations.Tag;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
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
@SwaggerDefinition(info = @Info(description = "Policy Xacml PDP Service", version = "v1.0", title = "Policy Xacml PDP"),
        consumes = {MediaType.APPLICATION_JSON}, produces = {MediaType.APPLICATION_JSON},
        schemes = {SwaggerDefinition.Scheme.HTTP, SwaggerDefinition.Scheme.HTTPS},
        tags = {@Tag(name = "policy-pdpx", description = "Policy Xacml PDP Service Operations")},
        securityDefinition = @SecurityDefinition(basicAuthDefinitions = {@BasicAuthDefinition(key = "basicAuth")}))
public class XacmlPdpRestController {

    @GET
    @Path("/healthcheck")
    @ApiOperation(value = "Perform a system healthcheck",
            notes = "Provides healthy status of the Policy Xacml PDP component", response = HealthCheckReport.class,
            authorizations = @Authorization(value = "basicAuth"))
    @ApiResponses(value = {@ApiResponse(code = 400, message = "Bad Request"),
            @ApiResponse(code = 401, message = "Unauthorized"),
            @ApiResponse(code = 500, message = "Internal Server Error")})
    public Response healthcheck() {
        return Response.status(Response.Status.OK).entity(new HealthCheckProvider().performHealthCheck()).build();
    }

    @GET
    @Path("/statistics")
    @ApiOperation(value = "Fetch current statistics",
            notes = "Provides current statistics of the Policy Xacml PDP component", response = StatisticsReport.class,
            authorizations = @Authorization(value = "basicAuth"))
    @ApiResponses(value = {@ApiResponse(code = 400, message = "Bad Request"),
            @ApiResponse(code = 401, message = "Unauthorized"),
            @ApiResponse(code = 500, message = "Internal Server Error")})
    public Response statistics() {
        return Response.status(Response.Status.OK).entity(new StatisticsProvider().fetchCurrentStatistics()).build();
    }

    @POST
    @Path("/decision")
    @ApiOperation(value = "Fetch the decision using specified decision parameters",
            notes = "Returns the policy decision from Policy Xacml PDP", response = Decision.class,
            authorizations = @Authorization(value = "basicAuth"),
            tags = {"Decision",})
    @ApiResponses(value = {@ApiResponse(code = 400, message = "Bad Request"),
            @ApiResponse(code = 401, message = "Unauthorized"),
            @ApiResponse(code = 500, message = "Internal Server Error")})
    public Response decision(Decision body) {
        return Response.status(Response.Status.OK).entity(new DecisionProvider().fetchDecision()).build();
    }
}
