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

package org.onap.policy.pdpx.main;

import java.util.Collections;
import org.apache.commons.lang3.StringUtils;
import org.onap.policy.common.utils.network.NetworkUtil;
import org.onap.policy.models.pdp.concepts.PdpMessage;
import org.onap.policy.models.pdp.concepts.PdpResponseDetails;
import org.onap.policy.models.pdp.concepts.PdpStateChange;
import org.onap.policy.models.pdp.concepts.PdpStatus;
import org.onap.policy.models.pdp.concepts.PdpUpdate;
import org.onap.policy.models.pdp.enums.PdpHealthStatus;
import org.onap.policy.models.pdp.enums.PdpResponseStatus;
import org.onap.policy.models.pdp.enums.PdpState;
import org.onap.policy.pdpx.main.rest.XacmlPdpApplicationManager;
import org.onap.policy.pdpx.main.startstop.XacmlPdpActivator;

/**
 * Current state of this XACML PDP.
 */
public class XacmlState {
    /**
     * The application manager.
     */
    private final XacmlPdpApplicationManager appManager;

    /**
     * Records the current state of this PDP.
     */
    private final PdpStatus status;

    /**
     * Constructs the object, initializing the state.
     */
    public XacmlState(XacmlPdpApplicationManager appManager, String pdpGroupName, String pdpType) {
        this.appManager = appManager;

        this.status = new PdpStatus();
        this.status.setName(NetworkUtil.getHostname());
        this.status.setPdpType(pdpType);
        this.status.setState(PdpState.PASSIVE);
        this.status.setPolicies(Collections.emptyList());
        this.status.setPdpGroup(pdpGroupName);
    }

    /**
     * Determines if this PDP should handle the given message.
     *
     * @param message message of interest
     * @return {@code true} if this PDP should handle the message, {@code false} otherwise
     */
    public boolean shouldHandle(PdpMessage message) {
        return message.appliesTo(status.getName(), status.getPdpGroup(), status.getPdpSubgroup());
    }

    /**
     * Generates a new heart beat message.
     *
     * @return a new heart beat message
     */
    public PdpStatus genHeartbeat() {
        // first, update status fields
        status.setHealthy(XacmlPdpActivator.getCurrent().isAlive() ? PdpHealthStatus.HEALTHY
                        : PdpHealthStatus.NOT_HEALTHY);

        return new PdpStatus(status);
    }

    /**
     * Updates the internal state based on the given message.
     *
     * @param message message from which to update the internal state
     * @return a response to the message
     */
    public PdpStatus updateInternalState(PdpStateChange message) {
        status.setState(message.getState());

        /*
         * NOTE: Do NOT update group & subgroup as state-change requests do not set those
         * fields to indicate new values; they only set them to do broadcasts to all PDPs
         * within a group/subgroup.
         */

        PdpStatus status2 = makeResponse(message, "");

        // these fields aren't needed in the response, so clear them out to avoid sending
        status2.setPolicies(null);

        return status2;
    }

    /**
     * Updates the internal state based on the given message. Assumes that the policies
     * have already been updated within the application manager.
     *
     * @param message message from which to update the internal state
     * @return a response to the message
     */
    public PdpStatus updateInternalState(PdpUpdate message, String errMessage) {
        status.setPdpSubgroup(message.getPdpSubgroup());
        status.setPolicies(appManager.getToscaPolicyIdentifiers());

        return makeResponse(message, errMessage);
    }

    /**
     * Updates the internal state to Terminated.
     *
     * @return the current PdpStatus with Terminated state
     */
    public PdpStatus terminatePdpMessage() {
        status.setState(PdpState.TERMINATED);
        return new PdpStatus(status);
    }

    /**
     * Makes a response to the given message, based on the current state.
     *
     * @param message message for which the response should be made
     * @param errMessage the error message to be sent to PAP
     * @return a new response
     */
    private PdpStatus makeResponse(PdpMessage message, String errMessage) {
        PdpResponseDetails resp = new PdpResponseDetails();

        if (StringUtils.isBlank(errMessage)) {
            resp.setResponseStatus(PdpResponseStatus.SUCCESS);
        } else {
            resp.setResponseStatus(PdpResponseStatus.FAIL);
            resp.setResponseMessage(errMessage);
        }
        resp.setResponseTo(message.getRequestId());

        PdpStatus status2 = new PdpStatus(status);
        status2.setResponse(resp);
        return status2;
    }
}
