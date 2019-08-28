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

package org.onap.policy.pdp.xacml.application.common;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.IOException;
import java.util.Properties;
import java.util.UUID;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.onap.policy.common.endpoints.event.comm.bus.internal.BusTopicParams;
import org.onap.policy.common.endpoints.http.server.HttpServletServer;
import org.onap.policy.common.endpoints.http.server.HttpServletServerFactoryInstance;
import org.onap.policy.common.endpoints.properties.PolicyEndPointProperties;
import org.onap.policy.common.gson.GsonMessageBodyHandler;
import org.onap.policy.common.utils.network.NetworkUtil;
import org.onap.policy.models.tosca.authorative.concepts.ToscaPolicyType;
import org.onap.policy.models.tosca.authorative.concepts.ToscaPolicyTypeIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PolicyApiTest {
    private static final String MY_TYPE = "my-type";

    private static final String MY_VERSION = "1.0.0";

    private static final Logger logger = LoggerFactory.getLogger(PolicyApiTest.class);

    private static final String NOT_A_TYPE = "other-type";
    private static final String INVALID_TYPE = "invalid";
    private static final String UNKNOWN_TYPE = "unknown";

    private static int port;
    private static BusTopicParams clientParams;

    private PolicyApi api;

    /**
     * Initializes {@link #clientParams} and starts a simple REST server to handle the
     * test requests.
     *
     * @throws IOException if an error occurs
     */
    @BeforeClass
    public static void setUpBeforeClass() throws IOException {
        port = NetworkUtil.allocPort();

        clientParams = new BusTopicParams();
        clientParams.setHostname("localhost");
        clientParams.setPort(port);
        clientParams.setClientName("policy-api");
        clientParams.setUseHttps(false);
        clientParams.setSerializationProvider(GsonMessageBodyHandler.class.getName());

        Properties props = new Properties();
        props.setProperty(PolicyEndPointProperties.PROPERTY_HTTP_SERVER_SERVICES, "policy-api");

        final String svcpfx = PolicyEndPointProperties.PROPERTY_HTTP_SERVER_SERVICES + ".policy-api";

        props.setProperty(svcpfx + PolicyEndPointProperties.PROPERTY_HTTP_HOST_SUFFIX, clientParams.getHostname());
        props.setProperty(svcpfx + PolicyEndPointProperties.PROPERTY_HTTP_PORT_SUFFIX,
                        Integer.toString(clientParams.getPort()));
        props.setProperty(svcpfx + PolicyEndPointProperties.PROPERTY_HTTP_REST_CLASSES_SUFFIX,
                        ApiRestController.class.getName());
        props.setProperty(svcpfx + PolicyEndPointProperties.PROPERTY_MANAGED_SUFFIX, "true");
        props.setProperty(svcpfx + PolicyEndPointProperties.PROPERTY_HTTP_HTTPS_SUFFIX, "false");
        props.setProperty(svcpfx + PolicyEndPointProperties.PROPERTY_HTTP_SERIALIZATION_PROVIDER,
                        clientParams.getSerializationProvider());

        HttpServletServerFactoryInstance.getServerFactory().build(props).forEach(HttpServletServer::start);
    }

    @AfterClass
    public static void tearDownAfterClass() {
        HttpServletServerFactoryInstance.getServerFactory().destroy();
    }

    /**
     * Resets {@link #clientParams} and populates {@link #api}.
     *
     * @throws PolicyApiException if an error occurs
     */
    @Before
    public void setUp() throws PolicyApiException {
        clientParams.setPort(port);
        clientParams.setSerializationProvider(GsonMessageBodyHandler.class.getName());

        api = new PolicyApi(clientParams);
    }

    @Test
    public void testPolicyApi() {
        clientParams.setSerializationProvider("unknown-class");
        assertThatThrownBy(() -> new PolicyApi(clientParams)).isInstanceOf(PolicyApiException.class);
    }

    @Test
    public void testGetPolicyType() throws Exception {

        assertNotNull(api.getPolicyType(new ToscaPolicyTypeIdentifier(MY_TYPE, MY_VERSION)));

        assertThatThrownBy(() -> api.getPolicyType(new ToscaPolicyTypeIdentifier(INVALID_TYPE, MY_VERSION)))
                        .isInstanceOf(PolicyApiException.class);

        assertThatThrownBy(() -> api.getPolicyType(new ToscaPolicyTypeIdentifier(UNKNOWN_TYPE, MY_VERSION)))
                        .isInstanceOf(NotFoundException.class);

        assertThatThrownBy(() -> api.getPolicyType(new ToscaPolicyTypeIdentifier(NOT_A_TYPE, MY_VERSION)))
                        .isInstanceOf(PolicyApiException.class);

        // connect to a port that has no server
        clientParams.setPort(NetworkUtil.allocPort());
        api = new PolicyApi(clientParams);

        assertThatThrownBy(() -> api.getPolicyType(new ToscaPolicyTypeIdentifier(MY_TYPE, MY_VERSION)))
            .isInstanceOf(PolicyApiException.class);
    }

    /**
     * Simple REST server to handle test requests.
     */

    @Path("/policy/api/v1")
    @Produces({"application/json", "application/yaml"})
    @Consumes({"application/json", "application/yaml"})
    public static class ApiRestController {

        /**
         * Retrieves the specified version of a particular policy type.
         *
         * @param policyTypeId ID of desired policy type
         * @param versionId version of desired policy type
         * @param requestId optional request ID
         *
         * @return the Response object containing the results of the API operation
         */
        @GET
        @Path("/policytypes/{policyTypeId}/versions/{versionId}")
        public Response getSpecificVersionOfPolicyType(@PathParam("policyTypeId") String policyTypeId,
                        @PathParam("versionId") String versionId, @HeaderParam("X-ONAP-RequestID") UUID requestId) {

            assertEquals(MY_VERSION, versionId);

            switch (policyTypeId) {
                case UNKNOWN_TYPE:
                    logger.info("request for unknown policy type");
                    return Response.status(Response.Status.NOT_FOUND).build();
                case INVALID_TYPE:
                    logger.info("invalid request for policy type");
                    return Response.status(Response.Status.BAD_REQUEST).build();
                case NOT_A_TYPE:
                    logger.info("invalid request for policy type");
                    return Response.status(Response.Status.OK).entity("string-type").build();
                default:
                    logger.info("request for policy type={} version={}", policyTypeId, versionId);
                    return Response.status(Response.Status.OK).entity(new ToscaPolicyType()).build();
            }
        }
    }
}
