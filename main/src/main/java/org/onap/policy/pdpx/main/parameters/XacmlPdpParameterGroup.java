/*-
 * ============LICENSE_START=======================================================
 * Copyright (C) 2019, 2021 AT&T Intellectual Property. All rights reserved.
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

import lombok.Getter;
import lombok.Setter;
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
@Getter
public class XacmlPdpParameterGroup implements ParameterGroup {
    private static final String PARAM_REST_SERVER = "restServerParameters";
    private static final String PARAM_POLICY_API = "policyApiParameters";
    private static final String PARAM_TOPIC_PARAMETER_GROUP = "topicParameterGroup";
    private static final String PARAM_APPLICATION_PATH = "applicationPath";

    @Setter
    private String name;

    private String pdpGroup;
    private String pdpType;
    private RestServerParameters restServerParameters;
    private RestServerParameters policyApiParameters;
    private TopicParameterGroup topicParameterGroup;
    private String applicationPath;

    /**
     * Create the xacml pdp parameter group.
     *
     * @param name the parameter group name
     * @param pdpGroup the pdp group name
     */
    public XacmlPdpParameterGroup(final String name, final String pdpGroup, final String pdpType,
            final RestServerParameters restServerParameters, final RestServerParameters policyApiParameters,
            final TopicParameterGroup topicParameterGroup, final String applicationPath) {
        this.name = name;
        this.pdpGroup = pdpGroup;
        this.pdpType = pdpType;
        this.restServerParameters = restServerParameters;
        this.policyApiParameters = policyApiParameters;
        this.topicParameterGroup = topicParameterGroup;
        this.applicationPath = applicationPath;
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
        if (!ParameterValidationUtils.validateStringParameter(pdpGroup)) {
            validationResult.setResult("pdpGroup", ValidationStatus.INVALID, "must be a non-blank string");
        }
        if (!ParameterValidationUtils.validateStringParameter(pdpType)) {
            validationResult.setResult("pdpType", ValidationStatus.INVALID, "must be a non-blank string");
        }
        if (restServerParameters == null) {
            validationResult.setResult(PARAM_REST_SERVER, ValidationStatus.INVALID,
                    "must have restServerParameters to configure xacml pdp rest server");
        } else {
            validationResult.setResult(PARAM_REST_SERVER, restServerParameters.validate());
        }
        if (policyApiParameters == null) {
            validationResult.setResult(PARAM_POLICY_API, ValidationStatus.INVALID,
                    "must have policyApiParameters to configure xacml pdp rest server");
        } else {
            // set the name - this only really matters for validation messages
            policyApiParameters.setName(PARAM_POLICY_API);
            validationResult.setResult(PARAM_POLICY_API, policyApiParameters.validate());
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
