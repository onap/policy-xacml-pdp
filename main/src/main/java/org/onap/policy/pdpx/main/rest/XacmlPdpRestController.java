/*-
 * ============LICENSE_START=======================================================
 * Copyright (C) 2018 Ericsson. All rights reserved.
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
import io.swagger.annotations.Info;
import io.swagger.annotations.SwaggerDefinition;
import io.swagger.annotations.Tag;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import org.onap.policy.common.endpoints.report.HealthCheckReport;



/**
 * Class to provide xacml pdp REST services.
 *
 */
@Path("/")
@Api
@Produces(MediaType.APPLICATION_JSON)
@SwaggerDefinition(info = @Info(description = "Policy Xacml PDP Service", version = "v1.0", title = "Policy Xacml PDP"),
        consumes = {MediaType.APPLICATION_JSON}, produces = {MediaType.APPLICATION_JSON},
        schemes = {SwaggerDefinition.Scheme.HTTP},
        tags = {@Tag(name = "policy-pdpx", description = "Policy Xacml PDP Service Operations")})
public class XacmlPdpRestController {

    @GET
    @Path("healthcheck")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Perform a system healthcheck",
            notes = "Provides healthy status of the Policy Xacml PDP component",
            response = HealthCheckReport.class)
    public Response healthcheck() {
        return Response.status(Response.Status.OK).entity(new HealthCheckProvider().performHealthCheck()).build();
    }

    @GET
    @Path("statistics")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Fetch current statistics",
            notes = "Provides current statistics of the Policy Xacml PDP component",
            response = StatisticsReport.class)
    public Response statistics() {
        return Response.status(Response.Status.OK).entity(new StatisticsProvider().fetchCurrentStatistics()).build();
    }
}
