/*-
 * ============LICENSE_START=======================================================
 * Copyright (C) 2019-2021 AT&T Intellectual Property. All rights reserved.
 * Modifications Copyright (C) 2021, 2023-2024 Nordix Foundation.
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

import jakarta.ws.rs.core.Response;
import java.net.HttpURLConnection;
import org.onap.policy.common.endpoints.http.client.HttpClient;
import org.onap.policy.models.tosca.authorative.concepts.ToscaConceptIdentifier;
import org.onap.policy.models.tosca.authorative.concepts.ToscaServiceTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Methods to access policy-api via REST service calls.
 */
public class PolicyApiCaller {
    private static final Logger logger = LoggerFactory.getLogger(PolicyApiCaller.class);

    private static final String POLICY_TYPE_URI = "/policy/api/v1/policytypes/";
    private static final String POLICY_TYPE_VERSION_URI = "/versions/";

    private final HttpClient httpClient;

    /**
     * Constructs the object.
     *
     * @param httpClient API REST client
     */
    public PolicyApiCaller(HttpClient httpClient) {
        this.httpClient = httpClient;
    }

    /**
     * Gets a policy type from policy-api.
     *
     * @param type policy type of interest
     * @return the desired policy type
     * @throws PolicyApiException if an error occurs
     */
    public ToscaServiceTemplate getPolicyType(ToscaConceptIdentifier type) throws PolicyApiException {

        try {
            Response resp = httpClient
                .get(POLICY_TYPE_URI + type.getName() + POLICY_TYPE_VERSION_URI + type.getVersion());

            return switch (resp.getStatus()) {
                case HttpURLConnection.HTTP_OK -> resp.readEntity(ToscaServiceTemplate.class);
                case HttpURLConnection.HTTP_NOT_FOUND -> {
                    logger.warn("policy-api not found {}", resp);
                    throw new NotFoundException(type.toString());
                }
                default -> {
                    logger.warn("policy-api request error {}", resp);
                    throw new PolicyApiException(type.toString());
                }
            };

        } catch (RuntimeException e) {
            logger.warn("policy-api connection error, client info: {} ", httpClient);
            throw new PolicyApiException(type.toString(), e);
        }
    }
}
