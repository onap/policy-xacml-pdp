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

import org.junit.Test;
import org.onap.policy.pdpx.main.CommonRest;
import org.onap.policy.pdpx.main.rest.model.StatisticsReport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Class to perform unit test of {@link XacmlPdpRestController}.
 *
 */
public class TestXacmlPdpStatistics extends CommonRest {

    private static final Logger LOGGER = LoggerFactory.getLogger(TestXacmlPdpStatistics.class);

    private int nupdates = 0;

    @Test
    public void testXacmlPdpStatistics_200() throws Exception {
        LOGGER.info("*************************** Running testXacmlPdpStatistics_200 ***************************");
        StatisticsReport report = getXacmlPdpStatistics();
        validateReport(report, 0, 200);
        updateXacmlPdpStatistics();
        report = getXacmlPdpStatistics();
        validateReport(report, 1, 200);
    }

    @Test
    public void testXacmlPdpStatistics_500() throws Exception {
        LOGGER.info("***************************** Running testXacmlPdpStatistics_500 *****************************");

        markActivatorDead();

        final StatisticsReport report = getXacmlPdpStatistics();
        validateReport(report, 0, 500);
    }

    private StatisticsReport getXacmlPdpStatistics() throws Exception {
        return sendHttpsRequest("statistics").get(StatisticsReport.class);
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

    private void validateReport(final StatisticsReport report, final int count, final int code) {
        assertEquals(code, report.getCode());
        assertEquals(count, report.getTotalPoliciesCount());
        assertEquals(count, report.getTotalPolicyTypesCount());
        assertEquals(count, report.getPermitDecisionsCount());
        assertEquals(count, report.getDenyDecisionsCount());
        assertEquals(count, report.getIndeterminantDecisionsCount());
        assertEquals(count, report.getNotApplicableDecisionsCount());
    }
}
