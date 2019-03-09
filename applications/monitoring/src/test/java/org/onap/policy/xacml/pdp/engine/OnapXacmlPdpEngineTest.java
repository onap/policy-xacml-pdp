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

package org.onap.policy.xacml.pdp.engine;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.junit.Assert.assertEquals;

import com.att.research.xacml.api.Decision;
import com.att.research.xacml.api.Response;
import com.att.research.xacml.api.Result;
import com.att.research.xacml.std.annotations.RequestParser;
import com.att.research.xacml.std.annotations.XACMLAction;
import com.att.research.xacml.std.annotations.XACMLRequest;
import com.att.research.xacml.std.annotations.XACMLResource;
import com.att.research.xacml.std.annotations.XACMLSubject;
import com.att.research.xacml.util.XACMLPolicyWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Properties;
import oasis.names.tc.xacml._3_0.core.schema.wd_17.PolicyType;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OnapXacmlPdpEngineTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(OnapXacmlPdpEngineTest.class);

    @Rule
    public TemporaryFolder policyFolder = new TemporaryFolder();

    /**
     * This is a simple annotation class to simulate
     * requests coming in.
     */
    @XACMLRequest(ReturnPolicyIdList = true)
    public class MyXacmlRequest {

        @XACMLSubject(includeInResults = true)
        String onapName = "DCAE";

        @XACMLResource(includeInResults = true)
        String resource = "onap.policies.Monitoring";

        @XACMLAction()
        String action = "configure";
    }

    @Test
    public void test() {
        //
        // Make sure we can at least load the engine
        //
        assertThatCode(() -> {
            OnapXacmlPdpEngine onapPdpEngine = new OnapXacmlPdpEngine();
            //
            // Load test properties into the engine
            //
            try (InputStream is = new FileInputStream("src/test/resources/xacml.properties")) {
                Properties properties = new Properties();
                properties.load(is);
                onapPdpEngine.initializeEngine(properties);
            }
            //
            // Make a simple decision - NO policies are loaded
            //
            Response response = onapPdpEngine.decision(RequestParser.parseRequest(new MyXacmlRequest()));
            for (Result result : response.getResults()) {
                LOGGER.info("Decision {}", result.getDecision());
                assertEquals(Decision.NOTAPPLICABLE, result.getDecision());
            }
            //
            // Now convert a Yaml Tosca Policy to a Xacml Policy
            //
            try (InputStream is = new FileInputStream("src/test/resources/vDNS.policy.input.yaml")) {
                List<PolicyType> policies = onapPdpEngine.convertPolicy(is);
                for (PolicyType policy : policies) {
                    LOGGER.info("Policy {}", policy);
                    File file = policyFolder.newFile(policy.getPolicyId() + ".xml");
                    LOGGER.info("Creating Policy {}", file.getAbsolutePath());
                    Path path = XACMLPolicyWriter.writePolicyFile(file.toPath(), policy);
                    //
                    // Dump it out so we can see it in debug
                    //
                    String strXacmlPolicyContents = new String(Files.readAllBytes(path));
                    LOGGER.info("Policy Contents: {}{}", System.lineSeparator(), strXacmlPolicyContents);
                }
            }
        }).doesNotThrowAnyException();
    }

}
