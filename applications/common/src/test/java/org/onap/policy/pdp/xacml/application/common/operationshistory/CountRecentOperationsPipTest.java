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
import javax.persistence.Query;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CountRecentOperationsPipTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(CountRecentOperationsPipTest.class);
    private static CountRecentOperationsPip pipEngine;

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
        pipEngine = new CountRecentOperationsPip();
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
        String persistenceUnit = CountRecentOperationsPip.ISSUER_NAME + ".persistenceunit";
        LOGGER.info("persistenceunit {}", persistenceUnit);
        em = Persistence.createEntityManagerFactory(properties.getProperty(persistenceUnit), properties)
                .createEntityManager();
        //
        //
        //
        LOGGER.info("Configured own entity manager", em.toString());
    }

    private Dbao createEntry(String cl, String target, String outcome) {
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
        return newEntry;
    }

    @Test
    public void testGetCountFromDb() throws Exception {
        //
        // Use reflection to run getCountFromDB
        //
        Method method = CountRecentOperationsPip.class.getDeclaredMethod("doDatabaseQuery",
                                                                            String.class,
                                                                            String.class,
                                                                            String.class,
                                                                            int.class,
                                                                            String.class);
        method.setAccessible(true);
        //
        // create entry
        //
        Dbao newEntry = createEntry("cl-foobar-1", "vnf-1", "SUCCESS");
        //
        // Test pipEngine
        //
        long count = (long) method.invoke(pipEngine, newEntry.getActor(), newEntry.getOperation(), newEntry.getTarget(),
                1, "HOUR");
        //
        // No entries yet
        //
        assertEquals(0, count);
        //
        // Add entry
        //
        em.getTransaction().begin();
        em.persist(newEntry);
        em.getTransaction().commit();
        //
        // Directly check ground truth
        //
        Query queryCount = em.createNativeQuery("select count(*) as numops from operationshistory")
                .setParameter(1, 1);
        LOGGER.info("{} entries", queryCount.getSingleResult());
        //
        // Test pipEngine
        //
        count = (long) method.invoke(pipEngine, newEntry.getActor(), newEntry.getOperation(), newEntry.getTarget(),
                1, "HOUR");
        //
        // Should count 1 entry now
        //
        assertEquals(1, count);
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
