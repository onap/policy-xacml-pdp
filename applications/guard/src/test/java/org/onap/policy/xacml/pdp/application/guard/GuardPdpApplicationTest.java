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

import com.att.research.xacml.api.Response;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.sql.Date;
import java.time.Instant;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;
import java.util.ServiceLoader;
import java.util.UUID;

import javax.persistence.EntityManager;
import javax.persistence.Persistence;

import org.apache.commons.lang3.tuple.Pair;
import org.junit.AfterClass;
import org.junit.Before;
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
import org.onap.policy.models.tosca.authorative.concepts.ToscaPolicyTypeIdentifier;
import org.onap.policy.pdp.xacml.application.common.TestUtils;
import org.onap.policy.pdp.xacml.application.common.XacmlApplicationException;
import org.onap.policy.pdp.xacml.application.common.XacmlApplicationServiceProvider;
import org.onap.policy.pdp.xacml.application.common.XacmlPolicyUtils;
import org.onap.policy.pdp.xacml.application.common.operationshistory.CountRecentOperationsPip;
import org.onap.policy.pdp.xacml.application.common.operationshistory.Dbao;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class GuardPdpApplicationTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(GuardPdpApplicationTest.class);
    private static Properties properties = new Properties();
    private static File propertiesFile;
    private static XacmlApplicationServiceProvider service;
    private static DecisionRequest requestVfCount1;
    private static DecisionRequest requestVfCount3;
    private static DecisionRequest requestVfCount6;
    private static StandardCoder gson = new StandardCoder();
    private static EntityManager em;
    private static final String DENY = "Deny";
    private static final String PERMIT = "Permit";

    @ClassRule
    public static final TemporaryFolder policyFolder = new TemporaryFolder();

    /**
     * Copies the xacml.properties and policies files into
     * temporary folder and loads the service provider saving
     * instance of provider off for other tests to use.
     */
    @BeforeClass
    public static void setup() throws Exception {
        LOGGER.info("Setting up class");
        //
        // Setup our temporary folder
        //
        XacmlPolicyUtils.FileCreator myCreator = (String filename) -> policyFolder.newFile(filename);
        propertiesFile = XacmlPolicyUtils.copyXacmlPropertiesContents("src/test/resources/xacml.properties",
                properties, myCreator);
        //
        // Load service
        //
        ServiceLoader<XacmlApplicationServiceProvider> applicationLoader =
                ServiceLoader.load(XacmlApplicationServiceProvider.class);
        //
        // Find the guard service application and save for use in all the tests
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
        LOGGER.info("{}", strDump);
        //
        // Tell it to initialize based on the properties file
        // we just built for it.
        //
        service.initialize(propertiesFile.toPath().getParent());
        //
        // Load Decision Requests
        //
        requestVfCount1 = gson.decode(
                TextFileUtils.getTextFileAsString(
                    "src/test/resources/requests/guard.vfCount.1.json"),
                    DecisionRequest.class);
        requestVfCount3 = gson.decode(
                TextFileUtils.getTextFileAsString(
                    "src/test/resources/requests/guard.vfCount.3.json"),
                    DecisionRequest.class);
        requestVfCount6 = gson.decode(
                TextFileUtils.getTextFileAsString(
                    "src/test/resources/requests/guard.vfCount.6.json"),
                    DecisionRequest.class);
        //
        // Create EntityManager for manipulating DB
        //
        String persistenceUnit = CountRecentOperationsPip.ISSUER_NAME + ".persistenceunit";
        em = Persistence.createEntityManagerFactory(
                GuardPdpApplicationTest.properties.getProperty(persistenceUnit), properties)
                .createEntityManager();
    }

    /**
     * Clears the database before each test.
     *
     */
    @Before
    public void startClean() throws Exception {
        em.getTransaction().begin();
        em.createQuery("DELETE FROM Dbao").executeUpdate();
        em.getTransaction().commit();
    }

    /**
     * Check that decision matches expectation.
     *
     * @param expected from the response
     * @param response received
     *
     **/
    public void checkDecision(String expected, DecisionResponse response) throws CoderException {
        LOGGER.info("Looking for {} Decision", expected);
        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isNotNull();
        assertThat(response.getStatus()).isEqualTo(expected);
        //
        // Dump it out as Json
        //
        LOGGER.info(gson.encode(response));
    }

    /**
     * Request a decision and check that it matches expectation.
     *
     * @param request to send to Xacml PDP
     * @param expected from the response
     *
     **/
    public void requestAndCheckDecision(DecisionRequest request, String expected) throws CoderException {
        //
        // Ask for a decision
        //
        Pair<DecisionResponse, Response> decision = service.makeDecision(request);
        //
        // Check decision
        //
        checkDecision(expected, decision.getKey());
    }

    @Test
    public void test1Basics() throws CoderException, IOException {
        LOGGER.info("**************** Running test1 ****************");
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
        assertThat(service.supportedPolicyTypes().size()).isEqualTo(4);
        assertThat(service.canSupportPolicyType(new ToscaPolicyTypeIdentifier(
                "onap.policies.controlloop.guard.FrequencyLimiter", "1.0.0"))).isTrue();
        assertThat(service.canSupportPolicyType(new ToscaPolicyTypeIdentifier(
                "onap.policies.controlloop.guard.FrequencyLimiter", "1.0.1"))).isFalse();
        assertThat(service.canSupportPolicyType(new ToscaPolicyTypeIdentifier(
                "onap.policies.controlloop.guard.MinMax", "1.0.0"))).isTrue();
        assertThat(service.canSupportPolicyType(new ToscaPolicyTypeIdentifier(
                "onap.policies.controlloop.guard.MinMax", "1.0.1"))).isFalse();
        assertThat(service.canSupportPolicyType(new ToscaPolicyTypeIdentifier(
                "onap.policies.controlloop.guard.Blacklist", "1.0.0"))).isTrue();
        assertThat(service.canSupportPolicyType(new ToscaPolicyTypeIdentifier(
                "onap.policies.controlloop.guard.Blacklist", "1.0.1"))).isFalse();
        assertThat(service.canSupportPolicyType(new ToscaPolicyTypeIdentifier(
                "onap.policies.controlloop.guard.coordination.FirstBlocksSecond", "1.0.0"))).isTrue();
        assertThat(service.canSupportPolicyType(new ToscaPolicyTypeIdentifier(
                "onap.policies.controlloop.guard.coordination.FirstBlocksSecond", "1.0.1"))).isFalse();
        assertThat(service.canSupportPolicyType(new ToscaPolicyTypeIdentifier("onap.foo", "1.0.1"))).isFalse();
    }

    @Test
    public void test2NoPolicies() throws CoderException {
        LOGGER.info("**************** Running test2 ****************");
        requestAndCheckDecision(requestVfCount1,PERMIT);
    }

    @Test
    public void test3FrequencyLimiter() throws CoderException, FileNotFoundException, IOException,
        XacmlApplicationException {
        LOGGER.info("**************** Running test3 ****************");
        //
        // Now load the vDNS frequency limiter Policy - make sure
        // the pdp can support it and have it load
        // into the PDP.
        //
        TestUtils.loadPolicies("src/test/resources/vDNS.policy.guard.frequency.output.tosca.yaml", service);
        //
        // Zero recent actions: should get permit
        //
        requestAndCheckDecision(requestVfCount1,PERMIT);
        //
        // Add entry into operations history DB
        //
        insertOperationEvent(requestVfCount1);
        //
        // Only one recent actions: should get permit
        //
        requestAndCheckDecision(requestVfCount1,PERMIT);
        //
        // Add entry into operations history DB
        //
        insertOperationEvent(requestVfCount1);
        //
        // Two recent actions, more than specified limit of 2: should get deny
        //
        requestAndCheckDecision(requestVfCount1,DENY);
    }

    @Test
    public void test4MinMax() throws CoderException, FileNotFoundException, IOException, XacmlApplicationException {
        LOGGER.info("**************** Running test4 ****************");
        //
        // Now load the vDNS min max Policy - make sure
        // the pdp can support it and have it load
        // into the PDP.
        //
        TestUtils.loadPolicies("src/test/resources/vDNS.policy.guard.minmax.output.tosca.yaml", service);
        //
        // vfcount=1 below min of 2: should get a Deny
        //
        requestAndCheckDecision(requestVfCount1, DENY);
        //
        // vfcount=3 between min of 2 and max of 5: should get a Permit
        //
        requestAndCheckDecision(requestVfCount3, PERMIT);
        //
        // vfcount=6 above max of 5: should get a Deny
        //
        requestAndCheckDecision(requestVfCount6,DENY);
        //
        // Add two entry into operations history DB
        //
        insertOperationEvent(requestVfCount1);
        insertOperationEvent(requestVfCount1);
        //
        // vfcount=3 between min of 2 and max of 5, but 2 recent actions is above frequency limit: should get a Deny
        //
        requestAndCheckDecision(requestVfCount3, DENY);
        //
        // vfcount=6 above max of 5: should get a Deny
        //
        requestAndCheckDecision(requestVfCount6, DENY);
    }

    @Test
    public void test5MissingFields() throws FileNotFoundException, IOException, XacmlApplicationException,
        CoderException {
        LOGGER.info("**************** Running test5 ****************");
        //
        // Most likely we would not get a policy with missing fields passed to
        // us from the API. But in case that happens, or we decide that some fields
        // will be optional due to re-working of how the XACML policies are built,
        // let's add support in for that.
        //
        TestUtils.loadPolicies("src/test/resources/guard.policy-minmax-missing-fields1.yaml", service);
        //
        // We can create a DecisionRequest on the fly - no need
        // to have it in the .json files
        //
        DecisionRequest request = new DecisionRequest();
        request.setOnapName("JUnit");
        request.setOnapComponent("test5MissingFields");
        request.setRequestId(UUID.randomUUID().toString());
        request.setAction("guard");
        Map<String, Object> guard = new HashMap<>();
        guard.put("actor", "FOO");
        guard.put("recipe", "bar");
        guard.put("vfCount", "4");
        Map<String, Object> resource = new HashMap<>();
        resource.put("guard", guard);
        request.setResource(resource);
        //
        // Ask for a decision - should get permit
        //
        Pair<DecisionResponse, Response> decision = service.makeDecision(request);
        LOGGER.info("Looking for Permit Decision {}", decision.getKey());
        assertThat(decision.getKey()).isNotNull();
        assertThat(decision.getKey().getStatus()).isNotNull();
        assertThat(decision.getKey().getStatus()).isEqualTo("Permit");
        //
        // Try a deny
        //
        guard.put("vfCount", "10");
        resource.put("guard", guard);
        request.setResource(resource);
        decision = service.makeDecision(request);
        LOGGER.info("Looking for Deny Decision {}", decision.getKey());
        assertThat(decision.getKey()).isNotNull();
        assertThat(decision.getKey().getStatus()).isNotNull();
        assertThat(decision.getKey().getStatus()).isEqualTo("Deny");
    }

    @Test
    public void test6Blacklist() throws CoderException, XacmlApplicationException {
        LOGGER.info("**************** Running test4 ****************");
        //
        // Setup requestVfCount1 to point to another target for this test
        //
        ((Map<String, Object>)requestVfCount3.getResource().get("guard")).put("targets", "vLoadBalancer-01");
        //
        // vfcount=1 above min of 2: should get a permit
        //
        requestAndCheckDecision(requestVfCount3, PERMIT);
        //
        // Now load the vDNS blacklist policy
        //
        TestUtils.loadPolicies("src/test/resources/vDNS.policy.guard.blacklist.output.tosca.yaml", service);
        //
        // vfcount=1 above min of 2: should get a permit
        //
        requestAndCheckDecision(requestVfCount3, DENY);
    }

    @SuppressWarnings("unchecked")
    private void insertOperationEvent(DecisionRequest request) {
        //
        // Get the properties
        //
        Map<String, Object> properties = (Map<String, Object>) request.getResource().get("guard");
        assertThat(properties).isNotNull();
        //
        // Add an entry
        //
        Dbao newEntry = new Dbao();
        newEntry.setActor(properties.get("actor").toString());
        newEntry.setOperation(properties.get("recipe").toString());
        newEntry.setClosedLoopName(properties.get("clname").toString());
        newEntry.setOutcome("SUCCESS");
        newEntry.setStarttime(Date.from(Instant.now().minusMillis(20000)));
        newEntry.setEndtime(Date.from(Instant.now()));
        newEntry.setRequestId(UUID.randomUUID().toString());
        newEntry.setTarget(properties.get("target").toString());
        LOGGER.info("Inserting {}", newEntry);
        em.getTransaction().begin();
        em.persist(newEntry);
        em.getTransaction().commit();
    }

    /**
     * Close the entity manager.
     */
    @AfterClass
    public static void cleanup() throws Exception {
        if (em != null) {
            em.close();
        }
    }

}
