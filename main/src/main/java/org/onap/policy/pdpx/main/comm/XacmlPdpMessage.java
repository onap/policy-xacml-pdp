/*-
 * ============LICENSE_START=======================================================
 * ONAP
 * ================================================================================
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

package org.onap.policy.pdpx.main.comm;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.ServiceLoader;
import org.onap.policy.common.endpoints.report.HealthCheckReport;
import org.onap.policy.models.pdp.concepts.PdpStatus;
import org.onap.policy.models.pdp.concepts.PolicyTypeIdent;
import org.onap.policy.models.pdp.enums.PdpHealthStatus;
import org.onap.policy.models.pdp.enums.PdpState;
import org.onap.policy.pdp.xacml.application.common.XacmlApplicationServiceProvider;
import org.onap.policy.pdpx.main.rest.XacmlPdpStatisticsManager;
import org.onap.policy.pdpx.main.rest.provider.HealthCheckProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class XacmlPdpMessage {

    // The logger for this class
    private static final Logger LOGGER = LoggerFactory.getLogger(XacmlPdpMessage.class);

    /**
     * Method used to format the status message.
     *
     * @param state of the PDP
     * @return status message of the PDP
     * @throws UnknownHostException if cannot get hostname
     */
    public PdpStatus formatStatusMessage(PdpState state) throws UnknownHostException {
        PdpStatus status = new PdpStatus();
        status.setInstance(InetAddress.getLocalHost().getHostName());
        status.setPdpType("xacml");
        HealthCheckProvider healthcheck = new HealthCheckProvider();
        HealthCheckReport report = healthcheck.performHealthCheck();

        if (report.isHealthy()) {
            status.setHealthy(PdpHealthStatus.HEALTHY);
        } else {
            status.setHealthy(PdpHealthStatus.NOT_HEALTHY);
        }

        status.setState(state);
        status.setSupportedPolicyTypes(locateExistingPolicyTypes());

        return status;

    }

    /**
     * Method used to format the heartbeast status message.
     *
     * @param state of the PDP
     * @return status message of the PDP
     * @throws UnknownHostException if cannot get hostname
     */
    public PdpStatus formatHeartbeatStatusMessage(PdpState state) throws UnknownHostException {
        final PdpStatus status = new PdpStatus();
        status.setPdpType("xacml");
        status.setState(state);
        HealthCheckProvider healthcheck = new HealthCheckProvider();
        HealthCheckReport report = healthcheck.performHealthCheck();

        if (report.isHealthy()) {
            status.setHealthy(PdpHealthStatus.HEALTHY);
        } else {
            status.setHealthy(PdpHealthStatus.NOT_HEALTHY);
        }
        status.setInstance(InetAddress.getLocalHost().getHostName());
        status.setSupportedPolicyTypes(locateExistingPolicyTypes());

        return status;

    }

    private List<PolicyTypeIdent> locateExistingPolicyTypes() {
        //
        // Load service
        //
        ServiceLoader<XacmlApplicationServiceProvider> applicationLoader =
                ServiceLoader.load(XacmlApplicationServiceProvider.class);
        //
        // Iterate through them and create the List
        //
        List<PolicyTypeIdent> policyTypeIdents = new ArrayList<>();
        StringBuilder strDump = new StringBuilder("Loaded applications:" + System.lineSeparator());
        Iterator<XacmlApplicationServiceProvider> iterator = applicationLoader.iterator();
        long types = 0;
        while (iterator.hasNext()) {
            XacmlApplicationServiceProvider application = iterator.next();
            List<String> supportedPolicyTypes = application.supportedPolicyTypes();
            types += application.supportedPolicyTypes().size();

            strDump.append(application.applicationName());
            strDump.append(" supports ");
            strDump.append(application.supportedPolicyTypes());
            strDump.append(System.lineSeparator());

            for (String name : supportedPolicyTypes) {
                PolicyTypeIdent id = new PolicyTypeIdent(name, "1.0.0");
                policyTypeIdents.add(id);
            }
        }
        LOGGER.debug("{}", strDump);

        //
        // Update statistics manager
        //
        XacmlPdpStatisticsManager.setTotalPolicyTypesCount(types);

        return policyTypeIdents;
    }
}
