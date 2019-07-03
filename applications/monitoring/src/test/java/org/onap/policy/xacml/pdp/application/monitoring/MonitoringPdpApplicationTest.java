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

import com.att.research.xacml.api.Response;
import java.io.File;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import java.util.ServiceLoader;
import org.apache.commons.lang3.tuple.Pair;
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
import org.onap.policy.models.tosca.authorative.concepts.ToscaPolicy;
import org.onap.policy.models.tosca.authorative.concepts.ToscaPolicyTypeIdentifier;
import org.onap.policy.pdp.xacml.application.common.TestUtils;
import org.onap.policy.pdp.xacml.application.common.XacmlApplicationException;
import org.onap.policy.pdp.xacml.application.common.XacmlApplicationServiceProvider;
import org.onap.policy.pdp.xacml.application.common.XacmlPolicyUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class MonitoringPdpApplicationTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(MonitoringPdpApplicationTest.class);
    private static Properties properties = new Properties();
    private static File propertiesFile;
    private static XacmlApplicationServiceProvider service;
    private static DecisionRequest requestSinglePolicy;
    private static DecisionRequest requestPolicyType;

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
        // Load Single Decision Request
        //
        requestPolicyType = gson.decode(
                TextFileUtils
                .getTextFileAsString("../../main/src/test/resources/decisions/decision.policytype.input.json"),
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
        assertThat(service.canSupportPolicyType(new ToscaPolicyTypeIdentifier("onap.Monitoring", "1.0.0"))).isTrue();
        assertThat(service.canSupportPolicyType(new ToscaPolicyTypeIdentifier("onap.Monitoring", "1.5.0"))).isTrue();
        assertThat(service.canSupportPolicyType(new ToscaPolicyTypeIdentifier(
                "onap.policies.monitoring.foobar", "1.0.1"))).isTrue();
        assertThat(service.canSupportPolicyType(new ToscaPolicyTypeIdentifier("onap.foobar", "1.0.0"))).isFalse();
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
        Pair<DecisionResponse, Response> decision = service.makeDecision(requestSinglePolicy);
        LOGGER.info("Decision {}", decision);

        assertThat(decision.getKey()).isNotNull();
        assertThat(decision.getKey().getPolicies().size()).isEqualTo(0);
    }

    @Test
    public void test3AddvDnsPolicy() throws IOException, CoderException, XacmlApplicationException {
        //
        // Now load the vDNS Policy - make sure
        // the pdp can support it and have it load
        // into the PDP.
        //
        //
        // Now load the monitoring policies
        //
        final List<ToscaPolicy> loadedPolicies = TestUtils.loadPolicies("src/test/resources/vDNS.policy.input.yaml",
                service);
        //
        // Ask for a decision
        //
        Pair<DecisionResponse, Response> decision = service.makeDecision(requestSinglePolicy);
        LOGGER.info("Decision {}", decision);

        assertThat(decision.getKey()).isNotNull();
        assertThat(decision.getKey().getPolicies().size()).isEqualTo(1);
        //
        // Dump it out as Json
        //
        LOGGER.info(gson.encode(decision.getKey()));
        //
        // Ask for a decision based on policy-type
        //
        decision = service.makeDecision(requestPolicyType);
        LOGGER.info("Decision {}", decision);

        assertThat(decision.getKey()).isNotNull();
        assertThat(decision.getKey().getPolicies().size()).isEqualTo(1);
        //
        // Dump it out as Json
        //
        LOGGER.info(gson.encode(decision.getKey()));
        //
        // Now unload it
        //
        LOGGER.info("Now testing unloading of policy");
        for (ToscaPolicy policy : loadedPolicies) {
            assertThat(service.unloadPolicy(policy)).isTrue();
        }
        //
        // Ask for a decision
        //
        decision = service.makeDecision(requestSinglePolicy);
        LOGGER.info("Decision {}", decision.getKey());

        assertThat(decision.getKey()).isNotNull();
        assertThat(decision.getKey().getPolicies().size()).isEqualTo(0);
    }

}
