/*-
 * ============LICENSE_START=======================================================
 * ONAP
 * ================================================================================
 * Copyright (C) 2019-2021 AT&T Intellectual Property. All rights reserved.
 * Modifications Copyright (C) 2021 Nordix Foundation.
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
import com.att.research.xacml.api.Response;
import com.att.research.xacml.api.Result;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import org.apache.commons.lang3.tuple.Pair;
import org.onap.policy.common.endpoints.event.comm.bus.internal.BusTopicParams;
import org.onap.policy.models.decisions.concepts.DecisionRequest;
import org.onap.policy.models.decisions.concepts.DecisionResponse;
import org.onap.policy.models.tosca.authorative.concepts.ToscaConceptIdentifier;
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
    private static final String RESOURCE_POLICYTYPE = "policy-type";
    private static final String RESOURCE_SCOPE = "scope";

    public static final String POLICYTYPE_AFFINITY = "onap.policies.optimization.resource.AffinityPolicy";
    public static final String POLICYTYPE_SUBSCRIBER = "onap.policies.optimization.service.SubscriberPolicy";
    public static final String POLICYTYPE_DISTANCE = "onap.policies.optimization.resource.DistancePolicy";
    public static final String POLICYTYPE_HPA = "onap.policies.optimization.resource.HpaPolicy";
    public static final String POLICYTYPE_OPTIMIZATION = "onap.policies.optimization.resource.OptimizationPolicy";
    public static final String POLICYTYPE_PCI = "onap.policies.optimization.resource.PciPolicy";
    public static final String POLICYTYPE_QUERY = "onap.policies.optimization.service.QueryPolicy";
    public static final String POLICYTYPE_VIMFIT = "onap.policies.optimization.resource.Vim_fit";
    public static final String POLICYTYPE_VNF = "onap.policies.optimization.resource.VnfPolicy";
    public static final String ONAP_OPTIMIZATION_DERIVED_POLICY_TYPE = "onap.policies.optimization.";

    private OptimizationPdpApplicationTranslator translator = new OptimizationPdpApplicationTranslator();

    /**
     * Constructor.
     */
    public OptimizationPdpApplication() {
        super();

        applicationName = "optimization";
        actions = Arrays.asList("optimize");

        this.supportedPolicyTypes.add(new ToscaConceptIdentifier(POLICYTYPE_AFFINITY, STRING_VERSION100));
        this.supportedPolicyTypes.add(new ToscaConceptIdentifier(POLICYTYPE_DISTANCE, STRING_VERSION100));
        this.supportedPolicyTypes.add(new ToscaConceptIdentifier(POLICYTYPE_HPA, STRING_VERSION100));
        this.supportedPolicyTypes.add(new ToscaConceptIdentifier(POLICYTYPE_OPTIMIZATION, STRING_VERSION100));
        this.supportedPolicyTypes.add(new ToscaConceptIdentifier(POLICYTYPE_PCI, STRING_VERSION100));
        this.supportedPolicyTypes.add(new ToscaConceptIdentifier(POLICYTYPE_QUERY, STRING_VERSION100));
        this.supportedPolicyTypes.add(new ToscaConceptIdentifier(POLICYTYPE_SUBSCRIBER, STRING_VERSION100));
        this.supportedPolicyTypes.add(new ToscaConceptIdentifier(POLICYTYPE_VIMFIT, STRING_VERSION100));
        this.supportedPolicyTypes.add(new ToscaConceptIdentifier(POLICYTYPE_VNF, STRING_VERSION100));
    }

    @Override
    public void initialize(Path pathForData, BusTopicParams policyApiParameters)
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
    public boolean canSupportPolicyType(ToscaConceptIdentifier policyTypeId) {
        //
        // For the time being, restrict this if the version isn't known.
        // Could be too difficult to support changing of versions dynamically.
        //
        for (ToscaConceptIdentifier supported : this.supportedPolicyTypes) {
            if (policyTypeId.equals(supported)) {
                LOGGER.info("optimization can support {}", supported);
                return true;
            }
        }
        //
        // Support derived types
        //
        return policyTypeId.getName().startsWith(ONAP_OPTIMIZATION_DERIVED_POLICY_TYPE);
    }

    @Override
    public Pair<DecisionResponse, Response> makeDecision(DecisionRequest request,
            Map<String, String[]> requestQueryParams) {
        //
        // In case we have a subcriber policy
        //
        Response xacmlSubscriberResponse = null;
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
            var subscriberRequest = new DecisionRequest(request);
            //
            // Override the PolicyType to ensure we are only looking at Subscriber Policies
            //
            if (subscriberRequest.getResource().containsKey(RESOURCE_POLICYTYPE)) {
                subscriberRequest.getResource().remove(RESOURCE_POLICYTYPE);
            }
            subscriberRequest.getResource().put(RESOURCE_POLICYTYPE, POLICYTYPE_SUBSCRIBER);
            //
            // Convert to a XacmlRequest and get a decision
            //
            try {
                xacmlSubscriberResponse =
                        this.xacmlDecision(OptimizationSubscriberRequest.createInstance(subscriberRequest));
            } catch (XacmlApplicationException e) {
                LOGGER.error("Could not create subscriberName request", e);
            }
            //
            // Check the response for subscriber attributes and add them
            // to the initial request.
            //
            if (xacmlSubscriberResponse != null && ! addSubscriberAttributes(xacmlSubscriberResponse, request)) {
                LOGGER.error("Failed to get subscriber role attributes");
                //
                // Convert to a DecisionResponse
                //
                return Pair.of(this.getTranslator().convertResponse(xacmlSubscriberResponse), xacmlSubscriberResponse);
            }
        }
        //
        // Make the decision
        //
        Pair<DecisionResponse, Response> decisionPair = super.makeDecision(request, requestQueryParams);
        //
        // Add back in advice from subscriber
        //
        if (xacmlSubscriberResponse != null) {
            addSubscriberAdvice(xacmlSubscriberResponse, decisionPair.getLeft());
        }
        return decisionPair;
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
        return request.getContext() != null
                && request.getContext().containsKey(RESOURCE_SUBSCRIBERNAME)
                && request.getContext().get(RESOURCE_SUBSCRIBERNAME) instanceof List
                && ! ((List<String>) request.getContext().get(RESOURCE_SUBSCRIBERNAME)).isEmpty();
    }

    private boolean addSubscriberAttributes(Response xacmlResponse, DecisionRequest initialRequest) {
        //
        // This has multiple results right now because of how the attributes were added to the
        // request. That will have to be fixed in the future, for now find the Permit result
        // and add the role attributes as they will be used in the next request.
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
                // PLD this is an assumption that all is good
                //
                return true;
            } else {
                LOGGER.error("XACML result not ok {} or Permit {}", result.getStatus(), result.getDecision());
            }
        }
        return false;
    }

    private void addSubscriberAdvice(Response xacmlResponse, DecisionResponse response) {
        //
        // Again find the Permit result
        //
        for (Result result : xacmlResponse.getResults()) {
            //
            // Check the result
            //
            if (result.getStatus().isOk() && Decision.PERMIT.equals(result.getDecision())) {
                this.translator.scanAdvice(result.getAssociatedAdvice(), response);
            }
        }
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
            if (! ToscaDictionary.ID_ADVICE_OPTIMIZATION_SUBSCRIBER.equals(advice.getId())) {
                LOGGER.error("Unsupported advice id {}", advice.getId());
                continue;
            }
            //
            // Get the attributes and add them
            //
            for (AttributeAssignment attribute : advice.getAttributeAssignments()) {
                //
                // If this is subscriber role
                //
                if (ToscaDictionary.ID_ADVICE_OPTIMIZATION_SUBSCRIBER_ROLE.equals(attribute.getAttributeId())) {
                    ((List<String>) initialRequest.getResource().get(RESOURCE_SCOPE))
                            .add(attribute.getAttributeValue().getValue().toString());
                }
            }
        }
    }

}
