/*-
 * ============LICENSE_START=======================================================
 * Copyright (C) 2019-2021 AT&T Intellectual Property. All rights reserved.
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

package org.onap.policy.pdpx.main.rest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
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
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.onap.policy.common.endpoints.event.comm.bus.internal.BusTopicParams;
import org.onap.policy.common.endpoints.http.client.HttpClient;
import org.onap.policy.common.endpoints.http.client.HttpClientConfigException;
import org.onap.policy.common.endpoints.http.client.HttpClientFactoryInstance;
import org.onap.policy.common.endpoints.parameters.RestServerParameters;
import org.onap.policy.common.endpoints.parameters.TopicParameterGroup;
import org.onap.policy.common.utils.network.NetworkUtil;
import org.onap.policy.common.utils.resources.ResourceUtils;
import org.onap.policy.models.decisions.concepts.DecisionRequest;
import org.onap.policy.models.decisions.concepts.DecisionResponse;
import org.onap.policy.models.errors.concepts.ErrorResponse;
import org.onap.policy.pdpx.main.PolicyXacmlPdpException;
import org.onap.policy.pdpx.main.parameters.CommonTestData;
import org.onap.policy.pdpx.main.parameters.XacmlPdpParameterGroup;
import org.onap.policy.pdpx.main.startstop.Main;
import org.onap.policy.pdpx.main.startstop.XacmlPdpActivator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TestDecision {

    private static final Logger LOGGER = LoggerFactory.getLogger(TestDecision.class);

    private static int port;
    private static Main main;
    private static HttpClient client;
    private static CommonTestData testData = new CommonTestData();
    private static final String APPLICATION_XACML_XML = "application/xacml+xml";
    private static final String APPLICATION_XACML_JSON = "application/xacml+json";

    @ClassRule
    public static final TemporaryFolder appsFolder = new TemporaryFolder();

    /**
     * BeforeClass setup environment.
     * @throws IOException Cannot create temp apps folder
     * @throws Exception exception if service does not start
     */
    @BeforeClass
    public static void beforeClass() throws Exception {
        System.setProperty("org.eclipse.jetty.util.log.class", "org.eclipse.jetty.util.log.StdErrLog");
        System.setProperty("org.eclipse.jetty.LEVEL", "OFF");

        port = NetworkUtil.allocPort();

        //
        // Copy test directory over of the application directories
        //
        Path src = Paths.get("src/test/resources/apps");
        File apps = appsFolder.newFolder("apps");
        Files.walk(src).forEach(source -> {
            copy(source, apps.toPath().resolve(src.relativize(source)));
        });
        //
        // Get the parameters file correct.
        //
        RestServerParameters rest =
            testData.toObject(testData.getRestServerParametersMap(port), RestServerParameters.class);
        RestServerParameters policyApiParameters =
                        testData.toObject(testData.getPolicyApiParametersMap(false), RestServerParameters.class);
        TopicParameterGroup topicParameterGroup =
                        testData.toObject(testData.getTopicParametersMap(false), TopicParameterGroup.class);
        XacmlPdpParameterGroup params =
                new XacmlPdpParameterGroup("XacmlPdpParameters", "XacmlPdpGroup", "xacml", rest, policyApiParameters,
                        topicParameterGroup, apps.getAbsolutePath());
        final Gson gson = new GsonBuilder().create();
        File fileParams = appsFolder.newFile("params.json");
        String jsonParams = gson.toJson(params);
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

    @AfterClass
    public static void after() throws PolicyXacmlPdpException {
        stopXacmlPdpService(main);
        client.shutdown();
    }

    @Test
    public void testDecision_UnsupportedAction() throws Exception {
        LOGGER.info("Running test testDecision_UnsupportedAction");

        DecisionRequest request = new DecisionRequest();
        request.setOnapName("DROOLS");
        request.setAction("foo");
        Map<String, Object> guard = new HashMap<String, Object>();
        guard.put("actor", "foo");
        guard.put("recipe", "bar");
        guard.put("target", "somevnf");
        guard.put("clname", "phoneyloop");
        request.setResource(guard);

        ErrorResponse response = getErrorDecision(request);
        LOGGER.info("Response {}", response);
        assertThat(response.getResponseCode()).isEqualTo(Status.BAD_REQUEST);
        assertThat(response.getErrorMessage()).isEqualToIgnoringCase("No application for action foo");
    }

    @Test
    public void testDecision_Guard() throws KeyManagementException, NoSuchAlgorithmException,
        ClassNotFoundException {
        LOGGER.info("Running test testDecision_Guard");

        DecisionRequest request = new DecisionRequest();
        request.setOnapName("DROOLS");
        request.setAction("guard");
        Map<String, Object> guard = new HashMap<String, Object>();
        guard.put("actor", "foo");
        guard.put("recipe", "bar");
        guard.put("target", "somevnf");
        guard.put("clname", "phoneyloop");
        request.setResource(guard);

        DecisionResponse response = getDecision(request);
        LOGGER.info("Response {}", response);
        assertThat(response.getStatus()).isEqualTo("Permit");
    }

    @Test
    public void testDecision_Native() throws IOException {
        LOGGER.info("Running test testDecision_Native");

        String xmlRequestAsString = ResourceUtils.getResourceAsString(
                "src/test/resources/decisions/decision.native.request.xml");
        if (xmlRequestAsString == null) {
            throw new IOException("failed to read the xml request");
        }

        String jsonRequestAsString = ResourceUtils.getResourceAsString(
                "src/test/resources/decisions/decision.native.request.json");
        if (jsonRequestAsString == null) {
            throw new IOException("failed to read the json request");
        }

        String responseFromXmlRequest = getNativeDecision(xmlRequestAsString, APPLICATION_XACML_XML);
        LOGGER.info("Response from xml request {}", responseFromXmlRequest);
        assertThat(responseFromXmlRequest).contains("NOTAPPLICABLE");

        String responseFromJsonRequest = getNativeDecision(jsonRequestAsString, APPLICATION_XACML_JSON);
        LOGGER.info("Response from json request {}", responseFromJsonRequest);
        assertThat(responseFromJsonRequest).contains("NOTAPPLICABLE");
    }

    private static Main startXacmlPdpService(File params) throws PolicyXacmlPdpException {
        final String[] XacmlPdpConfigParameters = {"-c", params.getAbsolutePath()};
        return new Main(XacmlPdpConfigParameters);
    }

    private static void stopXacmlPdpService(final Main main) throws PolicyXacmlPdpException {
        main.shutdown();
    }

    private DecisionResponse getDecision(DecisionRequest request) {
        Entity<DecisionRequest> entityRequest = Entity.entity(request, MediaType.APPLICATION_JSON);
        Response response = client.post("/decision", entityRequest, Collections.emptyMap());

        assertEquals(200, response.getStatus());

        return HttpClient.getBody(response, DecisionResponse.class);
    }

    private String getNativeDecision(String request, String mediaType) {
        Entity<String> entityRequest = Entity.entity(request, mediaType);
        Response response = client.post("/xacml", entityRequest, Collections.emptyMap());

        assertEquals(200, response.getStatus());

        return HttpClient.getBody(response, String.class);
    }

    private ErrorResponse getErrorDecision(DecisionRequest request) {
        Entity<DecisionRequest> entityRequest = Entity.entity(request, MediaType.APPLICATION_JSON);
        Response response = client.post("/decision", entityRequest, Collections.emptyMap());

        assertEquals(400, response.getStatus());

        return HttpClient.getBody(response, ErrorResponse.class);
    }

    private static HttpClient getNoAuthHttpClient() throws HttpClientConfigException {
        return HttpClientFactoryInstance.getClientFactory().build(BusTopicParams.builder()
                .clientName("testDecisionClient")
                .useHttps(false).allowSelfSignedCerts(false).hostname("localhost").port(port)
                .basePath("policy/pdpx/v1")
                .userName("healthcheck").password("zb!XztG34").managed(true).build());
    }

    private static void copy(Path source, Path dest) {
        try {
            LOGGER.info("Copying {} to {}", source, dest);
            Files.copy(source, dest, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            LOGGER.error("Failed to copy {} to {}", source, dest);
        }
    }
}