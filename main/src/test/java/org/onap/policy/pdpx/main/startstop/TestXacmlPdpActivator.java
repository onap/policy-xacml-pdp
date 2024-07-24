/*-
 * ============LICENSE_START=======================================================
 * Copyright (C) 2019, 2021 AT&T Intellectual Property. All rights reserved.
 * Modifications Copyright (C) 2019, 2023-2024 Nordix Foundation.
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.onap.policy.pdpx.main.CommonRest;
import org.onap.policy.pdpx.main.parameters.CommonTestData;
import org.onap.policy.pdpx.main.parameters.XacmlPdpParameterGroup;
import org.onap.policy.pdpx.main.parameters.XacmlPdpParameterHandler;
import org.springframework.test.util.ReflectionTestUtils;


/**
 * Class to perform unit test of XacmlPdpActivator.
 */
public class TestXacmlPdpActivator extends CommonRest {
    private static final String PROBE_FIELD_NAME = "probeHeartbeatTopicSec";

    private static XacmlPdpParameterGroup parGroup;

    private XacmlPdpActivator activator = null;

    /**
     * Loads properties.
     */
    @BeforeAll
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
    @BeforeEach
    public void setUp() {
        ReflectionTestUtils.setField(parGroup, PROBE_FIELD_NAME, 4);
        activator = new XacmlPdpActivator(parGroup);
    }

    @Test
    void testXacmlPdpActivator() {
        assertFalse(activator.isAlive());
        assertFalse(activator.isApiEnabled());
        activator.start();
        assertTrue(activator.isAlive());

        // XacmlPdp starts in PASSIVE state so the rest controller should not be alive
        assertFalse(activator.isApiEnabled());
        assertTrue(activator.getParameterGroup().isValid());
        assertEquals(CommonTestData.PDPX_PARAMETER_GROUP_NAME, activator.getParameterGroup().getName());
        assertEquals(CommonTestData.PDPX_GROUP, activator.getParameterGroup().getPdpGroup());

        activator.enableApi();
        assertTrue(activator.isApiEnabled());

        activator.disableApi();
        assertFalse(activator.isApiEnabled());
    }

    @Test
    void testXacmlPdpActivator_NoProbe() {
        ReflectionTestUtils.setField(parGroup, PROBE_FIELD_NAME, 0);
        activator = new XacmlPdpActivator(parGroup);
        activator.start();
        assertTrue(activator.isAlive());
    }

    @Test
    void testGetCurrent_testSetCurrent() {
        XacmlPdpActivator.setCurrent(activator);
        assertSame(activator, XacmlPdpActivator.getCurrent());
    }

    @Test
    void testTerminate() {
        activator.start();
        activator.stop();
        assertFalse(activator.isAlive());
    }

    /**
     * Teardown tests.
     */
    @AfterEach
    public void teardown() {
        if (activator != null && activator.isAlive()) {
            activator.stop();
        }
    }
}
