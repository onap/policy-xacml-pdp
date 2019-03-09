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
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.onap.policy.pdpx.main.PolicyXacmlPdpException;
import org.onap.policy.pdpx.main.parameters.CommonTestData;

/**
 * Class to perform unit test of Main.
 *
 */
public class TestMain {

    /**
     * setup.
     */
    @BeforeClass
    public static void setUp() {
        System.setProperty("org.eclipse.jetty.util.log.class", "org.eclipse.jetty.util.log.StdErrLog");
        System.setProperty("org.eclipse.jetty.LEVEL", "OFF");

    }

    @Test
    public void testMain() throws PolicyXacmlPdpException {
        final String[] xacmlPdpConfigParameters = {"-c", "parameters/XacmlPdpConfigParameters.json"};
        final Main main = new Main(xacmlPdpConfigParameters);
        assertTrue(main.getParameters().isValid());
        assertEquals(CommonTestData.PDPX_GROUP_NAME, main.getParameters().getName());
        main.shutdown();
    }

    @Test
    public void testMain_NoArguments() throws PolicyXacmlPdpException {
        final String[] xacmlPdpConfigParameters = {};
        final Main main = new Main(xacmlPdpConfigParameters);
        assertNull(main.getParameters());
        main.shutdown();
    }

    @Test
    public void testMain_InvalidArguments() {
        final String[] xacmlPdpConfigParameters = {"parameters/XacmlPdpConfigParameters.json"};
        final Main main = new Main(xacmlPdpConfigParameters);
        assertNull(main.getParameters());
    }

    @Test
    public void testMain_Help() {
        final String[] xacmlPdpConfigParameters = {"-h"};
        final Main main = new Main(xacmlPdpConfigParameters);
        final String message = "-h,--help                     outputs the usage of this command";
        Assert.assertTrue(main.getArgumentMessage().contains(message));

    }

    @Test
    public void testMain_InvalidParameters() {
        final String[] xacmlPdpConfigParameters = {"-c", "parameters/XacmlPdpConfigParameters_InvalidName.json"};
        final Main main = new Main(xacmlPdpConfigParameters);
        assertNull(main.getParameters());
    }
}
