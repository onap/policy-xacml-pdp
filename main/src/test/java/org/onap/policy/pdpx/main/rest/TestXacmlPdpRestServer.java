/*-
 * ============LICENSE_START=======================================================
 * Copyright (C) 2019, 2021 AT&T Intellectual Property. All rights reserved.
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

import javax.ws.rs.client.Invocation;
import org.junit.Test;
import org.onap.policy.common.endpoints.report.HealthCheckReport;
import org.onap.policy.pdpx.main.CommonRest;
import org.onap.policy.pdpx.main.XacmlState;
import org.onap.policy.pdpx.main.rest.model.StatisticsReport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Class to perform unit test of {@link XacmlPdpRestServer}.
 *
 */
public class TestXacmlPdpRestServer extends CommonRest {

    private static final Logger LOGGER = LoggerFactory.getLogger(TestXacmlPdpRestServer.class);
    private static final String NOT_ALIVE = "not alive";
    private static final String ALIVE = "alive";
    private static final String SELF = "self";
    private static final String NAME = XacmlState.PDP_NAME;
    private static final String HEALTHCHECK_ENDPOINT = "healthcheck";
    private static final String STATISTICS_ENDPOINT = "statistics";

    private int nupdates = 0;

    @Test
    public void testHealthCheckSuccess() throws Exception {
        LOGGER.info("***************************** Running testHealthCheckSuccess *****************************");
        final Invocation.Builder invocationBuilder = sendHttpsRequest(HEALTHCHECK_ENDPOINT);
        final HealthCheckReport report = invocationBuilder.get(HealthCheckReport.class);
        LOGGER.info("test1HealthCheckSuccess health report {}", report);
        validateHealthCheckReport(NAME, SELF, true, 200, ALIVE, report);
    }

    @Test
    public void testHealthCheckFailure() throws Exception {
        LOGGER.info("***************************** Running testHealthCheckFailure *****************************");

        markActivatorDead();

        final Invocation.Builder invocationBuilder = sendHttpsRequest(HEALTHCHECK_ENDPOINT);
        final HealthCheckReport report = invocationBuilder.get(HealthCheckReport.class);
        LOGGER.info("testHealthCheckFailure health report {}", report);
        validateHealthCheckReport(NAME, SELF, false, 500, NOT_ALIVE, report);
    }

    @Test
    public void testHttpsHealthCheckSuccess() throws Exception {
        LOGGER.info("***************************** Running testHttpsHealthCheckSuccess *****************************");
        final Invocation.Builder invocationBuilder = sendHttpsRequest(HEALTHCHECK_ENDPOINT);
        final HealthCheckReport report = invocationBuilder.get(HealthCheckReport.class);
        LOGGER.info("testHttpsHealthCheckSuccess health report {}", report);
        validateHealthCheckReport(NAME, SELF, true, 200, ALIVE, report);
    }

    @Test
    public void testStatistics_200() throws Exception {
        LOGGER.info("***************************** Running testStatistics_200 *****************************");
        Invocation.Builder invocationBuilder = sendHttpsRequest(STATISTICS_ENDPOINT);
        StatisticsReport report = invocationBuilder.get(StatisticsReport.class);
        LOGGER.info("testStatistics_200 health report {}", report);
        validateStatisticsReport(report, 0, 200);
        updateXacmlPdpStatistics();
        invocationBuilder = sendHttpsRequest(STATISTICS_ENDPOINT);
        report = invocationBuilder.get(StatisticsReport.class);
        LOGGER.info("testStatistics_200 health report {}", report);
        validateStatisticsReport(report, 1, 200);
    }

    @Test
    public void testStatistics_500() throws Exception {
        LOGGER.info("***************************** Running testStatistics_500 *****************************");

        markActivatorDead();

        final Invocation.Builder invocationBuilder = sendHttpsRequest(STATISTICS_ENDPOINT);
        final StatisticsReport report = invocationBuilder.get(StatisticsReport.class);
        LOGGER.info("testStatistics_500 health report {}", report);
        validateStatisticsReport(report, 0, 500);
    }

    @Test
    public void testHttpsStatistic() throws Exception {
        LOGGER.info("***************************** Running testHttpsStatistic *****************************");
        final Invocation.Builder invocationBuilder = sendHttpsRequest(STATISTICS_ENDPOINT);
        final StatisticsReport report = invocationBuilder.get(StatisticsReport.class);
        LOGGER.info("testHttpsStatistic health report {}", report);
        validateStatisticsReport(report, 0, 200);
    }

    private void updateXacmlPdpStatistics() {
        XacmlPdpStatisticsManager stats = XacmlPdpStatisticsManager.getCurrent();
        ++nupdates;
        stats.setTotalPolicyCount(nupdates);
        stats.setTotalPolicyTypesCount(nupdates);
        stats.updatePermitDecisionsCount("testApp");
        stats.updateDenyDecisionsCount("testApp");
        stats.updateIndeterminantDecisionsCount("testApp");
        stats.updateNotApplicableDecisionsCount("testApp");
    }

    private void validateStatisticsReport(final StatisticsReport report, final int count, final int code) {
        assertEquals(code, report.getCode());
        assertEquals(count, report.getTotalPoliciesCount());
        assertEquals(count, report.getTotalPolicyTypesCount());
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
