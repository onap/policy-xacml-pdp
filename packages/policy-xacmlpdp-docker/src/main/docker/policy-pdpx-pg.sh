#!/usr/bin/env sh
#
# ============LICENSE_START=======================================================
#  Copyright (C) 2022, 2024 Nordix Foundation.
#  Modifications Copyright (C) 2022 AT&T Intellectual Property.
# ================================================================================
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#      http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#
# SPDX-License-Identifier: Apache-2.0
# ============LICENSE_END=========================================================
#

set -x

KEYSTORE="${KEYSTORE:-$POLICY_HOME/etc/ssl/policy-keystore}"
TRUSTSTORE="${TRUSTSTORE:-$POLICY_HOME/etc/ssl/policy-truststore}"
KEYSTORE_PASSWD="${KEYSTORE_PASSWD:-Pol1cy_0nap}"
TRUSTSTORE_PASSWD="${TRUSTSTORE_PASSWD:-Pol1cy_0nap}"

if [ "$#" -ge 1 ]; then
    CONFIG_FILE=$1
else
    CONFIG_FILE=${CONFIG_FILE}
fi

if [ -z "$CONFIG_FILE" ]; then
    CONFIG_FILE="${POLICY_HOME}/etc/defaultConfig.json"
fi

if [ -f "${POLICY_HOME}/etc/mounted/policy-truststore" ]; then
    echo "overriding policy-truststore"
    cp -f "${POLICY_HOME}"/etc/mounted/policy-truststore "${TRUSTSTORE}"
fi

if [ -f "${POLICY_HOME}/etc/mounted/policy-keystore" ]; then
    echo "overriding policy-keystore"
    cp -f "${POLICY_HOME}"/etc/mounted/policy-keystore "${KEYSTORE}"
fi

if [ -f "${POLICY_HOME}/etc/mounted/xacml.properties" ]; then
    echo "overriding xacml.properties in guards application"
    cp -f "${POLICY_HOME}"/etc/mounted/xacml-pg.properties "${POLICY_HOME}"/apps/guard/
fi

if [ -f "${POLICY_HOME}/etc/mounted/logback.xml" ]; then
    echo "overriding logback.xml"
    cp -f "${POLICY_HOME}"/etc/mounted/logback.xml "${POLICY_HOME}"/etc/
fi

if [ -f "${POLICY_HOME}/etc/mounted/createguardtable-pg.sql" ]; then
    echo "overriding createguardtable.sql"
    cp -f "${POLICY_HOME}"/etc/mounted/createguardtable-pg.sql "${POLICY_HOME}"/postgres/sql/
fi

if [ -f "${POLICY_HOME}/etc/mounted/db-pg.sql" ]; then
    echo "adding additional db-pg.sql"
    cp -f "${POLICY_HOME}"/etc/mounted/db-pg.sql "${POLICY_HOME}"/postgres/sql/
fi

if [ -f "${POLICY_HOME}/etc/mounted/guard.xacml.properties" ]; then
    echo "overriding guard application xacml.properties"
    cp -f "${POLICY_HOME}"/etc/mounted/guard.xacml.properties "${POLICY_HOME}"/apps/guard/xacml.properties
fi

# Create operationshistory table
"${POLICY_HOME}"/postgres/bin/create-guard-table-pg.sh

echo "Policy Xacml PDP config file: $CONFIG_FILE"

$JAVA_HOME/bin/java -cp "${POLICY_HOME}/etc:${POLICY_HOME}/lib/*" -Dlogback.configurationFile="${POLICY_HOME}/etc/logback.xml" -Djavax.net.ssl.keyStore="${KEYSTORE}" -Djavax.net.ssl.keyStorePassword="${KEYSTORE_PASSWD}" -Djavax.net.ssl.trustStore="${TRUSTSTORE}" -Djavax.net.ssl.trustStorePassword="${TRUSTSTORE_PASSWD}" org.onap.policy.pdpx.main.startstop.Main -c "${CONFIG_FILE}"
