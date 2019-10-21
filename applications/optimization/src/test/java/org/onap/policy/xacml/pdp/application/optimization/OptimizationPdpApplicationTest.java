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

package org.onap.policy.xacml.pdp.application.optimization;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.att.research.xacml.api.Response;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Iterator;
import java.util.Properties;
import java.util.ServiceLoader;
import org.apache.commons.lang3.tuple.Pair;
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
    private static DecisionRequest requestAffinity;
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
        requestAffinity = gson.decode(
                TextFileUtils
                    .getTextFileAsString(
                            "../../main/src/test/resources/decisions/decision.optimization.affinity.input.json"),
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
        StringBuilder strDump = new StringBuilder("Loaded applications:" + System.lineSeparator());
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
            strDump.append(System.lineSeparator());
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
    public void test1Basics() {
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
    public void test2NoPolicies() {
        //
        // Ask for a decision
        //
        Pair<DecisionResponse, Response> decision = service.makeDecision(requestAffinity, null);
        LOGGER.info("Decision {}", decision.getKey());

        assertThat(decision.getKey()).isNotNull();
        assertThat(decision.getKey().getPolicies().size()).isEqualTo(0);
    }

    @Test
    public void test3AddOptimizationPolicies() throws CoderException, FileNotFoundException, IOException,
        XacmlApplicationException {
        //
        // Now load the optimization policies
        //
        TestUtils.loadPolicies("src/test/resources/vCPE.policies.optimization.input.tosca.yaml", service);
        //
        // Ask for a decision
        //
        Pair<DecisionResponse, Response> decision = service.makeDecision(requestAffinity, null);
        LOGGER.info("Decision {}", decision.getKey());

        assertThat(decision.getKey()).isNotNull();
        assertThat(decision.getKey().getPolicies().size()).isEqualTo(4);
        //
        // Dump it out as Json
        //
        LOGGER.info(gson.encode(decision.getKey()));
    }
}
