/*-
 * ============LICENSE_START=======================================================
 * ONAP
 * ================================================================================
 * Copyright (C) 2019-2022 AT&T Intellectual Property. All rights reserved.
 * Modifications Copyright (C) 2021, 2023-2024 Nordix Foundation.
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
import static org.assertj.core.api.Assertions.assertThatCode;

import com.att.research.xacml.api.Response;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Persistence;
import java.io.File;
import java.nio.file.Path;
import java.sql.Date;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.List;
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
import org.onap.policy.models.tosca.authorative.concepts.ToscaConceptIdentifier;
import org.onap.policy.models.tosca.authorative.concepts.ToscaPolicy;
import org.onap.policy.pdp.xacml.application.common.XacmlApplicationException;
import org.onap.policy.pdp.xacml.application.common.XacmlApplicationServiceProvider;
import org.onap.policy.pdp.xacml.application.common.XacmlPolicyUtils;
import org.onap.policy.pdp.xacml.application.common.operationshistory.CountRecentOperationsPip;
import org.onap.policy.pdp.xacml.xacmltest.TestUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@TestMethodOrder(MethodOrderer.MethodName.class)
class GuardPdpApplicationTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(GuardPdpApplicationTest.class);
    private static final Properties properties = new Properties();
    private static File propertiesFile;
    private static XacmlApplicationServiceProvider service;
    private static DecisionRequest requestVfCount;
    private static final StandardCoder gson = new StandardCoder();
    private static EntityManager em;
    private static EntityManagerFactory emf;
    private static final String DENY = "Deny";
    private static final String PERMIT = "Permit";

    @TempDir
    static Path policyFolder;

    /**
     * Copies the xacml.properties and policies files into temporary folder and loads the service provider saving
     * instance of provider off for other tests to use.
     */
    @BeforeAll
    static void setup() throws Exception {
        LOGGER.info("Setting up class");
        //
        // Set up our temporary folder
        //
        XacmlPolicyUtils.FileCreator myCreator = (String filename) -> policyFolder.resolve(filename).toFile();
        propertiesFile = XacmlPolicyUtils.copyXacmlPropertiesContents("src/test/resources/xacml.properties", properties,
            myCreator);
        //
        // Load service
        //
        ServiceLoader<XacmlApplicationServiceProvider> applicationLoader =
            ServiceLoader.load(XacmlApplicationServiceProvider.class);
        //
        // Find the guard service application and save for use in all the tests
        //
        StringBuilder strDump = new StringBuilder("Loaded applications:" + XacmlPolicyUtils.LINE_SEPARATOR);
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
        requestVfCount =
            gson.decode(TextFileUtils.getTextFileAsString("src/test/resources/requests/guard.vfCount.json"),
                DecisionRequest.class);
        //
        // Create EntityManager for manipulating DB
        //
        String persistenceUnit = CountRecentOperationsPip.ISSUER_NAME + ".persistenceunit";
        emf = Persistence.createEntityManagerFactory(
            GuardPdpApplicationTest.properties.getProperty(persistenceUnit), properties);
        em = emf.createEntityManager();
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
     * Clears the database before each test so there are no operations in it.
     */
    @BeforeEach
    void startClean() {
        em.getTransaction().begin();
        em.createQuery("DELETE FROM OperationsHistory").executeUpdate();
        em.getTransaction().commit();
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
        //
        // Guard does not return these
        //
        assertThat(response.getAdvice()).isNull();
        assertThat(response.getObligations()).isNull();
        assertThat(response.getAttributes()).isNull();
    }

    /**
     * Request a decision and check that it matches expectation.
     *
     * @param request  to send to Xacml PDP
     * @param expected from the response
     **/
    void requestAndCheckDecision(DecisionRequest request, String expected) throws CoderException {
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
    void test1Basics() {
        LOGGER.info("**************** Running test1Basics ****************");
        //
        // Make sure there's an application name
        //
        assertThat(service.applicationName()).isNotEmpty();
        //
        // Decisions
        //
        assertThat(service.actionDecisionsSupported()).hasSize(1);
        assertThat(service.actionDecisionsSupported()).contains("guard");
        //
        // Ensure it has the supported policy types and
        // can support the correct policy types.
        //
        assertThat(service.supportedPolicyTypes()).isNotEmpty();
        assertThat(service.supportedPolicyTypes()).hasSize(5);
        assertThat(service.canSupportPolicyType(
            new ToscaConceptIdentifier("onap.policies.controlloop.guard.common.FrequencyLimiter", "1.0.0")))
            .isTrue();
        assertThat(service.canSupportPolicyType(
            new ToscaConceptIdentifier("onap.policies.controlloop.guard.common.FrequencyLimiter", "1.0.1")))
            .isFalse();
        assertThat(service.canSupportPolicyType(
            new ToscaConceptIdentifier("onap.policies.controlloop.guard.common.MinMax", "1.0.0"))).isTrue();
        assertThat(service.canSupportPolicyType(
            new ToscaConceptIdentifier("onap.policies.controlloop.guard.common.MinMax", "1.0.1"))).isFalse();
        assertThat(service.canSupportPolicyType(
            new ToscaConceptIdentifier("onap.policies.controlloop.guard.common.Blacklist", "1.0.0"))).isTrue();
        assertThat(service.canSupportPolicyType(
            new ToscaConceptIdentifier("onap.policies.controlloop.guard.common.Blacklist", "1.0.1"))).isFalse();
        assertThat(service.canSupportPolicyType(new ToscaConceptIdentifier(
            "onap.policies.controlloop.guard.coordination.FirstBlocksSecond", "1.0.0"))).isTrue();
        assertThat(service.canSupportPolicyType(new ToscaConceptIdentifier(
            "onap.policies.controlloop.guard.coordination.FirstBlocksSecond", "1.0.1"))).isFalse();
        assertThat(service.canSupportPolicyType(new ToscaConceptIdentifier("onap.foo", "1.0.1"))).isFalse();
        assertThat(service.canSupportPolicyType(
            new ToscaConceptIdentifier("onap.policies.controlloop.guard.common.Filter", "1.0.0"))).isTrue();
    }

    @Test
    void test2NoPolicies() {
        LOGGER.info("**************** Running test2NoPolicies ****************");
        assertThatCode(() -> requestAndCheckDecision(requestVfCount, PERMIT)).doesNotThrowAnyException();
    }

    @Test
    void test3FrequencyLimiter() throws CoderException, XacmlApplicationException {
        LOGGER.info("**************** Running test3FrequencyLimiter ****************");
        //
        // Now load the vDNS frequency limiter Policy - make sure
        // the pdp can support it and have it load
        // into the PDP.
        //
        List<ToscaPolicy> loadedPolicies =
            TestUtils.loadPolicies("policies/vDNS.policy.guard.frequencylimiter.input.tosca.yaml", service);
        assertThat(loadedPolicies).hasSize(1);
        assertThat(loadedPolicies.get(0).getName()).isEqualTo("guard.frequency.scaleout");
        //
        // Zero recent actions: should get permit
        //
        requestAndCheckDecision(requestVfCount, PERMIT);
        //
        // Add entry into operations history DB
        //
        insertOperationEvent(requestVfCount);
        //
        // Two recent actions, more than specified limit of 2: should get deny
        //
        requestAndCheckDecision(requestVfCount, DENY);
    }

    @SuppressWarnings("unchecked")
    @Test
    void test4MinMax() throws CoderException, XacmlApplicationException {
        LOGGER.info("**************** Running test4MinMax ****************");
        //
        // Now load the vDNS min max Policy - make sure
        // the pdp can support it and have it load
        // into the PDP.
        //
        List<ToscaPolicy> loadedPolicies =
            TestUtils.loadPolicies("policies/vDNS.policy.guard.minmaxvnfs.input.tosca.yaml", service);
        assertThat(loadedPolicies).hasSize(1);
        assertThat(loadedPolicies.get(0).getName()).isEqualTo("guard.minmax.scaleout");
        //
        // vfcount=0 below min of 1: should get a Permit
        //
        requestAndCheckDecision(requestVfCount, PERMIT);
        //
        // vfcount=1 between min of 1 and max of 2: should get a Permit
        //
        ((Map<String, Object>) requestVfCount.getResource().get("guard")).put("vfCount", 1);
        requestAndCheckDecision(requestVfCount, PERMIT);
        //
        // vfcount=2 hits the max of 2: should get a Deny
        //
        ((Map<String, Object>) requestVfCount.getResource().get("guard")).put("vfCount", 2);
        requestAndCheckDecision(requestVfCount, DENY);
        //
        // vfcount=3 above max of 2: should get a Deny
        //
        ((Map<String, Object>) requestVfCount.getResource().get("guard")).put("vfCount", 3);
        requestAndCheckDecision(requestVfCount, DENY);
        //
        // Insert entry into operations history DB - to indicate a successful
        // VF Module Create.
        //
        insertOperationEvent(requestVfCount);
        //
        // vfcount=1 between min of 1 and max of 2; MinMax should succeed,
        // BUT the frequency limiter should fail
        //
        ((Map<String, Object>) requestVfCount.getResource().get("guard")).put("vfCount", 1);
        requestAndCheckDecision(requestVfCount, DENY);
    }

    @SuppressWarnings("unchecked")
    @Test
    void test5Blacklist() throws CoderException, XacmlApplicationException {
        LOGGER.info("**************** Running test5Blacklist ****************");
        //
        // Load the blacklist policy in with the others.
        //
        List<ToscaPolicy> loadedPolicies =
            TestUtils.loadPolicies("policies/vDNS.policy.guard.blacklist.input.tosca.yaml", service);
        assertThat(loadedPolicies).hasSize(1);
        assertThat(loadedPolicies.get(0).getName()).isEqualTo("guard.blacklist.scaleout");
        //
        // vfcount=0 below min of 1: should get a Permit because target is NOT blacklisted
        //
        requestAndCheckDecision(requestVfCount, PERMIT);
        //
        // vfcount=1 between min of 1 and max of 2: change the
        //
        ((Map<String, Object>) requestVfCount.getResource().get("guard")).put("target",
            "the-vfmodule-where-root-is-true");
        //
        // vfcount=0 below min of 1: should get a Deny because target IS blacklisted
        //
        requestAndCheckDecision(requestVfCount, DENY);
        //
        // vfcount=1 between min of 1 and max of 2: change the
        //
        ((Map<String, Object>) requestVfCount.getResource().get("guard")).put("target",
            "another-vfmodule-where-root-is-true");
        //
        // vfcount=0 below min of 1: should get a Deny because target IS blacklisted
        //
        requestAndCheckDecision(requestVfCount, DENY);
    }

    @SuppressWarnings("unchecked")
    @Test
    void test6Filters() throws Exception {
        LOGGER.info("**************** Running test6Filters ****************");
        //
        // Re-Load Decision Request - so we can start from scratch
        //
        requestVfCount =
            gson.decode(TextFileUtils.getTextFileAsString("src/test/resources/requests/guard.vfCount.json"),
                DecisionRequest.class);
        //
        // Ensure we are a permit to start
        //
        requestAndCheckDecision(requestVfCount, PERMIT);
        //
        // Load the filter policy in with the others.
        //
        List<ToscaPolicy> loadedPolicies =
            TestUtils.loadPolicies("src/test/resources/test.policy.guard.filters.yaml", service);
        assertThat(loadedPolicies).hasSize(2);
        //
        // Although the region is blacklisted, the id is not
        //
        requestAndCheckDecision(requestVfCount, PERMIT);
        //
        // Put in a different vnf id
        //
        ((Map<String, Object>) requestVfCount.getResource().get("guard")).put("generic-vnf.vnf-id",
            "different-vnf-id-should-be-denied");
        //
        // The region is blacklisted, and the id is not allowed
        //
        requestAndCheckDecision(requestVfCount, DENY);
        //
        // Let's switch to a different region
        //
        ((Map<String, Object>) requestVfCount.getResource().get("guard")).put("cloud-region.cloud-region-id",
            "RegionTwo");
        //
        // The region is whitelisted, and the id is also allowed
        //
        requestAndCheckDecision(requestVfCount, PERMIT);
        //
        // Put in a blacklisted vnf id
        //
        ((Map<String, Object>) requestVfCount.getResource().get("guard")).put("generic-vnf.vnf-id",
            "f17face5-69cb-4c88-9e0b-7426db7edddd");
        //
        // Although region is whitelisted,  the id is blacklisted
        //
        requestAndCheckDecision(requestVfCount, DENY);
        //
        // Let's switch to a different region
        //
        ((Map<String, Object>) requestVfCount.getResource().get("guard")).put("cloud-region.cloud-region-id",
            "RegionThree");
        //
        // There is no filter for this region, but the id is still blacklisted
        //
        requestAndCheckDecision(requestVfCount, DENY);
        //
        // Put in a different vnf id
        //
        ((Map<String, Object>) requestVfCount.getResource().get("guard")).put("generic-vnf.vnf-id",
            "different-vnf-id-should-be-permitted");
        //
        // There is no filter for this region, and the id is not blacklisted
        //
        requestAndCheckDecision(requestVfCount, PERMIT);
    }

    @Test
    void test7TimeInRange() throws Exception {
        LOGGER.info("**************** Running test7TimeInRange ****************");
        //
        // Re-Load Decision Request - so we can start from scratch
        //
        DecisionRequest requestInRange =
            gson.decode(TextFileUtils.getTextFileAsString("src/test/resources/requests/guard.timeinrange.json"),
                DecisionRequest.class);
        //
        // Load the test policy in with the others.
        //
        List<ToscaPolicy> loadedPolicies =
            TestUtils.loadPolicies("src/test/resources/test-time-in-range.yaml", service);
        assertThat(loadedPolicies).hasSize(1);
        //
        // Mock what the current date and time is. Set to 12 Noon
        // We actually do not care about time zone or the date yet, but these are here
        // for future.
        //
        OffsetDateTime offsetDateTime = OffsetDateTime.parse("2020-01-01T12:00:00+05:00");
        requestInRange.setCurrentDateTime(offsetDateTime);
        requestInRange.setCurrentDate(offsetDateTime.toLocalDate());
        requestInRange.setCurrentTime(offsetDateTime.toOffsetTime());
        requestInRange.setTimeZone(offsetDateTime.getOffset());

        requestAndCheckDecision(requestInRange, PERMIT);

        offsetDateTime = OffsetDateTime.parse("2020-01-01T07:59:59+05:00");
        requestInRange.setCurrentDateTime(offsetDateTime);
        requestInRange.setCurrentDate(offsetDateTime.toLocalDate());
        requestInRange.setCurrentTime(offsetDateTime.toOffsetTime());
        requestInRange.setTimeZone(offsetDateTime.getOffset());

        requestAndCheckDecision(requestInRange, DENY);

        offsetDateTime = OffsetDateTime.parse("2020-01-01T08:00:00+05:00");
        requestInRange.setCurrentDateTime(offsetDateTime);
        requestInRange.setCurrentDate(offsetDateTime.toLocalDate());
        requestInRange.setCurrentTime(offsetDateTime.toOffsetTime());
        requestInRange.setTimeZone(offsetDateTime.getOffset());

        requestAndCheckDecision(requestInRange, PERMIT);

        offsetDateTime = OffsetDateTime.parse("2020-01-01T23:59:59+05:00");
        requestInRange.setCurrentDateTime(offsetDateTime);
        requestInRange.setCurrentDate(offsetDateTime.toLocalDate());
        requestInRange.setCurrentTime(offsetDateTime.toOffsetTime());
        requestInRange.setTimeZone(offsetDateTime.getOffset());

        requestAndCheckDecision(requestInRange, PERMIT);

        offsetDateTime = OffsetDateTime.parse("2020-01-01T00:00:00+05:00");
        requestInRange.setCurrentDateTime(offsetDateTime);
        requestInRange.setCurrentDate(offsetDateTime.toLocalDate());
        requestInRange.setCurrentTime(offsetDateTime.toOffsetTime());
        requestInRange.setTimeZone(offsetDateTime.getOffset());

        requestAndCheckDecision(requestInRange, DENY);
    }

    @SuppressWarnings("unchecked")
    private void insertOperationEvent(DecisionRequest request) {
        //
        // Get the properties
        //
        Map<String, Object> localProps = (Map<String, Object>) request.getResource().get("guard");
        assertThat(localProps).isNotNull();
        //
        // Add an entry
        //
        OperationsHistory newEntry = new OperationsHistory();
        newEntry.setActor(localProps.get("actor").toString());
        newEntry.setOperation(localProps.get("operation").toString());
        newEntry.setClosedLoopName(localProps.get("clname").toString());
        newEntry.setOutcome("SUCCESS");
        newEntry.setStarttime(Date.from(Instant.now().minusMillis(20000)));
        newEntry.setEndtime(Date.from(Instant.now()));
        newEntry.setRequestId(UUID.randomUUID().toString());
        newEntry.setTarget(localProps.get("target").toString());
        LOGGER.info("Inserting {}", newEntry);
        em.getTransaction().begin();
        em.persist(newEntry);
        em.getTransaction().commit();
    }

}
