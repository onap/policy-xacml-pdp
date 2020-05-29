/*-
 * ============LICENSE_START=======================================================
 * Copyright (C) 2019-2020 AT&T Intellectual Property. All rights reserved.
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

package org.onap.policy.pdp.xacml.application.common;

import java.net.HttpURLConnection;
import javax.ws.rs.core.Response;
import org.onap.policy.common.endpoints.event.comm.bus.internal.BusTopicParams;
import org.onap.policy.common.endpoints.http.client.HttpClient;
import org.onap.policy.common.endpoints.http.client.HttpClientConfigException;
import org.onap.policy.common.endpoints.http.client.HttpClientFactoryInstance;
import org.onap.policy.common.endpoints.parameters.RestServerParameters;
import org.onap.policy.models.tosca.authorative.concepts.ToscaPolicyTypeIdentifier;
import org.onap.policy.models.tosca.authorative.concepts.ToscaServiceTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Methods to access policy-api via REST service calls.
 */
public class PolicyApiCaller {
    private static Logger logger = LoggerFactory.getLogger(PolicyApiCaller.class);

    private static final String POLICY_TYPE_URI = "/policy/api/v1/policytypes/";
    private static final String POLICY_TYPE_VERSION_URI = "/versions/";

    private final HttpClient httpClient;

    /**
     * Constructs the object.
     *
     * @param params target specification
     * @throws PolicyApiException if an error occurs
     */
    public PolicyApiCaller(RestServerParameters params) throws PolicyApiException {
        BusTopicParams busParams = new BusTopicParams();
        busParams.setClientName("policy-api");
        busParams.setHostname(params.getHost());
        busParams.setManaged(false);
        busParams.setPassword(params.getPassword());
        busParams.setPort(params.getPort());
        busParams.setUseHttps(params.isHttps());
        busParams.setUserName(params.getUserName());

        try {
            httpClient = makeClient(busParams);
        } catch (HttpClientConfigException e) {
            throw new PolicyApiException("connection to host: " + busParams.getHostname(), e);
        }
    }

    /**
     * Gets a policy type from policy-api.
     *
     * @param type policy type of interest
     * @return the desired policy type
     * @throws PolicyApiException if an error occurs
     */
    public ToscaServiceTemplate getPolicyType(ToscaPolicyTypeIdentifier type) throws PolicyApiException {

        try {
            Response resp = httpClient
                            .get(POLICY_TYPE_URI + type.getName() + POLICY_TYPE_VERSION_URI + type.getVersion());

            switch (resp.getStatus()) {
                case HttpURLConnection.HTTP_OK:
                    return resp.readEntity(ToscaServiceTemplate.class);
                case HttpURLConnection.HTTP_NOT_FOUND:
                    logger.warn("policy-api not found {}", resp);
                    throw new NotFoundException(type.toString());
                default:
                    logger.warn("policy-api request error {}", resp);
                    throw new PolicyApiException(type.toString());
            }

        } catch (RuntimeException e) {
            logger.warn("policy-api connection error");
            throw new PolicyApiException(type.toString(), e);
        }
    }

    // these methods may be overridden by junit tests

    protected HttpClient makeClient(BusTopicParams busParams) throws HttpClientConfigException {
        return HttpClientFactoryInstance.getClientFactory().build(busParams);
    }
}
