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

import java.nio.file.Path;
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

public class XacmlPolicyUtils {

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
                properties.put(refId + ".file", refPolicyPath.toAbsolutePath().toString());
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
            String refPolicyFile = refPolicy + ".file";
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
                logger.debug("{}", properties.getProperty(root + ".file", "NOT FOUND"));
            }
            //
            // Get the current set of referenced policy ids
            //
            Set<String> referencedPolicies = XACMLProperties.getReferencedPolicyIDs(properties);
            logger.debug("Referenced Policies: {}", properties.getProperty(XACMLProperties.PROP_REFERENCEDPOLICIES));
            for (String ref : referencedPolicies) {
                logger.debug("{}", properties.getProperty(ref + ".file", "NOT FOUND"));
            }
        }
    }
}
