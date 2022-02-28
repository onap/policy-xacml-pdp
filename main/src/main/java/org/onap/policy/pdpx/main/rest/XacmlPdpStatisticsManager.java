/*-
 * ============LICENSE_START=======================================================
 * Copyright (C) 2019, 2021-2022 AT&T Intellectual Property. All rights reserved.
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

import io.prometheus.client.Counter;
import lombok.Getter;
import lombok.Setter;
import lombok.Synchronized;
import org.onap.policy.common.utils.resources.PrometheusUtils;
import org.onap.policy.models.pdp.enums.PdpResponseStatus;

/**
 * Class to hold statistical data for xacmlPdp component.
 */
@Getter(onMethod_ = @Synchronized)
public class XacmlPdpStatisticsManager {
    @Getter
    @Setter
    private static XacmlPdpStatisticsManager current = null;
    protected static final String PROMETHEUS_NAMESPACE = "pdpx";

    protected static final Counter deploymentsCounter =
        Counter.build().namespace(PROMETHEUS_NAMESPACE).name(PrometheusUtils.POLICY_DEPLOYMENTS_METRIC)
            .labelNames(PrometheusUtils.OPERATION_METRIC_LABEL,
                PrometheusUtils.STATUS_METRIC_LABEL)
            .help(PrometheusUtils.POLICY_DEPLOYMENT_HELP)
            .register();

    private long totalPolicyTypesCount;
    private long totalPoliciesCount;
    private long errorCount;
    private long permitDecisionsCount;
    private long denyDecisionsCount;
    private long deploySuccessCount;
    private long deployFailureCount;
    private long undeploySuccessCount;
    private long undeployFailureCount;
    private long indeterminantDecisionsCount;
    private long notApplicableDecisionsCount;

    /**
     * Method to set the xacml pdp total policy types count. This
     * doesn't really increment, it depends on the applications
     * that are loaded. Which can be dynamic.
     *
     * @return the total
     */
    @Synchronized
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
    @Synchronized
    public long setTotalPolicyCount(long newCount) {
        totalPoliciesCount = newCount;
        return totalPoliciesCount;
    }

    /**
     * Method to update the number of error decisions.
     *
     * @return the errorDecisionsCount
     */
    @Synchronized
    public long updateErrorCount() {
        return ++errorCount;
    }

    /**
     * Method to update the number of permit decisions.
     *
     * @return the permitDecisionsCount
     */
    @Synchronized
    public long updatePermitDecisionsCount() {
        return ++permitDecisionsCount;
    }

    /**
     * Method to update the number of deny decisions.
     *
     * @return the denyDecisionsCount
     */
    @Synchronized
    public long updateDenyDecisionsCount() {
        return ++denyDecisionsCount;
    }

    /**
     * Method to update the number of successful deploys.
     *
     * @return the deploySuccessCount
     */
    @Synchronized
    public long updateDeploySuccessCount() {
        deploymentsCounter.labels(PrometheusUtils.DEPLOY_OPERATION,
            PdpResponseStatus.SUCCESS.name()).inc();
        return ++deploySuccessCount;
    }

    /**
     * Method to update the number of failed deploys.
     *
     * @return the deployFailureCount
     */
    @Synchronized
    public long updateDeployFailureCount() {
        deploymentsCounter.labels(PrometheusUtils.DEPLOY_OPERATION,
            PdpResponseStatus.FAIL.name()).inc();
        return ++deployFailureCount;
    }

    /**
     * Method to update the number of successful undeploys.
     *
     * @return the undeploySuccessCount
     */
    @Synchronized
    public long updateUndeploySuccessCount() {
        deploymentsCounter.labels(PrometheusUtils.UNDEPLOY_OPERATION,
            PdpResponseStatus.SUCCESS.name()).inc();
        return ++undeploySuccessCount;
    }

    /**
     * Method to update the number of failed undeploys.
     *
     * @return the undeployFailureCount
     */
    @Synchronized
    public long updateUndeployFailureCount() {
        deploymentsCounter.labels(PrometheusUtils.UNDEPLOY_OPERATION,
            PdpResponseStatus.FAIL.name()).inc();
        return ++undeployFailureCount;
    }

    /**
     * Method to update the number of indeterminant decisions.
     *
     * @return the indeterminantDecisionsCount
     */
    @Synchronized
    public long updateIndeterminantDecisionsCount() {
        return ++indeterminantDecisionsCount;
    }

    /**
     * Method to update the number of not applicable decisions.
     *
     * @return the notApplicableDecisionsCount
     */
    @Synchronized
    public long updateNotApplicableDecisionsCount() {
        return ++notApplicableDecisionsCount;
    }

    /**
     * Reset all the statistics counts to 0.
     */
    @Synchronized
    public void resetAllStatistics() {
        totalPolicyTypesCount = 0L;
        totalPoliciesCount = 0L;
        errorCount = 0L;
        permitDecisionsCount = 0L;
        denyDecisionsCount = 0L;
        deploySuccessCount = 0L;
        deployFailureCount = 0L;
        undeploySuccessCount = 0L;
        undeployFailureCount = 0L;
        indeterminantDecisionsCount = 0L;
        notApplicableDecisionsCount = 0L;
    }
}
