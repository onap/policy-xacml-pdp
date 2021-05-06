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

package org.onap.policy.pdpx.main.startstop;

import java.nio.file.Paths;
import java.util.Arrays;
import lombok.Getter;
import lombok.Setter;
import org.onap.policy.common.endpoints.event.comm.TopicEndpointManager;
import org.onap.policy.common.endpoints.event.comm.TopicSource;
import org.onap.policy.common.endpoints.event.comm.client.TopicSinkClient;
import org.onap.policy.common.endpoints.event.comm.client.TopicSinkClientException;
import org.onap.policy.common.endpoints.listeners.MessageTypeDispatcher;
import org.onap.policy.common.parameters.ParameterService;
import org.onap.policy.common.utils.services.ServiceManagerContainer;
import org.onap.policy.models.pdp.concepts.PdpStatus;
import org.onap.policy.models.pdp.enums.PdpMessageType;
import org.onap.policy.pdpx.main.PolicyXacmlPdpRuntimeException;
import org.onap.policy.pdpx.main.XacmlState;
import org.onap.policy.pdpx.main.comm.XacmlPdpHearbeatPublisher;
import org.onap.policy.pdpx.main.comm.listeners.XacmlPdpStateChangeListener;
import org.onap.policy.pdpx.main.comm.listeners.XacmlPdpUpdateListener;
import org.onap.policy.pdpx.main.parameters.XacmlPdpParameterGroup;
import org.onap.policy.pdpx.main.rest.XacmlPdpAafFilter;
import org.onap.policy.pdpx.main.rest.XacmlPdpApplicationManager;
import org.onap.policy.pdpx.main.rest.XacmlPdpRestController;
import org.onap.policy.pdpx.main.rest.XacmlPdpStatisticsManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class wraps a distributor so that it can be activated as a complete service together with
 * all its xacml pdp and forwarding handlers.
 */
public class XacmlPdpActivator extends ServiceManagerContainer {

    // The logger for this class
    private static final Logger LOGGER = LoggerFactory.getLogger(XacmlPdpActivator.class);

    private static final String[] MSG_TYPE_NAMES = {"messageName"};
    private static final String TOPIC = "POLICY-PDP-PAP";

    @Getter
    @Setter
    private static XacmlPdpActivator current = null;
    private final XacmlPdpRestServer restServer;

    // The parameters of this policy xacml pdp activator
    private final XacmlPdpParameterGroup xacmlPdpParameterGroup;

    /**
     * Listens for messages on the topic, decodes them into a {@link PdpStatus} message, and then
     * dispatches them to appropriate listener.
     */
    private final MessageTypeDispatcher msgDispatcher;

    /**
     * Instantiate the activator for policy xacml pdp as a complete service.
     *
     * @param xacmlPdpParameterGroup the parameters for the xacml pdp service
     */
    public XacmlPdpActivator(final XacmlPdpParameterGroup xacmlPdpParameterGroup) {
        LOGGER.info("Activator initializing using {}", xacmlPdpParameterGroup);

        TopicEndpointManager.getManager().addTopics(xacmlPdpParameterGroup.getTopicParameterGroup());

        final XacmlPdpHearbeatPublisher heartbeat;
        final TopicSinkClient sinkClient;
        final XacmlState state;

        try {
            var appmgr =
                            new XacmlPdpApplicationManager(Paths.get(xacmlPdpParameterGroup.getApplicationPath()),
                                    xacmlPdpParameterGroup.getPolicyApiParameters());
            XacmlPdpApplicationManager.setCurrent(appmgr);

            var stats = new XacmlPdpStatisticsManager();
            XacmlPdpStatisticsManager.setCurrent(stats);
            stats.setTotalPolicyTypesCount(appmgr.getPolicyTypeCount());
            stats.setTotalPolicyCount(appmgr.getPolicyCount());

            state = new XacmlState(appmgr, xacmlPdpParameterGroup.getPdpGroup(), xacmlPdpParameterGroup.getPdpType());

            this.xacmlPdpParameterGroup = xacmlPdpParameterGroup;
            this.msgDispatcher = new MessageTypeDispatcher(MSG_TYPE_NAMES);

            sinkClient = new TopicSinkClient(TOPIC);
            heartbeat = new XacmlPdpHearbeatPublisher(sinkClient, state);

            /*
             * since the dispatcher isn't registered with the topic yet, we can go ahead
             * and register the listeners with it.
             */
            msgDispatcher.register(PdpMessageType.PDP_STATE_CHANGE.name(),
                            new XacmlPdpStateChangeListener(sinkClient, state));
            msgDispatcher.register(PdpMessageType.PDP_UPDATE.name(),
                            new XacmlPdpUpdateListener(sinkClient, state, heartbeat, appmgr));

            restServer = new XacmlPdpRestServer(xacmlPdpParameterGroup.getRestServerParameters(),
                    XacmlPdpAafFilter.class, XacmlPdpRestController.class);

        } catch (RuntimeException | TopicSinkClientException e) {
            throw new PolicyXacmlPdpRuntimeException(e.getMessage(), e);
        }

        xacmlPdpParameterGroup.getRestServerParameters().setName(xacmlPdpParameterGroup.getName());

        // @formatter:off
        addAction("XACML PDP parameters",
            () -> ParameterService.register(xacmlPdpParameterGroup),
            () -> ParameterService.deregister(xacmlPdpParameterGroup.getName()));

        addAction("Message Dispatcher",
            this::registerMsgDispatcher,
            this::unregisterMsgDispatcher);

        addAction("topics",
            TopicEndpointManager.getManager()::start,
            TopicEndpointManager.getManager()::shutdown);

        addAction("Terminate PDP",
            () -> { },
            () -> sendTerminateMessage(sinkClient, state));
        // initial heart beats act as registration messages
        addAction("Heartbeat Publisher",
            heartbeat::start,
            heartbeat::terminate);

        // @formatter:on
    }

    /*
     * Method used to send a terminate message to the PAP.
     */
    private void sendTerminateMessage(TopicSinkClient sinkClient, XacmlState state) {
        PdpStatus terminateStatus = state.terminatePdpMessage();
        sinkClient.send(terminateStatus);
    }

    /**
     * Get the parameters used by the activator.
     *
     * @return the parameters of the activator
     */
    public XacmlPdpParameterGroup getParameterGroup() {
        return xacmlPdpParameterGroup;
    }

    /**
     * Method to register the parameters to Common Parameter Service.
     *
     * @param xacmlPdpParameterGroup the xacml pdp parameter group
     */
    public void registerToParameterService(final XacmlPdpParameterGroup xacmlPdpParameterGroup) {
        ParameterService.register(xacmlPdpParameterGroup);
    }

    /**
     * Method to deregister the parameters from Common Parameter Service.
     *
     * @param xacmlPdpParameterGroup the xacml pdp parameter group
     */
    public void deregisterToParameterService(final XacmlPdpParameterGroup xacmlPdpParameterGroup) {
        ParameterService.deregister(xacmlPdpParameterGroup.getName());
    }

    /**
     * Registers the dispatcher with the topic source(s).
     */
    private void registerMsgDispatcher() {
        for (TopicSource source : TopicEndpointManager.getManager().getTopicSources(Arrays.asList(TOPIC))) {
            source.register(msgDispatcher);
        }
    }

    /**
     * Unregisters the dispatcher from the topic source(s).
     */
    private void unregisterMsgDispatcher() {
        for (TopicSource source : TopicEndpointManager.getManager().getTopicSources(Arrays.asList(TOPIC))) {
            source.unregister(msgDispatcher);
        }
    }

    /**
     * Start the xacmlpdp rest controller.
     */
    public void startXacmlRestController() {
        if (isXacmlRestControllerAlive()) {
            LOGGER.info("Xacml rest controller already running");
        } else {
            restServer.start();
        }
    }

    /**
     * Stop the xacmlpdp rest controller.
     */
    public void stopXacmlRestController() {
        if (isXacmlRestControllerAlive()) {
            restServer.stop();
        } else {
            LOGGER.info("Xacml rest controller already stopped");
        }
    }

    public boolean isXacmlRestControllerAlive() {
        return restServer.isAlive();
    }
}
