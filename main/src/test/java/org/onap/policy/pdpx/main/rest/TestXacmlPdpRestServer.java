/*-
 * ============LICENSE_START=======================================================
 * Copyright (C) 2019, 2021-2022 AT&T Intellectual Property. All rights reserved.
 * Modifications Copyright (C) 2023-2024 Nordix Foundation.
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

import static org.junit.jupiter.api.Assertions.assertEquals;

import jakarta.ws.rs.client.Invocation;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.onap.policy.common.utils.report.HealthCheckReport;
import org.onap.policy.pdpx.main.CommonRest;
import org.onap.policy.pdpx.main.XacmlState;
import org.onap.policy.pdpx.main.rest.model.StatisticsReport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Class to perform unit test of {@link org.onap.policy.pdpx.main.startstop.XacmlPdpRestServer}.
 */
class TestXacmlPdpRestServer extends CommonRest {

    private static final Logger LOGGER = LoggerFactory.getLogger(TestXacmlPdpRestServer.class);
    private static final String NOT_ALIVE = "not alive";
    private static final String ALIVE = "alive";
    private static final String SELF = "self";
    private static final String NAME = XacmlState.PDP_NAME;
    private static final String HEALTHCHECK_ENDPOINT = "healthcheck";
    private static final String STATISTICS_ENDPOINT = "statistics";

    private int nupdates = 0;

    @Test
    void testHealthCheckSuccess() throws Exception {
        LOGGER.info("***************************** Running testHealthCheckSuccess *****************************");
        final Invocation.Builder invocationBuilder = sendHttpsRequest(HEALTHCHECK_ENDPOINT);
        final HealthCheckReport report = invocationBuilder.get(HealthCheckReport.class);
        LOGGER.info("test1HealthCheckSuccess health report {}", report);
        validateHealthCheckReport(true, 200, ALIVE, report);
    }

    @Test
    void testHealthCheckFailure() throws Exception {
        LOGGER.info("***************************** Running testHealthCheckFailure *****************************");

        markActivatorDead();

        final Invocation.Builder invocationBuilder = sendHttpsRequest(HEALTHCHECK_ENDPOINT);
        final HealthCheckReport report = invocationBuilder.get(HealthCheckReport.class);
        LOGGER.info("testHealthCheckFailure health report {}", report);
        validateHealthCheckReport(false, 500, NOT_ALIVE, report);
    }

    @Test
    void testHttpsHealthCheckSuccess() throws Exception {
        LOGGER.info("***************************** Running testHttpsHealthCheckSuccess *****************************");
        final Invocation.Builder invocationBuilder = sendHttpsRequest(HEALTHCHECK_ENDPOINT);
        final HealthCheckReport report = invocationBuilder.get(HealthCheckReport.class);
        LOGGER.info("testHttpsHealthCheckSuccess health report {}", report);
        validateHealthCheckReport(true, 200, ALIVE, report);
    }

    @Test
    void testStatistics_200() throws Exception {
        LOGGER.info("***************************** Running testStatistics_200 *****************************");
        Invocation.Builder invocationBuilder = sendHttpsRequest(STATISTICS_ENDPOINT);
        StatisticsReport report = invocationBuilder.get(StatisticsReport.class);
        LOGGER.info("testStatistics_200 health report {}", report);
        validateStatisticsReport(report, 0, 200, new HashMap<>());
        updateXacmlPdpStatistics();
        invocationBuilder = sendHttpsRequest(STATISTICS_ENDPOINT);
        report = invocationBuilder.get(StatisticsReport.class);
        LOGGER.info("testStatistics_200 health report {}", report);
        validateStatisticsReport(report, 1, 200, returnStatisticsMap());
    }

    @Test
    void testStatistics_500() throws Exception {
        LOGGER.info("***************************** Running testStatistics_500 *****************************");

        markActivatorDead();

        final Invocation.Builder invocationBuilder = sendHttpsRequest(STATISTICS_ENDPOINT);
        final StatisticsReport report = invocationBuilder.get(StatisticsReport.class);
        LOGGER.info("testStatistics_500 health report {}", report);
        validateStatisticsReport(report, 0, 500, new HashMap<>());
    }

    @Test
    void testHttpsStatistic() throws Exception {
        LOGGER.info("***************************** Running testHttpsStatistic *****************************");
        final Invocation.Builder invocationBuilder = sendHttpsRequest(STATISTICS_ENDPOINT);
        final StatisticsReport report = invocationBuilder.get(StatisticsReport.class);
        LOGGER.info("testHttpsStatistic health report {}", report);
        validateStatisticsReport(report, 0, 200, new HashMap<>());
    }

    private Map<String, Map<String, Integer>> returnStatisticsMap() {
        Map<String, Integer> testAppMetrics1 = new HashMap<>();
        Map<String, Integer> testAppMetrics2 = new HashMap<>();
        testAppMetrics1.put("permit_decisions_count", 1);
        testAppMetrics1.put("deny_decisions_count", 1);
        testAppMetrics2.put("indeterminant_decisions_count", 1);
        testAppMetrics2.put("not_applicable_decisions_count", 1);
        Map<String, Map<String, Integer>> statisticsMap = new HashMap<>();
        statisticsMap.put("testApp1", testAppMetrics1);
        statisticsMap.put("testApp2", testAppMetrics2);
        return statisticsMap;
    }

    private void updateXacmlPdpStatistics() {
        XacmlPdpStatisticsManager stats = XacmlPdpStatisticsManager.getCurrent();
        ++nupdates;
        stats.setTotalPolicyCount(nupdates);
        stats.setTotalPolicyTypesCount(nupdates);
        stats.updatePermitDecisionsCount("testApp1");
        stats.updateDenyDecisionsCount("testApp1");
        stats.updateIndeterminantDecisionsCount("testApp2");
        stats.updateNotApplicableDecisionsCount("testApp2");
    }

    private void validateStatisticsReport(final StatisticsReport report, final int count,
                                          final int code, final Map<String, Map<String, Integer>> decisionsMap) {
        assertEquals(code, report.getCode());
        assertEquals(count, report.getTotalPoliciesCount());
        assertEquals(count, report.getTotalPolicyTypesCount());
        assertEquals(count, report.getPermitDecisionsCount());
        assertEquals(count, report.getDenyDecisionsCount());
        assertEquals(count, report.getIndeterminantDecisionsCount());
        assertEquals(count, report.getNotApplicableDecisionsCount());
        assertEquals(decisionsMap, report.getApplicationMetrics());
    }

    private void validateHealthCheckReport(final boolean healthy, final int code,
                                           final String message, final HealthCheckReport report) {
        assertEquals(TestXacmlPdpRestServer.NAME, report.getName());
        assertEquals(TestXacmlPdpRestServer.SELF, report.getUrl());
        assertEquals(healthy, report.isHealthy());
        assertEquals(code, report.getCode());
        assertEquals(message, report.getMessage());
    }
}
