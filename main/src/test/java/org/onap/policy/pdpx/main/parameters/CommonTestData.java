/*-
 * ============LICENSE_START=======================================================
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

    /**
     * Returns an instance of RestServerParameters for test cases.
     *
     * @param isEmpty boolean value to represent that object created should be empty or not
     * @return the restServerParameters object
     */
    public RestServerParameters getRestServerParameters(final boolean isEmpty) {
        final RestServerParameters restServerParameters;
        if (!isEmpty) {
            restServerParameters = new RestServerBuilder().setHost(REST_SERVER_HOST).setPort(REST_SERVER_PORT)
                    .setUserName(REST_SERVER_USER).setPassword(REST_SERVER_PASSWORD)
                    .isHttps(REST_SERVER_HTTPS).isAaf(REST_SERVER_AAF).build();

        } else {
            restServerParameters = new RestServerBuilder().setHost(null).setPort(0).setUserName(null).setPassword(null)
                    .isHttps(REST_SERVER_HTTPS).isAaf(REST_SERVER_AAF).build();
        }
        return restServerParameters;
    }

}
