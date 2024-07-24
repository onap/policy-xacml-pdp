/*-
 * ============LICENSE_START=======================================================
 * ONAP
 * ================================================================================
 * Copyright (C) 2020-2021 AT&T Intellectual Property. All rights reserved.
 * Modifications Copyright (C) 2023-2024 Nordix Foundation.
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
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Persistence;
import java.io.File;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Date;
import java.util.Map;
import java.util.Properties;
import java.util.ServiceLoader;
import java.util.UUID;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.io.TempDir;
import org.onap.policy.common.utils.coder.CoderException;
import org.onap.policy.common.utils.coder.StandardCoder;
import org.onap.policy.common.utils.resources.TextFileUtils;
import org.onap.policy.guard.OperationsHistory;
import org.onap.policy.models.decisions.concepts.DecisionRequest;
import org.onap.policy.models.decisions.concepts.DecisionResponse;
import org.onap.policy.pdp.xacml.application.common.XacmlApplicationException;
import org.onap.policy.pdp.xacml.application.common.XacmlApplicationServiceProvider;
import org.onap.policy.pdp.xacml.application.common.XacmlPolicyUtils;
import org.onap.policy.pdp.xacml.application.common.operationshistory.CountRecentOperationsPip;
import org.onap.policy.pdp.xacml.xacmltest.TestUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@TestMethodOrder(MethodOrderer.MethodName.class)
class SonCoordinationTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(SonCoordinationTest.class);
    private static final Properties properties = new Properties();
    private static File propertiesFile;
    private static XacmlApplicationServiceProvider service;
    private static DecisionRequest requestVpciNode1;
    private static DecisionRequest requestVsonhNode1;
    private static final StandardCoder gson = new StandardCoder();
    private static EntityManager em;
    private static EntityManagerFactory emf;
    private static final String DENY = "Deny";
    private static final String PERMIT = "Permit";

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
        // Set up our temporary folder
        //
        XacmlPolicyUtils.FileCreator myCreator =
            (String filename) -> policyFolder.resolve(filename).toFile();
        propertiesFile = XacmlPolicyUtils.copyXacmlPropertiesContents(
            "src/test/resources/xacml.properties", properties, myCreator);
        //
        // Load service
        //
        ServiceLoader<XacmlApplicationServiceProvider> applicationLoader =
            ServiceLoader.load(XacmlApplicationServiceProvider.class);
        //
        // Find the guard service application and save for use in all the tests
        //
        StringBuilder strDump =
            new StringBuilder("Loaded applications:" + XacmlPolicyUtils.LINE_SEPARATOR);
        for (XacmlApplicationServiceProvider application : applicationLoader) {
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
            strDump.append(XacmlPolicyUtils.LINE_SEPARATOR);
        }
        LOGGER.info("{}", strDump);
        //
        // Tell it to initialize based on the properties file
        // we just built for it.
        //
        service.initialize(propertiesFile.toPath().getParent(), null);
        //
        // Load Decision Requests
        //
        requestVpciNode1 = gson.decode(
            TextFileUtils.getTextFileAsString(
                "src/test/resources/requests/coordination.cl.vPci.node.1.json"),
            DecisionRequest.class);
        requestVsonhNode1 = gson.decode(
            TextFileUtils.getTextFileAsString(
                "src/test/resources/requests/coordination.cl.vSonh.node.1.json"),
            DecisionRequest.class);
        String persistenceUnit = CountRecentOperationsPip.ISSUER_NAME + ".persistenceunit";
        emf = Persistence.createEntityManagerFactory(
            SonCoordinationTest.properties.getProperty(persistenceUnit), properties);
        em = emf.createEntityManager();
    }

    /**
     * Clears the database before each test.
     */
    @BeforeEach
    void startClean() {
        em.getTransaction().begin();
        em.createQuery("DELETE FROM OperationsHistory").executeUpdate();
        em.getTransaction().commit();
    }

    /**
     * Close the entity manager.
     */
    @AfterAll
    static void cleanup() {
        if (em != null) {
            em.close();
        }
        if (emf != null) {
            emf.close();
        }
    }

    /**
     * Check that decision matches expectation.
     *
     * @param expected from the response
     * @param response received
     **/
    void checkDecision(String expected, DecisionResponse response) throws CoderException {
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
     * @param request  to send to Xacml PDP
     * @param expected from the response
     **/
    void requestAndCheckDecision(DecisionRequest request, String expected)
        throws CoderException {

        //
        // Ask for a decision
        //
        Pair<DecisionResponse, Response> decision = service.makeDecision(request, null);
        //
        // Check decision
        //
        checkDecision(expected, decision.getKey());
    }

    @Test
    void test1() throws CoderException, XacmlApplicationException {
        LOGGER.info("**************** Running vPci and vSonh Control Loops ****************");
        //
        // Now load the test coordination policy - make sure
        // the pdp can support it and have it load
        // into the PDP.
        //
        TestUtils.loadPolicies(
            "src/test/resources/test.policy.guard.coordination.vPciBlocksVsonh.tosca.yaml",
            service);
        TestUtils.loadPolicies(
            "src/test/resources/test.policy.guard.coordination.vSonhBlocksVpci.tosca.yaml",
            service);
        //
        // vSonh doesn't have open action: vPci should get permit
        //
        requestAndCheckDecision(requestVpciNode1, PERMIT);
        //
        // vPci doesn't have open action: vSonh should get permit
        //
        requestAndCheckDecision(requestVsonhNode1, PERMIT);
        //
        // Open vSonh on node1
        //
        long vsonhId = insertOperationEvent(requestVsonhNode1);
        //
        // Under current coordination policy vPci should get a deny
        //
        requestAndCheckDecision(requestVpciNode1, DENY);
        //
        // Close vSonh on node1
        //
        updateOperationEvent(vsonhId, "Success");
        //
        // With vSonh closed on node 1, vPci now should get a permit
        //
        requestAndCheckDecision(requestVpciNode1, PERMIT);
        //
        // Open vPci on node1
        //
        long vpciId = insertOperationEvent(requestVpciNode1);
        //
        // Under current coordination policy vSonh should get a deny
        //
        requestAndCheckDecision(requestVsonhNode1, DENY);
        //
        // Close cl1 on node1
        //
        updateOperationEvent(vpciId, "Failed");
        //
        // With vPci closed on node 1, vSonh now should get a permit
        //
        requestAndCheckDecision(requestVsonhNode1, PERMIT);
    }

    @SuppressWarnings("unchecked")
    private long insertOperationEvent(DecisionRequest request) {
        //
        // Get the properties
        //
        Map<String, Object> localProps = (Map<String, Object>) request.getResource().get("guard");
        //
        // Add an entry
        //
        OperationsHistory newEntry = new OperationsHistory();
        newEntry.setActor(localProps.get("actor").toString());
        newEntry.setOperation(localProps.get("operation").toString());
        newEntry.setClosedLoopName(localProps.get("clname").toString());
        newEntry.setOutcome("Started");
        newEntry.setStarttime(Date.from(Instant.now().minusMillis(20000)));
        newEntry.setEndtime(Date.from(Instant.now()));
        newEntry.setRequestId(UUID.randomUUID().toString());
        newEntry.setTarget(localProps.get("target").toString());
        em.getTransaction().begin();
        em.persist(newEntry);
        em.getTransaction().commit();
        return newEntry.getId();
    }

    private void updateOperationEvent(long id, String outcome) {
        OperationsHistory updateEntry = em.find(OperationsHistory.class, id);
        updateEntry.setOutcome(outcome);
        updateEntry.setEndtime(Date.from(Instant.now()));
        em.getTransaction().begin();
        em.persist(updateEntry);
        em.getTransaction().commit();
    }

}
