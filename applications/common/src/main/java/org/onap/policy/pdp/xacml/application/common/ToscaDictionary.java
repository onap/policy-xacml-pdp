/*-
 * ============LICENSE_START=======================================================
 * ONAP
 * ================================================================================
 * Copyright (C) 2019-2020 AT&T Intellectual Property. All rights reserved.
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
import com.att.research.xacml.api.XACML3;
import com.att.research.xacml.std.IdentifierImpl;

public final class ToscaDictionary {

    /*
     * These are the ID's for various TOSCA Policy Types we are supporting in the Applications.
     */
    public static final String URN_ONAP = "urn:org:onap";
    public static final Identifier ID_URN_ONAP =
            new IdentifierImpl(URN_ONAP);

    public static final Identifier ID_RESOURCE_POLICY_ID =
            XACML3.ID_RESOURCE_RESOURCE_ID;

    public static final Identifier ID_RESOURCE_POLICY_TYPE =
            new IdentifierImpl(ID_URN_ONAP, "policy-type");

    public static final Identifier ID_RESOURCE_POLICY_TYPE_VERSION =
            new IdentifierImpl(ID_URN_ONAP, "policy-type-version");

    /*
     * These ID's are for identifying Subjects
     */

    public static final Identifier ID_SUBJECT_ONAP_NAME =
            XACML3.ID_SUBJECT_SUBJECT_ID;

    public static final Identifier ID_SUBJECT_ONAP_COMPONENT =
            new IdentifierImpl(ID_URN_ONAP, "onap-component");

    public static final Identifier ID_SUBJECT_ONAP_INSTANCE =
            new IdentifierImpl(ID_URN_ONAP, "onap-instance");

    /*
     * These ID's are for Matchable Attributes
     */

    public static final String ID_RESOURCE_MATCHABLE = URN_ONAP + ":matchable:";

    /*
     * These ID's are for Legacy Guard Policies
     */
    public static final Identifier ID_RESOURCE_GUARD_ACTOR =
            new IdentifierImpl(ID_URN_ONAP, "guard:actor:actor-id");
    public static final Identifier ID_RESOURCE_GUARD_RECIPE =
            new IdentifierImpl(ID_URN_ONAP, "guard:operation:operation-id");
    public static final Identifier ID_RESOURCE_GUARD_CLNAME =
            new IdentifierImpl(ID_URN_ONAP, "guard:clname:clname-id");
    public static final Identifier ID_RESOURCE_GUARD_TARGETID =
            new IdentifierImpl(ID_URN_ONAP, "guard:target:target-id");
    public static final Identifier ID_SUBJECT_GUARD_REQUESTID =
            new IdentifierImpl(ID_URN_ONAP, "guard:request:request-id");
    public static final Identifier ID_RESOURCE_GUARD_VFCOUNT =
            new IdentifierImpl(ID_URN_ONAP, "guard:target:vf-count");
    public static final Identifier ID_RESOURCE_GUARD_MIN =
            new IdentifierImpl(ID_URN_ONAP, "guard:target:min");
    public static final Identifier ID_RESOURCE_GUARD_MAX =
            new IdentifierImpl(ID_URN_ONAP, "guard:target:max");
    public static final Identifier ID_RESOURCE_GUARD_TIMESTART =
            new IdentifierImpl(ID_URN_ONAP, "guard.target:timestart");
    public static final Identifier ID_RESOURCE_GUARD_TIMEEND =
            new IdentifierImpl(ID_URN_ONAP, "guard.target:timeend");

    /*
     * This id specifically for guard is provided by the
     * operational history database PIP.
     */
    public static final String GUARD_OPERATIONCOUNT = "guard:operation:operation-count";
    public static final Identifier ID_RESOURCE_GUARD_OPERATIONCOUNT =
            new IdentifierImpl(ID_URN_ONAP, GUARD_OPERATIONCOUNT);

    public static final String GUARD_OPERATIONOUTCOME = "guard:operation:operation-outcome";
    public static final Identifier ID_RESOURCE_GUARD_OPERATIONOUTCOME =
            new IdentifierImpl(ID_URN_ONAP, GUARD_OPERATIONOUTCOME);

    public static final String GUARD_ISSUER_PREFIX = URN_ONAP + ":xacml:guard:";

    /*
     * This id is specifically for advice returned from guard
     */
    public static final Identifier ID_ADVICE_GUARD =
            new IdentifierImpl(ID_URN_ONAP, "guard:advice");
    public static final Identifier ID_ADVICE_GUARD_REQUESTID =
            new IdentifierImpl(ID_URN_ONAP, "guard:advice:request-id");

    /*
     * These id's are specifically for optimization subscriber policies
     */
    public static final Identifier ID_SUBJECT_OPTIMIZATION_SUBSCRIBER_NAME =
            new IdentifierImpl(ID_URN_ONAP, "optimization:subscriber:name");

    /*
     * These ids are specifically for optimization advice
     */
    public static final Identifier ID_ADVICE_OPTIMIZATION_SUBSCRIBER =
            new IdentifierImpl(ID_URN_ONAP, "optimization:advice:subscriber");
    public static final Identifier ID_ADVICE_OPTIMIZATION_SUBSCRIBER_ROLE =
            new IdentifierImpl(ID_URN_ONAP, "optimization:advice:subscriber:role");
    public static final Identifier ID_ADVICE_OPTIMIZATION_SUBSCRIBER_STATUS =
            new IdentifierImpl(ID_URN_ONAP, "optimization:advice:subscriber:status");

    /*
     * Obligation specific ID's
     */

    public static final Identifier ID_OBLIGATION_REST_BODY =
            new IdentifierImpl(ID_URN_ONAP, "rest:body");

    public static final Identifier ID_OBLIGATION_POLICY_CONTENT =
            new IdentifierImpl(ID_URN_ONAP, ":obligation:policycontent");

    public static final Identifier ID_OBLIGATION_POLICY_CONTENT_CATEGORY =
            XACML3.ID_ATTRIBUTE_CATEGORY_RESOURCE;

    public static final Identifier ID_OBLIGATION_POLICY_CONTENT_DATATYPE =
            XACML3.ID_DATATYPE_STRING;

    public static final Identifier ID_OBLIGATION_POLICY_ID =
            new IdentifierImpl(ID_URN_ONAP, ":obligation:policyid");

    public static final Identifier ID_OBLIGATION_POLICY_ID_CATEGORY =
            XACML3.ID_ATTRIBUTE_CATEGORY_RESOURCE;

    public static final Identifier ID_OBLIGATION_POLICY_ID_DATATYPE =
            XACML3.ID_DATATYPE_STRING;

    public static final Identifier ID_OBLIGATION_POLICY_WEIGHT =
            new IdentifierImpl(ID_URN_ONAP, ":obligation:weight");

    public static final Identifier ID_OBLIGATION_POLICY_WEIGHT_CATEGORY =
            XACML3.ID_ATTRIBUTE_CATEGORY_RESOURCE;

    public static final Identifier ID_OBLIGATION_POLICY_WEIGHT_DATATYPE =
            XACML3.ID_DATATYPE_INTEGER;

    public static final Identifier ID_OBLIGATION_POLICY_TYPE =
            new IdentifierImpl(ID_URN_ONAP, ":obligation:policytype");

    public static final Identifier ID_OBLIGATION_POLICY_TYPE_CATEGORY =
            XACML3.ID_ATTRIBUTE_CATEGORY_RESOURCE;

    public static final Identifier ID_OBLIGATION_POLICY_TYPE_DATATYPE =
            XACML3.ID_DATATYPE_STRING;



    private ToscaDictionary() {
        super();
    }

}
