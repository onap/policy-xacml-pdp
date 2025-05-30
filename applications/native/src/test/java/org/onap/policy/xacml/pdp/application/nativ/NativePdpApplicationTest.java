/*-
 * ============LICENSE_START=======================================================
 * ONAP
 * ================================================================================
 * Copyright (C) 2020-2021 AT&T Intellectual Property. All rights reserved.
 * Modifications Copyright (C) 2021, 2024 Nordix Foundation.
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

package org.onap.policy.xacml.pdp.application.nativ;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import com.att.research.xacml.api.Decision;
import com.att.research.xacml.api.Request;
import com.att.research.xacml.api.Response;
import com.att.research.xacml.std.dom.DOMRequest;
import com.att.research.xacml.std.dom.DOMResponse;
import java.io.File;
import java.nio.file.Path;
import java.util.Map;
import java.util.Properties;
import java.util.ServiceLoader;
import oasis.names.tc.xacml._3_0.core.schema.wd_17.PolicySetType;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.onap.policy.common.utils.coder.CoderException;
import org.onap.policy.common.utils.coder.StandardYamlCoder;
import org.onap.policy.common.utils.resources.ResourceUtils;
import org.onap.policy.common.utils.resources.TextFileUtils;
import org.onap.policy.models.tosca.authorative.concepts.ToscaConceptIdentifier;
import org.onap.policy.models.tosca.authorative.concepts.ToscaPolicy;
import org.onap.policy.models.tosca.authorative.concepts.ToscaServiceTemplate;
import org.onap.policy.models.tosca.simple.concepts.JpaToscaServiceTemplate;
import org.onap.policy.pdp.xacml.application.common.ToscaPolicyConversionException;
import org.onap.policy.pdp.xacml.application.common.XacmlApplicationException;
import org.onap.policy.pdp.xacml.application.common.XacmlApplicationServiceProvider;
import org.onap.policy.pdp.xacml.application.common.XacmlPolicyUtils;
import org.onap.policy.pdp.xacml.xacmltest.TestUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class NativePdpApplicationTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(NativePdpApplicationTest.class);
    private static final String PERMIT = "Permit";
    private static final StandardYamlCoder yamlCoder = new StandardYamlCoder();
    private static final Properties properties = new Properties();
    private static File propertiesFile;
    private static NativePdpApplication service;
    private static Request request;

    @TempDir
    static Path policyFolder;

    /**
     * Copies the xacml.properties and policies files into
     * temporary folder and loads the service provider saving
     * instance of provider off for other tests to use.
     */
    @BeforeAll
    static void setup() throws Exception {
        LOGGER.info("Setting up class");
        //
        // Setup our temporary folder
        //
        XacmlPolicyUtils.FileCreator myCreator = (String filename) -> policyFolder.resolve(filename).toFile();
        propertiesFile = XacmlPolicyUtils.copyXacmlPropertiesContents("src/test/resources/xacml.properties",
            properties, myCreator);
        //
        // Load service
        //
        ServiceLoader<XacmlApplicationServiceProvider> applicationLoader =
            ServiceLoader.load(XacmlApplicationServiceProvider.class);
        //
        // Find the native application and save for use in all the tests
        //
        StringBuilder strDump = new StringBuilder("Loaded applications:" + XacmlPolicyUtils.LINE_SEPARATOR);
        for (XacmlApplicationServiceProvider application : applicationLoader) {
            //
            // Is it our service?
            //
            if (application instanceof NativePdpApplication) {
                //
                // Should be the first and only one
                //
                assertThat(service).isNull();
                service = (NativePdpApplication) application;
            }
            strDump.append(application.applicationName());
            strDump.append(" supports ");
            strDump.append(application.supportedPolicyTypes());
            strDump.append(XacmlPolicyUtils.LINE_SEPARATOR);
        }
        LOGGER.info("{}", strDump);
        //
        // Tell it to initialize based on the properties file
        // we just built for it.
        //
        service.initialize(propertiesFile.toPath().getParent(), null);
        //
        // Load XACML Request
        //
        request = DOMRequest.load(
            TextFileUtils.getTextFileAsString(
                "src/test/resources/requests/native.policy.request.xml"));
    }

    @Test
    void testUncommon() {
        NativePdpApplicationTranslator translator = new NativePdpApplicationTranslator();
        assertThatExceptionOfType(ToscaPolicyConversionException.class).isThrownBy(() ->
            translator.convertRequest(null)
        ).withMessageContaining("Do not call native convertRequest");

        assertThat(translator.convertResponse(null)).isNull();

        NativePdpApplication application = new NativePdpApplication();
        assertThat(application.canSupportPolicyType(new ToscaConceptIdentifier(
            "onap.policies.native.Xacml", "1.0.0"))).isTrue();
        assertThat(application.canSupportPolicyType(new ToscaConceptIdentifier(
            "onap.policies.native.ToscaXacml", "1.0.0"))).isTrue();
        assertThat(application.canSupportPolicyType(new ToscaConceptIdentifier(
            "onap.policies.native.SomethingElse", "1.0.0"))).isFalse();
        assertThat(application.actionDecisionsSupported()).contains("native");
    }

    @Test
    void testBadPolicies() throws Exception {
        NativePdpApplicationTranslator translator = new NativePdpApplicationTranslator();
        String policyYaml = ResourceUtils.getResourceAsString("src/test/resources/policies/bad.native.policies.yaml");
        //
        // Serialize it into a class
        //
        ToscaServiceTemplate serviceTemplate = yamlCoder.decode(policyYaml, ToscaServiceTemplate.class);
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
                if ("bad.base64".equals(policy.getName())) {
                    assertThatExceptionOfType(ToscaPolicyConversionException.class).isThrownBy(() ->
                        translator.convertPolicy(policy)
                    ).as(policy.getName()).withMessageContaining("error on Base64 decoding the native policy");
                } else if ("bad.noproperties".equals(policy.getName())) {
                    assertThatExceptionOfType(ToscaPolicyConversionException.class).isThrownBy(() ->
                        translator.convertPolicy(policy)
                    ).as(policy.getName()).withMessageContaining("Cannot decode NativeDefinition from null properties");
                } else if ("bad.policy".equals(policy.getName())) {
                    assertThatExceptionOfType(ToscaPolicyConversionException.class).isThrownBy(() ->
                        translator.convertPolicy(policy)
                    ).as(policy.getName()).withMessageContaining("Invalid XACML Policy");
                }
            }
        }
    }

    @Test
    void testNativePolicy() throws Exception {

        LOGGER.info("*********** Running native policy test *************");
        //
        // Now load the TOSCA compliant native policy - make sure
        // the pdp can support it and have it load into the PDP.
        //
        TestUtils.loadPolicies("src/test/resources/policies/native.policy.yaml", service);
        //
        // Send the request and verify decision result
        //
        requestAndCheckDecision(request);
    }

    @Test
    void testNestedApplyNativeToscaXacmlPolicy() throws Exception {
        String policySetTypeYaml = ResourceUtils
            .getResourceAsString("src/test/resources/policies/native.toscaxacml.policy.nestedapply.yaml");
        checkPolicySetType(policySetTypeYaml, "nestedapply.policy.set");
    }

    @Test
    void testOperatorsNativeToscaXacmlPolicy() throws Exception {
        String policySetTypeYaml = ResourceUtils
            .getResourceAsString("src/test/resources/policies/native.toscaxacml.policy.operators.yaml");
        checkPolicySetType(policySetTypeYaml, "operators.policy.set");
    }

    @Test
    void testExprNativeToscaXacmlPolicy() throws Exception {
        String policySetTypeYaml = ResourceUtils
            .getResourceAsString("src/test/resources/policies/native.toscaxacml.policy.expr.yaml");
        checkPolicySetType(policySetTypeYaml, "expr.check.set");
    }

    @Test
    void testFunctionNativeToscaXacmlPolicy() throws Exception {
        String policySetTypeYaml = ResourceUtils
            .getResourceAsString("src/test/resources/policies/native.toscaxacml.policy.function.yaml");
        checkPolicySetType(policySetTypeYaml, "function.list.check.set");
    }

    @Test
    void testCompareWithNativeToscaXacmlPolicy() throws Exception {
        String policySetTypeYaml = ResourceUtils
            .getResourceAsString("src/test/resources/policies/native.toscaxacml.policy.compareWith.yaml");
        checkPolicySetType(policySetTypeYaml, "compareWith.policy.set");
    }

    @Test
    void testRefNativeToscaXacmlPolicy() throws Exception {
        String policySetTypeYaml = ResourceUtils
            .getResourceAsString("src/test/resources/policies/native.toscaxacml.policy.ref.yaml");
        checkPolicySetType(policySetTypeYaml, "ref.policy.set");
    }

    @Test
    void testDefaultNativeToscaXacmlPolicy() throws Exception {
        String policySetTypeYaml = ResourceUtils
            .getResourceAsString("src/test/resources/policies/native.toscaxacml.policy.default.yaml");
        checkPolicySetType(policySetTypeYaml, "default.policy.set");
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "src/test/resources/policies/bad.native.toscaxacml.policy.yaml",
        "src/test/resources/policies/bad.native.toscaxacml.policy.compareWith.yaml",
        "src/test/resources/policies/bad.native.toscaxacml.policy.expr.yaml",
        "src/test/resources/policies/bad.native.toscaxacml.policy.expr1.yaml",
        "src/test/resources/policies/bad.native.toscaxacml.policy.nestedapply.yaml",
        "src/test/resources/policies/bad.native.toscaxacml.policy.operator.yaml",
        "src/test/resources/policies/bad.native.toscaxacml.policy.target.yaml",
        "src/test/resources/policies/bad.native.toscaxacml.policy.apply.yaml",
        "src/test/resources/policies/bad.native.toscaxacml.policy.condition.yaml",
        "src/test/resources/policies/bad.native.toscaxacml.policy.condition1.yaml",
        "src/test/resources/policies/bad.native.toscaxacml.policy.decision.yaml",
        "src/test/resources/policies/bad.native.toscaxacml.policy.ref.yaml",
        "src/test/resources/policies/bad.native.toscaxacml.policy.advice.yaml"
        })
    void testBadToscaXacmlPolicyRule(String filename) throws Exception {
        LOGGER.debug("Testing {}", filename);
        NativePdpApplicationTranslator translator = new NativePdpApplicationTranslator();
        String policyYaml = ResourceUtils
            .getResourceAsString(filename);
        ToscaServiceTemplate serviceTemplate = yamlCoder.decode(policyYaml, ToscaServiceTemplate.class);
        JpaToscaServiceTemplate jtst = new JpaToscaServiceTemplate();
        jtst.fromAuthorative(serviceTemplate);
        ToscaServiceTemplate completedJtst = jtst.toAuthorative();

        for (Map<String, ToscaPolicy> policies : completedJtst.getToscaTopologyTemplate().getPolicies()) {
            for (ToscaPolicy policy : policies.values()) {
                assertThatExceptionOfType(Exception.class).isThrownBy(() ->
                        translator.convertPolicy(policy)
                    ).as((String) policy.getMetadata().get("policy-id"));

            }
        }
    }

    /**
     * Request a decision and check that it matches expectation.
     *
     * @param request to send to XACML PDP
     * @throws Exception on errors requesting a decision and checking the returned decision
     **/
    private void requestAndCheckDecision(Request request) throws Exception {
        //
        // Ask for a decision
        //
        Response decision = service.makeNativeDecision(request);
        //
        // Check decision
        //
        checkDecision(decision);
    }

    /**
     * Check that decision matches expectation.
     *
     * @param response received
     * @throws Exception on errors checking the decision
     **/
    private void checkDecision(Response response) throws Exception {
        LOGGER.info("Looking for {} Decision", NativePdpApplicationTest.PERMIT);
        assertThat(response).isNotNull();
        Decision decision = response.getResults().iterator().next().getDecision();
        assertThat(decision).isNotNull();
        assertThat(decision).hasToString(NativePdpApplicationTest.PERMIT);
        LOGGER.info("Xacml response we received {}", DOMResponse.toString(response));
    }

    private void checkPolicySetType(String policySetTypeYaml, String name)
        throws ToscaPolicyConversionException, CoderException {
        NativePdpApplicationTranslator translator = new NativePdpApplicationTranslator();
        ToscaServiceTemplate serviceTemplate = yamlCoder.decode(policySetTypeYaml, ToscaServiceTemplate.class);
        JpaToscaServiceTemplate jtst = new JpaToscaServiceTemplate();
        jtst.fromAuthorative(serviceTemplate);
        ToscaServiceTemplate completedJtst = jtst.toAuthorative();
        for (Map<String, ToscaPolicy> policies : completedJtst.getToscaTopologyTemplate().getPolicies()) {
            for (ToscaPolicy policy : policies.values()) {
                try {
                    service.loadPolicy(policy);
                } catch (XacmlApplicationException e) {
                    LOGGER.error("Application failed to load policy", e);
                }
                PolicySetType policySetType = (PolicySetType) translator.convertPolicy(policy);
                assertThat(policySetType).isNotNull();
                assertThat(policySetType.getPolicySetId()).isEqualTo(name);
            }
        }
    }
}
