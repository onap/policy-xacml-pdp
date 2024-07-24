/*-
 * ============LICENSE_START=======================================================
 * Copyright (C) 2019-2021 AT&T Intellectual Property. All rights reserved.
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
 * ============LICENSE_END=========================================================
 */

package org.onap.policy.pdp.xacml.application.common.operationshistory;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import com.att.research.xacml.api.Attribute;
import com.att.research.xacml.api.AttributeValue;
import com.att.research.xacml.api.pip.PIPException;
import com.att.research.xacml.api.pip.PIPFinder;
import com.att.research.xacml.api.pip.PIPRequest;
import com.att.research.xacml.api.pip.PIPResponse;
import com.att.research.xacml.std.pip.StdPIPResponse;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Persistence;
import jakarta.persistence.Query;
import java.io.FileInputStream;
import java.io.IOException;
import java.sql.Date;
import java.time.Instant;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.Properties;
import java.util.Queue;
import java.util.UUID;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.onap.policy.guard.OperationsHistory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ExtendWith(MockitoExtension.class)
class CountRecentOperationsPipTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(CountRecentOperationsPipTest.class);

    private static final String ACTOR = "my-actor";
    private static final String RECIPE = "my-recipe";
    private static final String TARGET = "my-target";
    private static final String TEST_PROPERTIES = "src/test/resources/test.properties";

    private static EntityManagerFactory emf;
    private static EntityManager em;

    @Mock
    private PIPRequest pipRequest;

    @Mock
    private PIPFinder pipFinder;

    @Mock
    private PIPResponse resp1;

    @Mock
    private PIPResponse resp2;

    @Mock
    private PIPResponse resp3;

    private Properties properties;
    private Queue<PIPResponse> responses;
    private Queue<String> attributes;

    private CountRecentOperationsPip pipEngine;

    /**
     * Establishes a connection to the DB and keeps it open until all tests have
     * completed.
     *
     * @throws IOException if properties cannot be loaded
     */
    @BeforeAll
    static void setUpBeforeClass() throws IOException {
        //
        // Load our test properties to use
        //
        Properties props2 = new Properties();
        try (FileInputStream is = new FileInputStream(TEST_PROPERTIES)) {
            props2.load(is);
        }
        //
        // Connect to in-mem db
        //
        String persistenceUnit = CountRecentOperationsPip.ISSUER_NAME + ".persistenceunit";
        LOGGER.info("persistenceunit {}", persistenceUnit);
        emf = Persistence.createEntityManagerFactory(props2.getProperty(persistenceUnit), props2);
        em = emf.createEntityManager();
        //
        //
        //
        LOGGER.info("Configured own entity manager {}", em);
    }

    /**
     * Close the entity manager.
     */
    @AfterAll
    static void cleanup() {
        if (emf != null) {
            emf.close();
        }
        if (em != null) {
            em.close();
        }
    }

    /**
     * Create an instance of our engine.
     *
     * @throws Exception if an error occurs
     */
    @BeforeEach
    void setUp() throws Exception {
        lenient().when(pipRequest.getIssuer()).thenReturn("urn:org:onap:xacml:guard:tw:1:hour");

        pipEngine = new MyPip();

        properties = new Properties();
        try (FileInputStream is = new FileInputStream(TEST_PROPERTIES)) {
            properties.load(is);
        }

        responses = new LinkedList<>(Arrays.asList(resp1, resp2, resp3));
        attributes = new LinkedList<>(Arrays.asList(ACTOR, RECIPE, TARGET));
    }

    @Test
    void testAttributesRequired() {
        assertEquals(3, pipEngine.attributesRequired().size());
    }

    @Test
    void testConfigure_DbException() {
        properties.put("jakarta.persistence.jdbc.url", "invalid");
        assertThatCode(() ->
            pipEngine.configure("issuer", properties)
        ).doesNotThrowAnyException();
    }

    @Test
    void testGetAttributes_NullIssuer() throws PIPException {
        when(pipRequest.getIssuer()).thenReturn(null);
        assertEquals(StdPIPResponse.PIP_RESPONSE_EMPTY, pipEngine.getAttributes(pipRequest, pipFinder));
    }

    @Test
    void testGetAttributes_WrongIssuer() throws PIPException {
        when(pipRequest.getIssuer()).thenReturn("wrong-issuer");
        assertEquals(StdPIPResponse.PIP_RESPONSE_EMPTY, pipEngine.getAttributes(pipRequest, pipFinder));
    }

    @Test
    void testGetAttributes_NullActor() throws PIPException {
        attributes = new LinkedList<>(Arrays.asList(null, RECIPE, TARGET));
        assertEquals(StdPIPResponse.PIP_RESPONSE_EMPTY, pipEngine.getAttributes(pipRequest, pipFinder));
    }

    @Test
    void testGetAttributes_NullRecipe() throws PIPException {
        attributes = new LinkedList<>(Arrays.asList(ACTOR, null, TARGET));
        assertEquals(StdPIPResponse.PIP_RESPONSE_EMPTY, pipEngine.getAttributes(pipRequest, pipFinder));
    }

    @Test
    void testGetAttributes_NullTarget() throws PIPException {
        attributes = new LinkedList<>(Arrays.asList(ACTOR, RECIPE, null));
        assertEquals(StdPIPResponse.PIP_RESPONSE_EMPTY, pipEngine.getAttributes(pipRequest, pipFinder));
    }

    @Test
    void testShutdown() {
        pipEngine.shutdown();
        assertThatExceptionOfType(PIPException.class).isThrownBy(() -> pipEngine.getAttributes(pipRequest, pipFinder))
            .withMessageContaining("Engine is shutdown");
    }

    @Test
    void testGetCountFromDb() throws Exception {
        //
        // Configure it using properties
        //
        pipEngine.configure("issuer", properties);
        LOGGER.info("PIP configured now creating our entity manager");
        LOGGER.info("properties {}", properties);
        //
        // create entry
        //
        OperationsHistory newEntry = createEntry();
        //
        // No entries yet
        //
        assertEquals(0, getCount(newEntry));
        //
        // Add entry
        //
        em.getTransaction().begin();
        em.persist(newEntry);
        em.getTransaction().commit();
        //
        // Directly check ground truth
        //
        Query queryCount = em.createNativeQuery("select count(*) as numops from operationshistory");
        LOGGER.info("{} entries", queryCount.getSingleResult());
        //
        // Should count 1 entry now
        //
        assertEquals(1, getCount(newEntry));
    }

    @Test
    void testStringToChronosUnit() throws PIPException {
        // not configured yet
        OperationsHistory newEntry = createEntry();
        assertEquals(-1, getCount(newEntry));

        // now configure it
        pipEngine.configure("issuer", properties);

        String[] units = {"second", "minute", "hour", "day", "week", "month", "year"};

        for (String unit : units) {
            when(pipRequest.getIssuer()).thenReturn("urn:org:onap:xacml:guard:tw:1:" + unit);

            /*
             * It would be better to use assertEquals below, but the test DB doesn't
             * support week, month, or year.
             */

            // should run without throwing an exception
            getCount(newEntry);
        }

        // invalid time unit
        when(pipRequest.getIssuer()).thenReturn("urn:org:onap:xacml:guard:tw:1:invalid");
        assertEquals(-1, getCount(newEntry));
    }

    private long getCount(OperationsHistory newEntry) throws PIPException {
        responses = new LinkedList<>(Arrays.asList(resp1, resp2, resp3));
        attributes = new LinkedList<>(Arrays.asList(newEntry.getActor(),
            newEntry.getOperation(), newEntry.getTarget()));

        PIPResponse result = pipEngine.getAttributes(pipRequest, pipFinder);

        Attribute attr = result.getAttributes().iterator().next();
        AttributeValue<?> value = attr.getValues().iterator().next();

        return ((Number) value.getValue()).longValue();
    }

    private OperationsHistory createEntry() {
        //
        // Create entry
        //
        OperationsHistory newEntry = new OperationsHistory();
        newEntry.setClosedLoopName("cl-foobar-1");
        newEntry.setTarget("vnf-1");
        newEntry.setOutcome("SUCCESS");
        newEntry.setActor("Controller");
        newEntry.setOperation("operationA");
        newEntry.setStarttime(Date.from(Instant.now().minusMillis(20000)));
        newEntry.setEndtime(Date.from(Instant.now()));
        newEntry.setRequestId(UUID.randomUUID().toString());
        return newEntry;
    }

    private class MyPip extends CountRecentOperationsPip {

        @Override
        protected PIPResponse getAttribute(PIPRequest pipRequest, PIPFinder pipFinder) {
            return responses.remove();
        }

        @Override
        protected String findFirstAttributeValue(PIPResponse pipResponse) {
            return attributes.remove();
        }
    }
}
