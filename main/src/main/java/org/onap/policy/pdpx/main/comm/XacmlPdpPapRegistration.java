/*-
 * ============LICENSE_START=======================================================
 * ONAP
 * ================================================================================
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

package org.onap.policy.pdpx.main.comm;

import org.onap.policy.common.endpoints.event.comm.client.TopicSinkClient;
import org.onap.policy.common.endpoints.event.comm.client.TopicSinkClientException;
import org.onap.policy.models.pdp.concepts.PdpStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class XacmlPdpPapRegistration {

    private static final Logger LOGGER = LoggerFactory.getLogger(XacmlPdpPapRegistration.class);
    private final TopicSinkClient client;

    /**
     * Constructs the object.
     * @param client name of the TopickSinkClient
     */
    public XacmlPdpPapRegistration(TopicSinkClient client) {
        this.client = client;
    }

    /**
     * Sends PDP register and unregister message to the PAP.
     * @param status of the PDP
     * @throws TopicSinkClientException if the topic sink does not exist
     */
    public void pdpRegistration(PdpStatus status) throws TopicSinkClientException {
        try {
            if (!client.send(status)) {
                LOGGER.error("Failed to send to topic sink " + client.getTopic());
                return;
            }
        } catch (IllegalStateException e) {
            LOGGER.error("Failed ot send to topic sink " + client.getTopic(), e);
            return;
        }
    }
}
