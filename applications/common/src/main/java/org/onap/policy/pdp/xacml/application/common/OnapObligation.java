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

import com.att.research.xacml.api.AttributeAssignment;
import com.att.research.xacml.api.Obligation;
import lombok.Getter;
import lombok.ToString;

@Getter
@ToString
public class OnapObligation {

    private String policyId;
    private String policyType;
    private String policyContent;
    private Integer weight;

    /**
     * Constructor.
     *
     * @param obligation Obligation object
     */
    public OnapObligation(Obligation obligation) {
        //
        // Scan through the obligations for them
        //
        for (AttributeAssignment assignment : obligation.getAttributeAssignments()) {
            scanAttribute(assignment);
        }
    }

    /**
     * scanAttribute - scans the assignment for a supported obligation assignment. Applications
     * can override this class and provide their own custom attribute assignments if desired.
     *
     * @param assignment AttributeAssignment object
     * @return true if found an ONAP supported attribute
     */
    protected boolean scanAttribute(AttributeAssignment assignment) {
        if (ToscaDictionary.ID_OBLIGATION_POLICY_ID.equals(assignment.getAttributeId())) {
            policyId = assignment.getAttributeValue().getValue().toString();
            return true;
        } else if (ToscaDictionary.ID_OBLIGATION_POLICY_TYPE.equals(assignment.getAttributeId())) {
            policyType = assignment.getAttributeValue().getValue().toString();
            return true;
        } else if (ToscaDictionary.ID_OBLIGATION_POLICY_CONTENT.equals(assignment.getAttributeId())) {
            policyContent = assignment.getAttributeValue().getValue().toString();
            return true;
        } else if (ToscaDictionary.ID_OBLIGATION_POLICY_WEIGHT.equals(assignment.getAttributeId())) {
            weight = Integer.decode(assignment.getAttributeValue().getValue().toString());
            return true;
        }
        return false;
    }

}
