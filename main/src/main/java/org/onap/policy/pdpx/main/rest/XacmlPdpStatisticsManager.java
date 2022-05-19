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
import java.util.HashMap;
import java.util.Map;
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
    protected static final String POLICY_DECISIONS_METRIC = "policy_decisions";
    public static final String POLICY_DECISIONS_HELP = "The total number of policy decisions.";
    public static final String PERMIT_OPERATION = "permit";
    public static final String DENY_OPERATION = "deny";
    public static final String INDETERMINANT_OPERATION = "indeterminant";
    public static final String NOT_APPLICABLE_OPERATION = "not_applicable";
    public static final String APPLICATION = "application";

    protected static final Counter deploymentsCounter =
        Counter.build().namespace(PROMETHEUS_NAMESPACE).name(PrometheusUtils.POLICY_DEPLOYMENTS_METRIC)
            .labelNames(PrometheusUtils.OPERATION_METRIC_LABEL,
                PrometheusUtils.STATUS_METRIC_LABEL)
            .help(PrometheusUtils.POLICY_DEPLOYMENT_HELP)
            .register();

    protected static final Counter decisionsCounter =
        Counter.build().namespace(PROMETHEUS_NAMESPACE).name(POLICY_DECISIONS_METRIC)
            .labelNames(APPLICATION, PrometheusUtils.STATUS_METRIC_LABEL)
            .help(POLICY_DECISIONS_HELP)
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
    private Map<String, Map<String, Integer>> applicationMetrics;

    /**
     * Used to update our Map of ApplicationNames to statistics.
     * A typical applicationsMetric map could look something like this:
     * {
     *     "testApp1": {
     *         "updatePermitDecisionsCount": 1,
     *         "updateDenyDecisionsCount": 1
     *     },
     *     "testApp2": {
     *         "updateIndeterminantDecisionsCount": 1,
     *         "updateNotApplicableDecisionsCount": 1
     *     }
     * }
     * @param appName - the current app we are updating decisions for
     * @param updateMethod - the kind of decision we made for our app
     */
    @Synchronized
    public void updateApplicationMetrics(String appName, String updateMethod) {
        if (applicationMetrics == null) {
            applicationMetrics = new HashMap<>();
        }
        if (!applicationMetrics.containsKey(appName)) {
            Map<String, Integer> appMap = new HashMap<>();
            appMap.put(updateMethod, 1);
            applicationMetrics.put(appName, appMap);
        } else {
            int newTotal = applicationMetrics.get(appName).getOrDefault(updateMethod, 0) + 1;
            applicationMetrics.get(appName).put(updateMethod, newTotal);
        }
    }

    /**
     * Method to set the xacml pdp total policy types count. This
     * doesn't really increment, it depends on the applications
     * that are loaded. Which can be dynamic.
     */
    @Synchronized
    public void setTotalPolicyTypesCount(long newCount) {
        totalPolicyTypesCount = newCount;
    }

    /**
     * Method to set the xacml pdp total policies count. This
     * doesn't really increment, it depends on the applications
     * that are loaded. Which can be dynamic.
     */
    @Synchronized
    public void setTotalPolicyCount(long newCount) {
        totalPoliciesCount = newCount;
    }

    /**
     * Method to update the number of error decisions.
     */
    @Synchronized
    public void updateErrorCount() {
        ++errorCount;
    }

    /**
     * Method to update the number of permit decisions.
     */
    @Synchronized
    public void updatePermitDecisionsCount(String appName) {
        decisionsCounter.labels(appName, PERMIT_OPERATION).inc();
        updateApplicationMetrics(appName, "updatePermitDecisionsCount");
        ++permitDecisionsCount;
    }

    /**
     * Method to update the number of deny decisions.
     */
    @Synchronized
    public void updateDenyDecisionsCount(String appName) {
        decisionsCounter.labels(appName, DENY_OPERATION).inc();
        updateApplicationMetrics(appName, "updateDenyDecisionsCount");
        ++denyDecisionsCount;
    }

    /**
     * Method to update the number of indeterminant decisions.
     */
    @Synchronized
    public void updateIndeterminantDecisionsCount(String appName) {
        decisionsCounter.labels(appName, INDETERMINANT_OPERATION).inc();
        updateApplicationMetrics(appName, "updateIndeterminantDecisionsCount");
        ++indeterminantDecisionsCount;
    }

    /**
     * Method to update the number of not applicable decisions.
     */
    @Synchronized
    public void updateNotApplicableDecisionsCount(String appName) {
        decisionsCounter.labels(appName, NOT_APPLICABLE_OPERATION).inc();
        updateApplicationMetrics(appName, "updateNotApplicableDecisionsCount");
        ++notApplicableDecisionsCount;
    }

    /**
     * Method to update the number of successful deploys.
     */
    @Synchronized
    public void updateDeploySuccessCount() {
        deploymentsCounter.labels(PrometheusUtils.DEPLOY_OPERATION,
            PdpResponseStatus.SUCCESS.name()).inc();
        ++deploySuccessCount;
    }

    /**
     * Method to update the number of failed deploys.
     */
    @Synchronized
    public void updateDeployFailureCount() {
        deploymentsCounter.labels(PrometheusUtils.DEPLOY_OPERATION,
            PdpResponseStatus.FAIL.name()).inc();
        ++deployFailureCount;
    }

    /**
     * Method to update the number of successful undeploys.
     */
    @Synchronized
    public void updateUndeploySuccessCount() {
        deploymentsCounter.labels(PrometheusUtils.UNDEPLOY_OPERATION,
            PdpResponseStatus.SUCCESS.name()).inc();
        ++undeploySuccessCount;
    }

    /**
     * Method to update the number of failed undeploys.
     */
    @Synchronized
    public void updateUndeployFailureCount() {
        deploymentsCounter.labels(PrometheusUtils.UNDEPLOY_OPERATION,
            PdpResponseStatus.FAIL.name()).inc();
        ++undeployFailureCount;
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
        applicationMetrics = new HashMap<>();
    }
}
