/*-
 * ============LICENSE_START=======================================================
 * ONAP
 * ================================================================================
 * Copyright (C) 2019-2022 AT&T Intellectual Property. All rights reserved.
 * Modifications Copyright (C) 2019-2021, 2024 Nordix Foundation.
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

package org.onap.policy.xacml.pdp.application.optimization;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import com.att.research.xacml.api.Response;
import com.google.common.collect.Lists;
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
import org.onap.policy.common.utils.coder.StandardYamlCoder;
import org.onap.policy.common.utils.resources.ResourceUtils;
import org.onap.policy.common.utils.resources.TextFileUtils;
import org.onap.policy.models.decisions.concepts.DecisionRequest;
import org.onap.policy.models.decisions.concepts.DecisionResponse;
import org.onap.policy.models.tosca.authorative.concepts.ToscaConceptIdentifier;
import org.onap.policy.models.tosca.authorative.concepts.ToscaPolicy;
import org.onap.policy.models.tosca.authorative.concepts.ToscaServiceTemplate;
import org.onap.policy.models.tosca.simple.concepts.JpaToscaServiceTemplate;
import org.onap.policy.pdp.xacml.application.common.XacmlApplicationException;
import org.onap.policy.pdp.xacml.application.common.XacmlApplicationServiceProvider;
import org.onap.policy.pdp.xacml.application.common.XacmlPolicyUtils;
import org.onap.policy.pdp.xacml.xacmltest.TestUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@TestMethodOrder(MethodOrderer.MethodName.class)
class OptimizationPdpApplicationTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(OptimizationPdpApplicationTest.class);
    private static final Properties properties = new Properties();
    private static File propertiesFile;
    private static XacmlApplicationServiceProvider service;
    private static final StandardCoder gson = new StandardCoder();
    private static DecisionRequest baseRequest;
    private static final String[] listPolicyTypeFiles = {
        "onap.policies.Optimization",
        "onap.policies.optimization.Resource",
        "onap.policies.optimization.Service",
        "onap.policies.optimization.resource.AffinityPolicy",
        "onap.policies.optimization.resource.DistancePolicy",
        "onap.policies.optimization.resource.HpaPolicy",
        "onap.policies.optimization.resource.OptimizationPolicy",
        "onap.policies.optimization.resource.PciPolicy",
        "onap.policies.optimization.service.QueryPolicy",
        "onap.policies.optimization.service.SubscriberPolicy",
        "onap.policies.optimization.resource.Vim_fit",
        "onap.policies.optimization.resource.VnfPolicy"};

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
                    "src/test/resources/decision.optimization.input.json"),
            DecisionRequest.class);
        //
        // Setup our temporary folder
        //
        XacmlPolicyUtils.FileCreator myCreator = (String filename) -> policyFolder.resolve(filename).toFile();
        propertiesFile = XacmlPolicyUtils.copyXacmlPropertiesContents("src/test/resources/xacml.properties",
            properties, myCreator);
        //
        // Copy the test policy types into data area
        //
        for (String policy : listPolicyTypeFiles) {
            String policyType = ResourceUtils.getResourceAsString("policytypes/" + policy + ".yaml");
            LOGGER.info("Copying {}", policyType);
            Files.write(Paths.get(policyFolder.toFile().getAbsolutePath(), policy + "-1.0.0.yaml"),
                policyType.getBytes());
        }
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
            if (application instanceof OptimizationPdpApplication) {
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
        service.initialize(propertiesFile.toPath().getParent(), null);
    }

    /**
     * Simply test some of the simple methods for the application.
     */
    @Test
    void test01Basics() {
        //
        // Make sure there's an application name
        //
        assertThat(service.applicationName()).isNotEmpty();
        //
        // Does it return the correct decisions
        //
        assertThat(service.actionDecisionsSupported()).hasSize(1);
        assertThat(service.actionDecisionsSupported()).contains("optimize");
        //
        // Ensure it has the supported policy types and
        // can support the correct policy types.
        //
        assertThat(service.canSupportPolicyType(new ToscaConceptIdentifier(
            "onap.policies.optimization.resource.AffinityPolicy", "1.0.0"))).isTrue();
        assertThat(service.canSupportPolicyType(new ToscaConceptIdentifier(
            "onap.policies.optimization.service.SubscriberPolicy", "1.0.0"))).isTrue();
        assertThat(service.canSupportPolicyType(new ToscaConceptIdentifier(
            "onap.policies.optimization.service.CustomUseCase", "1.0.0"))).isTrue();
        assertThat(service.canSupportPolicyType(new ToscaConceptIdentifier(
            "onap.foobar", "1.0.0"))).isFalse();
    }

    /**
     * With no policies loaded, there should be 0 policies returned.
     *
     * @throws CoderException CoderException
     */
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
        // Optimization applications should not have this information returned. Except advice
        // for subscriber details, which does get checked in the tests following.
        //
        assertThat(decision.getKey().getAdvice()).isNull();
        assertThat(decision.getKey().getObligations()).isNull();
        assertThat(decision.getKey().getAttributes()).isNull();
    }

    /**
     * Should return ONLY default policies.
     *
     * @throws XacmlApplicationException could not load policies
     */
    @Test
    void test03OptimizationDefault() throws XacmlApplicationException {
        //
        // Now load all the optimization policies
        //
        List<ToscaPolicy> loadedPolicies = TestUtils.loadPolicies("src/test/resources/test-optimization-policies.yaml",
            service);
        assertThat(loadedPolicies).isNotNull().hasSize(14);

        validateDecisionCount(2);
    }

    /**
     * Should only return default HPA policy type.
     */
    @SuppressWarnings("unchecked")
    @Test
    void test04OptimizationDefaultHpa() {
        //
        // Add in policy type
        //
        List<String> policyTypes = Lists.newArrayList("onap.policies.optimization.resource.HpaPolicy");
        baseRequest.getResource().put("policy-type", policyTypes);
        //
        // Ask for a decision for default HPA policy
        //
        DecisionResponse response = makeDecision();

        assertThat(response).isNotNull();
        assertThat(response.getPolicies()).hasSize(1);
        response.getPolicies().forEach((key, value) -> {
            assertThat(((Map<String, Object>) value)).containsEntry("type",
                "onap.policies.optimization.resource.HpaPolicy");
        });
        //
        // Validate it
        //
        validateDecision(response, baseRequest);
    }

    /**
     * Refine for US only policies.
     */
    @SuppressWarnings("unchecked")
    @Test
    void test05OptimizationDefaultGeography() {
        //
        // Remove all the policy-type resources from the request
        //
        cleanOutResources();
        //
        // Add US to the geography list
        //
        ((List<String>) baseRequest.getResource().get("geography")).add("US");

        validateDecisionCount(2);
    }

    /**
     * Add more refinement for service.
     */
    @SuppressWarnings("unchecked")
    @Test
    void test06OptimizationDefaultGeographyAndService() {
        //
        // Add vCPE to the service list
        //
        ((List<String>) baseRequest.getResource().get("services")).add("vCPE");

        validateDecisionCount(3);
    }

    /**
     * Add more refinement for specific resource.
     */
    @SuppressWarnings("unchecked")
    @Test
    void test07OptimizationDefaultGeographyAndServiceAndResource() {
        //
        // Add vG to the resource list
        //
        ((List<String>) baseRequest.getResource().get("resources")).add("vG");

        validateDecisionCount(6);
    }

    /**
     * Now we need to add in subscriberName in order to get scope for gold.
     */
    @SuppressWarnings("unchecked")
    @Test
    void test08OptimizationGeographyAndServiceAndResourceAndScopeIsGoldSubscriber() {
        //
        // Add gold as a scope
        //
        ((List<String>) baseRequest.getContext().get("subscriberName")).add("subscriber_a");

        validateDecisionCount(6, 2);
    }

    /**
     * Add a subscriber that should be platinum.
     */
    @SuppressWarnings("unchecked")
    @Test
    void test09OptimizationGeographyAndServiceAndResourceAndScopeGoldOrPlatinumSubscriber() {
        //
        // Add platinum to the scope list: this is now gold OR platinum
        //
        ((List<String>) baseRequest.getResource().get("scope")).remove("gold");
        ((List<String>) baseRequest.getContext().get("subscriberName")).add("subscriber_x");

        validateDecisionCount(8, 2);
    }

    /**
     * Remove gold subscriber, keep the platinum one.
     */
    @SuppressWarnings("unchecked")
    @Test
    void test10OptimizationGeographyAndServiceAndResourceAndScopeNotGoldStillPlatinum() {
        //
        // Add gold as a scope
        //
        ((List<String>) baseRequest.getResource().get("scope")).remove("gold");
        ((List<String>) baseRequest.getResource().get("scope")).remove("platinum");
        ((List<String>) baseRequest.getContext().get("subscriberName")).remove("subscriber_a");

        validateDecisionCount(7);
    }

    /**
     * Filter by Affinity policy.
     */
    @Test
    void test11OptimizationPolicyTypeDefault() {
        //
        // Add in policy type
        //
        List<String> policyTypes = Lists.newArrayList("onap.policies.optimization.resource.AffinityPolicy");
        baseRequest.getResource().put("policy-type", policyTypes);

        validateDecisionCount(1);
    }

    /**
     * Now filter by HPA policy type.
     */
    @SuppressWarnings("unchecked")
    @Test
    void test12OptimizationPolicyTypeDefault() {
        //
        // Add in another policy type
        //
        ((List<String>) baseRequest.getResource().get("policy-type"))
            .add("onap.policies.optimization.resource.HpaPolicy");

        validateDecisionCount(2);
    }

    @Test
    void test999BadSubscriberPolicies() throws Exception {
        final StandardYamlCoder yamlCoder = new StandardYamlCoder();
        //
        // Decode it
        //
        String policyYaml = ResourceUtils.getResourceAsString("src/test/resources/bad-subscriber-policies.yaml");
        //
        // Serialize it into a class
        //
        JpaToscaServiceTemplate jtst = new JpaToscaServiceTemplate();
        ToscaServiceTemplate serviceTemplate = yamlCoder.decode(policyYaml, ToscaServiceTemplate.class);
        jtst.fromAuthorative(serviceTemplate);
        //
        // Make sure all the fields are setup properly
        //
        ToscaServiceTemplate completedJtst = jtst.toAuthorative();
        //
        // Get the policies
        //
        for (Map<String, ToscaPolicy> policies : completedJtst.getToscaTopologyTemplate().getPolicies()) {
            for (ToscaPolicy policy : policies.values()) {
                if ("missing-subscriberProperties".equals(policy.getName())) {
                    assertThatExceptionOfType(XacmlApplicationException.class).isThrownBy(() ->
                        service.loadPolicy(policy));
                } else if ("missing-subscriberName".equals(policy.getName())) {
                    assertThatExceptionOfType(XacmlApplicationException.class).isThrownBy(() ->
                        service.loadPolicy(policy));
                } else if ("missing-subscriberRole".equals(policy.getName())) {
                    assertThatExceptionOfType(XacmlApplicationException.class).isThrownBy(() ->
                        service.loadPolicy(policy));
                }
            }
        }
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

    private DecisionResponse validateDecisionCount(int expectedPolicyCount) {
        //
        // Ask for a decision for default
        //
        DecisionResponse response = makeDecision();

        assertThat(response).isNotNull();
        assertThat(response.getPolicies()).hasSize(expectedPolicyCount);
        //
        // Optimization applications should not have this information returned
        //
        assertThat(response.getObligations()).isNull();
        assertThat(response.getAttributes()).isNull();
        //
        // Validate it
        //
        validateDecision(response, baseRequest);

        return response;
    }

    private void validateDecisionCount(int expectedPolicyCount, int expectedAdviceCount) {
        DecisionResponse response = validateDecisionCount(expectedPolicyCount);

        assertThat(response.getAdvice()).hasSize(expectedAdviceCount);
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

            validateMatchable((Collection<String>) request.getResource().get("scope"),
                (Collection<String>) policyProperties.get("scope"));

            validateMatchable((Collection<String>) request.getResource().get("services"),
                (Collection<String>) policyProperties.get("services"));

            validateMatchable((Collection<String>) request.getResource().get("resources"),
                (Collection<String>) policyProperties.get("resources"));

            validateMatchable((Collection<String>) request.getResource().get("geography"),
                (Collection<String>) policyProperties.get("geography"));
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

    @SuppressWarnings("unchecked")
    private void cleanOutResources() {
        ((List<String>) baseRequest.getResource().get("scope")).clear();
        ((List<String>) baseRequest.getResource().get("services")).clear();
        ((List<String>) baseRequest.getResource().get("resources")).clear();
        ((List<String>) baseRequest.getResource().get("geography")).clear();
        if (((List<String>) baseRequest.getResource().get("policy-type")) != null) {
            baseRequest.getResource().remove("policy-type");
        }
    }
}
