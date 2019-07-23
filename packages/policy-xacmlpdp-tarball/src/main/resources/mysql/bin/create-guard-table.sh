#!/bin/bash
#
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
SQL_FILE="${POLICY_HOME}/mysql/sql/createguardtable.sql"

# Remove escape backslashes if present and save output in temp file
sed 's/\\//g' "${POLICY_HOME}"/apps/guard/xacml.properties > temp.properties

# Extract Maria DB Credential properties from xacml.properties file
DB_HOSTNAME=$(awk -F[/:] '$1 == "javax.persistence.jdbc.url=jdbc" { print $3 $5 }' temp.properties)
DB_USERNAME=$(awk -F= '$1 == "javax.persistence.jdbc.user" { print $2 }' temp.properties)
DB_PASSWORD=$(awk -F= '$1 == "javax.persistence.jdbc.password" { print $2 }' temp.properties | base64 -d)

# Remove temp file
if [ -f temp.properties ]
  then
    rm temp.properties
fi

if [ -z "$DB_HOSTNAME" ]
  then
    echo "No Mariadb host provided in xacml.properties."
    exit 2
fi

if [ -z "$DB_USERNAME" ]
  then
    echo "No Mariadb username provided in xacml.properties."
    exit 2
fi

if [ -z "$DB_PASSWORD" ]
  then
    echo "No Mariadb password provided in xacml.properties."
    exit 2
fi

# Execute mysql command using sql file to create table
mysql -u${DB_USERNAME} -p${DB_PASSWORD} -h${DB_HOSTNAME} < "${SQL_FILE}"

