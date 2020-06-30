/*-
 * ============LICENSE_START=======================================================
 * Copyright (C) 2019-2020 AT&T Intellectual Property. All rights reserved.
 * Modifications Copyright (C) 2020 Nordix Foundation
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

package org.onap.policy.pdpx.main.rest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.ServiceLoader;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.onap.policy.common.endpoints.event.comm.bus.internal.BusTopicParams;
import org.onap.policy.common.endpoints.http.client.HttpClient;
import org.onap.policy.common.endpoints.http.client.HttpClientConfigException;
import org.onap.policy.common.endpoints.http.client.internal.JerseyClient;
import org.onap.policy.common.endpoints.parameters.RestServerParameters;
import org.onap.policy.common.endpoints.parameters.TopicParameterGroup;
import org.onap.policy.common.utils.coder.CoderException;
import org.onap.policy.common.utils.coder.StandardCoder;
import org.onap.policy.common.utils.network.NetworkUtil;
import org.onap.policy.models.decisions.concepts.DecisionRequest;
import org.onap.policy.models.decisions.concepts.DecisionResponse;
import org.onap.policy.pdp.xacml.application.common.XacmlApplicationException;
import org.onap.policy.pdp.xacml.application.common.XacmlApplicationServiceProvider;
import org.onap.policy.pdp.xacml.application.common.XacmlPolicyUtils;
import org.onap.policy.pdp.xacml.xacmltest.TestUtils;
import org.onap.policy.pdpx.main.PolicyXacmlPdpException;
import org.onap.policy.pdpx.main.parameters.CommonTestData;
import org.onap.policy.pdpx.main.parameters.XacmlPdpParameterGroup;
import org.onap.policy.pdpx.main.startstop.Main;
import org.onap.policy.pdpx.main.startstop.XacmlPdpActivator;
import org.onap.policy.xacml.pdp.application.monitoring.MonitoringPdpApplication;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TestAbbreviateDecisionResults {

    private static final Logger LOGGER = LoggerFactory.getLogger(TestDecision.class);

    private static int port;
    private static Main main;
    private static HttpClient client;
    private static CommonTestData testData = new CommonTestData();

    private static Properties properties = new Properties();
    private static File propertiesFile;
    private static XacmlApplicationServiceProvider service;

    private static RestServerParameters policyApiParameters;

    @ClassRule
    public static final TemporaryFolder appsFolder = new TemporaryFolder();

    /**
     * BeforeClass setup environment.
     *
     * @throws IOException Cannot create temp apps folder
     * @throws Exception   exception if service does not start
     */
    @BeforeClass
    public static void beforeClass() throws Exception {
        port = NetworkUtil.allocPort();
        //
        // Copy test directory over of the application directories
        //
        Path src = Paths.get("src/test/resources/apps");
        File apps = appsFolder.newFolder("apps");
        Files.walk(src).forEach(source -> {
            copy(source, apps.toPath().resolve(src.relativize(source)));
        });

        // Start the Monitoring Application
        startXacmlApplicationService(apps);

        // Load monitoring policy
        TestUtils.loadPolicies("../applications/monitoring/src/test/resources/vDNS.policy.input.yaml", service);

        // Create parameters for XacmlPdPService
        RestServerParameters rest = testData.toObject(testData.getRestServerParametersMap(port),
                RestServerParameters.class);
        policyApiParameters = testData.toObject(testData.getPolicyApiParametersMap(false), RestServerParameters.class);
        TopicParameterGroup topicParameterGroup = testData.toObject(testData.getTopicParametersMap(false),
                TopicParameterGroup.class);
        XacmlPdpParameterGroup params =
                new XacmlPdpParameterGroup("XacmlPdpParameters", "XacmlPdpGroup", rest, policyApiParameters,
                topicParameterGroup, apps.getAbsolutePath());
        StandardCoder gson = new StandardCoder();
        File fileParams = appsFolder.newFile("params.json");
        String jsonParams = gson.encode(params);
        LOGGER.info("Creating new params: {}", jsonParams);
        Files.write(fileParams.toPath(), jsonParams.getBytes());
        //
        // Start the service
        //
        main = startXacmlPdpService(fileParams);
        XacmlPdpActivator.getCurrent().startXacmlRestController();
        //
        // Make sure it is running
        //
        if (!NetworkUtil.isTcpPortOpen("localhost", port, 20, 1000L)) {
            throw new IllegalStateException("Cannot connect to port " + port);
        }
        //
        // Create a client
        //
        client = getNoAuthHttpClient();
    }

    /**
     * Clean up.
     */
    @AfterClass
    public static void after() throws PolicyXacmlPdpException {
        stopXacmlPdpService(main);
        client.shutdown();
    }

    /**
     * Tests if the Decision Response contains abbreviated results. Specifically, "properties", "name" and "version"
     * should have been removed from the response.
     */
    @Test
    public void testAbbreviateDecisionResult() throws HttpClientConfigException {

        LOGGER.info("Running testAbbreviateDecisionResult");

        // Create DecisionRequest
        DecisionRequest request = new DecisionRequest();
        request.setOnapName("DCAE");
        request.setOnapComponent("PolicyHandler");
        request.setOnapInstance("622431a4-9dea-4eae-b443-3b2164639c64");
        request.setAction("configure");
        Map<String, Object> resource = new HashMap<String, Object>();
        resource.put("policy-id", "onap.scaleout.tca");
        request.setResource(resource);

        // Query decision API
        DecisionResponse response = getDecision(request);
        LOGGER.info("Decision Response {}", response);

        assertFalse(response.getPolicies().isEmpty());

        @SuppressWarnings("unchecked")
        Map<String, Object> policy = (Map<String, Object>) response.getPolicies().get("onap.scaleout.tca");
        assertTrue(policy.containsKey("type"));
        assertFalse(policy.containsKey("properties"));
        assertFalse(policy.containsKey("name"));
        assertFalse(policy.containsKey("version"));
        assertTrue(policy.containsKey("metadata"));
    }

    private static Main startXacmlPdpService(File params) throws PolicyXacmlPdpException {
        final String[] XacmlPdpConfigParameters = { "-c", params.getAbsolutePath() };
        return new Main(XacmlPdpConfigParameters);
    }

    private static void stopXacmlPdpService(final Main main) throws PolicyXacmlPdpException {
        main.shutdown();
    }

    /**
     * Performs the POST request to Decision API.
     *
     */
    private DecisionResponse getDecision(DecisionRequest request) throws HttpClientConfigException {

        Entity<DecisionRequest> entityRequest = Entity.entity(request, MediaType.APPLICATION_JSON);
        Response response = client.post("", entityRequest, Collections.emptyMap());

        assertEquals(200, response.getStatus());
        return HttpClient.getBody(response, DecisionResponse.class);
    }

    /**
     * Create HttpClient.
     *
     */
    private static HttpClient getNoAuthHttpClient()
            throws HttpClientConfigException, KeyManagementException, NoSuchAlgorithmException, ClassNotFoundException {
        BusTopicParams clientParams = new BusTopicParams();
        clientParams.setClientName("testName");
        clientParams.setUseHttps(false);
        clientParams.setAllowSelfSignedCerts(false);
        clientParams.setHostname("localhost");
        clientParams.setPort(port);
        clientParams.setBasePath("policy/pdpx/v1/decision?abbrev=true");
        clientParams.setUserName("healthcheck");
        clientParams.setPassword("zb!XztG34");
        clientParams.setManaged(true);
        client = new JerseyClient(clientParams);
        return client;
    }

    private static void copy(Path source, Path dest) {
        try {
            LOGGER.info("Copying {} to {}", source, dest);
            Files.copy(source, dest, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            LOGGER.error("Failed to copy {} to {}", source, dest);
        }
    }

    /**
     * Initializes the Monitoring application service.
     *
     * @param apps - the path to xacml.properties file
     */
    private static void startXacmlApplicationService(File apps)
            throws XacmlApplicationException, CoderException, IOException {
        LOGGER.info("****** Starting Xacml Application Service ******");
        //
        // Setup our temporary folder
        //
        XacmlPolicyUtils.FileCreator myCreator = (String filename) -> {
            new File(apps, "monitoring/" + filename).delete();
            return appsFolder.newFile("apps/monitoring/" + filename);
        };
        propertiesFile = XacmlPolicyUtils.copyXacmlPropertiesContents(
                "../applications/monitoring/src/test/resources/xacml.properties", properties, myCreator);
        //
        // Load XacmlApplicationServiceProvider service
        //
        ServiceLoader<XacmlApplicationServiceProvider> applicationLoader = ServiceLoader
                .load(XacmlApplicationServiceProvider.class);
        //
        // Look for our class instance and save it
        //
        StringBuilder strDump = new StringBuilder("Loaded applications:" + XacmlPolicyUtils.LINE_SEPARATOR);
        for (XacmlApplicationServiceProvider application : applicationLoader) {
            //
            // Is it our service?
            //
            if (application instanceof MonitoringPdpApplication) {
                //
                // Should be the first and only one
                //
                assertThat(service).isNull();
                service = application;
            }
            strDump.append(application.applicationName());
            strDump.append(" supports ");
            strDump.append(application.supportedPolicyTypes());
            strDump.append(XacmlPolicyUtils.LINE_SEPARATOR);
        }
        LOGGER.debug("{}", strDump);
        //
        // Tell it to initialize based on the properties file
        // we just built for it.
        //
        service.initialize(propertiesFile.toPath().getParent(), policyApiParameters);
    }
}
