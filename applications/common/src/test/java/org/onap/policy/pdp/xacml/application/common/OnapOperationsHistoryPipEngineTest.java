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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.att.research.xacml.api.Attribute;
import com.att.research.xacml.api.AttributeValue;
import com.att.research.xacml.api.Identifier;
import com.att.research.xacml.api.XACML3;
import com.att.research.xacml.api.pip.PIPEngine;
import com.att.research.xacml.api.pip.PIPException;
import com.att.research.xacml.api.pip.PIPFinder;
import com.att.research.xacml.api.pip.PIPRequest;
import com.att.research.xacml.api.pip.PIPResponse;
import com.att.research.xacml.std.StdMutableAttribute;
import com.att.research.xacml.std.datatypes.DataTypes;
import com.att.research.xacml.std.pip.StdMutablePIPResponse;
import com.att.research.xacml.std.pip.StdPIPRequest;
import com.att.research.xacml.std.pip.StdPIPResponse;
import com.att.research.xacml.std.pip.engines.StdConfigurableEngine;
import com.att.research.xacml.util.FactoryException;
import com.google.common.base.Strings;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Properties;
import java.util.UUID;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;
import javax.persistence.Query;

import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

public class OnapOperationsHistoryPipEngineTest {
    private static OnapOperationsHistoryPipEngine pipEngine;
    private static final String ISSUER = "issuerIntw:mid:end";

    private static EntityManagerFactory emf;
    private static EntityManager em;

    /*
     * Junit props
     */
    protected static final String PU_KEY = "OperationsHistoryPU";
    protected static final String JUNITPU = "OperationsHistoryPUTest";

    /**
     * Set up test class.
     */
    @BeforeClass
    public static void setupTest() {
        pipEngine = null;
        try {
            pipEngine = new OnapOperationsHistoryPipEngine();
        } catch (Exception e) {
            fail("OnapOperationsHistoryPipEngine constructor failed");
        }

        // Set PU
        System.setProperty(PU_KEY, JUNITPU);

        // Connect to in-mem db
        emf = Persistence.createEntityManagerFactory(JUNITPU);
        em = emf.createEntityManager();
    }

    /**
     * Clean up test class.
     */
    @AfterClass
    public static void tearDown() {
        em.close();
        emf.close();
    }

    // @Test
    // public void testAttributesRequired() {
    //     assertTrue(pipEngine.attributesRequired().isEmpty());
    // }

    // @Test
    // public void testAttributesProvided() {
    //     assertTrue(pipEngine.attributesProvided().isEmpty());
    // }

    @Test
    public void testGetCountFromDb() {

        // Use reflection to run getCountFromDB
        Method method = null;
        int count = -1;
        try {
            method = OnapOperationsHistoryPipEngine.class.getDeclaredMethod("doDatabaseQuery",
                                                                            String.class,
                                                                            String.class,
                                                                            String.class,
                                                                            int.class,
                                                                            String.class);
            method.setAccessible(true);
            count = (int) method.invoke(pipEngine, "actor", "op", "target", 1, "MINUTE");
        } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException
                | NoSuchMethodException e) {
            fail(e.getLocalizedMessage());
        }
        // No entries yet
        assertEquals(0, count);

        // Add an entry
        String addEntry = "insert into operationshistory "
            + "(id, outcome, closedLoopName, actor, operation, target, endtime)"
            + "values(88, 'success','testcl', 'actor', 'op', 'target', CURRENT_TIMESTAMP())";
        Query nq2 = em.createNativeQuery(addEntry);
        em.getTransaction().begin();
        nq2.executeUpdate();
        em.getTransaction().commit();

        try {
            count = (int) method.invoke(pipEngine, "actor", "op", "target", 1, "MINUTE");
        } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
            fail(e.getLocalizedMessage());
        }
        // Should count 1 entry now
        assertEquals(1, count);
    }
}
