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

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import org.onap.policy.common.endpoints.parameters.TopicParameters;
import org.onap.policy.common.parameters.ParameterGroup;
import org.onap.policy.common.utils.coder.Coder;
import org.onap.policy.common.utils.coder.CoderException;
import org.onap.policy.common.utils.coder.StandardCoder;

/**
 * Class to hold/create all parameters for test cases.
 *
 */
public class CommonTestData {

    private static final String REST_SERVER_PASSWORD = "zb!XztG34";
    private static final String REST_SERVER_USER = "healthcheck";
    private static final int REST_SERVER_PORT = 6969;
    private static final String REST_SERVER_HOST = "0.0.0.0";
    private static final boolean REST_SERVER_HTTPS = false;
    private static final boolean REST_SERVER_AAF = false;
    public static final String PDPX_GROUP_NAME = "XacmlPdpGroup";
    public static final List<TopicParameters> TOPIC_PARAMS = Arrays.asList(getTopicParams());

    public static final Coder coder = new StandardCoder();

    /**
     * Returns topic parameters for test cases.
     *
     * @return topic parameters
     */
    public static TopicParameters getTopicParams() {
        final TopicParameters topicParams = new TopicParameters();
        topicParams.setTopic("POLICY-PDP-PAP");
        topicParams.setTopicCommInfrastructure("dmaap");
        topicParams.setServers(Arrays.asList("message-router"));
        return topicParams;
    }

    /**
     * Returns a property map for a RestServerParameters map for test cases.
     *
     * @param isEmpty boolean value to represent that object created should be empty or not
     * @return a property map suitable for constructing an object
     */
    public Map<String, Object> getRestServerParametersMap(final boolean isEmpty) {
        final Map<String, Object> map = new TreeMap<>();
        map.put("https", REST_SERVER_HTTPS);
        map.put("aaf", REST_SERVER_AAF);

        if (!isEmpty) {
            map.put("host", REST_SERVER_HOST);
            map.put("port", REST_SERVER_PORT);
            map.put("userName", REST_SERVER_USER);
            map.put("password", REST_SERVER_PASSWORD);
        }

        return map;
    }

    /**
     * Returns a property map for a RestServerParameters map for test cases.
     *
     * @param port the port for RestServer
     * @return a property map suitable for constructing an object
     */
    public Map<String, Object> getRestServerParametersMap(final int port) {
        final Map<String, Object> map = new TreeMap<>();
        map.put("https", REST_SERVER_HTTPS);
        map.put("aaf", REST_SERVER_AAF);
        map.put("host", REST_SERVER_HOST);
        map.put("port", port);
        map.put("userName", REST_SERVER_USER);
        map.put("password", REST_SERVER_PASSWORD);

        return map;
    }

    /**
     * Converts the contents of a map to a parameter class.
     *
     * @param source property map
     * @param clazz class of object to be created from the map
     * @return a new object represented by the map
     */
    public <T extends ParameterGroup> T toObject(final Map<String, Object> source, final Class<T> clazz) {
        try {
            return coder.decode(coder.encode(source), clazz);

        } catch (final CoderException e) {
            throw new RuntimeException("cannot create " + clazz.getName() + " from map", e);
        }
    }

    /**
     * Returns a property map for a TopicParameters map for test cases.
     *
     * @param isEmpty boolean value to represent that object created should be empty or not
     * @return a property map suitable for constructing an object
     */
    public Map<String, Object> getTopicParametersMap(final boolean isEmpty) {
        final Map<String, Object> map = new TreeMap<>();
        if (!isEmpty) {
            map.put("topicSources", TOPIC_PARAMS);
            map.put("topicSinks", TOPIC_PARAMS);
        }
        return map;
    }
}
