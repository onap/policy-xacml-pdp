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

import com.att.research.xacml.api.Identifier;
import com.att.research.xacml.util.XACMLProperties;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;
import java.util.StringJoiner;
import java.util.stream.Collectors;

import oasis.names.tc.xacml._3_0.core.schema.wd_17.IdReferenceType;
import oasis.names.tc.xacml._3_0.core.schema.wd_17.ObjectFactory;
import oasis.names.tc.xacml._3_0.core.schema.wd_17.PolicySetType;
import oasis.names.tc.xacml._3_0.core.schema.wd_17.PolicyType;
import oasis.names.tc.xacml._3_0.core.schema.wd_17.TargetType;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class XacmlPolicyUtils {

    private static final Logger LOGGER = LoggerFactory.getLogger(XacmlPolicyUtils.class);

    public static final String XACML_PROPERTY_FILE = "xacml.properties";

    private static final String DOT_FILE_SUFFIX = ".file";
    private static final String NOT_FOUND_MESSAGE = "NOT FOUND";

    private XacmlPolicyUtils() {
        super();
    }

    /**
     * Creates an empty PolicySetType object given the id and combining algorithm. Note,there
     * will also be an empty Target created. You can easily override that if need be.
     *
     * @param policyId Policy Id
     * @param policyCombiningAlgorithm Policy Combining Algorithm
     * @return PolicySetType object
     */
    public static PolicySetType createEmptyPolicySet(String policyId, Identifier policyCombiningAlgorithm) {
        PolicySetType policy = new PolicySetType();
        policy.setPolicySetId(policyId);
        policy.setPolicyCombiningAlgId(policyCombiningAlgorithm.stringValue());
        policy.setTarget(new TargetType());
        return policy;
    }

    /**
     * Creates an empty PolicySetType object given the id and combining algorithm. Note,there
     * will also be an empty Target created. You can easily override that if need be.
     *
     * @param policyId Policy Id
     * @param ruleCombiningAlgorithm Rule Combining Algorithm
     * @return PolicyType object
     */
    public static PolicyType createEmptyPolicy(String policyId, Identifier ruleCombiningAlgorithm) {
        PolicyType policy = new PolicyType();
        policy.setPolicyId(policyId);
        policy.setRuleCombiningAlgId(ruleCombiningAlgorithm.stringValue());
        policy.setTarget(new TargetType());
        return policy;
    }

    /**
     * This method adds a list of PolicyType objects to a root PolicySetType as
     * referenced policies.
     *
     * @param rootPolicy Root PolicySet being updated
     * @param referencedPolicies A list of PolicyType being added as a references
     * @return the rootPolicy PolicySet object
     */
    public static PolicySetType addPoliciesToXacmlRootPolicy(PolicySetType rootPolicy,
            PolicyType... referencedPolicies) {
        ObjectFactory factory = new ObjectFactory();
        //
        // Iterate each policy
        //
        for (PolicyType referencedPolicy : referencedPolicies) {
            IdReferenceType reference = new IdReferenceType();
            reference.setValue(referencedPolicy.getPolicyId());
            //
            // Add it in
            //
            rootPolicy.getPolicySetOrPolicyOrPolicySetIdReference().add(factory.createPolicyIdReference(reference));
        }
        //
        // Return the updated object
        //
        return rootPolicy;
    }

    /**
     * This method updates a root PolicySetType by adding in a PolicyType as a reference.
     *
     * @param rootPolicy Root PolicySet being updated
     * @param referencedPolicySets A list of PolicySetType being added as a references
     * @return the rootPolicy PolicySet object
     */
    public static PolicySetType addPolicySetsToXacmlRootPolicy(PolicySetType rootPolicy,
            PolicySetType... referencedPolicySets) {
        ObjectFactory factory = new ObjectFactory();
        //
        // Iterate each policy
        //
        for (PolicySetType referencedPolicySet : referencedPolicySets) {
            IdReferenceType reference = new IdReferenceType();
            reference.setValue(referencedPolicySet.getPolicySetId());
            //
            // Add it in
            //
            rootPolicy.getPolicySetOrPolicyOrPolicySetIdReference().add(factory.createPolicySetIdReference(reference));
        }
        //
        // Return the updated object
        //
        return rootPolicy;
    }

    /**
     * Adds in the root policy to the PDP properties object.
     *
     * @param properties Input properties
     * @param rootPolicyPath Path to the root policy file
     * @return Properties object
     */
    public static Properties addRootPolicy(Properties properties, Path rootPolicyPath) {
        //
        // Get the current set of referenced policy ids
        //
        Set<String> rootPolicies = XACMLProperties.getRootPolicyIDs(properties);
        //
        // Construct a unique id
        //
        int id = 1;
        while (true) {
            String refId = "root" + id;
            if (rootPolicies.contains(refId)) {
                id++;
            } else {
                rootPolicies.add(refId);
                properties.put(refId + DOT_FILE_SUFFIX, rootPolicyPath.toAbsolutePath().toString());
                break;
            }
        }
        //
        // Set the new comma separated list
        //
        properties.setProperty(XACMLProperties.PROP_ROOTPOLICIES,
                rootPolicies.stream().collect(Collectors.joining(",")));
        return properties;
    }

    /**
     * Adds in the referenced policy to the PDP properties object.
     *
     * @param properties Input properties
     * @param refPolicyPath Path to the referenced policy file
     * @return Properties object
     */
    public static Properties addReferencedPolicy(Properties properties, Path refPolicyPath) {
        //
        // Get the current set of referenced policy ids
        //
        Set<String> referencedPolicies = XACMLProperties.getReferencedPolicyIDs(properties);
        //
        // Construct a unique id
        //
        int id = 1;
        while (true) {
            String refId = "ref" + id;
            if (referencedPolicies.contains(refId)) {
                id++;
            } else {
                referencedPolicies.add(refId);
                properties.put(refId + DOT_FILE_SUFFIX, refPolicyPath.toAbsolutePath().toString());
                break;
            }
        }
        //
        // Set the new comma separated list
        //
        properties.setProperty(XACMLProperties.PROP_REFERENCEDPOLICIES,
                referencedPolicies.stream().collect(Collectors.joining(",")));
        return properties;
    }

    /**
     * Removes a root policy from the Properties object. Both in the line
     * that identifies the policy and the .file property that points to the path.
     *
     * @param properties Input Properties object to remove
     * @param rootPolicyPath The policy file path
     * @return Properties object
     */
    public static Properties removeRootPolicy(Properties properties, Path rootPolicyPath) {
        //
        // Get the current set of referenced policy ids
        //
        StringJoiner join = new StringJoiner(",");
        boolean found = false;
        Set<String> rootPolicies = XACMLProperties.getRootPolicyIDs(properties);
        for (String refPolicy : rootPolicies) {
            String refPolicyFile = refPolicy + DOT_FILE_SUFFIX;
            //
            // If the key and value match, then it will return true
            //
            if (properties.remove(refPolicyFile, rootPolicyPath.toString())) {
                //
                // Record that we actually removed it
                //
                found = true;
            } else {
                //
                // Retain it
                //
                join.add(refPolicy);
            }
        }
        //
        // Did we remove it?
        //
        if (found) {
            //
            // Now update the list of referenced properties
            //
            properties.setProperty(XACMLProperties.PROP_ROOTPOLICIES, join.toString());
        }
        return properties;
    }

    /**
     * Removes a referenced policy from the Properties object. Both in the line
     * that identifies the policy and the .file property that points to the path.
     *
     * @param properties Input Properties object to remove
     * @param refPolicyPath The policy file path
     * @return Properties object
     */
    public static Properties removeReferencedPolicy(Properties properties, Path refPolicyPath) {
        //
        // Get the current set of referenced policy ids
        //
        StringJoiner join = new StringJoiner(",");
        boolean found = false;
        Set<String> referencedPolicies = XACMLProperties.getReferencedPolicyIDs(properties);
        for (String refPolicy : referencedPolicies) {
            String refPolicyFile = refPolicy + DOT_FILE_SUFFIX;
            //
            // If the key and value match, then it will return true
            //
            if (properties.remove(refPolicyFile, refPolicyPath.toString())) {
                //
                // Record that we actually removed it
                //
                found = true;
            } else {
                //
                // Retain it
                //
                join.add(refPolicy);
            }
        }
        //
        // Did we remove it?
        //
        if (found) {
            //
            // Now update the list of referenced properties
            //
            properties.setProperty(XACMLProperties.PROP_REFERENCEDPOLICIES, join.toString());
        }
        return properties;
    }

    /**
     * Does a debug dump of referenced and root policy values.
     *
     * @param properties Input Properties object
     * @param logger Logger object to use
     */
    public static void debugDumpPolicyProperties(Properties properties, Logger logger) {
        //
        // I hate surrounding this all with an if, but by
        // doing so I clear sonar issues with passing System.lineSeparator()
        // as an argument.
        //
        if (logger.isDebugEnabled()) {
            //
            // Get the current set of referenced policy ids
            //
            Set<String> rootPolicies = XACMLProperties.getRootPolicyIDs(properties);
            logger.debug("Root Policies: {}", properties.getProperty(XACMLProperties.PROP_ROOTPOLICIES));
            for (String root : rootPolicies) {
                logger.debug("{}", properties.getProperty(root + DOT_FILE_SUFFIX, NOT_FOUND_MESSAGE));
            }
            //
            // Get the current set of referenced policy ids
            //
            Set<String> referencedPolicies = XACMLProperties.getReferencedPolicyIDs(properties);
            logger.debug("Referenced Policies: {}", properties.getProperty(XACMLProperties.PROP_REFERENCEDPOLICIES));
            for (String ref : referencedPolicies) {
                logger.debug("{}", properties.getProperty(ref + DOT_FILE_SUFFIX, NOT_FOUND_MESSAGE));
            }
        }
    }

    /**
     * Constructs a unique policy filename for a given policy.
     *
     * <P>It could be dangerous to use policy-id and policy-version if the user
     * gives us an invalid policy-id and policy-versions.
     *
     * <P>Should we append a UUID also to guarantee uniqueness?
     *
     * <P>How do we track that in case we need to know what policies we have loaded?
     *
     * @param policy PolicyType object
     * @param path Path for policy
     * @return Path unique file path for the Policy
     */
    public static Path constructUniquePolicyFilename(PolicyType policy, Path path) {
        //
        //
        // Can it be possible to produce an invalid filename?
        // Should we insert a UUID
        //
        String filename = policy.getPolicyId() + "_" + policy.getVersion() + ".xml";
        //
        // Construct the Path
        //
        return Paths.get(path.toAbsolutePath().toString(), filename);
    }

    /**
     * Load properties from given file.
     *
     * @throws IOException If unable to read file
     */
    public static Properties loadXacmlProperties(Path propertyPath) throws IOException {
        LOGGER.info("Loading xacml properties {}", propertyPath);
        try (InputStream is = Files.newInputStream(propertyPath)) {
            Properties properties = new Properties();
            properties.load(is);
            if (LOGGER.isInfoEnabled()) {
                LOGGER.info("Loaded xacml properties {} {}", System.lineSeparator(), properties);
                for (Entry<Object, Object> entrySet : properties.entrySet()) {
                    LOGGER.info("{} -> {}", entrySet.getKey(), entrySet.getValue());
                }
            }
            return properties;
        }
    }

    /**
     * Stores the XACML Properties to the given file location.
     *
     * @throws IOException If unable to store the file.
     */
    public static void storeXacmlProperties(Properties properties, Path propertyPath) throws IOException {
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info("Storing xacml properties {} {} {}", properties, System.lineSeparator(), propertyPath);
        }
        try (OutputStream os = Files.newOutputStream(propertyPath)) {
            String strComments = "#";
            properties.store(os, strComments);
        }
    }

    /**
     * Appends 'xacml.properties' to a root Path object
     *
     * @param rootPath Root Path object
     * @return Path to rootPath/xacml.properties file
     */
    public static Path getPropertiesPath(Path rootPath) {
        return Paths.get(rootPath.toAbsolutePath().toString(), XACML_PROPERTY_FILE);
    }

    @FunctionalInterface
    public interface FileCreator {
        public File createAFile(String filename) throws IOException;

    }

    /**
     * Copies a xacml.properties file to another location and all the policies defined within it.
     *
     * @param propertiesPath Path to an existing properties file
     * @param properties Properties object
     * @param creator A callback that can create files. Allows JUnit test to pass Temporary folder
     * @return File object that points to new Properties file
     * @throws IOException Could not read/write files
     */
    public static File copyXacmlPropertiesContents(String propertiesPath, Properties properties,
            FileCreator creator) throws IOException {
        //
        // Open the properties file
        //
        try (InputStream is = new FileInputStream(propertiesPath)) {
            //
            // Load in the properties
            //
            properties.load(is);
            //
            // Now we create a new xacml.properties in the temporary folder location
            //
            File propertiesFile = creator.createAFile(XACML_PROPERTY_FILE);
            //
            // Iterate through any root policies defined
            //
            for (String root : XACMLProperties.getRootPolicyIDs(properties)) {
                //
                // Get a file
                //
                Path rootPath = Paths.get(properties.getProperty(root + DOT_FILE_SUFFIX));
                LOGGER.info("Root file {} {}", rootPath, rootPath.getFileName());
                //
                // Construct new path for the root policy
                //
                File newRootPath = creator.createAFile(rootPath.getFileName().toString());
                //
                // Copy the policy file to the temporary folder
                //
                com.google.common.io.Files.copy(rootPath.toFile(), newRootPath);
                //
                // Change the properties object to point to where the new policy is
                // in the temporary folder
                //
                properties.setProperty(root + DOT_FILE_SUFFIX, newRootPath.getAbsolutePath());
            }
            //
            // Iterate through any referenced policies defined
            //
            for (String referenced : XACMLProperties.getReferencedPolicyIDs(properties)) {
                //
                // Get a file
                //
                Path refPath = Paths.get(properties.getProperty(referenced + DOT_FILE_SUFFIX));
                LOGGER.info("Referenced file {} {}", refPath, refPath.getFileName());
                //
                // Construct new path for the root policy
                //
                File newReferencedPath = creator.createAFile(refPath.getFileName().toString());
                //
                // Copy the policy file to the temporary folder
                //
                com.google.common.io.Files.copy(refPath.toFile(), newReferencedPath);
                //
                // Change the properties object to point to where the new policy is
                // in the temporary folder
                //
                properties.setProperty(referenced + DOT_FILE_SUFFIX, newReferencedPath.getAbsolutePath());
            }
            //
            // Save the new properties file to the temporary folder
            //
            try (OutputStream os = new FileOutputStream(propertiesFile.getAbsolutePath())) {
                properties.store(os, "");
            }
            //
            // Return the new path to the properties folder
            //
            return propertiesFile;
        }
    }
}
