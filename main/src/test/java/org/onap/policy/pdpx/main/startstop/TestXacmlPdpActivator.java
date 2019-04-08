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

package org.onap.policy.pdpx.main.startstop;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.io.FileInputStream;
import java.net.UnknownHostException;
import java.util.Properties;

import org.junit.After;
import org.junit.BeforeClass;
import org.junit.Test;
import org.onap.policy.common.endpoints.event.comm.client.TopicSinkClientException;
import org.onap.policy.pdpx.main.PolicyXacmlPdpException;
import org.onap.policy.pdpx.main.parameters.CommonTestData;
import org.onap.policy.pdpx.main.parameters.XacmlPdpParameterGroup;
import org.onap.policy.pdpx.main.parameters.XacmlPdpParameterHandler;


/**
 * Class to perform unit test of XacmlPdpActivator.
 *
 */
public class TestXacmlPdpActivator {
    private static XacmlPdpActivator activator = null;

    /**
     * Setup the tests.
     */
    @BeforeClass
    public static void setup() throws Exception {
        final String[] xacmlPdpConfigParameters =
            {"-c", "parameters/XacmlPdpConfigParameters.json", "-p", "parameters/topic.properties"};
        final XacmlPdpCommandLineArguments arguments = new XacmlPdpCommandLineArguments(xacmlPdpConfigParameters);
        final XacmlPdpParameterGroup parGroup = new XacmlPdpParameterHandler().getParameters(arguments);

        Properties props = new Properties();
        String propFile = arguments.getFullPropertyFilePath();
        try (FileInputStream stream = new FileInputStream(propFile)) {
            props.load(stream);
        }

        activator = new XacmlPdpActivator(parGroup, props);
    }

    @Test
    public void testXacmlPdpActivator() throws PolicyXacmlPdpException, TopicSinkClientException, UnknownHostException {
        assertFalse(activator.isAlive());
        activator.start();
        assertTrue(activator.isAlive());
        assertTrue(activator.getParameterGroup().isValid());
        assertEquals(CommonTestData.PDPX_GROUP_NAME, activator.getParameterGroup().getName());

    }

    @Test
    public void testGetCurrent_testSetCurrent() {
        assertSame(activator, XacmlPdpActivator.getCurrent());
    }

    @Test
    public void testTerminate() throws Exception {
        if (!activator.isAlive()) {
            activator.start();
        }
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
