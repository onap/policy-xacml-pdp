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

package org.onap.policy.pdpx.main.rest.provider;

import org.onap.policy.pdpx.main.rest.XacmlPdpStatisticsManager;
import org.onap.policy.pdpx.main.rest.model.StatisticsReport;
import org.onap.policy.pdpx.main.startstop.XacmlPdpActivator;

/**
 * Class to fetch statistics of xacmlPdp service.
 *
 */
public class StatisticsProvider {

    /**
     * Returns the current statistics of xacmlPdp service.
     *
     * @return Report containing statistics of xacmlPdp service
     */
    public StatisticsReport fetchCurrentStatistics() {
        final StatisticsReport report = new StatisticsReport();
        report.setCode(XacmlPdpActivator.isAlive() ? 200 : 500);
        report.setTotalPoliciesCount(XacmlPdpStatisticsManager.getTotalPoliciesCount());
        report.setPermitDecisionsCount(XacmlPdpStatisticsManager.getPermitDecisionsCount());
        report.setDenyDecisionsCount(XacmlPdpStatisticsManager.getDenyDecisionsCount());
        report.setIndeterminantDecisionsCount(XacmlPdpStatisticsManager.getIndeterminantDecisionsCount());
        report.setNotApplicableDecisionsCount(XacmlPdpStatisticsManager.getNotApplicableDecisionsCount());
        return report;
    }
}
