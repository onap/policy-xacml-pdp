/*-
 * ============LICENSE_START=======================================================
 * Copyright (C) 2019, 2021 AT&T Intellectual Property. All rights reserved.
 * Modifications Copyright (C) 2021, 2023-2024 Nordix Foundation.
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
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Response;
import java.io.IOException;
import java.util.Properties;
import java.util.UUID;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.onap.policy.common.endpoints.http.client.HttpClient;
import org.onap.policy.common.endpoints.http.client.HttpClientFactoryInstance;
import org.onap.policy.common.endpoints.http.server.HttpServletServer;
import org.onap.policy.common.endpoints.http.server.HttpServletServerFactoryInstance;
import org.onap.policy.common.endpoints.parameters.RestClientParameters;
import org.onap.policy.common.endpoints.properties.PolicyEndPointProperties;
import org.onap.policy.common.gson.GsonMessageBodyHandler;
import org.onap.policy.common.utils.network.NetworkUtil;
import org.onap.policy.models.tosca.authorative.concepts.ToscaConceptIdentifier;
import org.onap.policy.models.tosca.authorative.concepts.ToscaPolicyType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class PolicyApiCallerTest {
    private static final String MY_TYPE = "my-type";

    private static final String MY_VERSION = "1.0.0";

    private static final Logger logger = LoggerFactory.getLogger(PolicyApiCallerTest.class);

    private static final String CLIENT_NAME = "policy-api";
    private static final String NOT_A_TYPE = "other-type";
    private static final String INVALID_TYPE = "invalid";
    private static final String UNKNOWN_TYPE = "unknown";

    private static int port;
    private static RestClientParameters clientParams;

    private static HttpClient apiClient;

    private PolicyApiCaller api;

    /**
     * Initializes {@link #clientParams} and starts a simple REST server to handle the
     * test requests.
     *
     * @throws IOException if an error occurs
     */
    @BeforeAll
    static void setUpBeforeClass() throws Exception {
        port = NetworkUtil.allocPort();

        clientParams = mock(RestClientParameters.class);
        when(clientParams.getClientName()).thenReturn("apiClient");
        when(clientParams.getHostname()).thenReturn("localhost");
        when(clientParams.getPort()).thenReturn(port);

        Properties props = new Properties();
        props.setProperty(PolicyEndPointProperties.PROPERTY_HTTP_SERVER_SERVICES, CLIENT_NAME);

        final String svcpfx =
            PolicyEndPointProperties.PROPERTY_HTTP_SERVER_SERVICES + "." + CLIENT_NAME;

        props.setProperty(svcpfx + PolicyEndPointProperties.PROPERTY_HTTP_HOST_SUFFIX, clientParams.getHostname());
        props.setProperty(svcpfx + PolicyEndPointProperties.PROPERTY_HTTP_PORT_SUFFIX,
            Integer.toString(clientParams.getPort()));
        props.setProperty(svcpfx + PolicyEndPointProperties.PROPERTY_HTTP_REST_CLASSES_SUFFIX,
            ApiRestController.class.getName());
        props.setProperty(svcpfx + PolicyEndPointProperties.PROPERTY_MANAGED_SUFFIX, "true");
        props.setProperty(svcpfx + PolicyEndPointProperties.PROPERTY_HTTP_HTTPS_SUFFIX, "false");
        props.setProperty(svcpfx + PolicyEndPointProperties.PROPERTY_HTTP_SERIALIZATION_PROVIDER,
            GsonMessageBodyHandler.class.getName());

        HttpServletServerFactoryInstance.getServerFactory().build(props).forEach(HttpServletServer::start);
        apiClient = HttpClientFactoryInstance.getClientFactory().build(clientParams);

        assertTrue(NetworkUtil.isTcpPortOpen(clientParams.getHostname(), clientParams.getPort(), 100, 100));
    }

    @AfterAll
    static void tearDownAfterClass() {
        HttpServletServerFactoryInstance.getServerFactory().destroy();
    }

    /**
     * Resets {@link #clientParams} and populates {@link #api}.
     *
     * @throws PolicyApiException if an error occurs
     */
    @BeforeEach
    void setUp() throws PolicyApiException {
        when(clientParams.getPort()).thenReturn(port);

        api = new PolicyApiCaller(apiClient);
    }

    @Test
    void testGetPolicyType() throws Exception {

        assertNotNull(api.getPolicyType(new ToscaConceptIdentifier(MY_TYPE, MY_VERSION)));

        assertThatThrownBy(() -> api.getPolicyType(new ToscaConceptIdentifier(INVALID_TYPE, MY_VERSION)))
            .isInstanceOf(PolicyApiException.class);

        assertThatThrownBy(() -> api.getPolicyType(new ToscaConceptIdentifier(UNKNOWN_TYPE, MY_VERSION)))
            .isInstanceOf(NotFoundException.class);

        assertThatThrownBy(() -> api.getPolicyType(new ToscaConceptIdentifier(NOT_A_TYPE, MY_VERSION)))
            .isInstanceOf(PolicyApiException.class);

        // connect to a port that has no server
        RestClientParameters params2 = mock(RestClientParameters.class);
        when(params2.getClientName()).thenReturn("apiClient");
        when(params2.getHostname()).thenReturn("localhost");
        when(params2.getPort()).thenReturn(NetworkUtil.allocPort());

        HttpClient apiClient2 = HttpClientFactoryInstance.getClientFactory().build(params2);
        api = new PolicyApiCaller(apiClient2);

        assertThatThrownBy(() -> api.getPolicyType(new ToscaConceptIdentifier(MY_TYPE, MY_VERSION)))
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
         * @param versionId    version of desired policy type
         * @param requestId    optional request ID
         * @return the Response object containing the results of the API operation
         */
        @GET
        @Path("/policytypes/{policyTypeId}/versions/{versionId}")
        public Response getSpecificVersionOfPolicyType(@PathParam("policyTypeId") String policyTypeId,
                                                       @PathParam("versionId") String versionId,
                                                       @HeaderParam("X-ONAP-RequestID") UUID requestId) {

            assertEquals(MY_VERSION, versionId);

            return switch (policyTypeId) {
                case UNKNOWN_TYPE -> {
                    logger.info("request for unknown policy type");
                    yield Response.status(Response.Status.NOT_FOUND).build();
                }
                case INVALID_TYPE -> {
                    logger.info("invalid request for policy type");
                    yield Response.status(Response.Status.BAD_REQUEST).build();
                }
                case NOT_A_TYPE -> {
                    logger.info("invalid request for policy type");
                    yield Response.status(Response.Status.OK).entity("string-type").build();
                }
                default -> {
                    logger.info("request for policy type={} version={}", policyTypeId, versionId);
                    yield Response.status(Response.Status.OK).entity(new ToscaPolicyType()).build();
                }
            };
        }
    }
}
