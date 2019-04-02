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

import java.net.UnknownHostException;
import org.onap.policy.common.utils.network.NetworkUtil;
import org.onap.policy.models.pdp.concepts.PdpStatus;
import org.onap.policy.models.pdp.enums.PdpHealthStatus;
import org.onap.policy.models.pdp.enums.PdpState;
import org.onap.policy.pdpx.main.rest.XacmlPdpApplicationManager;
import org.onap.policy.pdpx.main.startstop.XacmlPdpActivator;
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
    public PdpStatus formatStatusMessage(PdpState state) {
        PdpStatus status = new PdpStatus();
        status.setName(NetworkUtil.getHostname());

        if (XacmlPdpActivator.getCurrent().isAlive()) {
            status.setHealthy(PdpHealthStatus.HEALTHY);
        } else {
            status.setHealthy(PdpHealthStatus.NOT_HEALTHY);
        }

        status.setPdpType("xacml");
        status.setState(state);
        status.setSupportedPolicyTypes(XacmlPdpApplicationManager.getToscaPolicyTypeIdents());

        return status;

    }
}
