/*-
 * ============LICENSE_START=======================================================
 * ONAP
 * ================================================================================
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

package org.onap.policy.xacml.pdp.application.guard;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

import java.io.File;
import java.io.IOException;
import java.util.Iterator;
import java.util.Properties;
import java.util.ServiceLoader;

import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runners.MethodSorters;
import org.onap.policy.common.utils.coder.CoderException;
import org.onap.policy.common.utils.coder.StandardCoder;
import org.onap.policy.common.utils.resources.TextFileUtils;
import org.onap.policy.models.decisions.concepts.DecisionRequest;
import org.onap.policy.models.decisions.serialization.DecisionRequestMessageBodyHandler;
import org.onap.policy.models.decisions.serialization.DecisionResponseMessageBodyHandler;
import org.onap.policy.pdp.xacml.application.common.XacmlApplicationServiceProvider;
import org.onap.policy.pdp.xacml.application.common.XacmlPolicyUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class GuardPdpApplicationTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(GuardPdpApplicationTest.class);
    private static Properties properties = new Properties();
    private static File propertiesFile;
    private static XacmlApplicationServiceProvider service;
    private static DecisionRequest requestSinglePolicy;
    private static StandardCoder gson = new StandardCoder();

    @ClassRule
    public static final TemporaryFolder policyFolder = new TemporaryFolder();

    /**
     * Copies the xacml.properties and policies files into
     * temporary folder and loads the service provider saving
     * instance of provider off for other tests to use.
     */
    @BeforeClass
    public static void setUp() throws Exception {
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
        // Iterate through them - I could store the object as
        // XacmlApplicationServiceProvider pointer.
        //
        // Try this later.
        //
        StringBuilder strDump = new StringBuilder("Loaded applications:" + System.lineSeparator());
        Iterator<XacmlApplicationServiceProvider> iterator = applicationLoader.iterator();
        while (iterator.hasNext()) {
            XacmlApplicationServiceProvider application = iterator.next();
            //
            // Is it our service?
            //
            if (application instanceof GuardPdpApplication) {
                //
                // Should be the first and only one
                //
                assertThat(service).isNull();
                service = application;
            }
            strDump.append(application.applicationName());
            strDump.append(" supports ");
            strDump.append(application.supportedPolicyTypes());
            strDump.append(System.lineSeparator());
        }
        LOGGER.debug("{}", strDump);
        //
        // Tell it to initialize based on the properties file
        // we just built for it.
        //
        service.initialize(propertiesFile.toPath().getParent());
    }

    @Test
    public void test1Basics() throws CoderException, IOException {
        //
        // Load Single Decision Request
        //
        requestSinglePolicy = gson.decode(
                TextFileUtils
                    .getTextFileAsString("../../main/src/test/resources/decisions/decision.single.input.json"),
                    DecisionRequest.class);
        //
        // Make sure there's an application name
        //
        assertThat(service.applicationName()).isNotEmpty();
        //
        // Decisions
        //
        assertThat(service.actionDecisionsSupported().size()).isEqualTo(1);
        assertThat(service.actionDecisionsSupported()).contains("guard");
        //
        // Ensure it has the supported policy types and
        // can support the correct policy types.
        //
        assertThat(service.supportedPolicyTypes()).isNotEmpty();
        assertThat(service.supportedPolicyTypes().size()).isEqualTo(2);
        assertThat(service.canSupportPolicyType("onap.policies.controlloop.guard.FrequencyLimiter", "1.0.0"))
            .isTrue();
        assertThat(service.canSupportPolicyType("onap.policies.controlloop.guard.FrequencyLimiter", "1.0.1"))
            .isFalse();
        assertThat(service.canSupportPolicyType("onap.policies.controlloop.guard.MinMax", "1.0.0")).isTrue();
        assertThat(service.canSupportPolicyType("onap.policies.controlloop.guard.MinMax", "1.0.1")).isFalse();
        assertThat(service.canSupportPolicyType("onap.foo", "1.0.1")).isFalse();
    }
}
