#!/usr/bin/env sh
#
# ============LICENSE_START=======================================================
#  Copyright (C) 2022-2024 Nordix Foundation. All rights reserved.
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

SQL_FILE="${POLICY_HOME}/postgres/sql/createguardtable-pg.sql"
SQL_ADDON_FILE="${POLICY_HOME}/postgres/sql/db-pg.sql"

# Remove escape backslashes if present and save output in temp file
sed 's/\\//g' "${POLICY_HOME}"/apps/guard/xacml-pg.properties > /tmp/temp.xacml-pg.properties

# Remove temp file
if [ ! -f /tmp/temp.xacml-pg.properties ]; then
    echo "Temporary guard xacml properties file not found!"
    exit 1
fi

# Extract Maria DB Credential properties from xacml.properties file
DB_HOSTNAME=$(awk -F[/:] '$1 == "jakarta.persistence.jdbc.url=jdbc" { print $3 $5 }' /tmp/temp.xacml-pg.properties)
DB_USERNAME=$(awk -F= '$1 == "jakarta.persistence.jdbc.user" { print $2 }' /tmp/temp.xacml-pg.properties)
DB_PASSWORD=$(awk -F= '$1 == "jakarta.persistence.jdbc.password" { st = index($0,"="); print substr($0,st+1) }' /tmp/temp.xacml-pg.properties)

# Remove temp file
rm /tmp/temp.xacml-pg.properties

if [ -z "$DB_HOSTNAME" ]; then
    echo "No db host provided in guard xacml-pg.properties."
    exit 2
fi

if [ -z "$DB_USERNAME" ]; then
    echo "No db username provided in guard xacml-pg.properties."
    exit 2
fi

if [ -z "$DB_PASSWORD" ]; then
    echo "No db password provided in guard xacml-pg.properties."
    exit 2
fi

# Execute sql command using sql file to create table
psql -U postgres -h ${DB_HOSTNAME} -f ${SQL_FILE}

# Execute additional SQL configuration if provided
if [ -f "${POLICY_HOME}/postgres/sql/db-pg.sql" ]; then
    echo "additional SQL to be loaded found"
    psql -U postgres -h ${DB_HOSTNAME} -f ${SQL_ADDON_FILE}
fi

