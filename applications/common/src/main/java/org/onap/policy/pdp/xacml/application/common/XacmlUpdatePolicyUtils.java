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

import java.nio.file.Path;
import java.util.Properties;
import java.util.StringJoiner;
import oasis.names.tc.xacml._3_0.core.schema.wd_17.IdReferenceType;
import oasis.names.tc.xacml._3_0.core.schema.wd_17.ObjectFactory;
import oasis.names.tc.xacml._3_0.core.schema.wd_17.PolicySetType;
import oasis.names.tc.xacml._3_0.core.schema.wd_17.PolicyType;

public class XacmlUpdatePolicyUtils {

    private static final String FILE_APPEND = ".file";

    private XacmlUpdatePolicyUtils() {
        super();
    }

    /**
     * This method updates a root PolicySetType by adding in a PolicyType as a reference.
     *
     * @param rootPolicy Root PolicySet being updated
     * @param referencedPolicies A list of PolicyType being added as a references
     * @return the rootPolicy PolicySet object
     */
    public static PolicySetType updateXacmlRootPolicy(PolicySetType rootPolicy, PolicyType... referencedPolicies) {
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
            rootPolicy.getPolicySetOrPolicyOrPolicySetIdReference().add(factory.createPolicySetIdReference(reference));
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
    public static PolicySetType updateXacmlRootPolicy(PolicySetType rootPolicy, PolicySetType... referencedPolicySets) {
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
     * Set the properties to ensure it points to correct root policy file. This will overwrite
     * any previous property set.
     *
     * @param properties Properties object that will get updated with root policy details
     * @param rootPolicyPath Path to root Policy
     * @return properties Properties object that was passed in
     */
    public static Properties setXacmlRootProperties(Properties properties, Path rootPolicyPath) {
        //
        // Clear out the old entries
        //
        clearPolicyProperties(properties, "xacml.rootPolicies");
        //
        // Rebuild the list
        //
        properties.setProperty("xacml.rootPolicies", "root");
        properties.setProperty("root.file", rootPolicyPath.toAbsolutePath().toString());
        return properties;
    }

    /**
     * Set the properties to ensure it points to correct referenced policy files. This will overwrite
     * any previous properties set for referenced policies.
     *
     * @param properties Properties object that will get updated with referenced policy details
     * @param referencedPolicies list of Paths to referenced Policies
     * @return Properties object passed in
     */
    public static Properties setXacmlReferencedProperties(Properties properties, Path... referencedPolicies) {
        //
        // Clear out the old entries
        //
        clearPolicyProperties(properties, "xacml.referencedPolicies");
        //
        // Rebuild the list
        //
        int id = 1;
        StringJoiner joiner = new StringJoiner(",");
        for (Path policy : referencedPolicies) {
            String ref = "ref" + id++;
            joiner.add(ref);
            properties.setProperty(ref + FILE_APPEND, policy.toAbsolutePath().toString());
        }
        properties.setProperty("xacml.referencedPolicies", joiner.toString());
        return properties;
    }

    /**
     * Clear policy references from a Properties object.
     *
     * @param properties Properties object to clear
     * @param strField Key field
     * @return Properties object passed in
     */
    public static Properties clearPolicyProperties(Properties properties, String strField) {
        String policyValue = properties.getProperty(strField);

        String[] policies = policyValue.split("\\s*,\\s*");

        for (String policy : policies) {
            if (properties.containsKey(policy + FILE_APPEND)) {
                properties.remove(policy + FILE_APPEND);
            }
        }

        return properties;
    }

}
