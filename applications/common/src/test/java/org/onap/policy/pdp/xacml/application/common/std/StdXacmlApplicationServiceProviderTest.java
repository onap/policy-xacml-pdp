/*-
 * ============LICENSE_START=======================================================
 * Copyright (C) 2019-2021 AT&T Intellectual Property. All rights reserved.
 * Modifications Copyright (C) 2024 Nordix Foundation.
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
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.att.research.xacml.api.Request;
import com.att.research.xacml.api.Response;
import com.att.research.xacml.api.pdp.PDPEngine;
import com.att.research.xacml.api.pdp.PDPEngineFactory;
import com.att.research.xacml.api.pdp.PDPException;
import com.att.research.xacml.util.FactoryException;
import com.att.research.xacml.util.XACMLProperties;
import com.google.common.io.Files;
import java.io.File;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Objects;
import java.util.Properties;
import java.util.Set;
import oasis.names.tc.xacml._3_0.core.schema.wd_17.PolicyType;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.onap.policy.common.endpoints.http.client.HttpClient;
import org.onap.policy.models.decisions.concepts.DecisionRequest;
import org.onap.policy.models.decisions.concepts.DecisionResponse;
import org.onap.policy.models.tosca.authorative.concepts.ToscaPolicy;
import org.onap.policy.pdp.xacml.application.common.ToscaPolicyConversionException;
import org.onap.policy.pdp.xacml.application.common.ToscaPolicyTranslator;
import org.onap.policy.pdp.xacml.application.common.XacmlApplicationException;
import org.onap.policy.pdp.xacml.application.common.XacmlPolicyUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ExtendWith(MockitoExtension.class)
class StdXacmlApplicationServiceProviderTest {
    private static final Logger logger = LoggerFactory.getLogger(StdXacmlApplicationServiceProviderTest.class);

    private static final String TEMP_DIR_NAME = "src/test/resources/temp";
    private static final File TEMP_DIR = new File(TEMP_DIR_NAME);
    private static final Path TEMP_PATH = TEMP_DIR.toPath();
    private static final File SOURCE_PROP_FILE = new File("src/test/resources/test.properties");
    private static final File PROP_FILE = new File(TEMP_DIR, XacmlPolicyUtils.XACML_PROPERTY_FILE);
    private static final String EXPECTED_EXCEPTION = "expected exception";
    private static final String POLICY_NAME = "my-name";
    private static final String POLICY_VERSION = "1.2.3";
    private static final String POLICY_TYPE = "my-type";

    @Mock
    private HttpClient apiClient;

    @Mock
    private ToscaPolicyTranslator trans;

    @Mock
    private PDPEngineFactory engineFactory;

    @Mock
    private PDPEngine engine;

    @Mock
    private Request req;

    @Mock
    private Response resp;

    private ToscaPolicy policy;

    private StdXacmlApplicationServiceProvider prov;

    /**
     * Creates the temp directory.
     */
    @BeforeAll
    static void setUpBeforeClass() {
        assertTrue(TEMP_DIR.mkdir());
    }

    /**
     * Deletes the temp directory and its contents.
     */
    @AfterAll
    static void tearDownAfterClass() {
        for (File file : Objects.requireNonNull(TEMP_DIR.listFiles())) {
            if (!file.delete()) {
                logger.warn("cannot delete: {}", file);
            }
        }

        if (!TEMP_DIR.delete()) {
            logger.warn("cannot delete: {}", TEMP_DIR);
        }
    }

    /**
     * Initializes objects, including the provider.
     *
     * @throws Exception if an error occurs
     */
    @BeforeEach
    void setUp() throws Exception {
        policy = new ToscaPolicy();
        policy.setType(POLICY_TYPE);
        policy.setName(POLICY_NAME);
        policy.setVersion(POLICY_VERSION);

        PolicyType internalPolicy = new PolicyType();
        internalPolicy.setPolicyId(POLICY_NAME);
        internalPolicy.setVersion(POLICY_VERSION);

        lenient().when(engineFactory.newEngine(any())).thenReturn(engine);

        lenient().when(engine.decide(req)).thenReturn(resp);

        lenient().when(trans.convertPolicy(policy)).thenReturn(internalPolicy);

        prov = new MyProv();

        Files.copy(SOURCE_PROP_FILE, PROP_FILE);
    }

    @Test
    void testApplicationName() {
        assertNotNull(prov.applicationName());
    }

    @Test
    void testActionDecisionsSupported() {
        assertTrue(prov.actionDecisionsSupported().isEmpty());
    }

    @Test
    void testInitialize_testGetXxx() throws XacmlApplicationException {
        prov.initialize(TEMP_PATH, apiClient);

        assertEquals(TEMP_PATH, prov.getDataPath());
        assertNotNull(prov.getEngine());

        Properties props = prov.getProperties();
        assertEquals("rootstart", props.getProperty("xacml.rootPolicies"));
    }

    @Test
    void testInitialize_Ex() {
        assertThatThrownBy(() -> prov.initialize(new File(TEMP_DIR_NAME + "-nonExistent").toPath(), apiClient))
            .isInstanceOf(XacmlApplicationException.class).hasMessage("Failed to load xacml.properties");
    }

    @Test
    void testSupportedPolicyTypes() {
        assertThat(prov.supportedPolicyTypes()).isEmpty();
    }

    @Test
    void testCanSupportPolicyType() {
        assertThatThrownBy(() -> prov.canSupportPolicyType(null)).isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void testLoadPolicy_ConversionError() throws ToscaPolicyConversionException {
        lenient().when(trans.convertPolicy(policy)).thenReturn(null);

        assertThatThrownBy(() -> prov.loadPolicy(policy)).isInstanceOf(XacmlApplicationException.class);
    }

    @Test
    void testLoadPolicy_testUnloadPolicy() throws Exception {
        prov.initialize(TEMP_PATH, apiClient);
        tryDeletePropFile();

        final Set<String> set = XACMLProperties.getRootPolicyIDs(prov.getProperties());

        // Load policy
        prov.loadPolicy(policy);

        // policy file should have been created
        File policyFile = new File(TEMP_DIR, "my-name_1.2.3.xml");
        assertTrue(policyFile.exists());

        // new property file should have been created
        assertTrue(PROP_FILE.exists());

        // should have re-created the engine
        verify(engineFactory, times(2)).newEngine(any());

        final Set<String> set2 = XACMLProperties.getRootPolicyIDs(prov.getProperties());
        assertEquals(set.size() + 1, set2.size());

        Set<String> set3 = new HashSet<>(set2);
        set3.removeAll(set);
        assertEquals("[root1]", set3.toString());


        /*
         * Prepare for unload.
         */
        tryDeletePropFile();

        assertTrue(prov.unloadPolicy(policy));

        // policy file should have been removed
        assertFalse(policyFile.exists());

        // new property file should have been created
        assertTrue(PROP_FILE.exists());

        // should have re-created the engine
        verify(engineFactory, times(3)).newEngine(any());

        set3 = XACMLProperties.getRootPolicyIDs(prov.getProperties());
        assertEquals(set.toString(), set3.toString());
    }

    @Test
    void testUnloadPolicy_NotDeployed() throws Exception {
        prov.initialize(TEMP_PATH, apiClient);

        assertFalse(prov.unloadPolicy(policy));

        // no additional calls
        verify(engineFactory, times(1)).newEngine(any());
    }

    @Test
    void testMakeDecision() throws ToscaPolicyConversionException {
        prov.createEngine(null);

        DecisionRequest decreq = mock(DecisionRequest.class);
        lenient().when(trans.convertRequest(decreq)).thenReturn(req);

        DecisionResponse decresp = mock(DecisionResponse.class);
        lenient().when(trans.convertResponse(resp)).thenReturn(decresp);

        Pair<DecisionResponse, Response> result = prov.makeDecision(decreq, any());
        assertSame(decresp, result.getKey());
        assertSame(resp, result.getValue());

        verify(trans).convertRequest(decreq);
        verify(trans).convertResponse(resp);
    }

    @Test
    void testGetTranslator() {
        assertSame(trans, prov.getTranslator());
    }

    @Test
    void testCreateEngine() throws FactoryException {
        // success
        prov.createEngine(null);
        assertSame(engine, prov.getEngine());

        // null - should be unchanged
        lenient().when(engineFactory.newEngine(any())).thenReturn(null);
        prov.createEngine(null);
        assertSame(engine, prov.getEngine());

        // exception - should be unchanged
        lenient().when(engineFactory.newEngine(any())).thenThrow(new FactoryException(EXPECTED_EXCEPTION));
        prov.createEngine(null);
        assertSame(engine, prov.getEngine());
    }

    @Test
    void testXacmlDecision() throws PDPException {
        prov.createEngine(null);

        // success
        assertSame(resp, prov.xacmlDecision(req));

        // exception
        lenient().when(engine.decide(req)).thenThrow(new PDPException(EXPECTED_EXCEPTION));
        assertNull(prov.xacmlDecision(req));
    }

    @Test
    void testGetPdpEngineFactory() throws XacmlApplicationException {
        // use the real engine factory
        engineFactory = null;

        prov = new MyProv();
        prov.initialize(TEMP_PATH, apiClient);

        assertNotNull(prov.getEngine());
    }

    private void tryDeletePropFile() {
        if (!PROP_FILE.delete()) {
            logger.warn("{} not deleted", PROP_FILE);
        }
    }

    private class MyProv extends StdXacmlApplicationServiceProvider {

        @Override
        protected ToscaPolicyTranslator getTranslator(String type) {
            return trans;
        }

        @Override
        protected PDPEngineFactory getPdpEngineFactory() throws FactoryException {
            return (engineFactory != null ? engineFactory : super.getPdpEngineFactory());
        }
    }
}
