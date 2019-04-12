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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.stream.Collectors;
import org.onap.policy.models.decisions.concepts.DecisionRequest;
import org.onap.policy.models.tosca.authorative.concepts.ToscaPolicy;
import org.onap.policy.models.tosca.authorative.concepts.ToscaPolicyIdentifier;
import org.onap.policy.models.tosca.authorative.concepts.ToscaPolicyTypeIdentifier;
import org.onap.policy.pdp.xacml.application.common.XacmlApplicationException;
import org.onap.policy.pdp.xacml.application.common.XacmlApplicationServiceProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class XacmlPdpApplicationManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(XacmlPdpApplicationManager.class);

    private static boolean needsInit = true;
    private static ServiceLoader<XacmlApplicationServiceProvider> applicationLoader;
    private static Map<String, XacmlApplicationServiceProvider> providerActionMap = new HashMap<>();
    private static List<ToscaPolicyTypeIdentifier> toscaPolicyTypeIdents = new ArrayList<>();
    private static Map<ToscaPolicy, XacmlApplicationServiceProvider> mapLoadedPolicies = new HashMap<>();


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
        // Iterate through the applications for actions and supported policy types
        //
        for (XacmlApplicationServiceProvider application : applicationLoader) {
            LOGGER.info("Application {} supports {}", application.applicationName(),
                    application.supportedPolicyTypes());
            //
            // We are not going to make this available unless the application can
            // install correctly.
            //
            boolean applicationInitialized = false;
            //
            // Have it initialize at a path
            //
            try {
                initializeApplicationPath(applicationPath, application);
                //
                // We are initialized
                //
                applicationInitialized = true;
            } catch (XacmlApplicationException e) {
                LOGGER.error("Failed to initialize path for {}", application.applicationName(), e);
            }
            if (applicationInitialized) {
                //
                // Iterate through the actions and save in the providerActionMap
                //
                for (String action : application.actionDecisionsSupported()) {
                    //
                    // Save the actions that it supports
                    //
                    providerActionMap.put(action, application);
                }
                //
                // Add all the supported policy types
                //
                toscaPolicyTypeIdents.addAll(application.supportedPolicyTypes());
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

    /**
     * getToscaPolicies.
     *
     * @return the map containing ToscaPolicies
     */
    public static Map<ToscaPolicy, XacmlApplicationServiceProvider> getToscaPolicies() {
        return mapLoadedPolicies;
    }

    /**
     * getToscaPolicyIdentifiers.
     *
     * @return list of ToscaPolicyIdentifier
     */
    public static List<ToscaPolicyIdentifier> getToscaPolicyIdentifiers() {
        //
        // converting map to return List of ToscaPolicyIdentiers
        //
        return mapLoadedPolicies.keySet().stream().map(ToscaPolicy::getIdentifier).collect(Collectors.toList());
    }

    public static List<ToscaPolicyTypeIdentifier> getToscaPolicyTypeIdents() {
        return toscaPolicyTypeIdents;
    }

    /**
     * Finds the appropriate application and removes the policy.
     *
     * @param policy Incoming policy
     */
    public static void removeUndeployedPolicy(ToscaPolicy policy) {

        for (XacmlApplicationServiceProvider application : applicationLoader) {
            try {
                if (application.unloadPolicy(policy)) {
                    LOGGER.info("Unloaded ToscaPolicy {} from application {}", policy.getMetadata(),
                            application.applicationName());
                    if (mapLoadedPolicies.remove(policy) == null) {
                        LOGGER.error("Failed to remove unloaded policy {} from map size {}", policy.getMetadata(),
                                mapLoadedPolicies.size());
                    }
                }
            } catch (XacmlApplicationException e) {
                LOGGER.error("Failed to undeploy the Tosca Policy", e);
            }
        }
    }

    /**
     * Finds the appropriate application and loads the policy.
     *
     * @param policy Incoming policy
     */
    public static void loadDeployedPolicy(ToscaPolicy policy) {

        for (XacmlApplicationServiceProvider application : applicationLoader) {
            try {
                //
                // There should be only one application per policytype. We can
                // put more logic surrounding enforcement of that later. For now,
                // just use the first one found.
                //
                if (application.canSupportPolicyType(policy.getTypeIdentifier())) {
                    if (application.loadPolicy(policy)) {
                        LOGGER.info("Loaded ToscaPolicy {} into application {}", policy.getMetadata(),
                                application.applicationName());
                        mapLoadedPolicies.put(policy, application);
                    }
                    return;
                }
            } catch (XacmlApplicationException e) {
                LOGGER.error("Failed to load the Tosca Policy", e);
            }
        }
    }

    /**
     * Returns the current count of policy types supported. This could be misleading a bit
     * as some applications can support wildcard of policy types. Eg. onap.Monitoring.* as
     * well as individual types/versions. Nevertheless useful for debugging and testing.
     *
     * @return Total count added from all applications
     */
    public static long getPolicyTypeCount() {
        long types = 0;
        for (XacmlApplicationServiceProvider application : applicationLoader) {
            types += application.supportedPolicyTypes().size();
        }
        return types;
    }

    private static void initializeApplicationPath(Path basePath, XacmlApplicationServiceProvider application)
            throws XacmlApplicationException {
        //
        // Making an assumption that all application names are unique, and
        // they can result in a valid directory being created.
        //
        Path path = Paths.get(basePath.toAbsolutePath().toString(), application.applicationName());
        LOGGER.info("initializeApplicationPath {} at this path {}", application.applicationName(), path);
        //
        // Create that the directory if it does not exist. Ideally
        // this is only for testing, but could be used for production
        // Probably better to have the docker container and/or helm
        // scripts setup the local directory.
        //
        if (! path.toFile().exists()) {
            try {
                //
                // Try to create the directory
                //
                Files.createDirectory(path);
            } catch (IOException e) {
                LOGGER.error("Failed to create application directory {}", path.toAbsolutePath().toString(), e);
            }
        }
        //
        // Have the application initialize
        //
        application.initialize(path);
    }

}
