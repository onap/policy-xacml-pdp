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

package org.onap.policy.pdp.xacml.application.common;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.onap.policy.common.utils.coder.CoderException;
import org.onap.policy.common.utils.coder.StandardCoder;
import org.onap.policy.common.utils.resources.ResourceUtils;
import org.onap.policy.models.tosca.authorative.concepts.ToscaPolicy;
import org.onap.policy.models.tosca.authorative.concepts.ToscaServiceTemplate;
import org.onap.policy.models.tosca.simple.concepts.JpaToscaServiceTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yaml.snakeyaml.Yaml;

public class TestUtils {
    private static final Logger LOGGER = LoggerFactory.getLogger(TestUtils.class);
    private static final StandardCoder standardCoder = new StandardCoder();

    private TestUtils() {
        super();
    }

    /**
     * Load the policies from a resource file into the given the application.
     *
     * @param resourceFile resource file
     * @param service XacmlApplicationServiceProvider
     * @throws CoderException exception if it cannot be decoded
     * @throws XacmlApplicationException If the application cannot load the policy
     */
    public static List<ToscaPolicy> loadPolicies(String resourceFile, XacmlApplicationServiceProvider service)
            throws CoderException, XacmlApplicationException {
        //
        // Our return object
        //
        List<ToscaPolicy> loadedPolicies = new ArrayList<>();
        //
        // Decode it
        //
        String policyYaml = ResourceUtils.getResourceAsString(resourceFile);
        Yaml yaml = new Yaml();
        Object yamlObject = yaml.load(policyYaml);
        String yamlAsJsonString = standardCoder.encode(yamlObject);
        //
        // Serialize it into a class
        //
        ToscaServiceTemplate serviceTemplate = standardCoder.decode(yamlAsJsonString, ToscaServiceTemplate.class);
        //
        // Make sure all the fields are setup properly
        //
        JpaToscaServiceTemplate jtst = new JpaToscaServiceTemplate();
        jtst.fromAuthorative(serviceTemplate);
        ToscaServiceTemplate completedJtst = jtst.toAuthorative();
        //
        // Get the policies
        //
        for (Map<String, ToscaPolicy> policies : completedJtst.getToscaTopologyTemplate().getPolicies()) {
            for (ToscaPolicy policy : policies.values()) {
                if (service.loadPolicy(policy)) {
                    loadedPolicies.add(policy);
                } else {
                    LOGGER.error("Application failed to load policy");
                }
            }
        }
        return loadedPolicies;
    }

}
