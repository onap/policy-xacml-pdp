/*-
 * ============LICENSE_START=======================================================
 * Copyright (C) 2020-2022 AT&T Intellectual Property. All rights reserved.
 * Modifications Copyright (C) 2024 Nordix Foundation.
 * Modifications Copyright (C) 2024 Deutsche Telekom AG.
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

package org.onap.policy.pdpx.main.rest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Map;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.onap.policy.common.utils.coder.CoderException;
import org.onap.policy.common.utils.coder.StandardYamlCoder;
import org.onap.policy.common.utils.resources.ResourceUtils;
import org.onap.policy.models.decisions.concepts.DecisionRequest;
import org.onap.policy.models.tosca.authorative.concepts.ToscaPolicy;
import org.onap.policy.models.tosca.authorative.concepts.ToscaServiceTemplate;
import org.onap.policy.models.tosca.simple.concepts.JpaToscaServiceTemplate;
import org.onap.policy.pdp.xacml.application.common.XacmlApplicationException;
import org.onap.policy.pdpx.main.parameters.CommonTestData;
import org.onap.policy.pdpx.main.parameters.XacmlApplicationParameters;
import org.onap.policy.xacml.pdp.application.nativ.NativePdpApplication;
import org.onap.policy.xacml.pdp.application.optimization.OptimizationPdpApplication;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class XacmlPdpApplicationManagerTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(XacmlPdpApplicationManagerTest.class);
    private static final StandardYamlCoder yamlCoder = new StandardYamlCoder();
    private static Path appsDirectory;
    private static ToscaServiceTemplate completedJtst;
    private static final CommonTestData testData = new CommonTestData();

    @TempDir
    static Path appsFolder;

    /**
     * setupTestEnvironment.
     *
     * @throws Exception Exception if anything is missing
     */
    @BeforeAll
    static void setupTestEnvironment() throws Exception {
        //
        // Load an example policy
        //
        String policyYaml = ResourceUtils
            .getResourceAsString("../applications/monitoring/src/test/resources/vDNS.policy.input.yaml");
        //
        // Serialize it into a class
        //
        ToscaServiceTemplate serviceTemplate;
        try {
            serviceTemplate = yamlCoder.decode(policyYaml, ToscaServiceTemplate.class);
        } catch (CoderException e) {
            throw new XacmlApplicationException("Failed to decode policy from resource file", e);
        }
        //
        // Make sure all the fields are set up properly
        //
        JpaToscaServiceTemplate jtst = new JpaToscaServiceTemplate();
        jtst.fromAuthorative(serviceTemplate);
        completedJtst = jtst.toAuthorative();
        //
        // We need at least 1 policy
        //
        assertThat(completedJtst).isNotNull();
        assertThat(completedJtst.getToscaTopologyTemplate().getPolicies()).isNotEmpty();
        //
        // Copy test directory over of the application directories
        //
        Path src = Paths.get("src/test/resources/apps");
        File apps = appsFolder.resolve("apps").toFile();
        Files.walk(src).forEach(source -> copy(source, apps.toPath().resolve(src.relativize(source))));
        appsDirectory = apps.toPath();
    }

    @Test
    void testXacmlPdpApplicationManagerBadPath() throws Exception {
        //
        // Make up a non-existent directory to initialize from
        //
        Path nonExistentPath = Paths.get(appsFolder.toFile().getAbsolutePath(), "nonexistent");
        final XacmlApplicationParameters xacmlApplicationParameters =
            testData.toObject(testData.getXacmlapplicationParametersMap(false,
                nonExistentPath.toString()), XacmlApplicationParameters.class);
        //
        // Create our app manager
        //
        XacmlPdpApplicationManager manager = new XacmlPdpApplicationManager(xacmlApplicationParameters, null);
        //
        // Still creates the manager, but the apps were not able to initialize
        //
        assertThat(manager).isNotNull();
        assertThat(manager.findNativeApplication()).isNull();
        //
        // Now create the directory
        //
        Files.createDirectory(nonExistentPath);
        manager = new XacmlPdpApplicationManager(xacmlApplicationParameters, null);
        //
        // Now it should have initialized the apps
        //
        assertThat(manager).isNotNull();
        assertThat(manager.findNativeApplication()).isNull();
    }

    @Test
    void testXacmlPdpApplicationManagerSimple() {
        final String[] exclusions = {"org.onap.policy.xacml.pdp.application.guard.GuardPdpApplication",
            "org.onap.policy.xacml.pdp.application.match.MatchPdpApplication"};
        final XacmlApplicationParameters xacmlApplicationParameters =
            testData.toObject(testData.getXacmlapplicationParametersMap(false,
                appsDirectory.toString(), exclusions), XacmlApplicationParameters.class);
        XacmlPdpApplicationManager manager = new XacmlPdpApplicationManager(xacmlApplicationParameters, null);
        //
        // Test the basics from the startup
        //
        assertThat(manager).isNotNull();
        assertThat(manager.getPolicyCount()).isZero();
        assertThat(manager.getPolicyTypeCount()).isEqualTo(19);
        assertThat(manager.getToscaPolicies()).isEmpty();
        assertThat(manager.getToscaPolicyIdentifiers()).isEmpty();
        assertThat(manager.getToscaPolicyTypeIdents()).hasSize(19);

        assertThat(manager.findNativeApplication()).isInstanceOf(NativePdpApplication.class);

        DecisionRequest request = new DecisionRequest();
        request.setAction("optimize");
        assertThat(manager.findApplication(request)).isInstanceOf(OptimizationPdpApplication.class);
        request.setAction("guard");
        assertThat(manager.findApplication(request)).isInstanceOf(TestGuardOverrideApplication.class);
        //
        // Test Exclusion
        //
        request.setAction("match");
        assertThat(manager.findApplication(request)).isNull();
        //
        // Try to unload a policy that isn't loaded
        //
        ToscaPolicy policy = null;
        for (Map<String, ToscaPolicy> map : completedJtst.getToscaTopologyTemplate().getPolicies()) {
            policy = map.get("onap.scaleout.tca");
        }
        assertThat(policy).isNotNull();
        //
        // Without this being set, it throws NonNull Exception
        //
        policy.setTypeVersion("1.0.0");
        //
        // Try loading and unloading
        //
        final ToscaPolicy policyFinal = policy;
        assertThatCode(() -> {
            manager.removeUndeployedPolicy(policyFinal);
            assertThat(manager.getPolicyCount()).isZero();
            manager.loadDeployedPolicy(policyFinal);
            assertThat(manager.getPolicyCount()).isEqualTo(1);
            manager.removeUndeployedPolicy(policyFinal);
            assertThat(manager.getPolicyCount()).isZero();
        }).doesNotThrowAnyException();
        //
        // try loading something unsupported
        //
        assertThatExceptionOfType(XacmlApplicationException.class).isThrownBy(() -> {
            ToscaPolicy unsupportedPolicy = new ToscaPolicy();
            unsupportedPolicy.setType("I.am.not.supported");
            unsupportedPolicy.setTypeVersion("5.5.5");
            manager.loadDeployedPolicy(unsupportedPolicy);
        });
    }

    private static void copy(Path source, Path dest) {
        try {
            Files.copy(source, dest, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            LOGGER.error("Failed to copy {} to {}", source, dest);
        }
    }

}
