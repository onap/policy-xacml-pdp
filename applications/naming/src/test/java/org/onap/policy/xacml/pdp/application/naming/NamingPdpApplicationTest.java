/*-
 * ============LICENSE_START=======================================================
 * ONAP
 * ================================================================================
 * Copyright (C) 2019-2022 AT&T Intellectual Property. All rights reserved.
 * Modifications Copyright (C) 2021, 2024 Nordix Foundation.
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

package org.onap.policy.xacml.pdp.application.naming;

import static org.assertj.core.api.Assertions.assertThat;

import com.att.research.xacml.api.Response;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.ServiceLoader;
import org.apache.commons.lang3.tuple.Pair;
import org.assertj.core.api.Condition;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.io.TempDir;
import org.onap.policy.common.utils.coder.CoderException;
import org.onap.policy.common.utils.coder.StandardCoder;
import org.onap.policy.common.utils.resources.ResourceUtils;
import org.onap.policy.common.utils.resources.TextFileUtils;
import org.onap.policy.models.decisions.concepts.DecisionRequest;
import org.onap.policy.models.decisions.concepts.DecisionResponse;
import org.onap.policy.models.tosca.authorative.concepts.ToscaConceptIdentifier;
import org.onap.policy.pdp.xacml.application.common.XacmlApplicationException;
import org.onap.policy.pdp.xacml.application.common.XacmlApplicationServiceProvider;
import org.onap.policy.pdp.xacml.application.common.XacmlPolicyUtils;
import org.onap.policy.pdp.xacml.xacmltest.TestUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@TestMethodOrder(MethodOrderer.MethodName.class)
class NamingPdpApplicationTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(NamingPdpApplicationTest.class);
    private static final Properties properties = new Properties();
    private static XacmlApplicationServiceProvider service;
    private static final StandardCoder gson = new StandardCoder();
    private static DecisionRequest baseRequest;

    @TempDir
    static Path policyFolder;

    /**
     * Copies the xacml.properties and policies files into
     * temporary folder and loads the service provider saving
     * instance of provider off for other tests to use.
     */
    @BeforeAll
    static void setUp() throws Exception {
        //
        // Load Single Decision Request
        //
        baseRequest = gson.decode(
            TextFileUtils
                .getTextFileAsString(
                    "src/test/resources/decision.naming.input.json"),
            DecisionRequest.class);

        //
        // Copy the test policy types into data area
        //
        String policy = "onap.policies.Naming";
        String policyType = ResourceUtils.getResourceAsString("policytypes/" + policy + ".yaml");
        LOGGER.info("Copying {}", policyType);
        Files.write(Paths.get(policyFolder.toFile().getAbsolutePath(), policy + "-1.0.0.yaml"),
            policyType.getBytes());
        //
        // Load service
        //
        ServiceLoader<XacmlApplicationServiceProvider> applicationLoader =
            ServiceLoader.load(XacmlApplicationServiceProvider.class);
        //
        // Iterate through Xacml application services and find
        // the optimization service. Save it for use throughout
        // all the Junit tests.
        //
        StringBuilder strDump = new StringBuilder("Loaded applications:" + XacmlPolicyUtils.LINE_SEPARATOR);
        for (XacmlApplicationServiceProvider application : applicationLoader) {
            //
            // Is it our service?
            //
            if (application instanceof NamingPdpApplication) {
                //
                // Should be the first and only one
                //
                assertThat(service).isNull();
                service = application;
            }
            strDump.append(application.applicationName());
            strDump.append(" supports ");
            strDump.append(application.supportedPolicyTypes());
            strDump.append(XacmlPolicyUtils.LINE_SEPARATOR);
        }
        LOGGER.debug("{}", strDump);
        assertThat(service).isNotNull();
        //
        // Tell it to initialize based on the properties file
        // we just built for it.
        //

        XacmlPolicyUtils.FileCreator myCreator = (String filename) -> policyFolder.resolve(filename).toFile();
        File propertiesFile = XacmlPolicyUtils.copyXacmlPropertiesContents("src/test/resources/xacml.properties",
            properties, myCreator);
        service.initialize(propertiesFile.toPath().getParent(), null);
    }

    @Test
    void test01Basics() {
        //
        // Make sure there's an application name
        //
        assertThat(service.applicationName()).isNotEmpty();
        //
        // Decisions
        //
        assertThat(service.actionDecisionsSupported()).hasSize(1);
        assertThat(service.actionDecisionsSupported()).contains("naming");
        //
        // Ensure it has the supported policy types and
        // can support the correct policy types.
        //
        assertThat(service.canSupportPolicyType(new ToscaConceptIdentifier(
            "onap.policies.Naming", "1.0.0"))).isTrue();
        assertThat(service.canSupportPolicyType(new ToscaConceptIdentifier(
            "onap.foobar", "1.0.0"))).isFalse();
    }

    @Test
    void test02NoPolicies() throws CoderException {
        //
        // Ask for a decision when there are no policies loaded
        //
        LOGGER.info("Request {}", gson.encode(baseRequest));
        Pair<DecisionResponse, Response> decision = service.makeDecision(baseRequest, null);
        LOGGER.info("Decision {}", decision.getKey());

        assertThat(decision.getKey()).isNotNull();
        assertThat(decision.getKey().getPolicies()).isEmpty();
        //
        // Naming applications should not have this information returned
        //
        assertThat(decision.getKey().getAdvice()).isNull();
        assertThat(decision.getKey().getObligations()).isNull();
        assertThat(decision.getKey().getAttributes()).isNull();
    }

    @Test
    void test03Naming() throws
        XacmlApplicationException {
        //
        // Now load all the optimization policies
        //
        TestUtils.loadPolicies("policies/sdnc.policy.naming.input.tosca.yaml", service);
        //
        // Ask for a decision for available default policies
        //
        DecisionResponse response = makeDecision();
        //
        // There is no default policy
        //
        assertThat(response).isNotNull();
        assertThat(response.getPolicies()).isEmpty();
        //
        // Ask for VNF
        //
        baseRequest.getResource().put("policy-type", List.of("onap.policies.Naming"));
        //
        // Ask for a decision for VNF default policies
        //
        response = makeDecision();
        assertThat(response).isNotNull();
        assertThat(response.getPolicies()).hasSize(1);
        //
        // Naming applications should not have this information returned
        //
        assertThat(response.getAdvice()).isNull();
        assertThat(response.getObligations()).isNull();
        assertThat(response.getAttributes()).isNull();
        //
        // Validate it
        //
        validateDecision(response, baseRequest);
    }

    private DecisionResponse makeDecision() {
        Pair<DecisionResponse, Response> decision = service.makeDecision(baseRequest, null);
        LOGGER.info("Request Resources {}", baseRequest.getResource());
        LOGGER.info("Decision {}", decision.getKey());
        for (Entry<String, Object> entrySet : decision.getKey().getPolicies().entrySet()) {
            LOGGER.info("Policy {}", entrySet.getKey());
        }
        return decision.getKey();
    }

    @SuppressWarnings("unchecked")
    private void validateDecision(DecisionResponse decision, DecisionRequest request) {
        for (Entry<String, Object> entrySet : decision.getPolicies().entrySet()) {
            LOGGER.info("Decision Returned Policy {}", entrySet.getKey());
            assertThat(entrySet.getValue()).isInstanceOf(Map.class);
            Map<String, Object> policyContents = (Map<String, Object>) entrySet.getValue();
            assertThat(policyContents).containsKey("properties");
            assertThat(policyContents.get("properties")).isInstanceOf(Map.class);
            Map<String, Object> policyProperties = (Map<String, Object>) policyContents.get("properties");

            validateMatchable((Collection<String>) request.getResource().get("nfRole"),
                (Collection<String>) policyProperties.get("nfRole"));

            validateMatchable((Collection<String>) request.getResource().get("naming-type"),
                (Collection<String>) policyProperties.get("naming-type"));

            validateMatchable((Collection<String>) request.getResource().get("property-name"),
                (Collection<String>) policyProperties.get("property-name"));
        }
    }

    private void validateMatchable(Collection<String> requestList, Collection<String> policyProperties) {
        LOGGER.info("Validating matchable: {} with {}", policyProperties, requestList);
        //
        // Null or empty implies '*' - that is any value is acceptable
        // for this policy.
        //
        if (policyProperties == null || policyProperties.isEmpty()) {
            return;
        }
        Condition<String> condition = new Condition<>(
            requestList::contains,
            "Request list is contained");
        assertThat(policyProperties).haveAtLeast(1, condition);

    }
}
