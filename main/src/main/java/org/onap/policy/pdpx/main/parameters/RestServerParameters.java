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

package org.onap.policy.pdpx.main.parameters;

import org.onap.policy.common.parameters.GroupValidationResult;
import org.onap.policy.common.parameters.ParameterGroup;
import org.onap.policy.common.parameters.ValidationStatus;
import org.onap.policy.common.utils.validation.ParameterValidationUtils;

/**
 * Class to hold all parameters needed for xacml pdp rest server.
 *
 */
public class RestServerParameters implements ParameterGroup {
    private String name;
    private String host;
    private int port;
    private String userName;
    private String password;

    /**
     * Constructor for instantiating RestServerParameters.
     *
     * @param host the host name
     * @param port the port
     * @param userName the user name
     * @param password the password
     */
    public RestServerParameters(final String host, final int port, final String userName, final String password) {
        super();
        this.host = host;
        this.port = port;
        this.userName = userName;
        this.password = password;
    }

    /**
     * Return the name of this RestServerParameters instance.
     *
     * @return name the name of this RestServerParameters
     */
    @Override
    public String getName() {
        return name;
    }

    /**
     * Return the host of this RestServerParameters instance.
     *
     * @return the host
     */
    public String getHost() {
        return host;
    }

    /**
     * Return the port of this RestServerParameters instance.
     *
     * @return the port
     */
    public int getPort() {
        return port;
    }

    /**
     * Return the user name of this RestServerParameters instance.
     *
     * @return the userName
     */
    public String getUserName() {
        return userName;
    }

    /**
     * Return the password of this RestServerParameters instance.
     *
     * @return the password
     */
    public String getPassword() {
        return password;
    }

    /**
     * Set the name of this RestServerParameters instance.
     *
     * @param name the name to set
     */
    @Override
    public void setName(final String name) {
        this.name = name;
    }

    /**
     * Validate the rest server parameters.
     *
     * @return the result of the validation
     */
    @Override
    public GroupValidationResult validate() {
        final GroupValidationResult validationResult = new GroupValidationResult(this);
        if (!ParameterValidationUtils.validateStringParameter(host)) {
            validationResult.setResult("host", ValidationStatus.INVALID,
                    "must be a non-blank string containing hostname/ipaddress of the xacml pdp rest server");
        }
        if (!ParameterValidationUtils.validateStringParameter(userName)) {
            validationResult.setResult("userName", ValidationStatus.INVALID,
                    "must be a non-blank string containing userName for xacml pdp rest server credentials");
        }
        if (!ParameterValidationUtils.validateStringParameter(password)) {
            validationResult.setResult("password", ValidationStatus.INVALID,
                    "must be a non-blank string containing password for xacml pdp rest server credentials");
        }
        if (!ParameterValidationUtils.validateIntParameter(port)) {
            validationResult.setResult("port", ValidationStatus.INVALID,
                    "must be a positive integer containing port of the xacml pdp rest server");
        }
        return validationResult;
    }
}
