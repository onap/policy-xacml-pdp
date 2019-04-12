/*-
 * ============LICENSE_START=======================================================
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
 * ============LICENSE_END=========================================================
 */

package org.onap.policy.pdp.xacml.application.common.operationshistory;

import static org.junit.Assert.assertEquals;

import java.io.FileInputStream;
import java.lang.reflect.Method;
import java.sql.Date;
import java.time.Instant;
import java.util.Properties;
import java.util.UUID;

import javax.persistence.EntityManager;
import javax.persistence.Persistence;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GetOperationOutcomePipTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(GetOperationOutcomePipTest.class);
    private static GetOperationOutcomePip pipEngine;

    private static EntityManager em;

    /**
     * Create an instance of our engine and also the persistence
     * factory.
     *
     * @throws Exception connectivity issues
     */
    @BeforeClass
    public static void setup() throws Exception {
        LOGGER.info("Setting up PIP Testing");
        //
        // Create instance
        //
        pipEngine = new GetOperationOutcomePip();
        //
        // Load our test properties to use
        //
        Properties properties = new Properties();
        try (FileInputStream is = new FileInputStream("src/test/resources/test.properties")) {
            properties.load(is);
        }
        //
        // Configure it using properties
        //
        pipEngine.configure("issuer", properties);
        LOGGER.info("PIP configured now creating our entity manager");
        LOGGER.info("properties {}", properties);
        //
        // Connect to in-mem db
        //
        String persistenceUnit = GetOperationOutcomePip.ISSUER_NAME + ".persistenceunit";
        LOGGER.info("persistenceunit {}", persistenceUnit);
        em = Persistence.createEntityManagerFactory(properties.getProperty(persistenceUnit), properties)
            .createEntityManager();
        //
        //
        //
        LOGGER.info("Configured own entity manager", em.toString());
    }

    private void insertEntry(String cl, String target, String outcome) {
        //
        // Create entry
        //
        Dbao newEntry = new Dbao();
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


    @Test
    public void testGetOutcomeFromDb() throws Exception {
        //
        // Use reflection to run getCountFromDB
        //
        Method method = GetOperationOutcomePip.class.getDeclaredMethod("doDatabaseQuery",
                                                                       String.class,
                                                                       String.class);
        method.setAccessible(true);
        //
        // Insert entry
        //
        insertEntry("testcl1", "testtarget1", "1");
        //
        // Test pipEngine
        //
        String outcome = (String) method.invoke(pipEngine, "testcl1", "testtarget1");
        //
        // outcome should be "1"
        //
        assertEquals("1", outcome);
        //
        // Insert more entries
        //
        insertEntry("testcl1", "testtarget1", "2");
        insertEntry("testcl2", "testtarget2", "3");
        insertEntry("testcl1", "testtarget2", "4");
        //
        // Test pipEngine
        //
        outcome = (String) method.invoke(pipEngine, "testcl1", "testtarget1");
        assertEquals("2", outcome);

        outcome = (String) method.invoke(pipEngine, "testcl2", "testtarget2");
        assertEquals("3", outcome);

        outcome = (String) method.invoke(pipEngine, "testcl1", "testtarget2");
        assertEquals("4", outcome);
    }

    /**
     * Close the entity manager.
     */
    @AfterClass
    public static void cleanup() {
        if (em != null) {
            em.close();
        }
    }

}
