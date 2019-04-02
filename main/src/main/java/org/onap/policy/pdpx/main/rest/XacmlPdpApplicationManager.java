/*-
 * ============LICENSE_START=======================================================
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

package org.onap.policy.pdpx.main.rest;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;

import org.onap.policy.models.decisions.concepts.DecisionRequest;
import org.onap.policy.models.pdp.concepts.ToscaPolicyTypeIdentifier;
import org.onap.policy.pdp.xacml.application.common.XacmlApplicationServiceProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class XacmlPdpApplicationManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(XacmlPdpApplicationManager.class);

    private static boolean needsInit = true;
    private static ServiceLoader<XacmlApplicationServiceProvider> applicationLoader;
    private static Map<String, XacmlApplicationServiceProvider> providerActionMap = new HashMap<>();
    private static List<ToscaPolicyTypeIdentifier> toscaPolicyTypeIdents = new ArrayList<>();

    private XacmlPdpApplicationManager() {
        super();
    }

    /**
     * One time to initialize the applications upon startup.
     */
    public static void initializeApplications(Path applicationPath) {
        //
        // If we have already done this
        //
        if (! needsInit) {
            LOGGER.error("Already initialized the applications");
            return;
        }
        //
        // Load service
        //
        applicationLoader = ServiceLoader.load(XacmlApplicationServiceProvider.class);
        //
        // Iterate through them
        //
        Iterator<XacmlApplicationServiceProvider> iterator = applicationLoader.iterator();
        while (iterator.hasNext()) {
            //
            // Get the application
            //
            XacmlApplicationServiceProvider application = iterator.next();
            LOGGER.info("Application {} supports {}", application.applicationName(),
                    application.supportedPolicyTypes());
            //
            // Iterate each application
            //
            int pathCount = 1;
            for (String action : application.actionDecisionsSupported()) {
                //
                // Save the actions that it supports
                //
                providerActionMap.put(action, application);
                //
                // Create a unique path for the application to store its data
                // May need to scan this name to remove unsafe characters etc.
                // But for debugging purposes, its good to use the application name
                //
                //
                Path path = Paths.get(applicationPath.toAbsolutePath().toString(),
                        application.applicationName(), Integer.toString(pathCount++));
                //
                // Have the application initialize
                //
                application.initialize(path);
            }

            // Get string list of supportedPolicyTypes
            List<String> supportedPolicyTypes = application.supportedPolicyTypes();

            // Iterate through the supportedPolicyTypes to set the toscaPolicyTypeIdents
            for (String name : supportedPolicyTypes) {
                ToscaPolicyTypeIdentifier ident = new ToscaPolicyTypeIdentifier(name, "1.0.0");
                toscaPolicyTypeIdents.add(ident);
            }
        }
        //
        // we have initialized
        //
        needsInit = false;
    }

    public static XacmlApplicationServiceProvider findApplication(DecisionRequest request) {
        return providerActionMap.get(request.getAction());
    }

    public static List<ToscaPolicyTypeIdentifier> getToscaPolicyTypeIdents() {
        return toscaPolicyTypeIdents;
    }

    /**
     * Returns the current count of policy types supported. This could be misleading a bit
     * as some applications can support wildcard of policy types. Eg. onap.Monitoring.* as
     * well as individual types/versions. Nevertheless useful for debugging and testing.
     *
     * @return Total count added from all applications
     */
    public static long getPolicyTypeCount() {
        Iterator<XacmlApplicationServiceProvider> iterator = applicationLoader.iterator();
        long types = 0;
        while (iterator.hasNext()) {
            XacmlApplicationServiceProvider application = iterator.next();
            types += application.supportedPolicyTypes().size();
        }
        return types;
    }

}
