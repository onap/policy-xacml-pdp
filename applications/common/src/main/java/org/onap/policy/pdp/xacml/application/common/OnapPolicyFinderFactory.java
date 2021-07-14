/*-
 * ============LICENSE_START=======================================================
 * Copyright (C) 2019-2021 AT&T Intellectual Property. All rights reserved.
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

import com.att.research.xacml.std.IdentifierImpl;
import com.att.research.xacml.std.StdStatusCode;
import com.att.research.xacml.std.StdVersion;
import com.att.research.xacml.std.dom.DOMStructureException;
import com.att.research.xacml.util.FactoryException;
import com.att.research.xacml.util.XACMLProperties;
import com.att.research.xacmlatt.pdp.policy.CombiningAlgorithm;
import com.att.research.xacmlatt.pdp.policy.CombiningAlgorithmFactory;
import com.att.research.xacmlatt.pdp.policy.Policy;
import com.att.research.xacmlatt.pdp.policy.PolicyDef;
import com.att.research.xacmlatt.pdp.policy.PolicyFinder;
import com.att.research.xacmlatt.pdp.policy.PolicyFinderFactory;
import com.att.research.xacmlatt.pdp.policy.PolicySet;
import com.att.research.xacmlatt.pdp.policy.PolicySetChild;
import com.att.research.xacmlatt.pdp.policy.Target;
import com.att.research.xacmlatt.pdp.policy.dom.DOMPolicyDef;
import com.att.research.xacmlatt.pdp.std.StdPolicyFinder;
import com.att.research.xacmlatt.pdp.util.ATTPDPProperties;
import com.google.common.base.Splitter;
import com.google.common.base.Strings;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Properties;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implements ONAP specific ability to find Policies for XACML PDP engine.
 *
 * @author pameladragosh
 *
 */
public class OnapPolicyFinderFactory extends PolicyFinderFactory {

    public static final String  PROP_FILE       = ".file";
    public static final String  PROP_URL        = ".url";

    private static Logger logger                           = LoggerFactory.getLogger(OnapPolicyFinderFactory.class);
    private List<PolicyDef> rootPolicies;
    private List<PolicyDef> referencedPolicies;
    private boolean needsInit                   = true;

    private Properties properties;

    /**
     * Constructor with properties passed. This will be preferred.
     *
     * @param properties Properties object
     */
    public OnapPolicyFinderFactory(Properties properties) {
        super(properties);
        logger.info("Constructed using properties {}", properties);
        //
        // Save our properties
        //
        this.properties = properties;
        //
        // Here we differ from the StdPolicyFinderFactory in that we initialize right away.
        // We do not wait for a policy request to happen to look for and load policies.
        //
        this.init();
    }

    /**
     * Loads the <code>PolicyDef</code> for the given <code>String</code> identifier by looking first
     * for a ".file" property associated with the ID and using that to load from a <code>File</code> and
     * looking for a ".url" property associated with the ID and using that to load from a <code>URL</code>.
     *
     * @param policyId the <code>String</code> identifier for the policy
     * @return a <code>PolicyDef</code> loaded from the given identifier
     */
    protected PolicyDef loadPolicyDef(String policyId) {
        String propLocation = this.properties.getProperty(policyId + PROP_FILE);
        if (propLocation != null) {
            //
            // Try to load it from the file
            //
            PolicyDef policy = this.loadPolicyFileDef(propLocation);
            if (policy != null) {
                return policy;
            }
        }

        logger.error("No known location for Policy {}", policyId);
        return null;
    }

    protected PolicyDef loadPolicyFileDef(String propLocation) {
        var fileLocation   = new File(propLocation);
        if (!fileLocation.exists()) {
            logger.error("Policy file {} does not exist.", fileLocation.getAbsolutePath());
            return null;
        }
        if (!fileLocation.canRead()) {
            logger.error("Policy file {} cannot be read.", fileLocation.getAbsolutePath());
            return null;
        }
        try {
            logger.info("Loading policy file {}", fileLocation);
            var policyDef = DOMPolicyDef.load(fileLocation);
            if (policyDef != null) {
                return policyDef;
            }
            return new Policy(StdStatusCode.STATUS_CODE_SYNTAX_ERROR, "DOM Could not load policy");
        } catch (DOMStructureException ex) {
            logger.error("Error loading policy file {}", fileLocation.getAbsolutePath(), ex);
            return new Policy(StdStatusCode.STATUS_CODE_SYNTAX_ERROR, ex.getMessage());
        }
    }

    /**
     * Finds the identifiers for all of the policies referenced by the given property name in the
     * <code>XACMLProperties</code> and loads them using the requested loading method.
     *
     * @param propertyName the <code>String</code> name of the property containing the list of policy identifiers
     * @return a <code>List</code> of <code>PolicyDef</code>s loaded from the given property name
     */
    protected List<PolicyDef> getPolicyDefs(String propertyName) {
        String policyIds = this.properties.getProperty(propertyName);
        if (Strings.isNullOrEmpty(policyIds)) {
            return Collections.emptyList();
        }

        Iterable<String> policyIdArray  = Splitter.on(',').trimResults().omitEmptyStrings().split(policyIds);
        if (policyIdArray == null) {
            return Collections.emptyList();
        }

        List<PolicyDef> listPolicyDefs  = new ArrayList<>();
        for (String policyId : policyIdArray) {
            var policyDef = this.loadPolicyDef(policyId);
            if (policyDef != null) {
                listPolicyDefs.add(policyDef);
            }
        }
        return listPolicyDefs;
    }

    protected synchronized void init() {
        if (! this.needsInit) {
            logger.info("Does not need initialization");
            return;
        }
        logger.info("Initializing OnapPolicyFinderFactory Properties ");

        //
        // Check for property that combines root policies into one policyset
        //
        String combiningAlgorithm = properties.getProperty(
                ATTPDPProperties.PROP_POLICYFINDERFACTORY_COMBINEROOTPOLICIES);
        if (combiningAlgorithm != null) {
            try {
                logger.info("Combining root policies with {}", combiningAlgorithm);
                //
                // Find the combining algorithm
                //
                CombiningAlgorithm<PolicySetChild> algorithm = CombiningAlgorithmFactory.newInstance()
                        .getPolicyCombiningAlgorithm(new IdentifierImpl(combiningAlgorithm));
                //
                // Create our root policy
                //
                var root = new PolicySet();
                root.setIdentifier(new IdentifierImpl(UUID.randomUUID().toString()));
                root.setVersion(StdVersion.newInstance("1.0"));
                root.setTarget(new Target());
                //
                // Set the algorithm
                //
                root.setPolicyCombiningAlgorithm(algorithm);
                //
                // Load all our root policies
                //
                for (PolicyDef policy : this.getPolicyDefs(XACMLProperties.PROP_ROOTPOLICIES)) {
                    root.addChild(policy);
                }
                //
                // Set this policy as the root
                //
                this.rootPolicies = new ArrayList<>();
                this.rootPolicies.add(root);
            } catch (Exception e) {
                logger.error("Failed to load Combining Algorithm Factory", e);
            }
        } else {
            logger.info("Loading root policies");
            this.rootPolicies       = this.getPolicyDefs(XACMLProperties.PROP_ROOTPOLICIES);
        }
        this.referencedPolicies = this.getPolicyDefs(XACMLProperties.PROP_REFERENCEDPOLICIES);
        logger.info("Root Policies: {}", this.rootPolicies.size());
        logger.info("Referenced Policies: {}", this.referencedPolicies.size());
        //
        // Make sure we set that we don't need initialization
        //
        this.needsInit  = false;
    }

    @Override
    public PolicyFinder getPolicyFinder() throws FactoryException {
        //
        // Force using any properties that were passed upon construction
        //
        return new StdPolicyFinder(this.rootPolicies, this.referencedPolicies, this.properties);
    }

    @Override
    public PolicyFinder getPolicyFinder(Properties properties) throws FactoryException {
        return new StdPolicyFinder(this.rootPolicies, this.referencedPolicies, properties);
    }

}
