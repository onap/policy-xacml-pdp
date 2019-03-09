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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.client.authentication.HttpAuthenticationFeature;
import org.junit.BeforeClass;
import org.junit.Test;
import org.onap.policy.common.endpoints.report.HealthCheckReport;
import org.onap.policy.common.utils.network.NetworkUtil;
import org.onap.policy.pdpx.main.PolicyXacmlPdpException;
import org.onap.policy.pdpx.main.parameters.CommonTestData;
import org.onap.policy.pdpx.main.parameters.RestServerParameters;
import org.onap.policy.pdpx.main.startstop.Main;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Class to perform unit test of HealthCheckMonitor.
 *
 */
public class TestXacmlPdpRestServer {

    private static final Logger LOGGER = LoggerFactory.getLogger(TestXacmlPdpRestServer.class);
    private static final String NOT_ALIVE = "not alive";
    private static final String ALIVE = "alive";
    private static final String SELF = "self";
    private static final String NAME = "Policy Xacml PDP";

    /**
     * setup.
     */
    @BeforeClass
    public static void setUp() {
        System.setProperty("org.eclipse.jetty.util.log.class", "org.eclipse.jetty.util.log.StdErrLog");
        System.setProperty("org.eclipse.jetty.LEVEL", "OFF");

    }

    @Test
    public void testHealthCheckSuccess() throws PolicyXacmlPdpException, InterruptedException {
        final String reportString = "Report [name=Policy Xacml PDP, url=self, healthy=true, code=200, message=alive]";
        try {
            final Main main = startXacmlPdpService();
            final HealthCheckReport report = performHealthCheck();
            validateReport(NAME, SELF, true, 200, ALIVE, reportString, report);
            stopXacmlPdpService(main);
        } catch (final Exception e) {
            LOGGER.error("testHealthCheckSuccess failed", e);
            fail("Test should not throw an exception");
        }

    }

    @Test
    public void testHealthCheckFailure() throws InterruptedException {
        final String reportString =
                "Report [name=Policy Xacml PDP, url=self, healthy=false, code=500, message=not alive]";
        final RestServerParameters restServerParams = new CommonTestData().getRestServerParameters(false);
        restServerParams.setName(CommonTestData.PDPX_GROUP_NAME);
        final XacmlPdpRestServer restServer = new XacmlPdpRestServer(restServerParams);

        try {
            restServer.start();
            final HealthCheckReport report = performHealthCheck();
            validateReport(NAME, SELF, false, 500, NOT_ALIVE, reportString, report);
            assertTrue(restServer.isAlive());
            assertTrue(restServer.toString().startsWith("XacmlPdpRestServer [servers="));
            restServer.shutdown();
        } catch (final Exception e) {
            LOGGER.error("testHealthCheckSuccess failed", e);
            fail("Test should not throw an exception");
        }
    }

    private Main startXacmlPdpService() {
        final String[] xacmlPdpConfigParameters = {"-c", "parameters/XacmlPdpConfigParameters.json"};
        return new Main(xacmlPdpConfigParameters);
    }

    private void stopXacmlPdpService(final Main main) throws PolicyXacmlPdpException {
        main.shutdown();
    }

    private HealthCheckReport performHealthCheck() throws InterruptedException, IOException {

        final ClientConfig clientConfig = new ClientConfig();

        final HttpAuthenticationFeature feature = HttpAuthenticationFeature.basic("healthcheck", "zb!XztG34");
        clientConfig.register(feature);

        final Client client = ClientBuilder.newClient(clientConfig);
        final WebTarget webTarget = client.target("http://localhost:6969/policy/pdpx/v1/healthcheck");

        final Invocation.Builder invocationBuilder = webTarget.request(MediaType.APPLICATION_JSON);

        if (!NetworkUtil.isTcpPortOpen("localhost", 6969, 6, 10000L)) {
            throw new IllegalStateException("Cannot connect to port 6969");
        }

        return invocationBuilder.get(HealthCheckReport.class);
    }

    private void validateReport(final String name, final String url, final boolean healthy, final int code,
            final String message, final String reportString, final HealthCheckReport report) {
        assertEquals(name, report.getName());
        assertEquals(url, report.getUrl());
        assertEquals(healthy, report.isHealthy());
        assertEquals(code, report.getCode());
        assertEquals(message, report.getMessage());
        assertEquals(reportString, report.toString());
    }
}
