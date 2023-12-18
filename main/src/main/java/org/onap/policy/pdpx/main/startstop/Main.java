/*-
 * ============LICENSE_START=======================================================
 * Copyright (C) 2019-2021 AT&T Intellectual Property. All rights reserved.
 * Modifications Copyright (C) 2019 Nordix Foundation.
 * Modifications Copyright (C) 2020 Bell Canada. All rights reserved.
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
import lombok.Getter;
import org.onap.policy.common.utils.resources.MessageConstants;
import org.onap.policy.pdpx.main.PolicyXacmlPdpException;
import org.onap.policy.pdpx.main.PolicyXacmlPdpRuntimeException;
import org.onap.policy.pdpx.main.parameters.XacmlPdpParameterGroup;
import org.onap.policy.pdpx.main.parameters.XacmlPdpParameterHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * This class initiates ONAP Policy Framework policy xacml pdp.
 *
 */
public class Main {
    private static final Logger LOGGER = LoggerFactory.getLogger(Main.class);

    // The policy xacml pdp Activator that activates the policy xacml pdp service
    private XacmlPdpActivator activator;

    @Getter
    private String argumentMessage;

    /**
     * Instantiates the policy xacml pdp service.
     *
     * @param args the command line arguments
     * @throws PolicyXacmlPdpException if an error occurs
     */
    public Main(final String[] args) throws PolicyXacmlPdpException {
        final var argumentString = Arrays.toString(args);
        LOGGER.info("Starting policy xacml pdp service with arguments - {}", argumentString);

        // Check the arguments
        final var arguments = new XacmlPdpCommandLineArguments();

        // The arguments return a string if there is a message to print and we should exit
        argumentMessage = arguments.parse(args);
        if (argumentMessage != null) {
            LOGGER.info(argumentMessage);
            return;
        }

        // Validate that the arguments are sane
        arguments.validate();

        // Read the parameters
        XacmlPdpParameterGroup parameterGroup = new XacmlPdpParameterHandler().getParameters(arguments);

        // Now, create the activator for the policy xacml pdp service
        activator = new XacmlPdpActivator(parameterGroup);
        XacmlPdpActivator.setCurrent(activator);

        // Start the activator
        activator.start();

        // Add a shutdown hook to shut everything down in an orderly manner
        Runtime.getRuntime().addShutdownHook(new Thread(this::shutdown));
        var successMsg = String.format(MessageConstants.START_SUCCESS_MSG, MessageConstants.POLICY_XACML_PDP);
        LOGGER.info(successMsg);
    }

    /**
     * Shut down Execution.
     *
     */
    public synchronized void shutdown() {
        // clear the xacml pdp activator
        if (activator != null && activator.isAlive()) {
            activator.stop();
            activator = null;
        }
    }

    /**
     * The main method. The NOSONAR is for security hotspot for checking
     * command line arguments. Since we use XacmlPdpCommandLineArguments
     * and validate the arguments the NOSONAR is ok to clear it.
     *
     * @param args the arguments
     */
    public static void main(final String[] args) { //NOSONAR
        try {
            new Main(args);
        } catch (RuntimeException | PolicyXacmlPdpException e) {
            throw new PolicyXacmlPdpRuntimeException(
                String.format(MessageConstants.START_FAILURE_MSG, MessageConstants.POLICY_XACML_PDP), e);
        }
    }
}
