/*-
 * ============LICENSE_START=======================================================
 * Copyright (C) 2018 Ericsson. All rights reserved.
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
 * Class to represent statistics report of xacmlPdp service.
 *
 */
public class StatisticsReport {

    private int code;
    private long totalXacmlPdpCount;
    private long xacmlPdpSuccessCount;
    private long xacmlPdpFailureCount;
    private long totalDownloadCount;
    private long downloadSuccessCount;
    private long downloadFailureCount;

    /**
     * Returns the code of this {@link StatisticsReport} instance.
     *
     * @return the code
     */
    public int getCode() {
        return code;
    }

    /**
     * Set code in this {@link StatisticsReport} instance.
     *
     * @param code the code to set
     */
    public void setCode(final int code) {
        this.code = code;
    }

    /**
     * Returns the totalXacmlPdpCount of this {@link StatisticsReport} instance.
     *
     * @return the totalXacmlPdpCount
     */
    public long getTotalXacmlPdpCount() {
        return totalXacmlPdpCount;
    }

    /**
     * Set totalXacmlPdpCount in this {@link StatisticsReport} instance.
     *
     * @param totalXacmlPdpCount the totalXacmlPdpCount to set
     */
    public void setTotalXacmlPdpCount(final long totalXacmlPdpCount) {
        this.totalXacmlPdpCount = totalXacmlPdpCount;
    }

    /**
     * Returns the xacmlPdpSuccessCount of this {@link StatisticsReport} instance.
     *
     * @return the xacmlPdpSuccessCount
     */
    public long getXacmlPdpSuccessCount() {
        return xacmlPdpSuccessCount;
    }

    /**
     * Set xacmlPdpSuccessCount in this {@link StatisticsReport} instance.
     *
     * @param xacmlPdpSuccessCount the xacmlPdpSuccessCount to set
     */
    public void setXacmlPdpSuccessCount(final long xacmlPdpSuccessCount) {
        this.xacmlPdpSuccessCount = xacmlPdpSuccessCount;
    }

    /**
     * Returns the xacmlPdpFailureCount of this {@link StatisticsReport} instance.
     *
     * @return the xacmlPdpFailureCount
     */
    public long getXacmlPdpFailureCount() {
        return xacmlPdpFailureCount;
    }

    /**
     * Set xacmlPdpFailureCount in this {@link StatisticsReport} instance.
     *
     * @param xacmlPdpFailureCount the xacmlPdpFailureCount to set
     */
    public void setXacmlPdpFailureCount(final long xacmlPdpFailureCount) {
        this.xacmlPdpFailureCount = xacmlPdpFailureCount;
    }

    /**
     * Returns the totalDownloadCount of this {@link StatisticsReport} instance.
     *
     * @return the totalDownloadCount
     */
    public long getTotalDownloadCount() {
        return totalDownloadCount;
    }

    /**
     * Set totalDownloadCount in this {@link StatisticsReport} instance.
     *
     * @param totalDownloadCount the totalDownloadCount to set
     */
    public void setTotalDownloadCount(final long totalDownloadCount) {
        this.totalDownloadCount = totalDownloadCount;
    }

    /**
     * Returns the downloadSuccessCount of this {@link StatisticsReport} instance.
     *
     * @return the downloadSuccessCount
     */
    public long getDownloadSuccessCount() {
        return downloadSuccessCount;
    }

    /**
     * Set downloadSuccessCount in this {@link StatisticsReport} instance.
     *
     * @param downloadSuccessCount the downloadSuccessCount to set
     */
    public void setDownloadSuccessCount(final long downloadSuccessCount) {
        this.downloadSuccessCount = downloadSuccessCount;
    }

    /**
     * Returns the downloadFailureCount of this {@link StatisticsReport} instance.
     *
     * @return the downloadFailureCount
     */
    public long getDownloadFailureCount() {
        return downloadFailureCount;
    }

    /**
     * Set downloadFailureCount in this {@link StatisticsReport} instance.
     *
     * @param downloadFailureCount the downloadFailureCount to set
     */
    public void setDownloadFailureCount(final long downloadFailureCount) {
        this.downloadFailureCount = downloadFailureCount;
    }


    /**
     * {@inheritDoc}.
     */
    @Override
    public String toString() {
        final StringBuilder builder = new StringBuilder();
        builder.append("StatisticsReport [code=");
        builder.append(getCode());
        builder.append(", totalXacmlPdpCount=");
        builder.append(getTotalXacmlPdpCount());
        builder.append(", xacmlPdpSuccessCount=");
        builder.append(getXacmlPdpSuccessCount());
        builder.append(", xacmlPdpFailureCount=");
        builder.append(getXacmlPdpFailureCount());
        builder.append(", totalDownloadCount=");
        builder.append(getTotalDownloadCount());
        builder.append(", downloadSuccessCount=");
        builder.append(getDownloadSuccessCount());
        builder.append(", downloadFailureCount=");
        builder.append(getDownloadFailureCount());
        builder.append("]");
        return builder.toString();
    }
}
