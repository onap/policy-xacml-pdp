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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.att.research.xacml.api.Status;
import com.att.research.xacml.api.pip.PIPException;
import com.att.research.xacml.api.pip.PIPFinder;
import com.att.research.xacml.api.pip.PIPRequest;
import com.att.research.xacml.api.pip.PIPResponse;
import com.att.research.xacml.std.pip.StdPIPResponse;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Persistence;
import java.io.FileInputStream;
import java.lang.reflect.Method;
import java.time.Instant;
import java.util.Date;
import java.util.Properties;
import java.util.UUID;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.onap.policy.guard.OperationsHistory;
import org.onap.policy.pdp.xacml.application.common.ToscaDictionary;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class GetOperationOutcomePipTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(GetOperationOutcomePipTest.class);
    private static final String TEST_PROPERTIES = "src/test/resources/test.properties";

    private static EntityManager em;

    private Properties properties;
    private GetOperationOutcomePip pipEngine;

    private final PIPRequest pipRequest = mock(PIPRequest.class);

    private final PIPFinder pipFinder = mock(PIPFinder.class);

    private final PIPResponse resp1 = mock(PIPResponse.class);

    private final Status okStatus = mock(Status.class);

    /**
     * Create an instance of our engine and also the persistence
     * factory.
     *
     * @throws Exception connectivity issues
     */
    @BeforeAll
    static void setupDatabase() throws Exception {
        LOGGER.info("Setting up PIP Testing");
        //
        // Load our test properties to use
        //
        Properties props = new Properties();
        try (FileInputStream is = new FileInputStream(TEST_PROPERTIES)) {
            props.load(is);
        }
        //
        // Connect to in-mem db
        //
        String persistenceUnit = GetOperationOutcomePip.ISSUER_NAME + ".persistenceunit";
        LOGGER.info("persistenceunit {}", persistenceUnit);
        em = Persistence.createEntityManagerFactory(props.getProperty(persistenceUnit), props)
            .createEntityManager();
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
    void setupEngine() throws Exception {
        when(pipRequest.getIssuer()).thenReturn("urn:org:onap:xacml:guard:tw:1:hour");
        //
        // Create instance
        //
        pipEngine = new GetOperationOutcomePip();
        //
        // Load the properties
        //
        properties = new Properties();
        try (FileInputStream is = new FileInputStream(TEST_PROPERTIES)) {
            properties.load(is);
        }
        //
        // Configure it using properties
        //
        pipEngine.configure("issuer", properties);
        LOGGER.info("PIP configured now creating our entity manager");
        LOGGER.info("properties {}", properties);
    }

    @Test
    void testAttributesRequired() {
        assertEquals(1, pipEngine.attributesRequired().size());
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
    void testGetAttributes() throws Exception {
        //
        //
        //
        when(pipRequest.getIssuer()).thenReturn(ToscaDictionary.GUARD_ISSUER_PREFIX + "clname:clfoo");
        when(pipFinder.getMatchingAttributes(any(), eq(pipEngine))).thenReturn(resp1);
        when(resp1.getStatus()).thenReturn(okStatus);
        when(okStatus.isOk()).thenReturn(true);

        assertNotEquals(StdPIPResponse.PIP_RESPONSE_EMPTY, pipEngine.getAttributes(pipRequest, pipFinder));

        pipEngine.shutdown();

        assertThatExceptionOfType(PIPException.class).isThrownBy(() -> pipEngine.getAttributes(pipRequest, pipFinder))
            .withMessageContaining("Engine is shutdown");
    }

    @Test
    void testGetOutcomeFromDb() throws Exception {
        //
        // Use reflection to run getCountFromDB
        //
        Method method = GetOperationOutcomePip.class.getDeclaredMethod("doDatabaseQuery",
            String.class);
        method.setAccessible(true);
        //
        // Test pipEngine
        //
        String outcome = (String) method.invoke(pipEngine, "testcl1");
        assertThat(outcome).isNull();
        //
        // Insert entry
        //
        insertEntry("testcl1", "testtarget1", "Started");
        //
        // Test pipEngine
        //
        outcome = (String) method.invoke(pipEngine, "testcl1");
        //
        // outcome should be "In_Progress"
        //
        assertEquals("In_Progress", outcome);
        //
        // Insert more entries
        //
        insertEntry("testcl2", "testtarget1", "Success");
        insertEntry("testcl3", "testtarget2", "Failed");
        //
        // Test pipEngine
        //
        outcome = (String) method.invoke(pipEngine, "testcl2");
        assertEquals("Complete", outcome);

        outcome = (String) method.invoke(pipEngine, "testcl3");
        assertEquals("Complete", outcome);

        //
        // Shut it down
        //
        pipEngine.shutdown();

        assertThat(method.invoke(pipEngine, "testcl1")).isNull();
    }

    private void insertEntry(String cl, String target, String outcome) {
        //
        // Create entry
        //
        OperationsHistory newEntry = new OperationsHistory();
        newEntry.setClosedLoopName(cl);
        newEntry.setTarget(target);
        newEntry.setOutcome(outcome);
        newEntry.setActor("Controller");
        newEntry.setOperation("operationA");
        newEntry.setStarttime(Date.from(Instant.now().minusMillis(20000)));
        newEntry.setEndtime(Date.from(Instant.now()));
        newEntry.setRequestId(UUID.randomUUID().toString());
        //
        // Add entry
        //
        em.getTransaction().begin();
        em.persist(newEntry);
        em.getTransaction().commit();
    }
}
