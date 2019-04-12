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

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.util.Properties;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.client.authentication.HttpAuthenticationFeature;
import org.junit.After;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runners.MethodSorters;
import org.onap.policy.common.endpoints.event.comm.client.TopicSinkClientException;
import org.onap.policy.common.endpoints.report.HealthCheckReport;
import org.onap.policy.common.utils.network.NetworkUtil;
import org.onap.policy.pdpx.main.PolicyXacmlPdpException;
import org.onap.policy.pdpx.main.parameters.CommonTestData;
import org.onap.policy.pdpx.main.parameters.RestServerParameters;
import org.onap.policy.pdpx.main.rest.model.StatisticsReport;
import org.onap.policy.pdpx.main.startstop.Main;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Class to perform unit test of {@link XacmlPdpRestServer}.
 *
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class TestXacmlPdpRestServer {

    private static final Logger LOGGER = LoggerFactory.getLogger(TestXacmlPdpRestServer.class);
    private static final String NOT_ALIVE = "not alive";
    private static final String ALIVE = "alive";
    private static final String SELF = "self";
    private static final String NAME = "Policy Xacml PDP";
    private static final String HEALTHCHECK_ENDPOINT = "healthcheck";
    private static final String STATISTICS_ENDPOINT = "statistics";
    private static String KEYSTORE = System.getProperty("user.dir") + "/src/test/resources/ssl/policy-keystore";
    private Main main;
    private XacmlPdpRestServer restServer;
    private static File applicationPath;

    @ClassRule
    public static final TemporaryFolder applicationFolder = new TemporaryFolder();

    /**
     * setup.
     *
     * @throws IOException exception if cannot create temporary folder
     */
    @BeforeClass
    public static void setUp() throws IOException {
        System.setProperty("org.eclipse.jetty.util.log.class", "org.eclipse.jetty.util.log.StdErrLog");
        System.setProperty("org.eclipse.jetty.LEVEL", "OFF");
        applicationPath = applicationFolder.newFolder();
    }

    /**
     * Method for cleanup after each test.
     */
    @After
    public void teardown() {
        try {
            if (NetworkUtil.isTcpPortOpen("localhost", 6969, 1, 1000L)) {
                if (main != null) {
                    stopXacmlPdpService(main);
                    main = null;
                }

                if (restServer != null) {
                    restServer.stop();
                    restServer = null;
                }
            }
        } catch (IOException | PolicyXacmlPdpException e) {
            LOGGER.error("teardown failed", e);
        } catch (InterruptedException ie) {
            Thread.interrupted();
            LOGGER.error("teardown failed", ie);
        }
    }

    @Test
    public void test1HealthCheckSuccess() throws IOException, InterruptedException, TopicSinkClientException {
        LOGGER.info("***************************** Running test1HealthCheckSuccess *****************************");
        main = startXacmlPdpService(true);
        final Invocation.Builder invocationBuilder = sendHttpRequest(HEALTHCHECK_ENDPOINT);
        final HealthCheckReport report = invocationBuilder.get(HealthCheckReport.class);
        LOGGER.info("test1HealthCheckSuccess health report {}", report);
        validateHealthCheckReport(NAME, SELF, true, 200, ALIVE, report);
    }

    @Test
    public void test7HealthCheckFailure() throws InterruptedException, IOException {
        LOGGER.info("***************************** Running test7HealthCheckFailure *****************************");
        final RestServerParameters restServerParams = new CommonTestData().getRestServerParameters(false);
        restServerParams.setName(CommonTestData.PDPX_GROUP_NAME);
        restServer = new XacmlPdpRestServer(restServerParams, applicationPath.getAbsolutePath());
        restServer.start();
        final Invocation.Builder invocationBuilder = sendHttpRequest(HEALTHCHECK_ENDPOINT);
        final HealthCheckReport report = invocationBuilder.get(HealthCheckReport.class);
        LOGGER.info("test7HealthCheckFailure health report {}", report);
        validateHealthCheckReport(NAME, SELF, false, 500, NOT_ALIVE, report);
        assertTrue(restServer.isAlive());
        assertTrue(restServer.toString().startsWith("XacmlPdpRestServer [servers="));
    }

    @Test
    public void test2HttpsHealthCheckSuccess() throws Exception {
        LOGGER.info("**************************** Running test2HttpsHealthCheckSuccess ****************************");
        main = startXacmlPdpService(false);
        final Invocation.Builder invocationBuilder = sendHttpsRequest(HEALTHCHECK_ENDPOINT);
        final HealthCheckReport report = invocationBuilder.get(HealthCheckReport.class);
        LOGGER.info("test2HttpsHealthCheckSuccess health report {}", report);
        validateHealthCheckReport(NAME, SELF, true, 200, ALIVE, report);
    }

    @Test
    public void test4Statistics_200() throws IOException, InterruptedException, TopicSinkClientException {
        LOGGER.info("***************************** Running test4Statistics_200 *****************************");
        XacmlPdpStatisticsManager.resetAllStatistics();
        main = startXacmlPdpService(true);
        Invocation.Builder invocationBuilder = sendHttpRequest(STATISTICS_ENDPOINT);
        StatisticsReport report = invocationBuilder.get(StatisticsReport.class);
        LOGGER.info("test4Statistics_200 health report {}", report);
        validateStatisticsReport(report, 0, 200);
        updateXacmlPdpStatistics();
        invocationBuilder = sendHttpRequest(STATISTICS_ENDPOINT);
        report = invocationBuilder.get(StatisticsReport.class);
        LOGGER.info("test4Statistics_200 health report {}", report);
        validateStatisticsReport(report, 1, 200);
        XacmlPdpStatisticsManager.resetAllStatistics();
    }

    @Test
    public void test5Statistics_500() throws IOException, InterruptedException {
        LOGGER.info("***************************** Running test5Statistics_500 *****************************");
        final RestServerParameters restServerParams = new CommonTestData().getRestServerParameters(false);
        restServerParams.setName(CommonTestData.PDPX_GROUP_NAME);
        restServer = new XacmlPdpRestServer(restServerParams, applicationPath.getAbsolutePath());
        restServer.start();
        final Invocation.Builder invocationBuilder = sendHttpRequest(STATISTICS_ENDPOINT);
        final StatisticsReport report = invocationBuilder.get(StatisticsReport.class);
        LOGGER.info("test5Statistics_500 health report {}", report);
        validateStatisticsReport(report, 0, 500);
        XacmlPdpStatisticsManager.resetAllStatistics();
    }

    @Test
    public void test6HttpsStatistic() throws Exception {
        LOGGER.info("***************************** Running test6HttpsStatistic *****************************");
        main = startXacmlPdpService(false);
        final Invocation.Builder invocationBuilder = sendHttpsRequest(STATISTICS_ENDPOINT);
        final StatisticsReport report = invocationBuilder.get(StatisticsReport.class);
        LOGGER.info("test6HttpsStatistic health report {}", report);
        validateStatisticsReport(report, 0, 200);
    }

    @Test
    public void test3StatisticsConstructorIsPrivate() {
        LOGGER.info("************************* Running test3StatisticsConstructorIsPrivate *************************");
        try {
            final Constructor<XacmlPdpStatisticsManager> constructor =
                    XacmlPdpStatisticsManager.class.getDeclaredConstructor();
            assertTrue(Modifier.isPrivate(constructor.getModifiers()));
            constructor.setAccessible(true);
            constructor.newInstance();
            fail("Expected an InstantiationException to be thrown");
        } catch (final Exception exp) {
            assertTrue(exp.getCause().toString().contains("Instantiation of the class is not allowed"));
        }
    }

    private Main startXacmlPdpService(final boolean http) throws TopicSinkClientException {
        final String[] xacmlPdpConfigParameters = new String[4];
        if (http) {
            xacmlPdpConfigParameters[0] = "-c";
            xacmlPdpConfigParameters[1] = "parameters/XacmlPdpConfigParameters.json";
            xacmlPdpConfigParameters[2] = "-p";
            xacmlPdpConfigParameters[3] = "parameters/topic.properties";
        } else {
            final Properties systemProps = System.getProperties();
            systemProps.put("javax.net.ssl.keyStore", KEYSTORE);
            systemProps.put("javax.net.ssl.keyStorePassword", "Pol1cy_0nap");
            System.setProperties(systemProps);
            xacmlPdpConfigParameters[0] = "-c";
            xacmlPdpConfigParameters[1] = "parameters/XacmlPdpConfigParameters_Https.json";
            xacmlPdpConfigParameters[2] = "-p";
            xacmlPdpConfigParameters[3] = "parameters/topic.properties";
        }
        return new Main(xacmlPdpConfigParameters);
    }

    private void stopXacmlPdpService(final Main main) throws PolicyXacmlPdpException {
        main.shutdown();
    }

    private Invocation.Builder sendHttpRequest(final String endpoint) throws IOException, InterruptedException {
        final ClientConfig clientConfig = new ClientConfig();

        final HttpAuthenticationFeature feature = HttpAuthenticationFeature.basic("healthcheck", "zb!XztG34");
        clientConfig.register(feature);

        final Client client = ClientBuilder.newClient(clientConfig);
        final WebTarget webTarget = client.target("http://localhost:6969/policy/pdpx/v1/" + endpoint);

        final Invocation.Builder invocationBuilder = webTarget.request(MediaType.APPLICATION_JSON);

        boolean isOpen = false;
        for (long time = 1000L; time <= 6000L; time += 1000L) {
            if (NetworkUtil.isTcpPortOpen("localhost", 6969, 6, time)) {
                isOpen = true;
                break;
            }
        }
        if (! isOpen) {
            throw new IllegalStateException("Cannot connect to port 6969");
        }
        return invocationBuilder;
    }

    private Invocation.Builder sendHttpsRequest(final String endpoint) throws Exception {

        final TrustManager[] noopTrustManager = new TrustManager[] {new X509TrustManager() {

            @Override
            public X509Certificate[] getAcceptedIssuers() {
                return new X509Certificate[0];
            }

            @Override
            public void checkClientTrusted(final java.security.cert.X509Certificate[] certs, final String authType) {}

            @Override
            public void checkServerTrusted(final java.security.cert.X509Certificate[] certs, final String authType) {}
        } };

        final SSLContext sc = SSLContext.getInstance("TLSv1.2");
        sc.init(null, noopTrustManager, new SecureRandom());
        final ClientBuilder clientBuilder =
                ClientBuilder.newBuilder().sslContext(sc).hostnameVerifier((host, session) -> true);
        final Client client = clientBuilder.build();
        final HttpAuthenticationFeature feature = HttpAuthenticationFeature.basic("healthcheck", "zb!XztG34");
        client.register(feature);

        final WebTarget webTarget = client.target("https://localhost:6969/policy/pdpx/v1/" + endpoint);

        final Invocation.Builder invocationBuilder = webTarget.request(MediaType.APPLICATION_JSON);

        if (!NetworkUtil.isTcpPortOpen("localhost", 6969, 6, 10000L)) {
            throw new IllegalStateException("cannot connect to port 6969");
        }
        return invocationBuilder;
    }

    private void updateXacmlPdpStatistics() {
        XacmlPdpStatisticsManager.updateTotalPoliciesCount();
        XacmlPdpStatisticsManager.updatePermitDecisionsCount();
        XacmlPdpStatisticsManager.updateDenyDecisionsCount();
        XacmlPdpStatisticsManager.updateIndeterminantDecisionsCount();
        XacmlPdpStatisticsManager.updateNotApplicableDecisionsCount();
    }

    private void validateStatisticsReport(final StatisticsReport report, final int count, final int code) {
        assertEquals(code, report.getCode());
        assertEquals(count, report.getTotalPoliciesCount());
        assertEquals(count, report.getPermitDecisionsCount());
        assertEquals(count, report.getDenyDecisionsCount());
        assertEquals(count, report.getIndeterminantDecisionsCount());
        assertEquals(count, report.getNotApplicableDecisionsCount());
    }

    private void validateHealthCheckReport(final String name, final String url, final boolean healthy, final int code,
            final String message, final HealthCheckReport report) {
        assertEquals(name, report.getName());
        assertEquals(url, report.getUrl());
        assertEquals(healthy, report.isHealthy());
        assertEquals(code, report.getCode());
        assertEquals(message, report.getMessage());
    }
}
