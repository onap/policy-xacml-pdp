/*-
 * ============LICENSE_START=======================================================
 * Copyright (C) 2019 AT&T Intellectual Property. All rights reserved.
 * Modifications Copyright (C) 2019 Nordix Foundation.
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

import org.onap.policy.common.endpoints.parameters.RestServerParameters;
import org.onap.policy.common.endpoints.parameters.TopicParameterGroup;
import org.onap.policy.common.parameters.GroupValidationResult;
import org.onap.policy.common.parameters.ParameterGroup;
import org.onap.policy.common.parameters.ValidationStatus;
import org.onap.policy.common.utils.validation.ParameterValidationUtils;

/**
 * Class to hold all parameters needed for xacml pdp component.
 *
 */
public class XacmlPdpParameterGroup implements ParameterGroup {
    private static final String PARAM_REST_SERVER = "restServerParameters";
    private static final String PARAM_TOPIC_PARAMETER_GROUP = "topicParameterGroup";
    private static final String PARAM_APPLICATION_PATH = "applicationPath";
    private String name;
    private RestServerParameters restServerParameters;
    private TopicParameterGroup topicParameterGroup;
    private String applicationPath;

    /**
     * Create the xacml pdp parameter group.
     *
     * @param name the parameter group name
     */
    public XacmlPdpParameterGroup(final String name, final RestServerParameters restServerParameters,
            final TopicParameterGroup topicParameterGroup, final String applicationPath) {
        this.name = name;
        this.restServerParameters = restServerParameters;
        this.topicParameterGroup = topicParameterGroup;
        this.applicationPath = applicationPath;
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
     * Return the topicParameterGroup of this parameter group instance.
     *
     * @return the topicParameterGroup
     */
    public TopicParameterGroup getTopicParameterGroup() {
        return topicParameterGroup;
    }

    /**
     * Returns the path where applications will store their data.
     *
     * @return String to the path
     */
    public String getApplicationPath() {
        return applicationPath;
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
            validationResult.setResult(PARAM_REST_SERVER, ValidationStatus.INVALID,
                    "must have restServerParameters to configure xacml pdp rest server");
        } else {
            validationResult.setResult(PARAM_REST_SERVER, restServerParameters.validate());
        }
        if (topicParameterGroup == null) {
            validationResult.setResult(PARAM_TOPIC_PARAMETER_GROUP, ValidationStatus.INVALID,
                    "must have topicParameterGroup to configure xacml pdp topic sink and source");
        } else {
            validationResult.setResult(PARAM_TOPIC_PARAMETER_GROUP, topicParameterGroup.validate());
        }
        //
        // Validate the application path directory
        //
        if (applicationPath == null || applicationPath.isEmpty()) {
            validationResult.setResult(PARAM_APPLICATION_PATH, ValidationStatus.INVALID,
                    "must have application path for applications to store policies and data.");
        }
        return validationResult;
    }
}
