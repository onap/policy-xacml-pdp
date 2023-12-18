/*-
 * ============LICENSE_START=======================================================
 * Copyright (C) 2020-2021 AT&T Intellectual Property. All rights reserved.
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
 * ============LICENSE_END=========================================================
 */

package org.onap.policy.tutorial.tutorial;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;

import com.att.research.xacml.api.Response;
import com.att.research.xacml.api.XACML3;
import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.Properties;
import java.util.ServiceLoader;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.onap.policy.common.utils.coder.CoderException;
import org.onap.policy.common.utils.coder.StandardCoder;
import org.onap.policy.common.utils.resources.TextFileUtils;
import org.onap.policy.models.decisions.concepts.DecisionRequest;
import org.onap.policy.models.decisions.concepts.DecisionResponse;
import org.onap.policy.pdp.xacml.application.common.XacmlApplicationServiceProvider;
import org.onap.policy.pdp.xacml.application.common.XacmlPolicyUtils;
import org.onap.policy.pdp.xacml.xacmltest.TestUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TutorialApplicationTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(TutorialApplicationTest.class);
    private static final Properties properties = new Properties();
    private static XacmlApplicationServiceProvider service;
    private static final StandardCoder gson = new StandardCoder();

    @ClassRule
    public static final TemporaryFolder policyFolder = new TemporaryFolder();

    /**
     * set up the tests.
     *
     * @throws Exception Should not have exceptions thrown.
     */
    @BeforeClass
    public static void setup() throws Exception {
        //
        // Set up our temporary folder
        //
        XacmlPolicyUtils.FileCreator myCreator = policyFolder::newFile;
        File propertiesFile = XacmlPolicyUtils
            .copyXacmlPropertiesContents("src/test/resources/xacml.properties", properties, myCreator);
        //
        // Load XacmlApplicationServiceProvider service
        //
        ServiceLoader<XacmlApplicationServiceProvider> applicationLoader =
                ServiceLoader.load(XacmlApplicationServiceProvider.class);
        //
        // Look for our class instance and save it
        //
        for (XacmlApplicationServiceProvider application : applicationLoader) {
            //
            // Is it our service?
            //
            if (application instanceof TutorialApplication) {
                service = application;
            }
        }
        //
        // Tell the application to initialize based on the properties file
        // we just built for it.
        //
        service.initialize(propertiesFile.toPath().getParent(), null);
        //
        // Now load the tutorial policies.
        //
        TestUtils.loadPolicies("src/test/resources/tutorial-policies.yaml", service);
    }

    @Test
    public void testSingleDecision() throws CoderException, IOException {
        //
        // Load a Decision request
        //
        DecisionRequest decisionRequest =
                gson.decode(TextFileUtils.getTextFileAsString("src/test/resources/tutorial-decision-request.json"),
                        DecisionRequest.class);
        LOGGER.info("{}", gson.encode(decisionRequest, true));
        //
        // Test a decision - should start with a permit
        //
        Pair<DecisionResponse, Response> decision = service.makeDecision(decisionRequest, null);
        LOGGER.info("{}", gson.encode(decision.getLeft(), true));
        assertEquals("Permit", decision.getLeft().getStatus());
        //
        // Check that there are attributes
        //
        assertThat(decision.getLeft().getAttributes()).isNotNull().hasSize(1)
                .containsKey(XACML3.ID_ATTRIBUTE_CATEGORY_RESOURCE.stringValue());
        //
        // This should be a "deny"
        //
        decisionRequest.getResource().put("user", "audit");
        LOGGER.info("{}", gson.encode(decisionRequest, true));
        decision = service.makeDecision(decisionRequest, null);
        LOGGER.info("{}", gson.encode(decision.getLeft(), true));
        assertEquals("Deny", decision.getLeft().getStatus());
        //
        // Check that there are attributes
        //
        assertThat(decision.getLeft().getAttributes()).isNotNull().hasSize(1)
                .containsKey(XACML3.ID_ATTRIBUTE_CATEGORY_RESOURCE.stringValue());
    }


    @Test
    public void testMultiDecision() throws CoderException, IOException {
        //
        // Load a Decision request
        //
        DecisionRequest decisionRequest = gson.decode(
                TextFileUtils.getTextFileAsString("src/test/resources/tutorial-decision-multi-request.json"),
                DecisionRequest.class);
        LOGGER.info("{}", gson.encode(decisionRequest, true));
        //
        // Test a decision - should start with a permit
        //
        Pair<DecisionResponse, Response> decision = service.makeDecision(decisionRequest, null);
        LOGGER.info("{}", gson.encode(decision.getLeft(), true));
        assertEquals("multi", decision.getLeft().getStatus());
        //
        // Check that there no attributes for the overall response
        //
        assertThat(decision.getLeft().getAttributes()).isNull();
        //
        // Check that there are 7 decisions with attributes
        //
        assertThat(decision.getLeft()).isInstanceOf(TutorialResponse.class);
        TutorialResponse tutorialResponse = (TutorialResponse) decision.getLeft();
        assertThat(tutorialResponse.getPermissions()).hasSize(7);
        tutorialResponse.getPermissions().forEach(this::checkPermission);
    }

    private void checkPermission(TutorialResponsePermission permission) {
        assertThat(permission.getAttributes()).hasSize(1);
        Object resourceAttributes = permission.getAttributes().get(XACML3.ID_ATTRIBUTE_CATEGORY_RESOURCE.stringValue());
        assertThat(resourceAttributes).isNotNull().isInstanceOf(Map.class);
        @SuppressWarnings("unchecked")
        String multiId = ((Map<String, String>) resourceAttributes).get("urn:org:onap:tutorial-multi-id");
        assertThat(Integer.parseInt(multiId)).isBetween(1, 7);
        switch (multiId) {
            case "1", "2", "4":
                assertThat(permission.getStatus()).isEqualTo("Permit");
                return;
            case "3", "5", "6", "7":
                assertThat(permission.getStatus()).isEqualTo("Deny");
                return;
            default:
                //
                // Should not get here as we check the value range in line 168.
                // But CodeStyle wants a default.
                //
                break;
        }
    }
}
