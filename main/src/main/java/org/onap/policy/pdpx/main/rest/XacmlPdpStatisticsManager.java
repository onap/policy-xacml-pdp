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

import lombok.Getter;
import lombok.Setter;

/**
 * Class to hold statistical data for xacmlPdp component.
 *
 */
public class XacmlPdpStatisticsManager {
    @Getter
    @Setter
    private static XacmlPdpStatisticsManager current = null;

    private long totalPolicyTypesCount;
    private long totalPoliciesCount;
    private long errorsCount;
    private long permitDecisionsCount;
    private long denyDecisionsCount;
    private long indeterminantDecisionsCount;
    private long notApplicableDecisionsCount;

    /**
     * Method to set the xacml pdp total policy types count. This
     * doesn't really increment, it depends on the applications
     * that are loaded. Which can be dynamic.
     *
     * @return the total
     */
    public long setTotalPolicyTypesCount(long newCount) {
        totalPolicyTypesCount = newCount;
        return totalPolicyTypesCount;
    }

    /**
     * Method to set the xacml pdp total policies count. This
     * doesn't really increment, it depends on the applications
     * that are loaded. Which can be dynamic.
     *
     * @return the total
     */
    public long setTotalPolicyCount(long newCount) {
        totalPoliciesCount = newCount;
        return totalPoliciesCount;
    }

    /**
     * Method to update the number of error decisions.
     *
     * @return the errorDecisionsCount
     */
    public long updateErrorCount() {
        return ++errorsCount;
    }

    /**
     * Method to update the number of permit decisions.
     *
     * @return the permitDecisionsCount
     */
    public long updatePermitDecisionsCount() {
        return ++permitDecisionsCount;
    }

    /**
     * Method to update the number of deny decisions.
     *
     * @return the denyDecisionsCount
     */
    public long updateDenyDecisionsCount() {
        return ++denyDecisionsCount;
    }

    /**
     * Method to update the number of indeterminant decisions.
     *
     * @return the indeterminantDecisionsCount
     */
    public long updateIndeterminantDecisionsCount() {
        return ++indeterminantDecisionsCount;
    }

    /**
     * Method to update the number of not applicable decisions.
     *
     * @return the notApplicableDecisionsCount
     */
    public long updateNotApplicableDecisionsCount() {
        return ++notApplicableDecisionsCount;
    }

    /**
     * Returns the current value of totalPolicyTypesCount.

     * @return the totalPolicyTypesCount
     */
    public long getTotalPolicyTypesCount() {
        return totalPolicyTypesCount;
    }

    /**
     * Returns the current value of totalPoliciesCount.

     * @return the totalPoliciesCount
     */
    public long getTotalPoliciesCount() {
        return totalPoliciesCount;
    }

    /**
     * Returns the current value of errorDecisionsCount.

     * @return the permitDecisionsCount
     */
    public long getErrorCount() {
        return errorsCount;
    }

    /**
     * Returns the current value of permitDecisionsCount.

     * @return the permitDecisionsCount
     */
    public long getPermitDecisionsCount() {
        return permitDecisionsCount;
    }

    /**
     * Returns the current value of denyDecisionsCount.

     * @return the denyDecisionsCount
     */
    public long getDenyDecisionsCount() {
        return denyDecisionsCount;
    }

    /**
     * Returns the current value of indeterminantDecisionsCount.

     * @return the indeterminantDecisionsCount
     */
    public long getIndeterminantDecisionsCount() {
        return indeterminantDecisionsCount;
    }

    /**
     * Returns the current value of notApplicableDecisionsCount.

     * @return the notApplicableDecisionsCount
     */
    public long getNotApplicableDecisionsCount() {
        return notApplicableDecisionsCount;
    }

    /**
     * Reset all the statistics counts to 0.
     */
    public void resetAllStatistics() {
        totalPolicyTypesCount = 0L;
        totalPoliciesCount = 0L;
        errorsCount = 0L;
        permitDecisionsCount = 0L;
        denyDecisionsCount = 0L;
        indeterminantDecisionsCount = 0L;
        notApplicableDecisionsCount = 0L;
    }
}
