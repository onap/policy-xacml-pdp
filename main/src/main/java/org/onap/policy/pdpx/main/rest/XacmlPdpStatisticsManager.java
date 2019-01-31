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

    private static long totalXacmlPdpCount;
    private static long xacmlPdpSuccessCount;
    private static long xacmlPdpFailureCount;
    private static long totalDownloadCount;
    private static long downloadSuccessCount;
    private static long downloadFailureCount;

    private XacmlPdpStatisticsManager() {
        throw new IllegalStateException("Instantiation of the class is not allowed");
    }

    /**
     * Method to update the total xacmlPdp count.
     *
     * @return the updated value of totalXacmlPdpCount
     */
    public static long updateTotalXacmlPdpCount() {
        return ++totalXacmlPdpCount;
    }

    /**
     * Method to update the xacmlPdp success count.
     *
     * @return the updated value of xacmlPdpSuccessCount
     */
    public static long updateXacmlPdpSuccessCount() {
        return ++xacmlPdpSuccessCount;
    }

    /**
     * Method to update the xacmlPdp failure count.
     *
     * @return the updated value of xacmlPdpFailureCount
     */
    public static long updateXacmlPdpFailureCount() {
        return ++xacmlPdpFailureCount;
    }

    /**
     * Method to update the total download count.
     *
     * @return the updated value of totalDownloadCount
     */
    public static long updateTotalDownloadCount() {
        return ++totalDownloadCount;
    }

    /**
     * Method to update the download success count.
     *
     * @return the updated value of downloadSuccessCount
     */
    public static long updateDownloadSuccessCount() {
        return ++downloadSuccessCount;
    }

    /**
     * Method to update the download failure count.
     *
     * @return the updated value of downloadFailureCount
     */
    public static long updateDownloadFailureCount() {
        return ++downloadFailureCount;
    }

    /**
     * Returns the current value of totalXacmlPdpCount.
     *
     * @return the totalXacmlPdpCount
     */
    public static long getTotalXacmlPdpCount() {
        return totalXacmlPdpCount;
    }

    /**
     * Returns the current value of xacmlPdpSuccessCount.
     *
     * @return the xacmlPdpSuccessCount
     */
    public static long getXacmlPdpSuccessCount() {
        return xacmlPdpSuccessCount;
    }

    /**
     * Returns the current value of xacmlPdpFailureCount.
     *
     * @return the xacmlPdpFailureCount
     */
    public static long getXacmlPdpFailureCount() {
        return xacmlPdpFailureCount;
    }

    /**
     * Returns the current value of totalDownloadCount.
     *
     * @return the totalDownloadCount
     */
    public static long getTotalDownloadCount() {
        return totalDownloadCount;
    }

    /**
     * Returns the current value of downloadSuccessCount.
     *
     * @return the downloadSuccessCount
     */
    public static long getDownloadSuccessCount() {
        return downloadSuccessCount;
    }

    /**
     * Returns the current value of downloadFailureCount.
     *
     * @return the downloadFailureCount
     */
    public static long getDownloadFailureCount() {
        return downloadFailureCount;
    }

    /**
     * Reset all the statistics counts to 0.
     */
    public static void resetAllStatistics() {
        totalXacmlPdpCount = 0L;
        xacmlPdpSuccessCount = 0L;
        xacmlPdpFailureCount = 0L;
        totalDownloadCount = 0L;
        downloadSuccessCount = 0L;
        downloadFailureCount = 0L;
    }
}
