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

package org.onap.policy.pdp.xacml.application.common;

import static org.assertj.core.api.Assertions.assertThatCode;

import com.att.research.xacml.api.XACML3;
import com.att.research.xacml.util.XACMLPolicyWriter;
import com.att.research.xacml.util.XACMLProperties;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map.Entry;
import java.util.Properties;
import oasis.names.tc.xacml._3_0.core.schema.wd_17.AnyOfType;
import oasis.names.tc.xacml._3_0.core.schema.wd_17.EffectType;
import oasis.names.tc.xacml._3_0.core.schema.wd_17.MatchType;
import oasis.names.tc.xacml._3_0.core.schema.wd_17.ObjectFactory;
import oasis.names.tc.xacml._3_0.core.schema.wd_17.PolicySetType;
import oasis.names.tc.xacml._3_0.core.schema.wd_17.PolicyType;
import oasis.names.tc.xacml._3_0.core.schema.wd_17.RuleType;
import oasis.names.tc.xacml._3_0.core.schema.wd_17.TargetType;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utility methods for storing policies to disk and updating Properties objects
 * that reference policies.
 *
 * @author pameladragosh
 *
 */
public class XacmlUpdatePolicyUtilsTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(XacmlUpdatePolicyUtilsTest.class);

    static Properties properties;

    static PolicySetType rootPolicy = new PolicySetType();

    static Path rootPath;

    static PolicyType policy1 = new PolicyType();
    static PolicyType policy2 = new PolicyType();

    static PolicySetType policySet3 = new PolicySetType();

    static Path path1;
    static Path path2;

    static Path policySetPath;

    /**
     * Temporary folder where we will store newly created policies.
     */
    @ClassRule
    public static TemporaryFolder policyFolder = new TemporaryFolder();

    /**
     * Setup the JUnit tests.
     *
     * @throws Exception thrown
     */
    @BeforeClass
    public static void setUp() throws Exception {
        assertThatCode(() -> {
            //
            // Load our test property object
            //
            try (InputStream is = new FileInputStream("src/test/resources/test.properties")) {
                properties = new Properties();
                properties.load(is);
            }
            //
            // Create a very basic Root policy
            //
            rootPolicy.setPolicySetId("root");
            rootPolicy.setTarget(new TargetType());
            rootPolicy.setPolicyCombiningAlgId(XACML3.ID_POLICY_FIRST_APPLICABLE.stringValue());
            File rootFile = policyFolder.newFile("root.xml");
            LOGGER.info("Creating Root Policy {}", rootFile.getAbsolutePath());
            rootPath = XACMLPolicyWriter.writePolicyFile(rootFile.toPath(), rootPolicy);
            //
            // Create policies
            //
            path1 = createPolicy(policy1, "policy1", "resource1");
            LOGGER.info(new String(Files.readAllBytes(path1)));
            path2 = createPolicy(policy2, "policy2", "resource2");
            LOGGER.info(new String(Files.readAllBytes(path2)));
            //
            // Create another PolicySet
            //
            policySet3.setPolicySetId("policyset1");
            policySet3.setTarget(new TargetType());
            policySet3.setPolicyCombiningAlgId(XACML3.ID_POLICY_FIRST_APPLICABLE.stringValue());
            ObjectFactory factory = new ObjectFactory();

            policySet3.getPolicySetOrPolicyOrPolicySetIdReference().add(factory.createPolicy(policy1));
            policySet3.getPolicySetOrPolicyOrPolicySetIdReference().add(factory.createPolicy(policy2));
            File policySetFile = policyFolder.newFile("policySet1.xml");
            LOGGER.info("Creating PolicySet {}", policySetFile.getAbsolutePath());
            policySetPath = XACMLPolicyWriter.writePolicyFile(policySetFile.toPath(), policySet3);

        }).doesNotThrowAnyException();
    }

    private static Path createPolicy(PolicyType policy, String id, String resource) throws IOException {
        //
        // Create Policy 1
        //
        policy.setPolicyId(id);
        MatchType matchPolicyId = ToscaPolicyConverterUtils.buildMatchTypeDesignator(
                XACML3.ID_FUNCTION_STRING_EQUAL,
                resource,
                XACML3.ID_DATATYPE_STRING,
                XACML3.ID_RESOURCE_RESOURCE_ID,
                XACML3.ID_ATTRIBUTE_CATEGORY_RESOURCE);
        //
        // This is our outer AnyOf - which is an OR
        //
        AnyOfType anyOf = new AnyOfType();
        //
        // Create AllOf (AND) of just Policy Id
        //
        anyOf.getAllOf().add(ToscaPolicyConverterUtils.buildAllOf(matchPolicyId));
        TargetType target = new TargetType();
        target.getAnyOf().add(anyOf);
        policy.setTarget(target);
        RuleType rule = new RuleType();
        rule.setRuleId(policy.getPolicyId() + ":rule");
        rule.setEffect(EffectType.PERMIT);
        rule.setTarget(new TargetType());
        //
        // Add the rule to the policy
        //
        policy.getCombinerParametersOrRuleCombinerParametersOrVariableDefinition().add(rule);
        //
        // Create a file
        //
        File file = policyFolder.newFile(policy.getPolicyId() + ".xml");
        LOGGER.info("Creating Policy {}", file.getAbsolutePath());
        return XACMLPolicyWriter.writePolicyFile(file.toPath(), policy);
    }

    @Test
    public void testUpdatingPolicies() {
        assertThatCode(() -> {
            //
            // Just update root and policies
            //
            XacmlUpdatePolicyUtils.updateXacmlRootPolicy(rootPolicy, policy1, policy2);
            try (OutputStream os = new ByteArrayOutputStream()) {
                XACMLPolicyWriter.writePolicyFile(os, rootPolicy);
                LOGGER.debug("New Root Policy:{}{}", System.lineSeparator(), os.toString());
            }
            //
            // Test updating the properties
            //
            XACMLProperties.setXacmlRootProperties(properties, rootPath);
            XACMLProperties.setXacmlReferencedProperties(properties, path1, path2);
            //
            // Dump this out so I can see what I'm doing
            //
            for (Entry<Object, Object> entry : properties.entrySet()) {
                LOGGER.info("{}={}", entry.getKey(), entry.getValue());
            }
            LOGGER.info("Properties {}", properties.toString());
            //
            // Somehow I have to figure out how to test this in assertj
            //
            //
            // Just update root and PolicySet
            //
            XacmlUpdatePolicyUtils.updateXacmlRootPolicy(rootPolicy, policySet3);
            try (OutputStream os = new ByteArrayOutputStream()) {
                XACMLPolicyWriter.writePolicyFile(os, rootPolicy);
                LOGGER.debug("New Root Policy:{}{}", System.lineSeparator(), os.toString());
            }
            //
            // Test updating the properties
            //
            XACMLProperties.setXacmlRootProperties(properties, rootPath);
            XACMLProperties.setXacmlReferencedProperties(properties, policySetPath);
            //
            // Dump this out so I can see what I'm doing
            //
            for (Entry<Object, Object> entry : properties.entrySet()) {
                LOGGER.info("{}={}", entry.getKey(), entry.getValue());
            }
            LOGGER.info("Properties {}", properties.toString());
            //
            // Somehow I have to figure out how to test this in assertj
            //

        }).doesNotThrowAnyException();
    }

    @Test
    public void testProperties() {
        Properties properties = new Properties();


    }
}
