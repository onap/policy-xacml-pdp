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

import lombok.Getter;
import org.onap.policy.common.utils.network.NetworkUtil;
import org.onap.policy.models.pdp.concepts.PdpStateChange;
import org.onap.policy.models.pdp.concepts.PdpStatus;
import org.onap.policy.models.pdp.concepts.PdpUpdate;
import org.onap.policy.models.pdp.enums.PdpHealthStatus;
import org.onap.policy.models.pdp.enums.PdpState;
import org.onap.policy.pdpx.main.rest.XacmlPdpApplicationManager;
import org.onap.policy.pdpx.main.startstop.XacmlPdpActivator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Getter
public class XacmlPdpMessage {

    // The logger for this class
    private static final Logger LOGGER = LoggerFactory.getLogger(XacmlPdpMessage.class);
    private String pdpGroup;
    private String pdpSubGroup;
    private PdpState pdpState;
    private String pdpName = NetworkUtil.getHostname();

    /**
     * Method used to format the initial registration status message.
     *
     * @param state of the PDP
     * @return status message of the PDP
     */
    public PdpStatus formatInitialStatusMessage(PdpState state) {
        PdpStatus status = new PdpStatus();
        status.setName(pdpName);

        if (XacmlPdpActivator.getCurrent().isAlive()) {
            status.setHealthy(PdpHealthStatus.HEALTHY);
        } else {
            status.setHealthy(PdpHealthStatus.NOT_HEALTHY);
        }

        status.setPdpType("xacml");
        status.setState(state);
        status.setSupportedPolicyTypes(XacmlPdpApplicationManager.getToscaPolicyTypeIdents());

        LOGGER.debug("formatStatusMessage state {} status{}", state, status);

        return status;

    }

    /**
     * Method used to format the PdpStatus message for heartbeat and PDP Updates.
     *
     * @return status message of the PDP
     */
    public PdpStatus formatPdpStatusMessage() {
        PdpStatus status = new PdpStatus();
        status.setName(pdpName);

        if (XacmlPdpActivator.getCurrent().isAlive() && XacmlPdpActivator.getCurrent() != null) {
            status.setHealthy(PdpHealthStatus.HEALTHY);
        } else {
            status.setHealthy(PdpHealthStatus.NOT_HEALTHY);
        }

        status.setPdpType("xacml");
        status.setState(pdpState);
        status.setPdpGroup(pdpGroup);
        status.setPdpSubgroup(pdpSubGroup);
        status.setSupportedPolicyTypes(XacmlPdpApplicationManager.getToscaPolicyTypeIdents());
        status.setPolicies(XacmlPdpApplicationManager.getToscaPolicyIdentifiers());

        return status;
    }

    /**
     * Method used to update PDP status attributes from PdpStateChange.
     */
    public void updateInternalStatus(PdpStateChange message) {
        pdpGroup = message.getPdpGroup();
        pdpSubGroup = message.getPdpSubgroup();
        pdpState = message.getState();
    }

    /**
     * Method used to update PDP status attributes from PdpUpdate.
     */
    public void updateInternalStatus(PdpUpdate message) {
        pdpGroup = message.getPdpGroup();
        pdpSubGroup = message.getPdpSubgroup();
    }
}
