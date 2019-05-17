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

package org.onap.policy.xacml.pdp.application.monitoring;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.onap.policy.models.tosca.authorative.concepts.ToscaPolicyTypeIdentifier;
import org.onap.policy.pdp.xacml.application.common.ToscaPolicyTranslator;
import org.onap.policy.pdp.xacml.application.common.std.StdCombinedPolicyResultsTranslator;
import org.onap.policy.pdp.xacml.application.common.std.StdXacmlApplicationServiceProvider;

/**
 * This is the engine class that manages the instance of the XACML PDP engine.
 *
 * <p>It is responsible for initializing it and shutting it down properly in a thread-safe manner.
 *
 *
 * @author pameladragosh
 *
 */
public class MonitoringPdpApplication extends StdXacmlApplicationServiceProvider {

    private static final String ONAP_MONITORING_BASE_POLICY_TYPE = "onap.Monitoring";
    private static final String ONAP_MONITORING_CDAP = "onap.policies.monitoring.cdap.tca.hi.lo.app";
    private static final String ONAP_MONITORING_APPSERVER =
            "onap.policies.monitoring.dcaegen2.collectors.datafile.datafile-app-server";
    private static final String ONAP_MONITORING_SONHANDLER = "onap.policies.monitoring.docker.sonhandler.app";
    private static final String ONAP_MONITORING_DERIVED_POLICY_TYPE = "onap.policies.monitoring";
    private static final String VERSION_100 = "1.0.0";

    private StdCombinedPolicyResultsTranslator translator = new StdCombinedPolicyResultsTranslator();
    private List<ToscaPolicyTypeIdentifier> supportedPolicyTypes = new ArrayList<>();

    /**
     * Constructor.
     */
    public MonitoringPdpApplication() {
        //
        // By default this supports just Monitoring policy types
        //
        supportedPolicyTypes.add(new ToscaPolicyTypeIdentifier(ONAP_MONITORING_BASE_POLICY_TYPE, VERSION_100));
        supportedPolicyTypes.add(new ToscaPolicyTypeIdentifier(ONAP_MONITORING_CDAP, VERSION_100));
        supportedPolicyTypes.add(new ToscaPolicyTypeIdentifier(ONAP_MONITORING_APPSERVER, VERSION_100));
        supportedPolicyTypes.add(new ToscaPolicyTypeIdentifier(ONAP_MONITORING_SONHANDLER, VERSION_100));
    }

    @Override
    public String applicationName() {
        return "monitoring";
    }

    @Override
    public List<String> actionDecisionsSupported() {
        return Arrays.asList("configure");
    }

    @Override
    public synchronized List<ToscaPolicyTypeIdentifier> supportedPolicyTypes() {
        return supportedPolicyTypes;
    }

    @Override
    public boolean canSupportPolicyType(ToscaPolicyTypeIdentifier policyTypeId) {
        //
        // For Monitoring, we will attempt to support all versions
        // of the policy type. Since we are only packaging a decision
        // back with a JSON payload of the property contents.
        //
        return (policyTypeId.getName().equals(ONAP_MONITORING_BASE_POLICY_TYPE)
                || policyTypeId.getName().equals(ONAP_MONITORING_CDAP)
                || policyTypeId.getName().equals(ONAP_MONITORING_APPSERVER)
                || policyTypeId.getName().equals(ONAP_MONITORING_SONHANDLER)
                || policyTypeId.getName().startsWith(ONAP_MONITORING_DERIVED_POLICY_TYPE));
    }

    @Override
    protected ToscaPolicyTranslator getTranslator(String type) {
        return translator;
    }

}
