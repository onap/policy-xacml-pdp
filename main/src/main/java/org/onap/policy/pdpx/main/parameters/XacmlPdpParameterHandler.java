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

package org.onap.policy.pdpx.main.parameters;

import java.io.File;
import org.onap.policy.common.parameters.ValidationResult;
import org.onap.policy.common.parameters.rest.RestClientParameters;
import org.onap.policy.common.utils.coder.Coder;
import org.onap.policy.common.utils.coder.StandardCoder;
import org.onap.policy.pdpx.main.PolicyXacmlPdpException;
import org.onap.policy.pdpx.main.startstop.XacmlPdpCommandLineArguments;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * This class handles reading, parsing and validating of policy xacml pdp parameters from JSON
 * files.
 */
public class XacmlPdpParameterHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(XacmlPdpParameterHandler.class);
    private static final Coder CODER = new StandardCoder();

    /**
     * Read the parameters from the parameter file.
     *
     * @param arguments the arguments passed to policy xacml pdp
     * @return the parameters read from the configuration file
     * @throws PolicyXacmlPdpException on parameter exceptions
     */
    public XacmlPdpParameterGroup getParameters(final XacmlPdpCommandLineArguments arguments)
        throws PolicyXacmlPdpException {
        XacmlPdpParameterGroup xacmlPdpParameterGroup = null;

        try {
            // Read the parameters from JSON
            xacmlPdpParameterGroup = CODER.decode(new File(arguments.getFullConfigurationFilePath()),
                XacmlPdpParameterGroup.class);
        } catch (final Exception e) {
            final String errorMessage = "error reading parameters from \"" + arguments.getConfigurationFilePath()
                + "\"\n" + "(" + e.getClass().getSimpleName() + "):" + e.getMessage();
            LOGGER.error(errorMessage);
            throw new PolicyXacmlPdpException(errorMessage, e);
        }

        // The JSON processing returns null if there is an empty file
        if (xacmlPdpParameterGroup == null) {
            final String errorMessage = "no parameters found in \"" + arguments.getConfigurationFilePath() + "\"";
            LOGGER.error(errorMessage);
            throw new PolicyXacmlPdpException(errorMessage);
        }

        RestClientParameters apiClientParams = xacmlPdpParameterGroup.getPolicyApiParameters();
        apiClientParams.setName(XacmlPdpParameterGroup.PARAM_POLICY_API);
        apiClientParams.setManaged(false);

        // validate the parameters
        final ValidationResult validationResult = xacmlPdpParameterGroup.validate();
        if (!validationResult.isValid()) {
            String returnMessage =
                "validation error(s) on parameters from \"" + arguments.getConfigurationFilePath() + "\"\n";
            returnMessage += validationResult.getResult();

            LOGGER.error(returnMessage);
            throw new PolicyXacmlPdpException(returnMessage);
        }

        return xacmlPdpParameterGroup;
    }
}
