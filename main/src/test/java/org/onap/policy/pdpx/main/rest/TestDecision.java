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

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;

import java.io.IOException;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;

import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.client.authentication.HttpAuthenticationFeature;
import org.junit.BeforeClass;
import org.junit.Test;
import org.onap.policy.common.utils.network.NetworkUtil;
import org.onap.policy.models.decisions.concepts.DecisionRequest;
import org.onap.policy.models.decisions.concepts.DecisionResponse;
import org.onap.policy.models.errors.concepts.ErrorResponse;
import org.onap.policy.pdpx.main.PolicyXacmlPdpException;
import org.onap.policy.pdpx.main.startstop.Main;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TestDecision {

    private static final Logger LOGGER = LoggerFactory.getLogger(TestDecision.class);

    @BeforeClass
    public static void beforeClass() {
        System.setProperty("org.eclipse.jetty.util.log.class", "org.eclipse.jetty.util.log.StdErrLog");
        System.setProperty("org.eclipse.jetty.LEVEL", "OFF");
    }

    @Test
    public void testDecision_UnsupportedAction() throws PolicyXacmlPdpException, InterruptedException, IOException {
        LOGGER.info("Running test testDecision_UnsupportedAction");
        final Main main = startXacmlPdpService();

        DecisionRequest request = new DecisionRequest();
        request.setOnapName("TEST");
        request.setAction("foo");

        ErrorResponse response = null;
        try {
            response = getErrorDecision(request);
        } catch (Exception e) {
            LOGGER.error("{}", e.getClass());
            fail(e.getMessage());
        }
        LOGGER.info("Response {}", response);
        assertThat(response.getErrorMessage()).isEqualToIgnoringCase("error");
        stopXacmlPdpService(main);
    }

    private Main startXacmlPdpService() {
        final String[] XacmlPdpConfigParameters = {"-c", "parameters/XacmlPdpConfigParameters.json"};
        return new Main(XacmlPdpConfigParameters);
    }

    private void stopXacmlPdpService(final Main main) throws PolicyXacmlPdpException {
        main.shutdown();
    }

    private DecisionResponse getDecision(DecisionRequest request) throws InterruptedException, IOException {
        final ClientConfig clientConfig = new ClientConfig();

        final HttpAuthenticationFeature feature = HttpAuthenticationFeature.basic("healthcheck", "zb!XztG34");
        clientConfig.register(feature);

        final Client client = ClientBuilder.newClient(clientConfig);
        final WebTarget webTarget = client.target("http://localhost:6969/policy/pdpx/v1/decision");

        final Invocation.Builder invocationBuilder = webTarget.request(MediaType.APPLICATION_JSON);

        if (!NetworkUtil.isTcpPortOpen("localhost", 6969, 6, 10000L)) {
            throw new IllegalStateException("Cannot connect to port 6969");
        }

        return invocationBuilder.post(Entity.json(request), DecisionResponse.class);
    }

    private ErrorResponse getErrorDecision(DecisionRequest request) throws InterruptedException, IOException {
        final ClientConfig clientConfig = new ClientConfig();

        final HttpAuthenticationFeature feature = HttpAuthenticationFeature.basic("healthcheck", "zb!XztG34");
        clientConfig.register(feature);

        final Client client = ClientBuilder.newClient(clientConfig);
        final WebTarget webTarget = client.target("http://localhost:6969/policy/pdpx/v1/decision");

        final Invocation.Builder invocationBuilder = webTarget.request(MediaType.APPLICATION_JSON);

        if (!NetworkUtil.isTcpPortOpen("localhost", 6969, 6, 10000L)) {
            throw new IllegalStateException("Cannot connect to port 6969");
        }

        return invocationBuilder.post(Entity.json(request), ErrorResponse.class);
    }


}