/*-
 * ============LICENSE_START=======================================================
 * Copyright (C) 2020 AT&T Intellectual Property. All rights reserved.
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
 * ============LICENSE_END=========================================================
 */

package org.onap.policy.tutorial.policyenforcement;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Scanner;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import org.onap.policy.common.endpoints.event.comm.Topic.CommInfrastructure;
import org.onap.policy.common.endpoints.event.comm.TopicEndpointManager;
import org.onap.policy.common.endpoints.event.comm.TopicListener;
import org.onap.policy.common.endpoints.event.comm.bus.internal.BusTopicParams;
import org.onap.policy.common.endpoints.http.client.HttpClient;
import org.onap.policy.common.endpoints.http.client.HttpClientConfigException;
import org.onap.policy.common.endpoints.http.client.HttpClientFactoryInstance;
import org.onap.policy.common.endpoints.parameters.TopicParameterGroup;
import org.onap.policy.common.endpoints.parameters.TopicParameters;
import org.onap.policy.common.utils.coder.CoderException;
import org.onap.policy.common.utils.coder.StandardCoder;
import org.onap.policy.models.decisions.concepts.DecisionRequest;
import org.onap.policy.models.decisions.concepts.DecisionResponse;
import org.onap.policy.models.pap.concepts.PolicyNotification;
import org.onap.policy.models.pap.concepts.PolicyStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class App extends Thread implements TopicListener {
    private static Logger logger                           = LoggerFactory.getLogger(App.class);
    private static final String MY_POLICYTYPEID = "onap.policies.monitoring.MyAnalytic";
    private String xacmlPdpHost;
    private String xacmlPdpPort;
    private DecisionRequest decisionRequest = new DecisionRequest();
    private Integer requestId = 1;
    private HttpClient client = null;

    /**
     * Constructor.
     *
     * @param args Command line arguments
     */
    public App(String[] args) {
        xacmlPdpHost = args[0];
        xacmlPdpPort = args[1];

        TopicParameters params = new TopicParameters();
        params.setTopicCommInfrastructure("dmaap");
        params.setFetchLimit(1);
        params.setFetchTimeout(5000);
        params.setTopic("POLICY-NOTIFICATION");
        params.setServers(Arrays.asList(args[2] + ":" + args[3]));
        TopicParameterGroup topicParams = new TopicParameterGroup();
        topicParams.setTopicSources(Arrays.asList(params));

        TopicEndpointManager.getManager().addTopics(topicParams);
        TopicEndpointManager.getManager().getDmaapTopicSource("POLICY-NOTIFICATION").register(this);

        decisionRequest.setOnapComponent("myComponent");
        decisionRequest.setOnapName("myName");
        decisionRequest.setOnapInstance("myInstanceId");
        decisionRequest.setAction("configure");
        Map<String, Object> resources = new HashMap<>();
        resources.put("policy-type", MY_POLICYTYPEID);
        decisionRequest.setResource(resources);
    }

    /**
     * Thread run method that creates a connection and gets an initial Decision on which policy(s)
     * we should be enforcing.
     * Then sits waiting for the user to enter q or Q from the keyboard to quit. While waiting,
     * listen on Dmaap topic for notification that the policy has changed.
     */
    @Override
    public void run() {
        logger.info("running - type q to stdin to quit");
        try {
            client = HttpClientFactoryInstance.getClientFactory().build(BusTopicParams.builder()
                    .clientName("myClientName").useHttps(true).allowSelfSignedCerts(true)
                    .hostname(xacmlPdpHost).port(Integer.parseInt(xacmlPdpPort))
                    .userName("healthcheck").password("zb!XztG34").basePath("policy/pdpx/v1")
                    .managed(true)
                    .serializationProvider("org.onap.policy.common.gson.GsonMessageBodyHandler")
                    .build());
        } catch (NumberFormatException | HttpClientConfigException e) {
            logger.error("Could not create Http client", e);
            return;
        }

        Map<String, Object> policies = getDecision(client, this.decisionRequest);
        if (policies.isEmpty()) {
            logger.info("Not enforcing any policies to start");
        }
        for (Entry<String, Object> entrySet : policies.entrySet()) {
            logger.info("Enforcing: {}", entrySet.getKey());
        }

        TopicEndpointManager.getManager().start();

        @SuppressWarnings("resource") // never close System.in
        Scanner input = new Scanner(System.in);
        while (!Thread.currentThread().isInterrupted()) {
            String quit = input.nextLine();
            if ("q".equalsIgnoreCase(quit)) {
                logger.info("quiting");
                break;
            }
        }

        TopicEndpointManager.getManager().shutdown();

    }

    /**
     * This method is called when a topic event is received.
     */
    @Override
    public void onTopicEvent(CommInfrastructure infra, String topic, String event) {
        logger.info("onTopicEvent {}", event);
        if (scanForPolicyType(event)) {
            Map<String, Object> newPolicies = getDecision(client, this.decisionRequest);
            if (newPolicies.isEmpty()) {
                logger.info("Not enforcing any policies");
            }
            for (Entry<String, Object> entrySet : newPolicies.entrySet()) {
                logger.info("Now Enforcing: {}", entrySet.getKey());
            }
        }
    }

    /**
     * Helper method that parses a DMaap message event for POLICY-NOTIFICATION
     * looking for our supported policy type to enforce.
     *
     * @param msg Dmaap Message
     * @return true if MY_POLICYTYPEID is in the message
     */
    private boolean scanForPolicyType(String msg) {
        StandardCoder gson = new StandardCoder();
        try {
            PolicyNotification notification = gson.decode(msg, PolicyNotification.class);
            for (PolicyStatus added : notification.getAdded()) {
                if (MY_POLICYTYPEID.equals(added.getPolicyTypeId())) {
                    return true;
                }
            }
            for (PolicyStatus deleted : notification.getDeleted()) {
                if (MY_POLICYTYPEID.equals(deleted.getPolicyTypeId())) {
                    return true;
                }
            }
        } catch (CoderException e) {
            logger.error("StandardCoder failed to parse PolicyNotification", e);
        }
        return false;
    }


    /**
     * Helper method that calls the XACML PDP Decision API to get a Decision
     * as to which policy we should be enforcing.
     *
     * @param client HttpClient to use to make REST call
     * @param decisionRequest DecisionRequest object to send
     * @return The Map of policies that was in the DecisionResponse object
     */
    private Map<String, Object> getDecision(HttpClient client, DecisionRequest decisionRequest) {
        decisionRequest.setRequestId(requestId.toString());
        requestId++;

        Entity<DecisionRequest> entityRequest =
                Entity.entity(decisionRequest, MediaType.APPLICATION_JSON);
        Response response = client.post("/decision", entityRequest, Collections.emptyMap());

        if (response.getStatus() != 200) {
            logger.error(
                    "Decision API failed - is the IP/port correct? {}", response.getStatus());
            return Collections.emptyMap();
        }

        DecisionResponse decisionResponse = HttpClient.getBody(response, DecisionResponse.class);

        return decisionResponse.getPolicies();
    }

    /**
     * Our Main application entry point.
     *
     * @param args command line arguments
     */
    public static void main(String[] args) {
        logger.info("Hello Welcome to ONAP Enforcement Tutorial!");

        App app = new App(args);

        app.start();

        try {
            app.join();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logger.warn("Thread interrupted");
        }

        logger.info("Tutorial ended");
    }

}
