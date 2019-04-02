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

    public String getHost() {
        return host;
    }

    public int getPort() {
        return port;
    }

    public String getUserName() {
        return userName;
    }

    public String getPassword() {
        return password;
    }

    public boolean isHttps() {
        return https;
    }

    public boolean isAaf() {
        return aaf;
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

    public RestServerBuilder setHttps(boolean https) {
        this.https = https;
        return this;
    }

    public RestServerBuilder setAaf(boolean aaf) {
        this.aaf = aaf;
        return this;
    }
}
