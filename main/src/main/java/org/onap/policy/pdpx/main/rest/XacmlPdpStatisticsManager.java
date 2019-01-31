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

/**
 * Class to hold statistical data for xacmlPdp component.
 *
 */
public class XacmlPdpStatisticsManager {

    private static long totalpoliciesCount;
    private static long permitDecisionsCount;
    private static long denyDecisionsCount;
    private static long indeterminantDecisionsCount;
    private static long notApplicableDecisionsCount;

    private XacmlPdpStatisticsManager() {
        throw new IllegalStateException("Instantiation of the class is not allowed");
    }

    /**
     * Method to update the xacml pdp total policies count.
     *
     * @return the totalpoliciesCount
     */
    public static long updateTotalpoliciesCount() {
        return ++totalpoliciesCount;
    }

    /**
     * Method to update the number of permit decisions.
     *
     * @return the permitDecisionsCount
     */
    public static long updatePermitDecisionsCount() {
        return ++permitDecisionsCount;
    }

    /**
     * Method to update the number of deny decisions.
     *
     * @return the denyDecisionsCount
     */
    public static long updateDenyDecisionsCount() {
        return ++denyDecisionsCount;
    }

    /**
     * Method to update the number of indeterminant decisions.
     *
     * @return the indeterminantDecisionsCount
     */
    public static long updateIndeterminantDecisionsCount() {
        return ++indeterminantDecisionsCount;
    }

    /**
     * Method to update the number of not applicable decisions.
     *
     * @return the notApplicableDecisionsCount
     */
    public static long updateNotApplicableDecisionsCount() {
        return ++notApplicableDecisionsCount;
    }

    /**
     * Returns the current value of totalpoliciesCount.

     * @return the totalpoliciesCount
     */
    public static long getTotalpoliciesCount() {
        return totalpoliciesCount;
    }

    /**
     * Returns the current value of permitDecisionsCount.

     * @return the permitDecisionsCount
     */
    public static long getPermitDecisionsCount() {
        return permitDecisionsCount;
    }

    /**
     * Returns the current value of denyDecisionsCount.

     * @return the denyDecisionsCount
     */
    public static long getDenyDecisionsCount() {
        return denyDecisionsCount;
    }

    /**
     * Returns the current value of indeterminantDecisionsCount.

     * @return the indeterminantDecisionsCount
     */
    public static long getIndeterminantDecisionsCount() {
        return indeterminantDecisionsCount;
    }

    /**
     * Returns the current value of notApplicableDecisionsCount.

     * @return the notApplicableDecisionsCount
     */
    public static long getNotApplicableDecisionsCount() {
        return notApplicableDecisionsCount;
    }

    /**
     * Reset all the statistics counts to 0.
     */
    public static void resetAllStatistics() {
        totalpoliciesCount = 0L;
        permitDecisionsCount = 0L;
        denyDecisionsCount = 0L;
        indeterminantDecisionsCount = 0L;
        notApplicableDecisionsCount = 0L;
    }
}
