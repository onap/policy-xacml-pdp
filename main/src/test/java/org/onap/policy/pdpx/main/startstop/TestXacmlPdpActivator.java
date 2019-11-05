/*-
 * ============LICENSE_START=======================================================
 * Copyright (C) 2019 AT&T Intellectual Property. All rights reserved.
 * Modifications Copyright (C) 2019 Nordix Foundation.
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

package org.onap.policy.pdpx.main.startstop;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.onap.policy.pdpx.main.CommonRest;
import org.onap.policy.pdpx.main.PolicyXacmlPdpException;
import org.onap.policy.pdpx.main.parameters.CommonTestData;
import org.onap.policy.pdpx.main.parameters.XacmlPdpParameterGroup;
import org.onap.policy.pdpx.main.parameters.XacmlPdpParameterHandler;


/**
 * Class to perform unit test of XacmlPdpActivator.
 *
 */
public class TestXacmlPdpActivator extends CommonRest {
    private static XacmlPdpParameterGroup parGroup;

    private XacmlPdpActivator activator = null;

    /**
     * Loads properties.
     */
    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        CommonRest.setUpBeforeClass();

        final String[] xacmlPdpConfigParameters = {"-c", CommonRest.CONFIG_FILE};
        final XacmlPdpCommandLineArguments arguments = new XacmlPdpCommandLineArguments(xacmlPdpConfigParameters);
        parGroup = new XacmlPdpParameterHandler().getParameters(arguments);

        // don't want the common "main" running
        CommonRest.stopMain();
    }

    /**
     * Creates the activator.
     */
    @Override
    @Before
    public void setUp() {
        activator = new XacmlPdpActivator(parGroup);
    }

    @Test
    public void testXacmlPdpActivator() throws Exception {
        assertFalse(activator.isAlive());
        assertFalse(activator.isXacmlRestControllerAlive());
        activator.start();
        assertTrue(activator.isAlive());

        // XacmlPdp starts in PASSIVE state so the rest controller should not be alive
        assertFalse(activator.isXacmlRestControllerAlive());
        assertTrue(activator.getParameterGroup().isValid());
        assertEquals(CommonTestData.PDPX_GROUP_NAME, activator.getParameterGroup().getName());

        activator.startXacmlRestController();
        assertTrue(activator.isXacmlRestControllerAlive());

        activator.stopXacmlRestController();
        assertFalse(activator.isXacmlRestControllerAlive());
    }

    @Test
    public void testGetCurrent_testSetCurrent() {
        XacmlPdpActivator.setCurrent(activator);
        assertSame(activator, XacmlPdpActivator.getCurrent());
    }

    @Test
    public void testTerminate() throws Exception {
        activator.start();
        activator.stop();
        assertFalse(activator.isAlive());
    }

    /**
     * Teardown tests.
     * @throws PolicyXacmlPdpException on termination errors
     */
    @After
    public void teardown() throws PolicyXacmlPdpException {
        if (activator != null && activator.isAlive()) {
            activator.stop();
        }
    }
}
