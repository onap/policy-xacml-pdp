/*-
 * ============LICENSE_START=======================================================
 * Copyright (C) 2019, 2021 AT&T Intellectual Property. All rights reserved.
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

package org.onap.policy.pdpx.main.rest.provider;

import org.onap.policy.common.utils.report.HealthCheckReport;
import org.onap.policy.pdpx.main.XacmlState;
import org.onap.policy.pdpx.main.startstop.XacmlPdpActivator;

/**
 * Class to fetch health check of xacml pdp service.
 */
public class HealthCheckProvider {

    private static final String NOT_ALIVE = "not alive";
    private static final String ALIVE = "alive";
    private static final String URL = "self";

    /**
     * Performs the health check of xacml pdp service.
     *
     * @return Report containing health check status
     */
    public HealthCheckReport performHealthCheck() {
        final var report = new HealthCheckReport();
        report.setName(XacmlState.PDP_NAME);
        report.setUrl(URL);

        boolean alive = XacmlPdpActivator.getCurrent().isAlive();

        report.setHealthy(alive);
        report.setCode(alive ? 200 : 500);
        report.setMessage(alive ? ALIVE : NOT_ALIVE);
        return report;
    }
}
