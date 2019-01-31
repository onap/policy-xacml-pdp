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
 * Class to represent statistics report of xacmlPdp service.
 *
 */
public class StatisticsReport {

    private int code;
    private long totalpoliciesCount;
    private long permitDecisionsCount;
    private long denyDecisionsCount;
    private long indeterminantDecisionsCount;
    private long notApplicableDecisionsCount;


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
     * Returns the totalpoliciesCount of this {@link StatisticsReport} instance.
     *
     * @return the totalpoliciesCount
     */
    public long getTotalpoliciesCount() {
        return totalpoliciesCount;
    }

    /**
     * Set totalpoliciesCount in this {@link StatisticsReport} instance.
     *
     * @param totalpoliciesCount the totalpoliciesCount to set
     */
    public void setTotalpoliciesCount(long totalpoliciesCount) {
        this.totalpoliciesCount = totalpoliciesCount;
    }

    /**
     * Returns the permitDecisionsCount of this {@link StatisticsReport} instance.
     *
     * @return the permitDecisionsCount
     */
    public long getPermitDecisionsCount() {
        return permitDecisionsCount;
    }

    /**
     * Set permitDecisionsCount in this {@link StatisticsReport} instance.
     *
     * @param permitDecisionsCount the permitDecisionsCount to set
     */
    public void setPermitDecisionsCount(long permitDecisionsCount) {
        this.permitDecisionsCount = permitDecisionsCount;
    }

    /**
     * Returns the denyDecisionsCount of this {@link StatisticsReport} instance.
     *
     * @return the denyDecisionsCount
     */
    public long getDenyDecisionsCount() {
        return denyDecisionsCount;
    }

    /**
     * Set denyDecisionsCount in this {@link StatisticsReport} instance.
     *
     * @param denyDecisionsCount the denyDecisionsCount to set
     */
    public void setDenyDecisionsCount(long denyDecisionsCount) {
        this.denyDecisionsCount = denyDecisionsCount;
    }

    /**
     * Returns the indeterminantDecisionsCount of this {@link StatisticsReport} instance.
     *
     * @return the indeterminantDecisionsCount
     */
    public long getIndeterminantDecisionsCount() {
        return indeterminantDecisionsCount;
    }

    /**
     * Set indeterminantDecisionsCount in this {@link StatisticsReport} instance.
     *
     * @param indeterminantDecisionsCount the indeterminantDecisionsCount to set
     */
    public void setIndeterminantDecisionsCount(long indeterminantDecisionsCount) {
        this.indeterminantDecisionsCount = indeterminantDecisionsCount;
    }

    /**
     * Returns the notApplicableDecisionsCount of this {@link StatisticsReport} instance.
     *
     * @return the notApplicableDecisionsCount
     */
    public long getNotApplicableDecisionsCount() {
        return notApplicableDecisionsCount;
    }

    /**
     * Set notApplicableDecisionsCount in this {@link StatisticsReport} instance.
     *
     * @param notApplicableDecisionsCount the notApplicableDecisionsCount to set
     */
    public void setNotApplicableDecisionsCount(long notApplicableDecisionsCount) {
        this.notApplicableDecisionsCount = notApplicableDecisionsCount;
    }

    /**
     * {@inheritDoc}.
     */
    @Override
    public String toString() {
        final StringBuilder builder = new StringBuilder();
        builder.append("StatisticsReport [code=");
        builder.append(getCode());
        builder.append(", totalpoliciesCount=");
        builder.append(getTotalpoliciesCount());
        builder.append(", permitDecisionsCount=");
        builder.append(getPermitDecisionsCount());
        builder.append(", denyDecisionsCount=");
        builder.append(getDenyDecisionsCount());
        builder.append(", indeterminantDecisionsCount=");
        builder.append(getIndeterminantDecisionsCount());
        builder.append(", notApplicableDecisionsCount=");
        builder.append(getNotApplicableDecisionsCount());
        builder.append("]");
        return builder.toString();
    }
}
