/*-
 * ============LICENSE_START=======================================================
 * ONAP
 * ================================================================================
 * Copyright (C) 2019-2020 AT&T Intellectual Property. All rights reserved.
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

package cucumber;

import com.att.research.xacml.std.annotations.XACMLAction;
import com.att.research.xacml.std.annotations.XACMLRequest;
import com.att.research.xacml.std.annotations.XACMLResource;
import com.att.research.xacml.std.annotations.XACMLSubject;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.junit.ClassRule;
import org.junit.rules.TemporaryFolder;

public class Stepdefs {

    /*

    private static final Logger logger = LoggerFactory.getLogger(Stepdefs.class);

    public static OnapXacmlPdpEngine onapPdpEngine;
    public static Properties properties;
    public static Map<String, Object> tcaPolicy;
    public static Request request;
    public static File pathProperties;
    public static File pathRootPolicy;

    /**
     * Temporary folder where we will store newly created policies.
     */
    @ClassRule
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

    /**
     * Initialization.
     */
    @Given("Initialization")
    public void initialization() {
        /*
        //
        // Everything initializes upon startup
        //
        assertThatCode(() -> {
            //
            // Assume XACML REST Controller loads PDP engine
            //
            onapPdpEngine = new OnapXacmlPdpEngine();
            //
            // Come up with defaults
            //
            File path = Paths.get("src/test/resources").toFile();
            /*
        //    try (InputStream is = new FileInputStream("src/test/resources/xacml.properties")) {
      //          properties = new Properties();
    //            properties.load(is);
  //              onapPdpEngine.initializeEngine(properties);
//            }
            onapPdpEngine.initialize(path.toPath());
            //
            // Store the properties in new path
            //
            // JUNIT IS CRASHING - THE TEMP FOLDER NOT CREATED -->
            //pathProperties = policyFolder.newFile("xacml.properties");
            //
            // Store the root policies
            //
            for (String rootPolicyId : XACMLProperties.getRootPolicyIDs(properties)) {
                logger.debug("Root policy id: " + rootPolicyId);
            }

        }).doesNotThrowAnyException();
        */
    }

    /**
     * Initialization.
     */
    @When("Decision Requested")
    public void decision_Requested() {
        /*
        //
        // Simulate a request coming in from Xacml REST server
        //
        assertThatCode(() -> {
            request = RequestParser.parseRequest(new MyXacmlRequest());
        }).doesNotThrowAnyException();
        */
    }

    /**
     * Initialization.
     */
    @Then("Decision Permit {int} Obligations")
    public void decision_Permit_Obligations(Integer int1) {
        /*
        Response response = onapPdpEngine.decision(request);
        for (Result result : response.getResults()) {
            logger.debug(result.getDecision().toString());
            assertEquals(Decision.PERMIT, result.getDecision());
            assertThat(result.getObligations().size()).isEqualTo(int1);
        }
        */
    }

    /**
     * Initialization.
     */
    @When("The application gets new Tosca Policy")
    public void the_application_gets_new_Tosca_Policy() {
        /*
        //
        // The Xacml PDP REST controller Would receive this from the PAP
        //
        // And then parse it looking for Policy Types
        //
        assertThatCode(() -> {
            try (InputStream is = new FileInputStream("src/test/resources/vDNS.policy.input.yaml")) {
                Yaml yaml = new Yaml();
                tcaPolicy = yaml.load(is);
                //
                // Do we test iterating and determining if supported?
                //

            }
        }).doesNotThrowAnyException();
        */
    }

    /**
     * Initialization.
     */
    @Then("Load Policy")
    public void load_Policy() {
        /*
        assertThatCode(() -> {
            //
            // Load the policies
            //
            List<PolicyType> convertedPolicies = onapPdpEngine.convertPolicies(tcaPolicy);
            //
            // Store these in temporary folder
            //
            int id = 1;
            List<Path> newReferencedPolicies = new ArrayList<>();
            for (PolicyType convertedPolicy : convertedPolicies) {
                //
                // I don't think we should use the policy id as the filename - there could
                // possibly be duplicates. eg. Not guaranteed to be unique.
                //
                File file = policyFolder.newFile("policy." + id + convertedPolicy.getPolicyId() + ".xml");
                logger.info("Creating Policy {}", file.getAbsolutePath());
                Path path = XACMLPolicyWriter.writePolicyFile(file.toPath(), convertedPolicy);
                //
                // Add it to our list
                //
                newReferencedPolicies.add(path);
            }
            //
            // Now updated the properties
            //
            Path[] args = new Path[newReferencedPolicies.size()];
            newReferencedPolicies.toArray(args);
            XACMLProperties.setXacmlReferencedProperties(properties, args);
            //
            // Reload the PDP engine
            //
            onapPdpEngine.initializeEngine(properties);
        }).doesNotThrowAnyException();
        */
    }

    /**
     * Initialization.
     */
    @Then("Save Configuration")
    public void save_Configuration() {
        /*
        assertThatCode(() -> {
            //
            // Save the configuration
            //
            onapPdpEngine.storeXacmlProperties(pathProperties.getAbsolutePath());
        }).doesNotThrowAnyException();
        */
    }
}
