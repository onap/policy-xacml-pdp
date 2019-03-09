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

package org.onap.policy.xacml.pdp.engine;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.junit.Assert.assertEquals;

import com.att.research.xacml.api.Decision;
import com.att.research.xacml.api.Response;
import com.att.research.xacml.api.Result;
import com.att.research.xacml.std.annotations.RequestParser;
import com.att.research.xacml.std.annotations.XACMLAction;
import com.att.research.xacml.std.annotations.XACMLRequest;
import com.att.research.xacml.std.annotations.XACMLResource;
import com.att.research.xacml.std.annotations.XACMLSubject;
import com.att.research.xacml.util.XACMLProperties;
import com.google.common.io.Files;

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

import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.onap.policy.pdp.xacml.application.common.ToscaPolicyConversionException;
import org.onap.policy.pdp.xacml.application.common.XacmlApplicationServiceProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yaml.snakeyaml.Yaml;

public class OnapXacmlPdpEngineTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(OnapXacmlPdpEngineTest.class);
    private static OnapXacmlPdpEngine onapPdpEngine;
    private static Properties properties = new Properties();
    private static File propertiesFile;

    @ClassRule
    public static final TemporaryFolder policyFolder = new TemporaryFolder();

    /**
     * This is a simple annotation class to simulate
     * requests coming in.
     */
    @XACMLRequest(ReturnPolicyIdList = true)
    public class MyXacmlRequest {

        @XACMLSubject(includeInResults = true)
        String onapName = "DCAE";

        @XACMLResource(includeInResults = true)
        String resource = "onap.policies.Monitoring";

        @XACMLAction()
        String action = "configure";
    }

    /**
     * Load a test engine.
     */
    @BeforeClass
    public static void setup() {
        assertThatCode(() -> {
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
                strDump.append(application.applicationName());
                strDump.append(" supports ");
                strDump.append(application.supportedPolicyTypes());
                strDump.append(System.lineSeparator());
            }
            LOGGER.debug("{}", strDump);
            //
            // Create the engine instance
            //
            onapPdpEngine = new OnapXacmlPdpEngine();
            //
            // Tell it to initialize based on the properties file
            // we just built for it.
            //
            onapPdpEngine.initialize(propertiesFile.toPath().getParent());
            //
            // Make sure there's an application name
            //
            assertThat(onapPdpEngine.applicationName()).isNotEmpty();
            //
            // Ensure it has the supported policy types and
            // can support the correct policy types.
            //
            assertThat(onapPdpEngine.canSupportPolicyType("onap.Monitoring", "1.0.0")).isTrue();
            assertThat(onapPdpEngine.canSupportPolicyType("onap.Monitoring", "1.5.0")).isTrue();
            assertThat(onapPdpEngine.canSupportPolicyType("onap.policies.monitoring.foobar", "1.0.1")).isTrue();
            assertThat(onapPdpEngine.canSupportPolicyType("onap.foobar", "1.0.0")).isFalse();
            assertThat(onapPdpEngine.supportedPolicyTypes()).contains("onap.Monitoring");
            //
            // Ensure it supports decisions
            //
            assertThat(onapPdpEngine.actionDecisionsSupported()).contains("configure");
        }).doesNotThrowAnyException();
    }

    @Test
    public void testNoPolicies() {
        //
        // Make a simple decision - NO policies are loaded
        //
        assertThatCode(() -> {
            Response response = onapPdpEngine.decision(RequestParser.parseRequest(new MyXacmlRequest()));
            for (Result result : response.getResults()) {
                LOGGER.info("Decision {}", result.getDecision());
                assertEquals(Decision.PERMIT, result.getDecision());
            }
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
            try (InputStream is = new FileInputStream("src/test/resources/vDNS.policy.input.yaml")) {
                Yaml yaml = new Yaml();
                Map<String, Object> toscaObject = yaml.load(is);
                List<Object> policies = (List<Object>) toscaObject.get("policies");
                //
                // What we should really do is split the policies out from the ones that
                // are not supported to ones that are. And then load these.
                //
                // In another future review....
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
                        assertThat(onapPdpEngine.canSupportPolicyType(
                                policyDefinition.get("type").toString(),
                                policyDefinition.get("version").toString()))
                            .isTrue();
                    }
                }
                //
                // Just go ahead and load them all for now
                //
                // Assuming all are supported etc.
                //
                onapPdpEngine.loadPolicies(toscaObject);

                //List<PolicyType> policies = onapPdpEngine.convertPolicies(is);
                //
                // Should have a policy
                ////                assertThat(policies.isEmpty()).isFalse();
            }
        }).doesNotThrowAnyException();
    }

    @Test
    public void testBadPolicies() {
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

        assertThatExceptionOfType(ToscaPolicyConversionException.class).isThrownBy(() -> {
            try (InputStream is =
                    new FileInputStream("src/test/resources/test.monitoring.policy.missingproperties.yaml")) {
                onapPdpEngine.convertPolicies(is);
            }
        }).withMessageContaining("missing properties section");
    }

}
