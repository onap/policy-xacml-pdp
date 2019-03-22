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

package org.onap.policy.xacml.pdp.application.monitoring;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.ServiceLoader;

import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runners.MethodSorters;
import org.onap.policy.common.utils.coder.CoderException;
import org.onap.policy.common.utils.coder.StandardCoder;
import org.onap.policy.common.utils.resources.TextFileUtils;
import org.onap.policy.models.decisions.concepts.DecisionRequest;
import org.onap.policy.models.decisions.concepts.DecisionResponse;
import org.onap.policy.pdp.xacml.application.common.XacmlApplicationServiceProvider;
import org.onap.policy.pdp.xacml.application.common.XacmlPolicyUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yaml.snakeyaml.Yaml;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class MonitoringPdpApplicationTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(MonitoringPdpApplicationTest.class);
    private static Properties properties = new Properties();
    private static File propertiesFile;
    private static XacmlApplicationServiceProvider service;
    private static DecisionRequest requestSinglePolicy;

    private static StandardCoder gson = new StandardCoder();

    @ClassRule
    public static final TemporaryFolder policyFolder = new TemporaryFolder();

    /**
     * Copies the xacml.properties and policies files into
     * temporary folder and loads the service provider saving
     * instance of provider off for other tests to use.
     */
    @BeforeClass
    public static void setup() throws Exception {
        //
        // Load Single Decision Request
        //
        requestSinglePolicy = gson.decode(
                TextFileUtils
                    .getTextFileAsString("../../main/src/test/resources/decisions/decision.single.input.json"),
                    DecisionRequest.class);
        //
        // Setup our temporary folder
        //
        XacmlPolicyUtils.FileCreator myCreator = (String filename) -> policyFolder.newFile(filename);
        propertiesFile = XacmlPolicyUtils.copyXacmlPropertiesContents("src/test/resources/xacml.properties",
                properties, myCreator);
        //
        // Load XacmlApplicationServiceProvider service
        //
        ServiceLoader<XacmlApplicationServiceProvider> applicationLoader =
                ServiceLoader.load(XacmlApplicationServiceProvider.class);
        //
        // Look for our class instance and save it
        //
        StringBuilder strDump = new StringBuilder("Loaded applications:" + System.lineSeparator());
        Iterator<XacmlApplicationServiceProvider> iterator = applicationLoader.iterator();
        while (iterator.hasNext()) {
            XacmlApplicationServiceProvider application = iterator.next();
            //
            // Is it our service?
            //
            if (application instanceof MonitoringPdpApplication) {
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
    }

    @Test
    public void test1Basics() {
        //
        // Make sure there's an application name
        //
        assertThat(service.applicationName()).isNotEmpty();
        //
        // Ensure it has the supported policy types and
        // can support the correct policy types.
        //
        assertThat(service.canSupportPolicyType("onap.Monitoring", "1.0.0")).isTrue();
        assertThat(service.canSupportPolicyType("onap.Monitoring", "1.5.0")).isTrue();
        assertThat(service.canSupportPolicyType("onap.policies.monitoring.foobar", "1.0.1")).isTrue();
        assertThat(service.canSupportPolicyType("onap.foobar", "1.0.0")).isFalse();
        assertThat(service.supportedPolicyTypes()).contains("onap.Monitoring");
        //
        // Ensure it supports decisions
        //
        assertThat(service.actionDecisionsSupported()).contains("configure");
    }

    @Test
    public void test2NoPolicies() {
        //
        // Ask for a decision
        //
        DecisionResponse response = service.makeDecision(requestSinglePolicy);
        LOGGER.info("Decision {}", response);

        assertThat(response).isNotNull();
        assertThat(response.getErrorMessage()).isNullOrEmpty();
        assertThat(response.getPolicies().size()).isEqualTo(0);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void test3AddvDnsPolicy() throws IOException, CoderException {
        //
        // Now load the vDNS Policy - make sure
        // the pdp can support it and have it load
        // into the PDP.
        //
        try (InputStream is = new FileInputStream("src/test/resources/vDNS.policy.input.yaml")) {
            //
            // Have yaml parse it
            //
            Yaml yaml = new Yaml();
            Map<String, Object> toscaObject = yaml.load(is);
            List<Object> policies = (List<Object>) toscaObject.get("policies");
            //
            // Sanity check to ensure the policy type and version are supported
            //
            for (Object policyObject : policies) {
                //
                // Get the contents
                //
                Map<String, Object> policyContents = (Map<String, Object>) policyObject;
                for (Entry<String, Object> entrySet : policyContents.entrySet()) {
                    LOGGER.info("Entry set {}", entrySet.getKey());
                    Map<String, Object> policyDefinition = (Map<String, Object>) entrySet.getValue();
                    //
                    // Find the type and make sure the engine supports it
                    //
                    assertThat(policyDefinition.containsKey("type")).isTrue();
                    assertThat(service.canSupportPolicyType(
                            policyDefinition.get("type").toString(),
                            policyDefinition.get("version").toString()))
                        .isTrue();
                }
            }
            //
            // Load the policies
            //
            service.loadPolicies(toscaObject);
            //
            // Ask for a decision
            //
            DecisionResponse response = service.makeDecision(requestSinglePolicy);
            LOGGER.info("Decision {}", response);

            assertThat(response).isNotNull();
            assertThat(response.getPolicies().size()).isEqualTo(1);
            //
            // Dump it out as Json
            //
            LOGGER.info(gson.encode(response));
        }
    }

    @Test
    public void test4BadPolicies() {
        //
        // No need for service, just test some of the methods
        // for bad policies
        //
        MonitoringPdpApplication onapPdpEngine = new MonitoringPdpApplication();

        /*
        assertThatExceptionOfType(ToscaPolicyConversionException.class).isThrownBy(() -> {
            try (InputStream is =
                    new FileInputStream("src/test/resources/test.monitoring.policy.missingmetadata.yaml")) {
                onapPdpEngine.convertPolicies(is);
            }
        }).withMessageContaining("missing metadata section");

        assertThatExceptionOfType(ToscaPolicyConversionException.class).isThrownBy(() -> {
            try (InputStream is =
                    new FileInputStream("src/test/resources/test.monitoring.policy.missingtype.yaml")) {
                onapPdpEngine.convertPolicies(is);
            }
        }).withMessageContaining("missing type value");

        assertThatExceptionOfType(ToscaPolicyConversionException.class).isThrownBy(() -> {
            try (InputStream is =
                    new FileInputStream("src/test/resources/test.monitoring.policy.missingversion.yaml")) {
                onapPdpEngine.convertPolicies(is);
            }
        }).withMessageContaining("missing version value");

        assertThatExceptionOfType(ToscaPolicyConversionException.class).isThrownBy(() -> {
            try (InputStream is =
                    new FileInputStream("src/test/resources/test.monitoring.policy.badmetadata.1.yaml")) {
                onapPdpEngine.convertPolicies(is);
            }
        }).withMessageContaining("missing metadata policy-version");

        assertThatExceptionOfType(ToscaPolicyConversionException.class).isThrownBy(() -> {
            try (InputStream is =
                    new FileInputStream("src/test/resources/test.monitoring.policy.badmetadata.2.yaml")) {
                onapPdpEngine.convertPolicies(is);
            }
        }).withMessageContaining("missing metadata policy-id");

        */
    }

}
