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

import com.att.research.xacml.util.XACMLProperties;

import java.nio.file.Path;
import java.util.Properties;
import java.util.Set;
import java.util.stream.Collectors;

import oasis.names.tc.xacml._3_0.core.schema.wd_17.IdReferenceType;
import oasis.names.tc.xacml._3_0.core.schema.wd_17.ObjectFactory;
import oasis.names.tc.xacml._3_0.core.schema.wd_17.PolicySetType;
import oasis.names.tc.xacml._3_0.core.schema.wd_17.PolicyType;

public class XacmlUpdatePolicyUtils {

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

}
