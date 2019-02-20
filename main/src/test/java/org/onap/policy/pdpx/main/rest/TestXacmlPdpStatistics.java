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
import static org.junit.Assert.fail;

import java.io.IOException;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.client.authentication.HttpAuthenticationFeature;
import org.junit.Test;
import org.onap.policy.common.utils.network.NetworkUtil;
import org.onap.policy.pdpx.main.PolicyXacmlPdpException;
import org.onap.policy.pdpx.main.parameters.CommonTestData;
import org.onap.policy.pdpx.main.parameters.RestServerParameters;
import org.onap.policy.pdpx.main.rest.XacmlPdpStatisticsManager;
import org.onap.policy.pdpx.main.rest.model.StatisticsReport;
import org.onap.policy.pdpx.main.startstop.Main;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Class to perform unit test of {@link XacmlPdpRestController}.
 *
 */
public class TestXacmlPdpStatistics {

    private static final Logger LOGGER = LoggerFactory.getLogger(TestXacmlPdpStatistics.class);

    @Test
    public void testXacmlPdpStatistics_200() throws PolicyXacmlPdpException, InterruptedException {
        try {
            final Main main = startXacmlPdpService();
            StatisticsReport report = getXacmlPdpStatistics();
            validateReport(report, 0, 200);
            updateXacmlPdpStatistics();
            report = getXacmlPdpStatistics();
            validateReport(report, 1, 200);
            stopXacmlPdpService(main);
            XacmlPdpStatisticsManager.resetAllStatistics();
        } catch (final Exception e) {
            LOGGER.error("testApiStatistics_200 failed", e);
            fail("Test should not throw an exception");
        }
    }

    @Test
    public void testXacmlPdpStatistics_500() throws InterruptedException {
        final RestServerParameters restServerParams = new CommonTestData().getRestServerParameters(false);
        restServerParams.setName(CommonTestData.PDPX_GROUP_NAME);
        final XacmlPdpRestServer restServer = new XacmlPdpRestServer(restServerParams);

        try {
            restServer.start();
            final StatisticsReport report = getXacmlPdpStatistics();
            validateReport(report, 0, 500);
            restServer.shutdown();
            XacmlPdpStatisticsManager.resetAllStatistics();
        } catch (final Exception e) {
            LOGGER.error("testApiStatistics_500 failed", e);
            fail("Test should not throw an exception");
        }
    }


    private Main startXacmlPdpService() {
        final String[] XacmlPdpConfigParameters = {"-c", "parameters/XacmlPdpConfigParameters.json"};
        return new Main(XacmlPdpConfigParameters);
    }

    private void stopXacmlPdpService(final Main main) throws PolicyXacmlPdpException {
        main.shutdown();
    }

    private StatisticsReport getXacmlPdpStatistics() throws InterruptedException, IOException {

        final ClientConfig clientConfig = new ClientConfig();

        final HttpAuthenticationFeature feature = HttpAuthenticationFeature.basic("healthcheck", "zb!XztG34");
        clientConfig.register(feature);

        final Client client = ClientBuilder.newClient(clientConfig);
        final WebTarget webTarget = client.target("http://localhost:6969/policy/pdpx/v1/statistics");

        final Invocation.Builder invocationBuilder = webTarget.request(MediaType.APPLICATION_JSON);

        if (!NetworkUtil.isTcpPortOpen("localhost", 6969, 6, 10000L)) {
            throw new IllegalStateException("Cannot connect to port 6969");
        }

        return invocationBuilder.get(StatisticsReport.class);
    }

    private void updateXacmlPdpStatistics() {
        XacmlPdpStatisticsManager.updateTotalPoliciesCount();
        XacmlPdpStatisticsManager.updatePermitDecisionsCount();
        XacmlPdpStatisticsManager.updateDenyDecisionsCount();
        XacmlPdpStatisticsManager.updateIndeterminantDecisionsCount();
        XacmlPdpStatisticsManager.updateNotApplicableDecisionsCount();
    }

    private void validateReport(final StatisticsReport report, final int count, final int code) {
        assertEquals(code, report.getCode());
        assertEquals(count, report.getTotalPoliciesCount());
        assertEquals(count, report.getPermitDecisionsCount());
        assertEquals(count, report.getDenyDecisionsCount());
        assertEquals(count, report.getIndeterminantDecisionsCount());
        assertEquals(count, report.getNotApplicableDecisionsCount());
    }
}
