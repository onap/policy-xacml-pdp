#!/bin/bash -xv
#
# ============LICENSE_START=======================================================
#  Copyright (C) 2019 AT&T Intellectual Property. All rights reserved.
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

# Extract Maria DB Credential properties from xacml.properties file
DBURL_PROPERTY=$(echo $(grep '^javax.persistence.jdbc.url=' "${POLICY_HOME}"/apps/guard/xacml.properties ))
DB_HOSTNAME=$(echo $DBURL_PROPERTY | cut -f2 -d= | cut -f3 -d'/' | cut -f1 -d':')
DB_USERNAME=$(echo $(grep '^javax.persistence.jdbc.user=' /home/mm117s/testing/xacml.properties | cut -f2 -d=))
DB_PASSWORD=`echo $(echo $(grep '^javax.persistence.jdbc.password=' "${POLICY_HOME}"/apps/guard/xacml.properties | cut -f2 -d=)) | base64 --decode`

# Execute mysql command using sql file to create table
mysql -u${DB_USERNAME} -p${DB_PASSWORD} -h${DB_HOSTNAME} < "${SQL_FILE}"

