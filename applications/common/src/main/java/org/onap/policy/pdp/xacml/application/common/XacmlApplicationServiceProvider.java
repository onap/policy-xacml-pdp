/* ============LICENSE_START=======================================================
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

package org.onap.policy.pdp.xacml.application.common;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import org.json.JSONObject;

/**
 * This interface is how the XACML REST controller can communicate
 * with Policy Type implementation applications.
 * Applications should register themselves as this service provider
 * and implement these methods.
 *
 * @author pameladragosh
 *
 */
public interface XacmlApplicationServiceProvider {

    /**
     * Name of the application for auditing and organization of its data.
     *
     * @return String
     */
    public String           applicationName();

    /**
     * Returns a list of action decisions supported by the application.
     *
     * @return List of String (eg. "configure", "placement", "naming")
     */
    public List<String>     actionDecisionsSupported();

    /**
     * Initializes the application and gives it a Path for storing its
     * data. The Path may be already populated with previous data.
     *
     * @param pathForData Local Path
     */
    public void             initialize(Path pathForData);

    /**
     * Returns a list of supported Tosca Policy Types.
     *
     * @return List of Strings (eg. "onap.policy.foo.bar")
     */
    public List<String>     supportedPolicyTypes();

    /**
     * Asks whether the application can support the incoming
     * Tosca Policy Type and version.
     *
     * @param policyType String Tosca Policy Type
     * @param policyTypeVersion String of the Tosca Policy Type version
     * @return true if supported
     */
    public boolean          canSupportPolicyType(String policyType, String policyTypeVersion);

    /**
     * Load a Map representation of a Tosca Policy.
     *
     * @param toscaPolicies Map of Tosca Policy Objects
     */
    public void             loadPolicies(Map<String, Object> toscaPolicies);

    /**
     * Makes a decision given the incoming request and returns a response.
     *
     * <P>NOTE: I may want to change this to an object that represents the
     * schema.
     *
     * @param jsonSchema Incoming Json
     * @return response
     */
    public JSONObject       makeDecision(JSONObject jsonSchema);

}
