/*-
 * ============LICENSE_START=======================================================
 * ONAP
 * ================================================================================
 * Copyright (C) 2019 AT&T Intellectual Property. All rights reserved.
   Modifications Copyright (C) 2019 Nordix Foundation.
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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.att.research.xacml.api.Response;
import com.google.common.collect.Lists;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.ServiceLoader;
import org.apache.commons.lang3.tuple.Pair;
import org.assertj.core.api.Condition;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runners.MethodSorters;
import org.onap.policy.common.endpoints.parameters.RestServerParameters;
import org.onap.policy.common.utils.coder.CoderException;
import org.onap.policy.common.utils.coder.StandardCoder;
import org.onap.policy.common.utils.resources.ResourceUtils;
import org.onap.policy.common.utils.resources.TextFileUtils;
import org.onap.policy.models.decisions.concepts.DecisionRequest;
import org.onap.policy.models.decisions.concepts.DecisionResponse;
import org.onap.policy.models.tosca.authorative.concepts.ToscaPolicyTypeIdentifier;
import org.onap.policy.pdp.xacml.application.common.XacmlApplicationException;
import org.onap.policy.pdp.xacml.application.common.XacmlApplicationServiceProvider;
import org.onap.policy.pdp.xacml.application.common.XacmlPolicyUtils;
import org.onap.policy.pdp.xacml.xacmltest.TestUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class OptimizationPdpApplicationTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(OptimizationPdpApplicationTest.class);
    private static Properties properties = new Properties();
    private static File propertiesFile;
    private static XacmlApplicationServiceProvider service;
    private static StandardCoder gson = new StandardCoder();
    private static DecisionRequest baseRequest;
    private static RestServerParameters clientParams;
    private static String[] listPolicyTypeFiles = {
        "onap.policies.Optimization",
        "onap.policies.optimization.AffinityPolicy",
        "onap.policies.optimization.DistancePolicy",
        "onap.policies.optimization.HpaPolicy",
        "onap.policies.optimization.OptimizationPolicy",
        "onap.policies.optimization.PciPolicy",
        "onap.policies.optimization.QueryPolicy",
        "onap.policies.optimization.SubscriberPolicy",
        "onap.policies.optimization.Vim_fit",
        "onap.policies.optimization.VnfPolicy"};

    @ClassRule
    public static final TemporaryFolder policyFolder = new TemporaryFolder();

    /**
     * Copies the xacml.properties and policies files into
     * temporary folder and loads the service provider saving
     * instance of provider off for other tests to use.
     */
    @BeforeClass
    public static void setUp() throws Exception {
        clientParams = mock(RestServerParameters.class);
        when(clientParams.getHost()).thenReturn("localhost");
        when(clientParams.getPort()).thenReturn(6969);
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
        XacmlPolicyUtils.FileCreator myCreator = (String filename) -> policyFolder.newFile(filename);
        propertiesFile = XacmlPolicyUtils.copyXacmlPropertiesContents("src/test/resources/xacml.properties",
                properties, myCreator);
        //
        // Copy the test policy types into data area
        //
        for (String policy : listPolicyTypeFiles) {
            String policyType = ResourceUtils.getResourceAsString("policytypes/" + policy + ".yaml");
            LOGGER.info("Copying {}", policyType);
            Files.write(Paths.get(policyFolder.getRoot().getAbsolutePath(), policy + "-1.0.0.yaml"),
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
        Iterator<XacmlApplicationServiceProvider> iterator = applicationLoader.iterator();
        while (iterator.hasNext()) {
            XacmlApplicationServiceProvider application = iterator.next();
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
        service.initialize(propertiesFile.toPath().getParent(), clientParams);
    }

    @Test
    public void test01Basics() {
        //
        // Make sure there's an application name
        //
        assertThat(service.applicationName()).isNotEmpty();
        //
        // Decisions
        //
        assertThat(service.actionDecisionsSupported().size()).isEqualTo(1);
        assertThat(service.actionDecisionsSupported()).contains("optimize");
        //
        // Ensure it has the supported policy types and
        // can support the correct policy types.
        //
        assertThat(service.canSupportPolicyType(new ToscaPolicyTypeIdentifier(
                "onap.policies.optimization.AffinityPolicy", "1.0.0"))).isTrue();
        assertThat(service.canSupportPolicyType(new ToscaPolicyTypeIdentifier(
                "onap.foobar", "1.0.0"))).isFalse();
    }

    @Test
    public void test02NoPolicies() {
        //
        // Ask for a decision when there are no policies loaded
        //
        Pair<DecisionResponse, Response> decision = service.makeDecision(baseRequest, null);
        LOGGER.info("Decision {}", decision.getKey());

        assertThat(decision.getKey()).isNotNull();
        assertThat(decision.getKey().getPolicies().size()).isEqualTo(0);
    }

    @Test
    public void test03OptimizationDefault() throws CoderException, FileNotFoundException, IOException,
        XacmlApplicationException {
        //
        // Now load all the optimization policies
        //
        TestUtils.loadPolicies("src/test/resources/vCPE.policies.optimization.input.tosca.yaml", service);
        //
        // Ask for a decision for default
        //
        Pair<DecisionResponse, Response> decision = service.makeDecision(baseRequest, null);
        LOGGER.info("Decision {}", decision.getKey());

        assertThat(decision.getKey()).isNotNull();
        assertThat(decision.getKey().getPolicies().size()).isEqualTo(1);
        //
        // Double check that the contents are what we expect
        //
        LOGGER.info(gson.encode(decision.getKey()));
        //
        // Validate it
        //
        validateDecision(decision.getKey(), baseRequest);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void test04OptimizationDefaultGeography() throws CoderException {
        //
        // Add US to the geography list
        //
        ((List<String>)baseRequest.getResource().get("geography")).add("US");
        //
        // Ask for a decision for default US Policy
        //
        Pair<DecisionResponse, Response> decision = service.makeDecision(baseRequest, null);
        LOGGER.info("Decision {}", decision.getKey());

        assertThat(decision.getKey()).isNotNull();
        assertThat(decision.getKey().getPolicies().size()).isEqualTo(2);
        //
        // Double check that the contents are what we expect
        //
        LOGGER.info(gson.encode(decision.getKey()));
        //
        // Validate it
        //
        validateDecision(decision.getKey(), baseRequest);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void test05OptimizationDefaultGeographyAndService() throws CoderException {
        //
        // Add vCPE to the service list
        //
        ((List<String>)baseRequest.getResource().get("services")).add("vCPE");
        //
        // Ask for a decision for default US policy for vCPE service
        //
        Pair<DecisionResponse, Response> decision = service.makeDecision(baseRequest, null);
        LOGGER.info("Decision {}", decision.getKey());

        assertThat(decision.getKey()).isNotNull();
        assertThat(decision.getKey().getPolicies().size()).isEqualTo(5);
        //
        // Double check that the contents are what we expect
        //
        LOGGER.info(gson.encode(decision.getKey()));
        //
        // Validate it
        //
        validateDecision(decision.getKey(), baseRequest);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void test06OptimizationDefaultGeographyAndServiceAndResource() throws CoderException {
        //
        // Add vCPE to the service list
        //
        ((List<String>)baseRequest.getResource().get("resources")).add("vG");
        //
        // Ask for a decision for default US service vCPE resource vG policy
        //
        Pair<DecisionResponse, Response> decision = service.makeDecision(baseRequest, null);
        LOGGER.info("Decision {}", decision.getKey());

        assertThat(decision.getKey()).isNotNull();
        assertThat(decision.getKey().getPolicies().size()).isEqualTo(9);
        //
        // Double check that the contents are what we expect
        //
        LOGGER.info(gson.encode(decision.getKey()));
        //
        // Validate it
        //
        validateDecision(decision.getKey(), baseRequest);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void test07OptimizationGeographyAndServiceAndResourceAndScope() throws CoderException {
        //
        // Add gold as a scope
        //
        ((List<String>)baseRequest.getResource().get("scope")).add("gold");
        //
        // Ask for a decision for specific US vCPE vG gold
        //
        Pair<DecisionResponse, Response> decision = service.makeDecision(baseRequest, null);
        LOGGER.info("Decision {}", decision.getKey());

        assertThat(decision.getKey()).isNotNull();
        assertThat(decision.getKey().getPolicies().size()).isEqualTo(10);
        //
        // Double check that the contents are what we expect
        //
        LOGGER.info(gson.encode(decision.getKey()));
        //
        // Validate it
        //
        validateDecision(decision.getKey(), baseRequest);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void test08OptimizationGeographyAndServiceAndResourceAndScopeIsGoldOrPlatinum() throws CoderException {
        //
        // Add platinum to the scope list: this is now gold OR platinum
        //
        ((List<String>)baseRequest.getResource().get("scope")).add("platinum");
        //
        // Ask for a decision for specific US vCPE vG (gold or platinum)
        //
        Pair<DecisionResponse, Response> decision = service.makeDecision(baseRequest, null);
        LOGGER.info("Decision {}", decision.getKey());

        assertThat(decision.getKey()).isNotNull();
        assertThat(decision.getKey().getPolicies().size()).isEqualTo(11);
        //
        // Double check that the contents are what we expect
        //
        LOGGER.info(gson.encode(decision.getKey()));
        //
        // Validate it
        //
        validateDecision(decision.getKey(), baseRequest);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void test09OptimizationGeographyAndServiceAndResourceAndScopeNotGold() throws CoderException {
        //
        // Add gold as a scope
        //
        ((List<String>)baseRequest.getResource().get("scope")).remove("gold");
        //
        // Ask for a decision for specific US vCPE vG gold
        //
        Pair<DecisionResponse, Response> decision = service.makeDecision(baseRequest, null);
        LOGGER.info("Decision {}", decision.getKey());

        assertThat(decision.getKey()).isNotNull();
        assertThat(decision.getKey().getPolicies().size()).isEqualTo(11);
        //
        // Double check that the contents are what we expect
        //
        LOGGER.info(gson.encode(decision.getKey()));
        //
        // Validate it
        //
        validateDecision(decision.getKey(), baseRequest);
    }

    @Test
    public void test10OptimizationPolicyTypeDefault() throws CoderException {
        //
        // Remove all the other resources from the request
        //
        cleanOutResources();
        //
        // Add in policy type
        //
        List<String> policyTypes = Lists.newArrayList("onap.policies.optimization.AffinityPolicy");
        baseRequest.getResource().put("policy-type", policyTypes);
        //
        // Ask for a decision for default
        //
        Pair<DecisionResponse, Response> decision = service.makeDecision(baseRequest, null);
        LOGGER.info("Decision {}", decision.getKey());

        assertThat(decision.getKey()).isNotNull();
        assertThat(decision.getKey().getPolicies().size()).isEqualTo(4);
        //
        // Double check that the contents are what we expect
        //
        LOGGER.info(gson.encode(decision.getKey()));
    }

    @Test
    public void test20OptimizationPolicyTypeDefault() throws CoderException {
        //
        // Remove all the other resources from the request
        //
        cleanOutResources();
        //
        // Add in policy type
        //
        List<String> policyTypes = Lists.newArrayList("onap.policies.optimization.HpaPolicy");
        baseRequest.getResource().put("policy-type", policyTypes);
        //
        // Ask for a decision for default
        //
        Pair<DecisionResponse, Response> decision = service.makeDecision(baseRequest, null);
        LOGGER.info("Decision {}", decision.getKey());

        assertThat(decision.getKey()).isNotNull();
        assertThat(decision.getKey().getPolicies().size()).isEqualTo(1);
        //
        // Double check that the contents are what we expect
        //
        LOGGER.info(gson.encode(decision.getKey()));
    }

    @SuppressWarnings("unchecked")
    private void validateDecision(DecisionResponse decision, DecisionRequest request) {
        for (Entry<String, Object> entrySet : decision.getPolicies().entrySet()) {
            LOGGER.info("Decision Returned Policy {}", entrySet.getKey());
            assertThat(entrySet.getValue()).isInstanceOf(Map.class);
            Map<String, Object> policyContents = (Map<String, Object>) entrySet.getValue();
            assertThat(policyContents.containsKey("properties")).isTrue();
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
        ((List<String>)baseRequest.getResource().get("scope")).clear();
        ((List<String>)baseRequest.getResource().get("services")).clear();
        ((List<String>)baseRequest.getResource().get("resources")).clear();
        ((List<String>)baseRequest.getResource().get("geography")).clear();
    }
}
