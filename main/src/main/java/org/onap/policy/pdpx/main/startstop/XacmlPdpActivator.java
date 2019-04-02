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

package org.onap.policy.pdpx.main.startstop;

import java.util.Arrays;
import java.util.Properties;
import org.onap.policy.common.endpoints.event.comm.TopicEndpoint;
import org.onap.policy.common.endpoints.event.comm.TopicSource;
import org.onap.policy.common.endpoints.event.comm.client.TopicSinkClient;
import org.onap.policy.common.endpoints.event.comm.client.TopicSinkClientException;
import org.onap.policy.common.endpoints.listeners.MessageTypeDispatcher;
import org.onap.policy.common.parameters.ParameterService;
import org.onap.policy.common.utils.services.ServiceManagerContainer;
import org.onap.policy.models.pdp.concepts.PdpStatus;
import org.onap.policy.models.pdp.concepts.PdpUpdate;
import org.onap.policy.models.pdp.enums.PdpMessageType;
import org.onap.policy.models.pdp.enums.PdpState;
import org.onap.policy.pdpx.main.PolicyXacmlPdpException;
import org.onap.policy.pdpx.main.PolicyXacmlPdpRuntimeException;
import org.onap.policy.pdpx.main.comm.XacmlPdpMessage;
import org.onap.policy.pdpx.main.comm.XacmlPdpPapRegistration;
import org.onap.policy.pdpx.main.comm.listeners.XacmlPdpStateChangeListener;
import org.onap.policy.pdpx.main.parameters.XacmlPdpParameterGroup;
import org.onap.policy.pdpx.main.rest.XacmlPdpRestServer;
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

    // The parameters of this policy xacml pdp activator
    private final XacmlPdpParameterGroup xacmlPdpParameterGroup;

    /**
     * The XACML PDP REST API server.
     */
    private XacmlPdpRestServer restServer;

    /**
     * Listens for messages on the topic, decodes them into a {@link PdpStatus} message, and then
     * dispatches them to {@link #pdpUpdateListener}.
     */
    private final MessageTypeDispatcher msgDispatcher;

    /**
     * Listens for {@link PdpUpdate} messages and then routes them to the listener associated with the
     * ID of the originating request.
     */
    private final XacmlPdpStateChangeListener pdpStateChangeListener;

    /**
     * The current activator.
     */
    private static XacmlPdpActivator current = null;

    private volatile boolean alive = false;

    private XacmlPdpPapRegistration register;

    private XacmlPdpMessage message;

    /**
     * Instantiate the activator for policy xacml pdp as a complete service.
     *
     * @param xacmlPdpParameterGroup the parameters for the xacml pdp service
     * @param topicProperties properties used to configure the topics
     */
    public XacmlPdpActivator(final XacmlPdpParameterGroup xacmlPdpParameterGroup, Properties topicProperties) {
        TopicEndpoint.manager.addTopicSinks(topicProperties);
        TopicEndpoint.manager.addTopicSources(topicProperties);

        try {
            TopicSinkClient sinkClient = new TopicSinkClient(TOPIC);
            this.xacmlPdpParameterGroup = xacmlPdpParameterGroup;
            this.msgDispatcher = new MessageTypeDispatcher(MSG_TYPE_NAMES);
            this.pdpStateChangeListener = new XacmlPdpStateChangeListener(sinkClient);
            this.register = new XacmlPdpPapRegistration(sinkClient);
            this.message = new XacmlPdpMessage();
        } catch (RuntimeException | TopicSinkClientException e) {
            throw new PolicyXacmlPdpRuntimeException(e.getMessage(), e);
        }

        xacmlPdpParameterGroup.getRestServerParameters().setName(xacmlPdpParameterGroup.getName());

        // @formatter:off
        addAction("XACML PDP parameters", () -> ParameterService.register(xacmlPdpParameterGroup),
            () -> ParameterService.deregister(xacmlPdpParameterGroup.getName()));

        addAction("Request ID Dispatcher",
            () -> msgDispatcher.register(PdpMessageType.PDP_STATE_CHANGE.name(), this.pdpStateChangeListener),
            () -> msgDispatcher.unregister(PdpMessageType.PDP_STATE_CHANGE.name()));

        addAction("Message Dispatcher",
            () -> registerMsgDispatcher(),
            () -> unregisterMsgDispatcher());

        addAction("topics",
            () -> TopicEndpoint.manager.start(),
            () -> TopicEndpoint.manager.shutdown());

        addAction("Create REST server",
            () -> {
                restServer = new XacmlPdpRestServer(xacmlPdpParameterGroup.getRestServerParameters(),
                        xacmlPdpParameterGroup.getApplicationPath());
            },
            () -> {
                restServer = null;
            });

        addAction("REST server",
            () -> restServer.start(),
            () -> restServer.stop());

        addAction("set alive", () -> setAlive(true), () -> setAlive(false));

        addAction("Initial Registration with PAP",
            () -> {
                register.pdpRegistration(message.formatStatusMessage(PdpState.PASSIVE));
            },
            () -> {
                register.pdpRegistration(message.formatStatusMessage(PdpState.TERMINATED));
            });
        // @formatter:on

        current = this;
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
        for (TopicSource source : TopicEndpoint.manager.getTopicSources(Arrays.asList(TOPIC))) {
            source.register(msgDispatcher);
        }
    }

    /**
     * Unregisters the dispatcher from the topic source(s).
     */
    private void unregisterMsgDispatcher() {
        for (TopicSource source : TopicEndpoint.manager.getTopicSources(Arrays.asList(TOPIC))) {
            source.unregister(msgDispatcher);
        }
    }

    /**
     * Returns the alive status of xacml pdp service.
     *
     * @return the alive
     */
    @Override
    public boolean isAlive() {
        return alive;
    }

    /**
     * Change the alive status of xacml pdp service.
     *
     * @param status the status
     */
    private void setAlive(final boolean status) {
        alive = status;
    }

    public static XacmlPdpActivator getCurrent() {
        return current;
    }

    public static void setCurrent(XacmlPdpActivator current) {
        XacmlPdpActivator.current = current;
    }
}
