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
 * Class to hold all parameters needed for xacml pdp component.
 *
 */
public class XacmlPdpParameterGroup implements ParameterGroup {
    private String name;
    private RestServerParameters restServerParameters;

    /**
     * Create the xacml pdp parameter group.
     *
     * @param name the parameter group name
     */
    public XacmlPdpParameterGroup(final String name, final RestServerParameters restServerParameters) {
        this.name = name;
        this.restServerParameters = restServerParameters;
    }

    /**
     * Return the name of this parameter group instance.
     *
     * @return name the parameter group name
     */
    @Override
    public String getName() {
        return name;
    }

    /**
     * Set the name of this parameter group instance.
     *
     * @param name the parameter group name
     */
    @Override
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Return the restServerParameters of this parameter group instance.
     *
     * @return the restServerParameters
     */
    public RestServerParameters getRestServerParameters() {
        return restServerParameters;
    }

    /**
     * Validate the parameter group.
     *
     * @return the result of the validation
     */
    @Override
    public GroupValidationResult validate() {
        final GroupValidationResult validationResult = new GroupValidationResult(this);
        if (!ParameterValidationUtils.validateStringParameter(name)) {
            validationResult.setResult("name", ValidationStatus.INVALID, "must be a non-blank string");
        }
        if (restServerParameters == null) {
            validationResult.setResult("restServerParameters", ValidationStatus.INVALID,
                    "must have restServerParameters to configure xacml pdp rest server");
        } else {
            validationResult.setResult("restServerParameters", restServerParameters.validate());
        }
        return validationResult;
    }
}
