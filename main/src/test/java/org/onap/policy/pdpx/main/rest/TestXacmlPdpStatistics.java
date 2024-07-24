/*-
 * ============LICENSE_START=======================================================
 * Copyright (C) 2019, 2022 AT&T Intellectual Property. All rights reserved.
 * Modifications Copyright (C) 2024 Nordix Foundation.
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

import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.onap.policy.pdpx.main.CommonRest;
import org.onap.policy.pdpx.main.rest.model.StatisticsReport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Class to perform unit test of {@link XacmlPdpRestController}.
 */
class TestXacmlPdpStatistics extends CommonRest {

    private static final Logger LOGGER = LoggerFactory.getLogger(TestXacmlPdpStatistics.class);

    private int nupdates = 0;

    @Test
    void testXacmlPdpStatistics_200() throws Exception {
        LOGGER.info("*************************** Running testXacmlPdpStatistics_200 ***************************");
        StatisticsReport report = getXacmlPdpStatistics();
        validateReport(report, 0, 200, new HashMap<>());
        updateXacmlPdpStatistics();
        report = getXacmlPdpStatistics();
        validateReport(report, 1, 200, returnStatisticsMap());
    }

    @Test
    void testXacmlPdpStatistics_500() throws Exception {
        LOGGER.info("***************************** Running testXacmlPdpStatistics_500 *****************************");
        markActivatorDead();
        final StatisticsReport report = getXacmlPdpStatistics();
        validateReport(report, 0, 500, new HashMap<>());
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

    private StatisticsReport getXacmlPdpStatistics() throws Exception {
        return sendHttpsRequest("statistics").get(StatisticsReport.class);
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

    private void validateReport(final StatisticsReport report, final int count,
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
}
