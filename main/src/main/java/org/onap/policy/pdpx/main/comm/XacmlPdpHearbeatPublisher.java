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

package org.onap.policy.pdpx.main.comm;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.stream.Collectors;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import org.onap.policy.common.endpoints.event.comm.client.TopicSinkClient;
import org.onap.policy.common.utils.network.NetworkUtil;
import org.onap.policy.models.pdp.concepts.PdpResponseDetails;
import org.onap.policy.models.pdp.concepts.PdpStateChange;
import org.onap.policy.models.pdp.concepts.PdpStatus;
import org.onap.policy.models.pdp.concepts.PdpUpdate;
import org.onap.policy.models.pdp.enums.PdpHealthStatus;
import org.onap.policy.models.pdp.enums.PdpResponseStatus;
import org.onap.policy.models.pdp.enums.PdpState;
import org.onap.policy.models.tosca.authorative.concepts.ToscaPolicy;
import org.onap.policy.models.tosca.authorative.concepts.ToscaPolicyIdentifier;
import org.onap.policy.pdpx.main.rest.XacmlPdpApplicationManager;
import org.onap.policy.pdpx.main.startstop.XacmlPdpActivator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class XacmlPdpHearbeatPublisher extends TimerTask {

    private static final Logger LOGGER = LoggerFactory.getLogger(XacmlPdpHearbeatPublisher.class);

    private final TopicSinkClient topicSinkClient;

    /**
     * Records the current state of this PDP.
     */
    private final PdpStatus status;

    /**
     * Current timer interval, in milliseconds.
     */
    private long intervalMs = 60000;

    private Timer timer;

    @Getter
    @Setter(AccessLevel.PRIVATE)
    private volatile boolean alive = false;


    /**
     * Constructor for instantiating XacmlPdpPublisher.
     *
     * @param topicSinkClient used to send heart beat message
     */
    public XacmlPdpHearbeatPublisher(TopicSinkClient topicSinkClient) {
        this.topicSinkClient = topicSinkClient;

        // create the initial state
        this.status = new PdpStatus();
        this.status.setName(NetworkUtil.getHostname());
        this.status.setPdpType("xacml");
        this.status.setState(PdpState.PASSIVE);
        this.status.setSupportedPolicyTypes(XacmlPdpApplicationManager.getToscaPolicyTypeIdents());
        this.status.setPolicies(Collections.emptyList());

        start();
    }

    @Override
    public void run() {
        LOGGER.info("Sending Xacml PDP heartbeat to the PAP");

        // update the status before generating the heart beat
        status.setHealthy(XacmlPdpActivator.getCurrent().isAlive() ? PdpHealthStatus.HEALTHY
                        : PdpHealthStatus.NOT_HEALTHY);

        topicSinkClient.send(new PdpStatus(status));
    }

    /**
     * Method to terminate the heartbeat.
     */
    public void terminate() {
        if (isAlive()) {
            timer.cancel();
            timer.purge();
            setAlive(false);
        }
    }

    /**
     * Restarts the timer if the interval has changed.
     *
     * @param intervalMs desired interval, or {@code null} to leave it unchanged
     */
    public void restart(Long intervalMs) {
        if (intervalMs != null && intervalMs > 0 && intervalMs != this.intervalMs) {
            terminate();

            this.intervalMs = intervalMs;
            start();
        }
    }

    /**
     * Starts the timer.
     */
    public void start() {
        if (!isAlive()) {
            this.timer = new Timer(false);
            this.timer.scheduleAtFixedRate(this, 0, this.intervalMs);
            setAlive(true);
        }
    }

    /**
     * Updates the internal state based on the given message.
     *
     * @param message message from which to update the internal state
     * @return a response to the message
     */
    public PdpStatus updateInternalState(PdpStateChange message) {
        status.setState(message.getState());
        status.setPdpGroup(message.getPdpGroup());
        status.setPdpSubgroup(message.getPdpSubgroup());

        PdpResponseDetails resp = new PdpResponseDetails();
        resp.setResponseStatus(PdpResponseStatus.SUCCESS);
        resp.setResponseTo(message.getRequestId());

        PdpStatus status2 = new PdpStatus(status);
        status2.setResponse(resp);

        // these fields aren't needed, so clear them out
        status2.setPolicies(null);

        return status2;
    }

    /**
     * Updates the internal state based on the given message.
     *
     * @param message message from which to update the internal state
     * @return a response to the message
     */
    public PdpStatus updateInternalState(PdpUpdate message) {
        status.setPdpGroup(message.getPdpGroup());
        status.setPdpSubgroup(message.getPdpSubgroup());

        PdpResponseDetails resp = new PdpResponseDetails();
        resp.setResponseStatus(PdpResponseStatus.SUCCESS);
        resp.setResponseTo(message.getRequestId());

        validatePolicies(message.getPolicies(), resp);

        PdpStatus status2 = new PdpStatus(status);
        status2.setResponse(resp);

        return status2;
    }

    /**
     * Verifies that this XACML-PDP has the given policies.
     *
     * @param policies the policies of interest
     * @param resp response that is updated to indicate any error
     */
    private void validatePolicies(List<ToscaPolicy> policies, PdpResponseDetails resp) {
        if (policies == null) {
            return;
        }

        // remember the identifiers so we can record them, assuming everything is valid
        List<ToscaPolicyIdentifier> idents =
                        policies.stream().map(ToscaPolicy::getIdentifier).collect(Collectors.toList());

        // see if all of the identifiers appear in the manager
        Set<ToscaPolicyIdentifier> desired = new HashSet<>(idents);
        desired.removeAll(XacmlPdpApplicationManager.getToscaPolicyIdentifiers());

        if (!desired.isEmpty()) {
            resp.setResponseStatus(PdpResponseStatus.FAIL);
            resp.setResponseMessage("policies do not exist: " + desired);
            return;
        }

        // all OK - record the list
        status.setPolicies(idents);
    }
}
