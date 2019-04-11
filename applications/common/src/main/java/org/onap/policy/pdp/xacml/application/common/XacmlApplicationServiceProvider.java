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

import org.onap.policy.models.decisions.concepts.DecisionRequest;
import org.onap.policy.models.decisions.concepts.DecisionResponse;
import org.onap.policy.models.tosca.authorative.concepts.ToscaPolicy;
import org.onap.policy.models.tosca.authorative.concepts.ToscaPolicyTypeIdentifier;

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
    String           applicationName();

    /**
     * Returns a list of action decisions supported by the application.
     *
     * @return List of String (eg. "configure", "placement", "naming")
     */
    List<String>     actionDecisionsSupported();

    /**
     * Initializes the application and gives it a Path for storing its
     * data. The Path may be already populated with previous data.
     *
     * @param pathForData Local Path
     */
    void             initialize(Path pathForData) throws XacmlApplicationException;

    /**
     * Returns a list of supported Tosca Policy Types.
     *
     * @return List of Strings (eg. "onap.policy.foo.bar")
     */
    List<ToscaPolicyTypeIdentifier>     supportedPolicyTypes();

    /**
     * Asks whether the application can support the incoming
     * Tosca Policy Type and version.
     *
     * @param toscaPolicyId Identifier for policy type
     * @return true if supported
     */
    boolean          canSupportPolicyType(ToscaPolicyTypeIdentifier toscaPolicyId);

    /**
     * Load a Tosca Policy.
     *
     * @param toscaPolicy object
     */
    void             loadPolicy(ToscaPolicy toscaPolicy) throws XacmlApplicationException;

    /**
     * Undeploy a Tosca Policy.
     *
     * @param toscaPolicy object
     */
    void             undeployPolicy(ToscaPolicy toscaPolicy) throws XacmlApplicationException;

    /**
     * Undeploy a Tosca Policy.
     *
     * @param toscaPolicy object
     */
    void             unDeployPolicy(ToscaPolicy toscaPolicy) throws XacmlApplicationException;

    /**
     * Makes a decision given the incoming request and returns a response.
     *
     * @param request Incoming DecisionRequest object
     * @return response Responding DecisionResponse object
     */
    DecisionResponse       makeDecision(DecisionRequest request);

}
