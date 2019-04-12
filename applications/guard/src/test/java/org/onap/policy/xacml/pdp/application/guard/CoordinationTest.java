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
import java.io.IOException;
import java.sql.Date;
import java.time.Instant;
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
import org.onap.policy.pdp.xacml.application.common.TestUtils;
import org.onap.policy.pdp.xacml.application.common.XacmlApplicationException;
import org.onap.policy.pdp.xacml.application.common.XacmlApplicationServiceProvider;
import org.onap.policy.pdp.xacml.application.common.XacmlPolicyUtils;
import org.onap.policy.pdp.xacml.application.common.operationshistory.CountRecentOperationsPip;
import org.onap.policy.pdp.xacml.application.common.operationshistory.Dbao;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class CoordinationTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(CoordinationTest.class);
    private static Properties properties = new Properties();
    private static File propertiesFile;
    private static XacmlApplicationServiceProvider service;
    private static DecisionRequest requestCl1Node1;
    private static DecisionRequest requestCl1Node2;
    private static DecisionRequest requestCl2Node1;
    private static DecisionRequest requestCl2Node2;
    private static StandardCoder gson = new StandardCoder();
    private static EntityManager em;
    private static final String DENY = "Deny";
    private static final String PERMIT = "Permit";
    private static final String OPEN = "Success";
    private static final String CLOSE = "Closed";


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
        requestCl1Node1 = gson.decode(
                TextFileUtils.getTextFileAsString(
                    "src/test/resources/requests/coordination.cl.1.node.1.json"),
                    DecisionRequest.class);
        requestCl2Node1 = gson.decode(
                TextFileUtils.getTextFileAsString(
                    "src/test/resources/requests/coordination.cl.2.node.1.json"),
                    DecisionRequest.class);
        requestCl1Node2 = gson.decode(
                TextFileUtils.getTextFileAsString(
                    "src/test/resources/requests/coordination.cl.1.node.2.json"),
                    DecisionRequest.class);
        requestCl2Node2 = gson.decode(
                TextFileUtils.getTextFileAsString(
                    "src/test/resources/requests/coordination.cl.2.node.2.json"),
                    DecisionRequest.class);
        //
        // Create EntityManager for manipulating DB
        //
        String persistenceUnit = CountRecentOperationsPip.ISSUER_NAME + ".persistenceunit";
        em = Persistence.createEntityManagerFactory(
                CoordinationTest.properties.getProperty(persistenceUnit), properties)
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
    public void test1() throws CoderException, IOException, XacmlApplicationException {
        LOGGER.info("**************** Running test1 ****************");
        //
        // Now load the test coordination policy - make sure
        // the pdp can support it and have it load
        // into the PDP.
        //
        TestUtils.loadPolicies("src/test/resources/test.policy.guard.coordination.firstBlocksSecond.tosca.yaml",
                service);
        //
        // cl1 doesn't have open action: cl2 should get permit
        //
        requestAndCheckDecision(requestCl2Node1, PERMIT);
        //
        // Open cl2 on node1
        //
        insertOperationEvent(requestCl2Node1, OPEN);
        //
        // Under current coordination policy cl1 always can go
        //
        requestAndCheckDecision(requestCl1Node1, PERMIT);
        //
        // Open cl1 on node1
        //
        insertOperationEvent(requestCl1Node1, OPEN);
        //
        // Close cl2 on node1
        //
        insertOperationEvent(requestCl2Node1, CLOSE);
        //
        // Try cl2 again, cl1 has open action on node1: should get deny
        //
        requestAndCheckDecision(requestCl2Node1, DENY);
        //
        // Close cl1 on node1
        //
        insertOperationEvent(requestCl1Node1, CLOSE);
        //
        // Under current coordination policy cl1 always can go
        //
        requestAndCheckDecision(requestCl1Node1, PERMIT);
        //
        // Open cl1 on node1
        //
        insertOperationEvent(requestCl1Node1, OPEN);
        //
        // Try cl2 on node2, cl1 only open on node1: should get permit
        //
        requestAndCheckDecision(requestCl2Node2, PERMIT);
        //
        // Open cl2 on node2
        //
        insertOperationEvent(requestCl2Node2, OPEN);
        //
        // Try cl2 on node1, cl1 open on node1: should get DENY
        //
        requestAndCheckDecision(requestCl2Node1, DENY);
    }

    @SuppressWarnings("unchecked")
    private void insertOperationEvent(DecisionRequest request, String outcome) {
        //
        // Get the properties
        //
        Map<String, Object> properties = (Map<String, Object>) request.getResource().get("guard");
        //
        // Add an entry
        //
        Dbao newEntry = new Dbao();
        newEntry.setActor(properties.get("actor").toString());
        newEntry.setOperation(properties.get("recipe").toString());
        newEntry.setClosedLoopName(properties.get("clname").toString());
        newEntry.setOutcome(outcome);
        newEntry.setStarttime(Date.from(Instant.now().minusMillis(20000)));
        newEntry.setEndtime(Date.from(Instant.now()));
        newEntry.setRequestId(UUID.randomUUID().toString());
        newEntry.setTarget(properties.get("target").toString());
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
