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

package org.onap.policy.xacml.pdp.application.optimization;

import com.att.research.xacml.api.Advice;
import com.att.research.xacml.api.AttributeAssignment;
import com.att.research.xacml.api.Decision;
import com.att.research.xacml.api.Request;
import com.att.research.xacml.api.Response;
import com.att.research.xacml.api.Result;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.apache.commons.lang3.tuple.Pair;
import org.onap.policy.common.endpoints.parameters.RestServerParameters;
import org.onap.policy.models.decisions.concepts.DecisionRequest;
import org.onap.policy.models.decisions.concepts.DecisionResponse;
import org.onap.policy.models.tosca.authorative.concepts.ToscaPolicyTypeIdentifier;
import org.onap.policy.pdp.xacml.application.common.ToscaDictionary;
import org.onap.policy.pdp.xacml.application.common.ToscaPolicyTranslator;
import org.onap.policy.pdp.xacml.application.common.XacmlApplicationException;
import org.onap.policy.pdp.xacml.application.common.std.StdXacmlApplicationServiceProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OptimizationPdpApplication extends StdXacmlApplicationServiceProvider {

    private static final Logger LOGGER = LoggerFactory.getLogger(OptimizationPdpApplication.class);
    private static final String STRING_VERSION100 = "1.0.0";
    private static final String RESOURCE_SUBSCRIBERNAME = "subscriberName";

    private OptimizationPdpApplicationTranslator translator = new OptimizationPdpApplicationTranslator();
    private List<ToscaPolicyTypeIdentifier> supportedPolicyTypes = new ArrayList<>();

    /**
     * Constructor.
     */
    public OptimizationPdpApplication() {
        this.supportedPolicyTypes.add(new ToscaPolicyTypeIdentifier(
                "onap.policies.optimization.resource.AffinityPolicy", STRING_VERSION100));
        this.supportedPolicyTypes.add(new ToscaPolicyTypeIdentifier(
                "onap.policies.optimization.resource.DistancePolicy", STRING_VERSION100));
        this.supportedPolicyTypes.add(new ToscaPolicyTypeIdentifier(
                "onap.policies.optimization.resource.HpaPolicy", STRING_VERSION100));
        this.supportedPolicyTypes.add(new ToscaPolicyTypeIdentifier(
                "onap.policies.optimization.resource.OptimizationPolicy", STRING_VERSION100));
        this.supportedPolicyTypes.add(new ToscaPolicyTypeIdentifier(
                "onap.policies.optimization.resource.PciPolicy", STRING_VERSION100));
        this.supportedPolicyTypes.add(new ToscaPolicyTypeIdentifier(
                "onap.policies.optimization.service.QueryPolicy", STRING_VERSION100));
        this.supportedPolicyTypes.add(new ToscaPolicyTypeIdentifier(
                "onap.policies.optimization.service.SubscriberPolicy", STRING_VERSION100));
        this.supportedPolicyTypes.add(new ToscaPolicyTypeIdentifier(
                "onap.policies.optimization.resource.Vim_fit", STRING_VERSION100));
        this.supportedPolicyTypes.add(new ToscaPolicyTypeIdentifier(
                "onap.policies.optimization.resource.VnfPolicy", STRING_VERSION100));
    }

    @Override
    public String applicationName() {
        return "optimization";
    }

    @Override
    public List<String> actionDecisionsSupported() {
        return Arrays.asList("optimize");
    }

    @Override
    public void initialize(Path pathForData, RestServerParameters policyApiParameters)
            throws XacmlApplicationException {
        //
        // Store our API parameters and path for translator so it
        // can go get Policy Types
        //
        this.translator.setPathForData(pathForData);
        this.translator.setApiRestParameters(policyApiParameters);
        //
        // Let our super class do its thing
        //
        super.initialize(pathForData, policyApiParameters);
    }

    @Override
    public synchronized List<ToscaPolicyTypeIdentifier> supportedPolicyTypes() {
        return Collections.unmodifiableList(supportedPolicyTypes);
    }

    @Override
    public boolean canSupportPolicyType(ToscaPolicyTypeIdentifier policyTypeId) {
        //
        // For the time being, restrict this if the version isn't known.
        // Could be too difficult to support changing of versions dynamically.
        //
        //
        // For the time being, restrict this if the version isn't known.
        // Could be too difficult to support changing of versions dynamically.
        //
        for (ToscaPolicyTypeIdentifier supported : this.supportedPolicyTypes) {
            if (policyTypeId.equals(supported)) {
                LOGGER.info("optimization can support {}", supported);
                return true;
            }
        }
        return false;
    }

    @Override
    public Pair<DecisionResponse, Response> makeDecision(DecisionRequest request,
            Map<String, String[]> requestQueryParams) {
        //
        // Check if there are subject attributes for subscriber
        //
        if (hasSubscriberAttributes(request)) {
            //
            // We must do an initial request to pull subscriber attributes
            //
            LOGGER.info("Request Subscriber attributes");
            //
            // Convert the request
            //
            DecisionRequest subscriberRequest = new DecisionRequest(request);
            //
            // Override the PolicyType to ensure we are only looking at Subscriber Policies
            //
            if (subscriberRequest.getResource().containsKey("policy-type")) {
                subscriberRequest.getResource().remove("policy-type");
            }
            subscriberRequest.getResource().put("policy-type", "onap.policies.optimization.service.SubscriberPolicy");
            //
            // Convert to a XacmlRequest and get a decision
            //
            Response xacmlResponse = null;
            try {
                xacmlResponse = this.xacmlDecision(OptimizationSubscriberRequest.createInstance(subscriberRequest));
            } catch (XacmlApplicationException e) {
                LOGGER.error("Could not create subscriberName request {}", e);
            }
            //
            // Check the response for subscriber attributes and add them
            // to the initial request.
            //
            if (! addSubscriberAttributes(xacmlResponse, request)) {
                LOGGER.error("Failed to get subscriber attributes");
                //
                // Convert to a DecisionResponse
                //
                return Pair.of(this.getTranslator().convertResponse(xacmlResponse), xacmlResponse);
            }
        }
        //
        // Convert to a XacmlRequest
        //
        Request xacmlRequest = this.getTranslator().convertRequest(request);
        //
        // Now get a decision
        //
        Response xacmlResponse = this.xacmlDecision(xacmlRequest);
        //
        // Convert to a DecisionResponse
        //
        return Pair.of(this.getTranslator().convertResponse(xacmlResponse), xacmlResponse);
    }

    @Override
    protected ToscaPolicyTranslator getTranslator(String type) {
        //
        // Return translator
        //
        return translator;
    }

    @SuppressWarnings("unchecked")
    private boolean hasSubscriberAttributes(DecisionRequest request) {
        return request.getSubject() != null
                && request.getSubject().containsKey(RESOURCE_SUBSCRIBERNAME)
                && request.getSubject().get(RESOURCE_SUBSCRIBERNAME) instanceof List
                && ! ((List<String>) request.getSubject().get(RESOURCE_SUBSCRIBERNAME)).isEmpty();
    }

    private boolean addSubscriberAttributes(Response xacmlResponse, DecisionRequest initialRequest) {
        //
        // Should only be one result
        //
        for (Result result : xacmlResponse.getResults()) {
            //
            // Check the result
            //
            if (result.getStatus().isOk() && result.getDecision().equals(Decision.PERMIT)) {
                //
                // Pull out the advice which has attributes
                //
                scanAdvice(result.getAssociatedAdvice(), initialRequest);
                //
                // PLD this is an assumption
                //
                return true;
            } else {
                LOGGER.error("XACML result not ok {} or Permit {}", result.getStatus(), result.getDecision());
            }
        }
        return false;
    }

    @SuppressWarnings("unchecked")
    private void scanAdvice(Collection<Advice> adviceCollection, DecisionRequest initialRequest) {
        //
        // There really should only be one advice object
        //
        for (Advice advice : adviceCollection) {
            //
            // Look for the optimization specific advice
            //
            if (ToscaDictionary.ID_ADVICE_OPTIMIZATION_SUBSCRIBER.equals(advice.getId())) {
                //
                // Get the attributes and add them
                for (AttributeAssignment attribute : advice.getAttributeAssignments()) {
                    //
                    // If this is subscriber role
                    //
                    if (ToscaDictionary.ID_ADVICE_OPTIMIZATION_SUBSCRIBER_ROLE.equals(attribute.getAttributeId())) {
                        ((List<String>) initialRequest.getResource().get("scope")).add(attribute.getAttributeValue()
                                .getValue().toString());
                    }
                }
            } else {
                LOGGER.error("Unsupported advice id {}", advice.getId());
            }
        }
    }

}
