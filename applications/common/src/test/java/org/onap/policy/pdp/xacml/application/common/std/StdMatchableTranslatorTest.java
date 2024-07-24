/*-
 * ============LICENSE_START=======================================================
 * Copyright (C) 2019-2021 AT&T Intellectual Property. All rights reserved.
 * Modifications Copyright (C) 2023-2024 Nordix Foundation.
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

package org.onap.policy.pdp.xacml.application.common.std;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.att.research.xacml.api.AttributeAssignment;
import com.att.research.xacml.api.Decision;
import com.att.research.xacml.api.IdReference;
import com.att.research.xacml.api.Obligation;
import com.att.research.xacml.api.Request;
import com.att.research.xacml.std.StdStatusCode;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Response;
import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;
import oasis.names.tc.xacml._3_0.core.schema.wd_17.AttributeValueType;
import oasis.names.tc.xacml._3_0.core.schema.wd_17.ObligationExpressionType;
import oasis.names.tc.xacml._3_0.core.schema.wd_17.PolicyType;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.onap.policy.common.endpoints.http.client.HttpClient;
import org.onap.policy.common.endpoints.http.client.HttpClientFactoryInstance;
import org.onap.policy.common.endpoints.http.server.HttpServletServer;
import org.onap.policy.common.endpoints.http.server.HttpServletServerFactoryInstance;
import org.onap.policy.common.endpoints.parameters.RestClientParameters;
import org.onap.policy.common.endpoints.properties.PolicyEndPointProperties;
import org.onap.policy.common.gson.GsonMessageBodyHandler;
import org.onap.policy.common.utils.coder.CoderException;
import org.onap.policy.common.utils.coder.StandardYamlCoder;
import org.onap.policy.common.utils.network.NetworkUtil;
import org.onap.policy.common.utils.resources.ResourceUtils;
import org.onap.policy.models.decisions.concepts.DecisionRequest;
import org.onap.policy.models.decisions.concepts.DecisionResponse;
import org.onap.policy.models.tosca.authorative.concepts.ToscaPolicy;
import org.onap.policy.models.tosca.authorative.concepts.ToscaServiceTemplate;
import org.onap.policy.models.tosca.simple.concepts.JpaToscaServiceTemplate;
import org.onap.policy.pdp.xacml.application.common.TestUtilsCommon;
import org.onap.policy.pdp.xacml.application.common.ToscaDictionary;
import org.onap.policy.pdp.xacml.application.common.ToscaPolicyConversionException;
import org.onap.policy.pdp.xacml.application.common.XacmlPolicyUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class StdMatchableTranslatorTest {

    private static final Logger logger = LoggerFactory.getLogger(StdMatchableTranslatorTest.class);
    private static final String CLIENT_NAME = "policy-api";
    private static final StandardYamlCoder yamlCoder = new StandardYamlCoder();
    private static int port;
    private static RestClientParameters clientParams;
    private static ToscaServiceTemplate testTemplate;
    private static HttpClient apiClient;

    @TempDir
    static java.nio.file.Path policyFolder;

    /**
     * Initializes {@link #clientParams} and starts a simple REST server to handle the
     * test requests.
     *
     * @throws IOException if an error occurs
     */
    @BeforeAll
    static void setUpBeforeClass() throws Exception {
        System.setProperty("org.eclipse.jetty.util.log.class", "org.eclipse.jetty.util.log.StdErrLog");
        System.setProperty("org.eclipse.jetty.LEVEL", "OFF");
        //
        // Setup our api server simulator
        //
        port = NetworkUtil.allocPort();

        clientParams = mock(RestClientParameters.class);
        when(clientParams.getClientName()).thenReturn("apiClient");
        when(clientParams.getHostname()).thenReturn("localhost");
        when(clientParams.getPort()).thenReturn(port);

        Properties props = getProperties();

        HttpServletServerFactoryInstance.getServerFactory().build(props).forEach(HttpServletServer::start);
        apiClient = HttpClientFactoryInstance.getClientFactory().build(clientParams);

        assertTrue(NetworkUtil.isTcpPortOpen(clientParams.getHostname(), clientParams.getPort(), 100, 100));
        //
        // Load our test policy type
        //
        String policyYaml = ResourceUtils.getResourceAsString("matchable/onap.policies.Test-1.0.0.yaml");
        //
        // Serialize it into a class
        //
        ToscaServiceTemplate serviceTemplate = yamlCoder.decode(policyYaml, ToscaServiceTemplate.class);
        //
        // Make sure all the fields are set up properly
        //
        JpaToscaServiceTemplate jtst = new JpaToscaServiceTemplate();
        jtst.fromAuthorative(serviceTemplate);
        testTemplate = jtst.toAuthorative();
        //
        // Make sure the Policy Types are there
        //
        assertEquals(3, testTemplate.getPolicyTypes().size());
        assertNotNull(testTemplate.getPolicyTypes().get("onap.policies.Base"));
        assertNotNull(testTemplate.getPolicyTypes().get("onap.policies.base.Middle"));
        assertNotNull(testTemplate.getPolicyTypes().get("onap.policies.base.middle.Test"));
        logger.info("Test Policy Type {}{}", XacmlPolicyUtils.LINE_SEPARATOR, testTemplate);
    }

    private static Properties getProperties() {
        Properties props = new Properties();
        props.setProperty(PolicyEndPointProperties.PROPERTY_HTTP_SERVER_SERVICES, CLIENT_NAME);

        final String svcpfx =
            PolicyEndPointProperties.PROPERTY_HTTP_SERVER_SERVICES + "." + CLIENT_NAME;

        props.setProperty(svcpfx + PolicyEndPointProperties.PROPERTY_HTTP_HOST_SUFFIX, clientParams.getHostname());
        props.setProperty(svcpfx + PolicyEndPointProperties.PROPERTY_HTTP_PORT_SUFFIX,
            Integer.toString(clientParams.getPort()));
        props.setProperty(svcpfx + PolicyEndPointProperties.PROPERTY_HTTP_REST_CLASSES_SUFFIX,
            ApiRestController.class.getName());
        props.setProperty(svcpfx + PolicyEndPointProperties.PROPERTY_MANAGED_SUFFIX, "true");
        props.setProperty(svcpfx + PolicyEndPointProperties.PROPERTY_HTTP_HTTPS_SUFFIX, "false");
        props.setProperty(svcpfx + PolicyEndPointProperties.PROPERTY_HTTP_SERIALIZATION_PROVIDER,
            GsonMessageBodyHandler.class.getName());
        return props;
    }

    @AfterAll
    public static void tearDownAfterClass() {
        HttpServletServerFactoryInstance.getServerFactory().destroy();
    }

    @Test
     void testMatchableTranslator() throws CoderException, ToscaPolicyConversionException, ParseException {
        //
        // Create our translator
        //
        StdMatchableTranslator translator = new StdMatchableTranslator();
        assertNotNull(translator);
        //
        // Set it up
        //
        translator.setPathForData(policyFolder.getRoot().toAbsolutePath());
        translator.setApiClient(apiClient);
        //
        // Load policies to test
        //
        String policyYaml = ResourceUtils.getResourceAsString(
            "src/test/resources/matchable/test.policies.input.tosca.yaml");
        //
        // Serialize it into a class
        //
        ToscaServiceTemplate serviceTemplate = yamlCoder.decode(policyYaml, ToscaServiceTemplate.class);
        //
        // Make sure all the fields are set up properly
        //
        JpaToscaServiceTemplate jtst = new JpaToscaServiceTemplate();
        jtst.fromAuthorative(serviceTemplate);
        ToscaServiceTemplate completedJtst = jtst.toAuthorative();
        //
        // Convert the policy
        //
        for (Map<String, ToscaPolicy> policies : completedJtst.getToscaTopologyTemplate().getPolicies()) {
            for (ToscaPolicy policy : policies.values()) {
                //
                // Test that we can convert the policy - assuming PolicyType
                //
                PolicyType translatedPolicy = (PolicyType) translator.convertPolicy(policy);
                assertNotNull(translatedPolicy);
                assertThat(translatedPolicy.getObligationExpressions().getObligationExpression()).hasSize(1);
                logger.info("Translated policy {} {}", XacmlPolicyUtils.LINE_SEPARATOR, translatedPolicy);
                //
                // Shortcut to create an obligation, we are just going to steal
                // the attributes from the translated policy.
                //
                List<AttributeAssignment> listAttributes = new ArrayList<>();
                ObligationExpressionType xacmlObligation = translatedPolicy.getObligationExpressions()
                    .getObligationExpression().get(0);
                assertThat(xacmlObligation.getAttributeAssignmentExpression()).hasSize(4);
                //
                // Copy into the list
                //
                xacmlObligation.getAttributeAssignmentExpression().forEach(assignment -> {
                    Object value = ((AttributeValueType) assignment.getExpression().getValue()).getContent().get(0);
                    listAttributes.add(TestUtilsCommon.createAttributeAssignment(assignment.getAttributeId(),
                        assignment.getCategory(), value));
                });
                //
                // Pretend we got multiple policies to match a fictional request
                //
                Obligation obligation1 = TestUtilsCommon.createXacmlObligation(
                    ToscaDictionary.ID_OBLIGATION_REST_BODY.stringValue(),
                    listAttributes);
                Obligation obligation2 = TestUtilsCommon.createXacmlObligation(
                    ToscaDictionary.ID_OBLIGATION_REST_BODY.stringValue(),
                    listAttributes);
                //
                // Should ignore this obligation
                //
                Obligation obligation3 = TestUtilsCommon.createXacmlObligation(
                    "nobody:cares",
                    listAttributes);
                //
                // Create a test XACML Response
                //
                Map<String, String> ids = new HashMap<>();
                ids.put("onap.policies.Test", "1.0.0");
                Collection<IdReference> policyIds = TestUtilsCommon.createPolicyIdList(ids);

                com.att.research.xacml.api.Response xacmlResponse = TestUtilsCommon.createXacmlResponse(
                    StdStatusCode.STATUS_CODE_OK, null, Decision.PERMIT,
                    Arrays.asList(obligation1, obligation2, obligation3), policyIds);
                //
                // Test the response
                //
                DecisionResponse decisionResponse = translator.convertResponse(xacmlResponse);
                assertNotNull(decisionResponse);
                assertThat(decisionResponse.getPolicies()).hasSize(1);
            }
        }
        //
        // Test request decisions
        //
        DecisionRequest decisionRequest = new DecisionRequest();
        decisionRequest.setAction("action");
        decisionRequest.setOnapComponent("onap-component");
        decisionRequest.setOnapName("onap");
        Map<String, Object> resource = new HashMap<>();
        resource.put("matchableString", "I should be matched");
        decisionRequest.setResource(resource);
        Request xacmlRequest = translator.convertRequest(decisionRequest);
        assertNotNull(xacmlRequest);
        assertThat(xacmlRequest.getRequestAttributes()).hasSize(3);
    }

    /**
     * Simple REST server to handle test requests.
     */

    @Path("/policy/api/v1")
    @Produces({"application/json", "application/yaml"})
    @Consumes({"application/json", "application/yaml"})
    public static class ApiRestController {

        /**
         * Retrieves the specified version of a particular policy type.
         *
         * @param policyTypeId ID of desired policy type
         * @param versionId    version of desired policy type
         * @param requestId    optional request ID
         * @return the Response object containing the results of the API operation
         */
        @GET
        @Path("/policytypes/{policyTypeId}/versions/{versionId}")
        public Response getSpecificVersionOfPolicyType(@PathParam("policyTypeId") String policyTypeId,
                                                       @PathParam("versionId") String versionId,
                                                       @HeaderParam("X-ONAP-RequestID") UUID requestId) {
            logger.info("request for policy type={} version={}", policyTypeId, versionId);
            return Response.status(Response.Status.OK).entity(testTemplate).build();

        }
    }
}
