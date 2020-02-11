/*-
 * ============LICENSE_START=======================================================
 * ONAP
 * ================================================================================
 * Copyright (C) 2020 AT&T Intellectual Property. All rights reserved.
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

package org.onap.policy.xacml.pdp.application.nativ;

import static org.assertj.core.api.Assertions.assertThat;
import com.att.research.xacml.api.Request;
import com.att.research.xacml.api.Response;
import com.att.research.xacml.std.dom.DOMRequest;
import com.att.research.xacml.std.dom.DOMResponse;
import java.io.File;
import java.util.Iterator;
import java.util.Properties;
import java.util.ServiceLoader;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.onap.policy.common.endpoints.parameters.RestServerParameters;
import org.onap.policy.common.utils.resources.TextFileUtils;
import org.onap.policy.pdp.xacml.application.common.XacmlApplicationServiceProvider;
import org.onap.policy.pdp.xacml.application.common.XacmlPolicyUtils;
import org.onap.policy.pdp.xacml.xacmltest.TestUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NativePdpApplicationTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(NativePdpApplicationTest.class);
    private static final String PERMIT = "Permit";
    private static Properties properties = new Properties();
    private static File propertiesFile;
    private static RestServerParameters clientParams = new RestServerParameters();
    private static NativePdpApplication service;
    private static Request request;

    @ClassRule
    public static final TemporaryFolder policyFolder = new TemporaryFolder();

    /**
     * Copies the xacml.properties and policies files into
     * temporary folder and loads the service provider saving
     * instance of provider off for other tests to use.
     */
    @BeforeClass
    public static void setup() throws Exception {
        LOGGER.info("Setting up class");
        //
        // Setup our temporary folder
        //
        XacmlPolicyUtils.FileCreator myCreator = (String filename) -> policyFolder.newFile(filename);
        propertiesFile = XacmlPolicyUtils.copyXacmlPropertiesContents("src/test/resources/xacml.properties",
                properties, myCreator);
        //
        // Load service
        //
        ServiceLoader<XacmlApplicationServiceProvider> applicationLoader =
                ServiceLoader.load(XacmlApplicationServiceProvider.class);
        //
        // Find the native application and save for use in all the tests
        //
        StringBuilder strDump = new StringBuilder("Loaded applications:" + XacmlPolicyUtils.LINE_SEPARATOR);
        Iterator<XacmlApplicationServiceProvider> iterator = applicationLoader.iterator();
        while (iterator.hasNext()) {
            XacmlApplicationServiceProvider application = iterator.next();
            //
            // Is it our service?
            //
            if (application instanceof NativePdpApplication) {
                //
                // Should be the first and only one
                //
                assertThat(service).isNull();
                service = (NativePdpApplication) application;
            }
            strDump.append(application.applicationName());
            strDump.append(" supports ");
            strDump.append(application.supportedPolicyTypes());
            strDump.append(XacmlPolicyUtils.LINE_SEPARATOR);
        }
        LOGGER.info("{}", strDump);
        //
        // Tell it to initialize based on the properties file
        // we just built for it.
        //
        service.initialize(propertiesFile.toPath().getParent(), clientParams);
        //
        // Load XACML Request
        //
        request = DOMRequest.load(
                TextFileUtils.getTextFileAsString(
                        "src/test/resources/requests/native.policy.request.xml"));

    }

    @Test
    public void testNativePolicy() throws Exception {

        LOGGER.info("*********** Running native policy test *************");
        //
        // Now load the TOSCA compliant native policy - make sure
        // the pdp can support it and have it load into the PDP.
        //
        TestUtils.loadPolicies("src/test/resources/policies/native.policy.yaml", service);
        //
        // Send the request and verify decision result
        //
        requestAndCheckDecision(request, PERMIT);
    }

    /**
     * Request a decision and check that it matches expectation.
     *
     * @param request to send to XACML PDP
     * @param expected from the response
     * @throws Exception on errors requesting a decision and checking the returned decision
     *
     **/
    private void requestAndCheckDecision(Request request, String expected) throws Exception {
        //
        // Ask for a decision
        //
        Response decision = service.makeNativeDecision(request);
        //
        // Check decision
        //
        checkDecision(expected, decision);
    }

    /**
     * Check that decision matches expectation.
     *
     * @param expected from the response
     * @param response received
     * @throws Exception on errors checking the decision
     *
     **/
    public void checkDecision(String expected, Response response) throws Exception {
        LOGGER.info("Looking for {} Decision", expected);
        assertThat(response).isNotNull();
        assertThat(response.getResults().iterator().next().getDecision()).isNotNull();
        assertThat(response.getResults().iterator().next().getDecision().toString()).isEqualTo(expected);
        LOGGER.info("Xacml response we received {}", DOMResponse.toString(response));
    }
}