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

package org.onap.policy.pdp.xacml.application.common.std;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;
import oasis.names.tc.xacml._3_0.core.schema.wd_17.PolicyType;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.onap.policy.common.endpoints.http.server.HttpServletServer;
import org.onap.policy.common.endpoints.http.server.HttpServletServerFactoryInstance;
import org.onap.policy.common.endpoints.parameters.RestServerParameters;
import org.onap.policy.common.endpoints.properties.PolicyEndPointProperties;
import org.onap.policy.common.gson.GsonMessageBodyHandler;
import org.onap.policy.common.utils.coder.CoderException;
import org.onap.policy.common.utils.coder.StandardYamlCoder;
import org.onap.policy.common.utils.network.NetworkUtil;
import org.onap.policy.common.utils.resources.ResourceUtils;
import org.onap.policy.models.tosca.authorative.concepts.ToscaPolicy;
import org.onap.policy.models.tosca.authorative.concepts.ToscaPolicyType;
import org.onap.policy.models.tosca.authorative.concepts.ToscaServiceTemplate;
import org.onap.policy.models.tosca.simple.concepts.JpaToscaServiceTemplate;
import org.onap.policy.pdp.xacml.application.common.ToscaPolicyConversionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class StdMatchableTranslatorTest {

    private static final Logger logger = LoggerFactory.getLogger(StdMatchableTranslatorTest.class);
    private static final String CLIENT_NAME = "policy-api";
    private static final StandardYamlCoder yamlCoder = new StandardYamlCoder();
    private static int port;
    private static RestServerParameters clientParams;
    private static ToscaPolicyType testPolicyType;

    @ClassRule
    public static final TemporaryFolder policyFolder = new TemporaryFolder();

    /**
     * Initializes {@link #clientParams} and starts a simple REST server to handle the
     * test requests.
     *
     * @throws IOException if an error occurs
     */
    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        System.setProperty("org.eclipse.jetty.util.log.class", "org.eclipse.jetty.util.log.StdErrLog");
        System.setProperty("org.eclipse.jetty.LEVEL", "OFF");
        //
        // Setup our api server simulator
        //
        port = NetworkUtil.allocPort();

        clientParams = mock(RestServerParameters.class);
        when(clientParams.getHost()).thenReturn("localhost");
        when(clientParams.getPort()).thenReturn(port);

        Properties props = new Properties();
        props.setProperty(PolicyEndPointProperties.PROPERTY_HTTP_SERVER_SERVICES, CLIENT_NAME);

        final String svcpfx =
                        PolicyEndPointProperties.PROPERTY_HTTP_SERVER_SERVICES + "." + CLIENT_NAME;

        props.setProperty(svcpfx + PolicyEndPointProperties.PROPERTY_HTTP_HOST_SUFFIX, clientParams.getHost());
        props.setProperty(svcpfx + PolicyEndPointProperties.PROPERTY_HTTP_PORT_SUFFIX,
                        Integer.toString(clientParams.getPort()));
        props.setProperty(svcpfx + PolicyEndPointProperties.PROPERTY_HTTP_REST_CLASSES_SUFFIX,
                        ApiRestController.class.getName());
        props.setProperty(svcpfx + PolicyEndPointProperties.PROPERTY_MANAGED_SUFFIX, "true");
        props.setProperty(svcpfx + PolicyEndPointProperties.PROPERTY_HTTP_HTTPS_SUFFIX, "false");
        props.setProperty(svcpfx + PolicyEndPointProperties.PROPERTY_AAF_SUFFIX, "false");
        props.setProperty(svcpfx + PolicyEndPointProperties.PROPERTY_HTTP_SERIALIZATION_PROVIDER,
                        GsonMessageBodyHandler.class.getName());

        HttpServletServerFactoryInstance.getServerFactory().build(props).forEach(HttpServletServer::start);

        assertTrue(NetworkUtil.isTcpPortOpen(clientParams.getHost(), clientParams.getPort(), 100, 100));
        //
        // Load our test policy type
        //
        String policyYaml = ResourceUtils.getResourceAsString("matchable/onap.policies.Test-1.0.0.yaml");
        //
        // Serialize it into a class
        //
        ToscaServiceTemplate serviceTemplate = yamlCoder.decode(policyYaml, ToscaServiceTemplate.class);
        //
        // Make sure all the fields are setup properly
        //
        JpaToscaServiceTemplate jtst = new JpaToscaServiceTemplate();
        jtst.fromAuthorative(serviceTemplate);
        ToscaServiceTemplate completedJtst = jtst.toAuthorative();
        //
        // Find the Policy Type - SHOULD only be one
        //
        assertEquals(1, completedJtst.getPolicyTypes().size());
        testPolicyType = completedJtst.getPolicyTypes().get("onap.policies.Test");
        assertNotNull(testPolicyType);
        logger.info("Test Policy Type {}{}", System.lineSeparator(), testPolicyType);
    }

    @AfterClass
    public static void tearDownAfterClass() {
        HttpServletServerFactoryInstance.getServerFactory().destroy();
    }

    @Test
    public void test() throws CoderException, ToscaPolicyConversionException {
        //
        // Create our translator
        //
        StdMatchableTranslator translator = new StdMatchableTranslator();
        assertNotNull(translator);
        //
        // Set it up
        //
        translator.setPathForData(policyFolder.getRoot().toPath());
        translator.setApiRestParameters(clientParams);
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
        // Make sure all the fields are setup properly
        //
        JpaToscaServiceTemplate jtst = new JpaToscaServiceTemplate();
        jtst.fromAuthorative(serviceTemplate);
        ToscaServiceTemplate completedJtst = jtst.toAuthorative();
        //
        // Get the policies
        //
        for (Map<String, ToscaPolicy> policies : completedJtst.getToscaTopologyTemplate().getPolicies()) {
            for (ToscaPolicy policy : policies.values()) {
                PolicyType translatedPolicy = translator.convertPolicy(policy);
                assertNotNull(translatedPolicy);
                logger.info("Translated policy {} {}", System.lineSeparator(), translatedPolicy);
            }
        }
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
         * @param versionId version of desired policy type
         * @param requestId optional request ID
         *
         * @return the Response object containing the results of the API operation
         */
        @GET
        @Path("/policytypes/{policyTypeId}/versions/{versionId}")
        public Response getSpecificVersionOfPolicyType(@PathParam("policyTypeId") String policyTypeId,
                        @PathParam("versionId") String versionId, @HeaderParam("X-ONAP-RequestID") UUID requestId) {
            logger.info("request for policy type={} version={}", policyTypeId, versionId);
            return Response.status(Response.Status.OK).entity(testPolicyType).build();

        }
    }
}
