#!/bin/bash
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

SQL_DIR="${POLICY_HOME}/mysql/sql"

if [ $# -eq 1 ]; then

	GUARD_TYPE="${1}"
	echo "GUARD_TYPE : $GUARD_TYPE"
	
	if [ ${GUARD_TYPE} -eq "frequency" ]; then
		SQL_FILE="${SQL_DIR}/vDNS.policy_guard_frequency.sql"
	else
		echo "Future modifications will allow usage other than frequency but not yet..."
	fi
	
	if [ -z "$SQL_FILE" ]
	then
		mysql -upolicy_user -ppolicy_user < "${SQL_FILE}"
	else
		echo "Cannot find SQL File for ${GUARD_TYPE} Guard type"
	fi
	
else
	echo "Usage : populate_operationshistory.sh <guard type>"
	echo "Example: populate_operationshistory.sh frequency"
fi
