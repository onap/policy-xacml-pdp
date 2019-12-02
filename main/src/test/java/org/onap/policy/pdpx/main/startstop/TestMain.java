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

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.onap.policy.pdpx.main.CommonRest;
import org.onap.policy.pdpx.main.PolicyXacmlPdpException;

/**
 * Class to perform unit test of Main.
 *
 */
public class TestMain extends CommonRest {

    private Main main;

    /**
     * Sets up properties and configuration.
     * @throws Exception if an error occurs
     */
    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        CommonRest.setUpBeforeClass();

        // don't want the common "main" running
        CommonRest.stopMain();
    }

    @Override
    @Before
    public void setUp() {
        main = null;
    }

    /**
     * Shuts "main" down.
     */
    @Override
    @After
    public void tearDown() {
        if (main != null) {
            main.shutdown();
        }
    }

    @Test
    public void testMain() throws PolicyXacmlPdpException {
        final String[] xacmlPdpConfigParameters = {"-c", CONFIG_FILE};
        assertThatCode(() -> {
            main = new Main(xacmlPdpConfigParameters);
            main.shutdown();
            main = null;
        }).doesNotThrowAnyException();
    }

    @Test
    public void testMain_NoArguments() {
        final String[] xacmlPdpConfigParameters = {};
        assertThatThrownBy(() -> new Main(xacmlPdpConfigParameters)).isInstanceOf(PolicyXacmlPdpException.class)
                        .hasMessage("policy xacml pdp configuration file was not specified as an argument");
    }

    @Test
    public void testMain_InvalidArguments() {
        final String[] xacmlPdpConfigParameters = {"parameters/XacmlPdpConfigParameters.json"};
        assertThatThrownBy(() -> new Main(xacmlPdpConfigParameters)).isInstanceOf(PolicyXacmlPdpException.class)
            .hasMessage("too many command line arguments specified : [parameters/XacmlPdpConfigParameters.json]");
    }

    @Test
    public void testMain_Help() throws PolicyXacmlPdpException {
        final String[] xacmlPdpConfigParameters = {"-h"};
        Assert.assertTrue(new Main(xacmlPdpConfigParameters).getArgumentMessage().contains("-h,--help"));

    }

    @Test
    public void testMain_InvalidParameters()  {
        final String[] xacmlPdpConfigParameters = {"-c", "parameters/XacmlPdpConfigParameters_InvalidName.json"};
        assertThatThrownBy(() -> new Main(xacmlPdpConfigParameters)).isInstanceOf(PolicyXacmlPdpException.class)
                        .hasMessageContaining("validation error");
    }
}
