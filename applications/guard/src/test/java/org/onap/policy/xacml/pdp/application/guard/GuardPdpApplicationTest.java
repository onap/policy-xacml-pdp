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

package org.onap.policy.xacml.pdp.application.guard;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.junit.Assert.assertEquals;

import com.att.research.xacml.util.XACMLProperties;
import com.google.common.io.Files;
import com.google.gson.Gson;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.ServiceLoader;



import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.onap.policy.common.utils.resources.TextFileUtils;
import org.onap.policy.models.decisions.concepts.DecisionRequest;
import org.onap.policy.models.decisions.serialization.DecisionRequestMessageBodyHandler;
import org.onap.policy.models.decisions.serialization.DecisionResponseMessageBodyHandler;
import org.onap.policy.pdp.xacml.application.common.XacmlApplicationServiceProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yaml.snakeyaml.Yaml;

public class GuardPdpApplicationTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(GuardPdpApplicationTest.class);
    private static Properties properties = new Properties();
    private static File propertiesFile;
    private static XacmlApplicationServiceProvider service;
    private static DecisionRequest requestSinglePolicy;

    private static Gson gsonDecisionRequest;
    private static Gson gsonDecisionResponse;

    @ClassRule
    public static final TemporaryFolder policyFolder = new TemporaryFolder();

    @Before
    public void setUp() throws Exception {

    }

    @Test
    public void testBasics() {
        assertThatCode(() -> {
            //
            // Create our Gson builder
            //
            gsonDecisionRequest = new DecisionRequestMessageBodyHandler().getGson();
            gsonDecisionResponse = new DecisionResponseMessageBodyHandler().getGson();
            //
            // Load Single Decision Request
            //
            requestSinglePolicy = gsonDecisionRequest.fromJson(
                    TextFileUtils
                        .getTextFileAsString("../../main/src/test/resources/decisions/decision.single.input.json"),
                        DecisionRequest.class);
            //
            // Copy all the properties and root policies to the temporary folder
            //
            try (InputStream is = new FileInputStream("src/test/resources/xacml.properties")) {
                //
                // Load it in
                //
                properties.load(is);
                propertiesFile = policyFolder.newFile("xacml.properties");
                //
                // Copy the root policies
                //
                for (String root : XACMLProperties.getRootPolicyIDs(properties)) {
                    //
                    // Get a file
                    //
                    Path rootPath = Paths.get(properties.getProperty(root + ".file"));
                    LOGGER.debug("Root file {} {}", rootPath, rootPath.getFileName());
                    //
                    // Construct new file name
                    //
                    File newRootPath = policyFolder.newFile(rootPath.getFileName().toString());
                    //
                    // Copy it
                    //
                    Files.copy(rootPath.toFile(), newRootPath);
                    assertThat(newRootPath).exists();
                    //
                    // Point to where the new policy is in the temp dir
                    //
                    properties.setProperty(root + ".file", newRootPath.getAbsolutePath());
                }
                try (OutputStream os = new FileOutputStream(propertiesFile.getAbsolutePath())) {
                    properties.store(os, "");
                    assertThat(propertiesFile).exists();
                }
            }
            //
            // Load service
            //
            ServiceLoader<XacmlApplicationServiceProvider> applicationLoader =
                    ServiceLoader.load(XacmlApplicationServiceProvider.class);
            //
            // Iterate through them - I could store the object as
            // XacmlApplicationServiceProvider pointer.
            //
            // Try this later.
            //
            StringBuilder strDump = new StringBuilder("Loaded applications:" + System.lineSeparator());
            Iterator<XacmlApplicationServiceProvider> iterator = applicationLoader.iterator();
            while (iterator.hasNext()) {
                XacmlApplicationServiceProvider application = iterator.next();
                //
                // Is it our service?
                //
                if (application instanceof GuardPdpApplication) {
                    //
                    // Should be the first and only one
                    //
                    assertThat(service).isNull();
                    service = application;
                }
                strDump.append(application.applicationName());
                strDump.append(" supports ");
                strDump.append(application.supportedPolicyTypes());
                strDump.append(System.lineSeparator());
            }
            LOGGER.debug("{}", strDump);
            //
            // Tell it to initialize based on the properties file
            // we just built for it.
            //
            service.initialize(propertiesFile.toPath().getParent());
            //
            // Make sure there's an application name
            //
            assertThat(service.applicationName()).isNotEmpty();
            //
            // Decisions
            //
            assertThat(service.actionDecisionsSupported().size()).isEqualTo(1);
            assertThat(service.actionDecisionsSupported()).contains("guard");
            //
            // Ensure it has the supported policy types and
            // can support the correct policy types.
            //
            assertThat(service.supportedPolicyTypes()).isNotEmpty();
            assertThat(service.supportedPolicyTypes().size()).isEqualTo(2);
            assertThat(service.canSupportPolicyType("onap.policies.controlloop.guard.FrequencyLimiter", "1.0.0"))
                .isTrue();
            assertThat(service.canSupportPolicyType("onap.policies.controlloop.guard.FrequencyLimiter", "1.0.1"))
                .isFalse();
            assertThat(service.canSupportPolicyType("onap.policies.controlloop.guard.MinMax", "1.0.0")).isTrue();
            assertThat(service.canSupportPolicyType("onap.policies.controlloop.guard.MinMax", "1.0.1")).isFalse();
            assertThat(service.canSupportPolicyType("onap.foo", "1.0.1")).isFalse();
            //
            // Ensure it supports decisions
            //
            assertThat(service.actionDecisionsSupported()).contains("guard");
        }).doesNotThrowAnyException();
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testvDnsPolicy() {
        //
        // Now load the vDNS Policy - make sure
        // the pdp can support it and have it load
        // into the PDP.
        //
        assertThatCode(() -> {
            try (InputStream is =
                 new FileInputStream("src/test/resources/vDNS.policy.guard.frequency.input.tosca.yaml")) {
                Yaml yaml = new Yaml();
                Map<String, Object> tosca = yaml.load(is);
                LOGGER.debug("tosca {}", tosca);
                //
                // TODO: refactor this as separate utility method to extract the list of policies
                //
                Map<String, Object> topologyTemplate = (Map<String, Object>) tosca.get("topology_template");
                LOGGER.debug("topology_template {}", tosca);
                List<Map<String, Object>> policies = (List<Map<String, Object>>) topologyTemplate.get("policies");
                LOGGER.debug("policies {}", policies);
                //
                // Extract information on each guard policy (there's only one in the file specified)
                //
                for (Map<String, Object> policy : policies) {
                    LOGGER.debug("policy {}", policy);
                    LOGGER.debug("policy.size {}", policy.size());
                    assertEquals(1, policy.size());
                    //
                    // TODO: refactor this as separate utility method to extract the policy name
                    //
                    String policyName = null;
                    for (String name : policy.keySet()) {
                        policyName = name;
                    }
                    LOGGER.info("policyName {}", policyName);
                    //
                    // Get the policy info
                    //
                    Map<String, Object> policyInfo = (Map<String, Object>) policy.get(policyName);
                    String type = (String) policyInfo.get("type");
                    LOGGER.info("type {}", type);
                    String version = (String) policyInfo.get("version");
                    LOGGER.info("version {}", version);
                    Map<String, String> metadata = (Map<String, String>) policyInfo.get("metadata");
                    for ( Entry<String, String> entry : metadata.entrySet() ) {
                        LOGGER.info("metadata {}", entry);
                    }
                    Map<String, String> properties = (Map<String, String>) policyInfo.get("properties");
                    for ( Entry<String, String> entry : properties.entrySet() ) {
                        LOGGER.info("property {}", entry);
                    }
                    assertEquals(policyName, metadata.get("policy-id"));
                    //
                    // TODO
                    // Now that we've correctly extracted our guard policy from tosca, we must
                    // check that the type and version are valid
                    // check that we have an implementation of that valid guard type and version
                    // generate and load the XACML file implementing that guard type with the specified properties
                    // test requests against the policy
                    //
                }
            }
        }).doesNotThrowAnyException();
    }
}
