/*-
 * ============LICENSE_START=======================================================
 * Copyright (C) 2018 Ericsson. All rights reserved.
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

import org.onap.policy.common.parameters.ParameterService;
import org.onap.policy.pdpx.main.PolicyXacmlPdpException;
import org.onap.policy.pdpx.main.parameters.XacmlPdpParameterGroup;
import org.onap.policy.pdpx.main.rest.XacmlPdpRestServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class wraps a distributor so that it can be activated as a complete service together with
 * all its xacml pdp and forwarding handlers.
 */
public class XacmlPdpActivator {
    // The logger for this class
    private static final Logger LOGGER = LoggerFactory.getLogger(XacmlPdpActivator.class);

    // The parameters of this policy xacml pdp activator
    private final XacmlPdpParameterGroup xacmlPdpParameterGroup;

    private static boolean alive = false;

    private XacmlPdpRestServer restServer;

    /**
     * Instantiate the activator for policy xacml pdp as a complete service.
     *
     * @param xacmlPdpParameterGroup the parameters for the xacml pdp service
     */
    public XacmlPdpActivator(final XacmlPdpParameterGroup xacmlPdpParameterGroup) {
        this.xacmlPdpParameterGroup = xacmlPdpParameterGroup;
    }

    /**
     * Initialize xacml pdp as a complete service.
     *
     * @throws PolicyXacmlPdpException on errors in initializing the service
     */
    public void initialize() throws PolicyXacmlPdpException {
        LOGGER.debug("Policy xacml pdp starting as a service . . .");
        startXacmlPdpRestServer();
        registerToParameterService(xacmlPdpParameterGroup);
        XacmlPdpActivator.setAlive(true);
        LOGGER.debug("Policy xacml pdp started as a service");
    }

    /**
     * Starts the xacml pdp rest server using configuration parameters.
     *
     * @throws PolicyXacmlPdpException if server start fails
     */
    private void startXacmlPdpRestServer() throws PolicyXacmlPdpException {
        xacmlPdpParameterGroup.getRestServerParameters().setName(xacmlPdpParameterGroup.getName());
        restServer = new XacmlPdpRestServer(xacmlPdpParameterGroup.getRestServerParameters());
        if (!restServer.start()) {
            throw new PolicyXacmlPdpException("Failed to start xacml pdp rest server. Check log for more details...");
        }
    }

    /**
     * Terminate policy xacml pdp.
     *
     * @throws PolicyXacmlPdpException on termination errors
     */
    public void terminate() throws PolicyXacmlPdpException {
        try {
            deregisterToParameterService(xacmlPdpParameterGroup);
            XacmlPdpActivator.setAlive(false);

            // Stop the xacml pdp rest server
            restServer.stop();
        } catch (final Exception exp) {
            LOGGER.error("Policy xacml pdp service termination failed", exp);
            throw new PolicyXacmlPdpException(exp.getMessage(), exp);
        }
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
     * Returns the alive status of xacml pdp service.
     *
     * @return the alive
     */
    public static boolean isAlive() {
        return alive;
    }

    /**
     * Change the alive status of xacml pdp service.
     *
     * @param status the status
     */
    public static void setAlive(final boolean status) {
        alive = status;
    }
}
