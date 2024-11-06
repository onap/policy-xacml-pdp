/*-
 * ============LICENSE_START=======================================================
 * Copyright (C) 2019, 2021 AT&T Intellectual Property. All rights reserved.
 * Modifications Copyright (C) 2019, 2023-2024 Nordix Foundation.
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

import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import org.onap.policy.common.parameters.topic.TopicParameters;
import org.onap.policy.common.utils.coder.Coder;
import org.onap.policy.common.utils.coder.CoderException;
import org.onap.policy.common.utils.coder.StandardCoder;

/**
 * Class to hold/create all parameters for test cases.
 */
public class CommonTestData {

    private static final String CLIENT_NAME = "clientName";
    private static final String PASS_KEY = "password";
    private static final String USER_KEY = "userName";
    private static final String PORT_KEY = "port";
    private static final String SERVER_HOST_KEY = "host";
    private static final String API_HOST_KEY = "hostname";
    private static final String HTTPS_KEY = "useHttps";

    private static final String REST_SERVER_PASS = "zb!XztG34";
    private static final String REST_SERVER_USER = "healthcheck";
    private static final int REST_SERVER_PORT = 6969;
    private static final String REST_SERVER_HOST = "0.0.0.0";
    private static final boolean REST_SERVER_HTTPS = false;
    private static final String POLICY_API_PASS = "zb!XztG34";
    private static final String POLICY_API_USER = "healthcheck";
    private static final int POLICY_API_PORT = 6970;
    private static final String POLICY_API_HOST = "0.0.0.0";
    private static final boolean POLICY_API_HTTPS = false;
    public static final String PDPX_PARAMETER_GROUP_NAME = "XacmlPdpParameters";
    public static final String PDPX_GROUP = "XacmlPdpGroup";
    public static final String PDPX_TYPE = "xacml";
    public static final List<TopicParameters> TOPIC_PARAMS = List.of(getTopicParams());

    public static final Coder coder = new StandardCoder();

    /**
     * Returns topic parameters for test cases.
     *
     * @return topic parameters
     */
    public static TopicParameters getTopicParams() {
        final TopicParameters topicParams = new TopicParameters();
        topicParams.setTopic("POLICY-PDP-PAP");
        topicParams.setTopicCommInfrastructure("noop");
        topicParams.setServers(List.of("message-router"));
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
        map.put(HTTPS_KEY, REST_SERVER_HTTPS);

        if (!isEmpty) {
            map.put(SERVER_HOST_KEY, REST_SERVER_HOST);
            map.put(PORT_KEY, REST_SERVER_PORT);
            map.put(USER_KEY, REST_SERVER_USER);
            map.put(PASS_KEY, REST_SERVER_PASS);
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
        map.put(HTTPS_KEY, REST_SERVER_HTTPS);
        map.put(SERVER_HOST_KEY, REST_SERVER_HOST);
        map.put(PORT_KEY, port);
        map.put(USER_KEY, REST_SERVER_USER);
        map.put(PASS_KEY, REST_SERVER_PASS);

        return map;
    }

    /**
     * Converts the contents of a map to a parameter class.
     *
     * @param source property map
     * @param clazz  class of object to be created from the map
     * @return a new object represented by the map
     */
    public <T> T toObject(final Map<String, Object> source, final Class<T> clazz) {
        try {
            return coder.decode(coder.encode(source), clazz);

        } catch (final CoderException e) {
            throw new RuntimeException("cannot create " + clazz.getName() + " from map", e);
        }
    }

    /**
     * Returns a property map for a RestServerParameters map for test cases.
     *
     * @param isEmpty boolean value to represent that object created should be empty or not
     * @return a property map suitable for constructing an object
     */
    public Map<String, Object> getPolicyApiParametersMap(final boolean isEmpty) {
        final Map<String, Object> map = new TreeMap<>();
        map.put(CLIENT_NAME, XacmlPdpParameterGroup.PARAM_POLICY_API);
        map.put(HTTPS_KEY, POLICY_API_HTTPS);

        if (!isEmpty) {
            map.put(API_HOST_KEY, POLICY_API_HOST);
            map.put(PORT_KEY, POLICY_API_PORT);
            map.put(USER_KEY, POLICY_API_USER);
            map.put(PASS_KEY, POLICY_API_PASS);
        }

        return map;
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

    /**
     * Returns a property map for a XacmlApplicationParameters map for test cases.
     *
     * @param isEmpty    boolean value to represent that object created should be empty or not
     * @param tempPath   Application Path string
     * @param exclusions An optional list of application classnames for exclusion
     * @return a property map suitable for constructing an object
     */
    public Map<String, Object> getXacmlapplicationParametersMap(boolean isEmpty, String tempPath,
                                                                String... exclusions) {
        final Map<String, Object> map = new TreeMap<>();
        if (!isEmpty) {
            map.put("applicationPath", tempPath);
            if (exclusions != null) {
                map.put("exclusions", List.of(exclusions));
            }
        }
        return map;
    }
}
