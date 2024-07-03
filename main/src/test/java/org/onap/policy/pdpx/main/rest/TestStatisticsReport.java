/*-
 * ============LICENSE_START=======================================================
 * Copyright (C) 2019-2020 AT&T Intellectual Property. All rights reserved.
 * Modifications Copyright (C) 2024 Nordix Foundation
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
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.onap.policy.pdpx.main.rest.model.StatisticsReport;

/**
 * Class to perform unit testing of {@link StatisticsReport}.
 *
 */
public class TestStatisticsReport {

    static StatisticsReport report;

    @BeforeAll
    static void setUp() {
        report = new StatisticsReport();
    }

    @Test
    void testStatisticsReportConstructor() {
        assertNotNull(report);
    }

    @Test
    void testGettersAndSetters() {
        report.setCode(123);
        report.setApplicationMetrics(null);
        report.setDenyDecisionsCount(123456);
        report.setDeployFailureCount(1111);
        report.setDeploySuccessCount(2222);
        report.setPermitDecisionsCount(3333);
        report.setIndeterminantDecisionsCount(4444);
        report.setNotApplicableDecisionsCount(5555);
        report.setTotalErrorCount(6666);
        report.setTotalPoliciesCount(7777);
        report.setUndeployFailureCount(8888);
        report.setUndeploySuccessCount(9999);
        report.setTotalPolicyTypesCount(9898);

        assertThat(report.toString()).contains("code=123", "totalPolicyTypesCount=9898",
            "totalPoliciesCount=7777", "totalErrorCount=6666", "permitDecisionsCount=3333",
            "denyDecisionsCount=123456", "deploySuccessCount=2222", "deployFailureCount=1111",
            "undeploySuccessCount=9999", "undeployFailureCount=8888",
            "indeterminantDecisionsCount=4444", "notApplicableDecisionsCount=5555",
            "applicationMetrics=null");
    }
}
