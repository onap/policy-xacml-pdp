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
 *
 * SPDX-License-Identifier: Apache-2.0
 * ============LICENSE_END=========================================================
 */

package org.onap.policy.pdp.xacml.application.common;

import static org.junit.Assert.*;
import com.att.research.xacml.util.FactoryException;
import com.att.research.xacml.util.XACMLProperties;
import com.att.research.xacmlatt.pdp.policy.PolicyFinder;
import com.google.common.io.Files;
import java.io.File;
import java.io.IOException;
import java.util.Properties;
import org.junit.Before;
import org.junit.Test;

public class OnapPolicyFinderFactoryTest {
    private static final String DIR = "src/test/resources/finder/";
    private static final String SUFFIX = ".xml";
    private static final String POLICY_ID = "combined";
    private static final String NOT_FOUND = "not-found";

    private Properties props;

    private OnapPolicyFinderFactory factory;

    @Before
    public void setUp() {
        props = new Properties();
    }

    @Test
    public void testLoadPolicyDef() {
        fail("Not yet implemented");
    }

    @Test
    public void testLoadPolicyFileDef_NotFound() {
        props.setProperty(XACMLProperties.PROP_REFERENCEDPOLICIES, NOT_FOUND);
        props.setProperty(NOT_FOUND + OnapPolicyFinderFactory.PROP_FILE, DIR + NOT_FOUND);

        factory = new OnapPolicyFinderFactory(props);

        assertEquals(0, factory.getReferencedPolicyCount());
        assertEquals(0, factory.getRootPolicyCount());
    }

    @Test
    public void testLoadPolicyFileDef() {
        props.setProperty(XACMLProperties.PROP_REFERENCEDPOLICIES, POLICY_ID);
        props.setProperty(POLICY_ID + OnapPolicyFinderFactory.PROP_FILE, DIR + POLICY_ID + SUFFIX);

        factory = new OnapPolicyFinderFactory(props);

        assertEquals(1, factory.getReferencedPolicyCount());
        assertEquals(0, factory.getRootPolicyCount());
    }

    @Test
    public void testLoadPolicyUrlDef() {
        fail("Not yet implemented");
    }

    @Test
    public void testGetPolicyDefs() {
        fail("Not yet implemented");
    }

    @Test
    public void testInit_Referenced() {
        props.setProperty(XACMLProperties.PROP_REFERENCEDPOLICIES, POLICY_ID);
        props.setProperty(POLICY_ID + OnapPolicyFinderFactory.PROP_FILE, DIR + POLICY_ID + SUFFIX);

        factory = new OnapPolicyFinderFactory(props);

        assertEquals(1, factory.getReferencedPolicyCount());
        assertEquals(0, factory.getRootPolicyCount());

        // TODO test combining
        // TODO test root
        // TODO test reference
    }

    @Test
    public void testInit_Root() {
        props.setProperty(XACMLProperties.PROP_ROOTPOLICIES, POLICY_ID);
        props.setProperty(POLICY_ID + OnapPolicyFinderFactory.PROP_FILE, DIR + POLICY_ID + SUFFIX);

        factory = new OnapPolicyFinderFactory(props);

        assertEquals(0, factory.getReferencedPolicyCount());
        assertEquals(1, factory.getRootPolicyCount());

        // TODO test combining
        // TODO test root
        // TODO test reference
    }

    @Test
    public void testGetPolicyFinder() throws FactoryException {
        assertNotNull(new OnapPolicyFinderFactory(props).getPolicyFinder());
    }

    @Test
    public void testGetPolicyFinderProperties() throws FactoryException {
        assertNotNull(new OnapPolicyFinderFactory(props).getPolicyFinder(new Properties()));
    }

}
