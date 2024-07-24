/*-
 * ============LICENSE_START=======================================================
 * ONAP
 * ================================================================================
 * Copyright (C) 2019-2020 AT&T Intellectual Property. All rights reserved.
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

package org.onap.policy.pdp.xacml.application.common;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.att.research.xacml.api.XACML3;
import com.att.research.xacml.util.XACMLPolicyWriter;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Properties;
import oasis.names.tc.xacml._3_0.core.schema.wd_17.AnyOfType;
import oasis.names.tc.xacml._3_0.core.schema.wd_17.EffectType;
import oasis.names.tc.xacml._3_0.core.schema.wd_17.MatchType;
import oasis.names.tc.xacml._3_0.core.schema.wd_17.ObjectFactory;
import oasis.names.tc.xacml._3_0.core.schema.wd_17.PolicySetType;
import oasis.names.tc.xacml._3_0.core.schema.wd_17.PolicyType;
import oasis.names.tc.xacml._3_0.core.schema.wd_17.RuleType;
import oasis.names.tc.xacml._3_0.core.schema.wd_17.TargetType;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utility methods for storing policies to disk and updating Properties objects
 * that reference policies.
 *
 * @author pameladragosh
 */
class XacmlPolicyUtilsTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(XacmlPolicyUtilsTest.class);

    static Properties properties;

    static PolicySetType rootPolicy = XacmlPolicyUtils.createEmptyPolicySet("root", XACML3.ID_POLICY_FIRST_APPLICABLE);

    static Path rootPath;

    static PolicyType policy1 = XacmlPolicyUtils.createEmptyPolicy("policy1", XACML3.ID_RULE_DENY_UNLESS_PERMIT);
    static PolicyType policy2 = XacmlPolicyUtils.createEmptyPolicy("policy2", XACML3.ID_RULE_DENY_UNLESS_PERMIT);
    static PolicyType policy3 = XacmlPolicyUtils.createEmptyPolicy("policy3", XACML3.ID_RULE_DENY_UNLESS_PERMIT);
    static PolicyType policy4 = XacmlPolicyUtils.createEmptyPolicy("policy4", XACML3.ID_RULE_DENY_UNLESS_PERMIT);

    static PolicySetType policySet5 = XacmlPolicyUtils.createEmptyPolicySet(
        "policyset1", XACML3.ID_POLICY_FIRST_APPLICABLE);

    static Path path1;
    static Path path2;
    static Path path3;
    static Path path4;

    static Path policySetPath;

    /**
     * Temporary folder where we will store newly created policies.
     */
    @TempDir
    static Path policyFolder;

    /**
     * Setup the JUnit tests by finishing creating the policies and
     * writing them out to the temporary folder.
     */
    @BeforeAll
    public static void setUp() {
        assertThatCode(() -> {
            //
            // Load our test property object
            //
            try (InputStream is = new FileInputStream("src/test/resources/test.properties")) {
                properties = new Properties();
                properties.load(is);
            }
            //
            // Change "/" to file separator in file names
            //
            if (!"/".equals(File.separator)) {
                List<String> fileProps = properties.keySet().stream().map(Object::toString)
                    .filter(key -> key.endsWith(".file")).toList();
                for (String fileProp : fileProps) {
                    properties.setProperty(fileProp, properties.getProperty(fileProp).replace("/", File.separator));
                }
            }
            //
            // Save root policy
            //
            Path rootFile = XacmlPolicyUtils.constructUniquePolicyFilename(rootPolicy,
                policyFolder.toAbsolutePath());
            LOGGER.info("Creating Root Policy {}", rootFile.toAbsolutePath());
            rootPath = XacmlPolicyUtils.writePolicyFile(rootFile, rootPolicy);
            //
            // Create policies - Policies 1 and 2 will become references in the
            // root policy. While Policies 3 and 4 will become references in the
            // soon-to-be created PolicySet 5 below.
            //
            path1 = createPolicyContents(policy1, "resource1");
            LOGGER.info(new String(Files.readAllBytes(path1)));
            path2 = createPolicyContents(policy2, "resource2");
            LOGGER.info(new String(Files.readAllBytes(path2)));
            path3 = createPolicyContents(policy3, "resourc31");
            LOGGER.info(new String(Files.readAllBytes(path3)));
            path4 = createPolicyContents(policy4, "resource4");
            LOGGER.info(new String(Files.readAllBytes(path4)));
            //
            // Create our PolicySet
            //
            policySet5.setPolicySetId("policyset5");
            policySet5.setTarget(new TargetType());
            policySet5.setPolicyCombiningAlgId(XACML3.ID_POLICY_FIRST_APPLICABLE.stringValue());
            ObjectFactory factory = new ObjectFactory();
            //
            // Add Policies 3 and 4 to the PolicySet
            //
            policySet5.getPolicySetOrPolicyOrPolicySetIdReference().add(factory.createPolicy(policy1));
            policySet5.getPolicySetOrPolicyOrPolicySetIdReference().add(factory.createPolicy(policy2));
            assertThat(policySet5.getPolicySetOrPolicyOrPolicySetIdReference()).hasSize(2);
            //
            // Save that to disk
            //
            File policySetFile = policyFolder.resolve("policySet5.xml").toFile();
            LOGGER.info("Creating PolicySet {}", policySetFile.getAbsolutePath());
            policySetPath = XACMLPolicyWriter.writePolicyFile(policySetFile.toPath(), policySet5);

        }).doesNotThrowAnyException();
    }

    /**
     * Helper method that creates a very simple Policy and Rule and saves it to disk.
     *
     * @param policy   Policy to store contents in
     * @param resource A simple resource id for the Target
     * @return Path object of the policy
     */
    private static Path createPolicyContents(PolicyType policy, String resource) {
        //
        // Create The Match
        //
        MatchType matchPolicyId = ToscaPolicyTranslatorUtils.buildMatchTypeDesignator(
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
        anyOf.getAllOf().add(ToscaPolicyTranslatorUtils.buildAllOf(matchPolicyId));
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
        // Save it to disk
        //
        Path policyFile = XacmlPolicyUtils.constructUniquePolicyFilename(policy,
            policyFolder.toAbsolutePath());
        LOGGER.info("Creating Policy {}", policyFile.toAbsolutePath());
        return XacmlPolicyUtils.writePolicyFile(policyFile, policy);
    }

    @Test
    void testUncommonConditions() {
        Path fileTemp = policyFolder.toFile().toPath();
        assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() ->
            XacmlPolicyUtils.writePolicyFile(fileTemp, "not a policy")
        );

        Path rootPathZ = policyFolder.toAbsolutePath();
        assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() ->
            XacmlPolicyUtils.constructUniquePolicyFilename("not a policy", rootPathZ)
        );
    }

    @Test
    void testUpdatingPolicies() {
        assertThatCode(() -> {
            //
            // Just update root and policies
            //
            XacmlPolicyUtils.addPoliciesToXacmlRootPolicy(rootPolicy, policy1, policy2);
            //
            // Make sure it is correct
            //
            assertThat(rootPolicy.getPolicySetOrPolicyOrPolicySetIdReference()).hasSize(2);
            //
            // Save to disk
            //
            try (OutputStream os = new ByteArrayOutputStream()) {
                XACMLPolicyWriter.writePolicyFile(os, rootPolicy);
                LOGGER.debug("New Root Policy:{}{}", XacmlPolicyUtils.LINE_SEPARATOR, os);
            }
            //
            // Just update root and PolicySet
            //
            XacmlPolicyUtils.addPolicySetsToXacmlRootPolicy(rootPolicy, policySet5);
            try (OutputStream os = new ByteArrayOutputStream()) {
                XACMLPolicyWriter.writePolicyFile(os, rootPolicy);
                LOGGER.debug("New Root Policy:{}{}", XacmlPolicyUtils.LINE_SEPARATOR, os);
            }
        }).doesNotThrowAnyException();
    }

    @Test
    void testRemovingReferencedProperties() {
        //
        // Dump what we are starting with
        //
        XacmlPolicyUtils.debugDumpPolicyProperties(properties, LOGGER);
        //
        // Remove referenced policies
        //
        Path ref = Paths.get("src/test/resources/ref1.xml");
        XacmlPolicyUtils.removeReferencedPolicy(properties, ref);
        XacmlPolicyUtils.debugDumpPolicyProperties(properties, LOGGER);
        assertThat(properties.getProperty("refstart1.file")).isNullOrEmpty();

        ref = Paths.get("src/test/resources/ref2.xml");
        XacmlPolicyUtils.removeReferencedPolicy(properties, ref);
        XacmlPolicyUtils.debugDumpPolicyProperties(properties, LOGGER);
        assertThat(properties.getProperty("refstart2.file")).isNullOrEmpty();

        //
        // Test one that isn't in there
        //
        ref = Paths.get("src/test/resources/NotThere.xml");
        XacmlPolicyUtils.removeReferencedPolicy(properties, ref);
        XacmlPolicyUtils.debugDumpPolicyProperties(properties, LOGGER);
        assertThat(properties.getProperty("refstart3.file")).isNotBlank();

        ref = Paths.get("src/test/resources/ref3.xml");
        XacmlPolicyUtils.removeReferencedPolicy(properties, ref);
        XacmlPolicyUtils.debugDumpPolicyProperties(properties, LOGGER);
        assertThat(properties.getProperty("refstart3.file")).isNullOrEmpty();

        ref = Paths.get("src/test/resources/ref4.xml");
        XacmlPolicyUtils.removeReferencedPolicy(properties, ref);
        XacmlPolicyUtils.debugDumpPolicyProperties(properties, LOGGER);
        assertThat(properties.getProperty("refstart4.file")).isNullOrEmpty();
    }

    @Test
    void testRemovingRootProperties() {
        //
        // Dump what we are starting with
        //
        XacmlPolicyUtils.debugDumpPolicyProperties(properties, LOGGER);
        //
        // Remove root policies
        //
        Path ref = Paths.get("src/test/resources/root.xml");
        XacmlPolicyUtils.removeRootPolicy(properties, ref);
        XacmlPolicyUtils.debugDumpPolicyProperties(properties, LOGGER);
        assertThat(properties.getProperty("root.file")).isNullOrEmpty();

        //
        // Test one that isn't in there
        //
        ref = Paths.get("src/test/resources/NotThere.xml");
        XacmlPolicyUtils.removeRootPolicy(properties, ref);
        XacmlPolicyUtils.debugDumpPolicyProperties(properties, LOGGER);
        assertThat(properties.getProperty("refstart3.file")).isNotBlank();
    }

    @Test
    void testCopyingProperties() throws Exception {
        //
        // Copy to this folder
        //
        File copyFolder = policyFolder.resolve("copy").toFile();
        assertTrue(copyFolder.mkdirs());
        assertThat(copyFolder).exists();
        //
        // Mock up a properties object
        //
        Properties mockProperties = new Properties();
        XacmlPolicyUtils.addRootPolicy(mockProperties, rootPath);
        XacmlPolicyUtils.addReferencedPolicy(mockProperties, path1);
        //
        // Write the properties out to a file
        //
        Path fileProperties = XacmlPolicyUtils.getPropertiesPath(policyFolder.toAbsolutePath());
        XacmlPolicyUtils.storeXacmlProperties(mockProperties, fileProperties);
        //
        // Now we can test the copy method
        //
        XacmlPolicyUtils.FileCreator myCreator = (String filename) -> policyFolder.resolve("copy/" + filename).toFile();
        File propertiesFile = XacmlPolicyUtils.copyXacmlPropertiesContents(
            fileProperties.toAbsolutePath().toString(), mockProperties, myCreator);

        assertThat(propertiesFile).canRead();
        assertThat(Path.of(copyFolder.getAbsolutePath(),
            rootPath.getFileName().toString()).toFile()).canRead();
        assertThat(Path.of(copyFolder.getAbsolutePath(),
            path1.getFileName().toString()).toFile()).canRead();
    }
}
