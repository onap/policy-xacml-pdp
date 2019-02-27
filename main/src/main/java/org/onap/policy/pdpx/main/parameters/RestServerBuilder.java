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

public class RestServerBuilder {
    private String host;
    private int port;
    private String userName;
    private String password;
    private boolean https;
    private boolean aaf;

    /**
     * Builder for instantiating RestServerParameters.
     *
     * @param host the host name
     * @param port the port
     * @param userName the user name
     * @param password the password
     * @param https the https flag
     * @param aaf the aaf flag
     */
    public RestServerBuilder(final String host, final int port, final String userName, final String password,
            final boolean https, final boolean aaf) {
        this.host = host;
        this.port = port;
        this.userName = userName;
        this.password = password;
        this.https = https;
        this.aaf = aaf;
    }

    public RestServerBuilder() {
        //empty constructor
    }

    public RestServerBuilder setHost(String host) {
        this.host = host;
        return this;
    }

    public RestServerBuilder setPort(int port) {
        this.port = port;
        return this;
    }

    public RestServerBuilder setUserName(String userName) {
        this.userName = userName;
        return this;
    }

    public RestServerBuilder setPassword(String password) {
        this.password = password;
        return this;
    }

    public RestServerBuilder isHttps(boolean https) {
        this.https = https;
        return this;
    }

    public RestServerBuilder isAaf(boolean aaf) {
        this.aaf = aaf;
        return this;
    }

    public RestServerParameters build() {
        return new RestServerParameters(host,port,userName,password,https,aaf);
    }
}
