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

package org.onap.policy.pdp.xacml.application.common;

import static org.junit.Assert.assertEquals;

import java.io.FileInputStream;
import java.lang.reflect.Method;
import java.sql.Date;
import java.time.Instant;
import java.util.Properties;
import java.util.UUID;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;
import javax.persistence.Query;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OnapOperationsHistoryPipEngineTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(OnapOperationsHistoryPipEngineTest.class);
    private static OnapOperationsHistoryPipEngine pipEngine;

    private static EntityManager em;

    /**
     * Create an instance of our engine and also the persistence
     * factory.
     *
     * @throws Exception connectivity issues
     */
    @BeforeClass
    public static void setUp() throws Exception {
        //
        // Create instance
        //
        pipEngine = new OnapOperationsHistoryPipEngine();
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

        // Connect to in-mem db
        EntityManagerFactory emf = Persistence.createEntityManagerFactory("PipEngineTest");
        em = emf.createEntityManager();
    }

    /**
     * Close the entity manager.
     */
    @AfterClass
    public static void tearDown() {
        if (em != null) {
            em.close();
        }
    }

    @Test
    public void testGetCountFromDb() throws Exception {

        // Add an entry
        OnapOperationsHistoryDbao newEntry = new OnapOperationsHistoryDbao();
        newEntry.setActor("Controller");
        newEntry.setOperation("operationA");
        newEntry.setClName("cl-foobar-1");
        newEntry.setOutcome("SUCCESS");
        newEntry.setStarttime(Date.from(Instant.now().minusMillis(20000)));
        newEntry.setEndtime(Date.from(Instant.now()));
        newEntry.setRequestId(UUID.randomUUID().toString());
        newEntry.setTarget("vnf-1");

        // Use reflection to run getCountFromDB
        Method method = OnapOperationsHistoryPipEngine.class.getDeclaredMethod("doDatabaseQuery",
                                                                            String.class,
                                                                            String.class,
                                                                            String.class,
                                                                            int.class,
                                                                            String.class);
        method.setAccessible(true);
        int count = (int) method.invoke(pipEngine, newEntry.getActor(), newEntry.getOperation(), newEntry.getTarget(),
                1, "HOUR");

        // No entries yet
        assertEquals(0, count);


        em.getTransaction().begin();
        em.persist(newEntry);
        em.getTransaction().commit();

        Query queryCount = em.createQuery("SELECT COUNT(h) FROM OnapOperationsHistoryDbao h");
        LOGGER.info("{} entries", queryCount.getSingleResult());


        count = (int) method.invoke(pipEngine, newEntry.getActor(), newEntry.getOperation(), newEntry.getTarget(),
                1, "HOUR");
        // Should count 1 entry now
        assertEquals(1, count);
    }

}
