/*-
 * ============LICENSE_START=======================================================
 * Copyright (C) 2019, 2021 AT&T Intellectual Property. All rights reserved.
 * Modifications Copyright (C) 2019, 2024 Nordix Foundation.
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
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.onap.policy.common.parameters.BeanValidationResult;
import org.onap.policy.common.parameters.ParameterGroupImpl;
import org.onap.policy.common.parameters.ValidationStatus;
import org.onap.policy.common.parameters.annotations.Min;
import org.onap.policy.common.parameters.annotations.NotBlank;
import org.onap.policy.common.parameters.annotations.NotNull;
import org.onap.policy.common.parameters.annotations.Valid;
import org.onap.policy.common.parameters.rest.RestClientParameters;
import org.onap.policy.common.parameters.rest.RestServerParameters;
import org.onap.policy.common.parameters.topic.TopicParameterGroup;
import org.onap.policy.models.base.Validated;

/**
 * Class to hold all parameters needed for xacml pdp component.
 */
@Getter
@NotNull
@NotBlank
@NoArgsConstructor
public class XacmlPdpParameterGroup extends ParameterGroupImpl {
    public static final String PARAM_POLICY_API = "policyApiParameters";

    private String pdpGroup;
    private String pdpType;
    @Valid
    private RestServerParameters restServerParameters;
    @Valid
    private RestClientParameters policyApiParameters;
    @Valid
    private TopicParameterGroup topicParameterGroup;
    @Valid
    private XacmlApplicationParameters applicationParameters;
    /**
     * Frequency, in seconds, with which to probe the heartbeat topic before sending the
     * first heartbeat. Set to zero to disable probing.
     */
    @Min(0)
    private long probeHeartbeatTopicSec = 4;


    /**
     * Create the xacml pdp parameter group.
     *
     * @param name     the parameter group name
     * @param pdpGroup the pdp group name
     */
    public XacmlPdpParameterGroup(final String name, final String pdpGroup, final String pdpType,
                                  final RestServerParameters restServerParameters,
                                  final RestClientParameters policyApiParameters,
                                  final TopicParameterGroup topicParameterGroup,
                                  final XacmlApplicationParameters applicationParameters) {
        super(name);
        this.pdpGroup = pdpGroup;
        this.pdpType = pdpType;
        this.restServerParameters = restServerParameters;
        this.policyApiParameters = policyApiParameters;
        this.topicParameterGroup = topicParameterGroup;
        this.applicationParameters = applicationParameters;
    }

    /**
     * Validate the parameter group.
     *
     * @return the result of the validation
     */
    @Override
    public BeanValidationResult validate() {
        final BeanValidationResult validationResult = super.validate();

        if (policyApiParameters != null && StringUtils.isBlank(policyApiParameters.getHostname())) {
            var sub = new BeanValidationResult(PARAM_POLICY_API, policyApiParameters);
            sub.addResult("hostname", policyApiParameters.getHostname(), ValidationStatus.INVALID, Validated.IS_NULL);
            validationResult.addResult(sub);
        }

        return validationResult;
    }
}
