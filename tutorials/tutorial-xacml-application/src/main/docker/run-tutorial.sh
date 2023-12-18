#! /bin/bash

# ============LICENSE_START====================================================
#  Copyright 2022 Nordix Foundation.
# =============================================================================
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
# ============LICENSE_END======================================================

if [ -z "$CONTAINER_LOCATION" ]
then
    export CONTAINER_LOCATION="nexus3.onap.org:10001/"
fi

echo "Looking up the XACML tutorial version . . ."

POLICY_XACML_TUTORIAL_VERSION=$(
    docker images --filter "reference=onap/policy-xacml-tutorial" |
    grep -v "REPOSITORY" |
    sort    |
    head -1 |
    awk '{print $2}'
)

if [ -z "$POLICY_XACML_TUTORIAL_VERSION" ]
then
    echo "Look up of the XACML tutorial version failed, have you built the tutorial docker image?"
    exit 1
else
    export POLICY_XACML_TUTORIAL_VERSION
    echo "Look up of the XACML tutorial version completed, version is ${POLICY_XACML_TUTORIAL_VERSION}"
fi

echo "Looking up latest versions of Policy Framework images . . ."
GETVERS_SCRIPT=$(mktemp)
curl -qL --silent "https://raw.githubusercontent.com/onap/policy-docker/master/csit/get-versions.sh" > "$GETVERS_SCRIPT"
export GERRIT_BRANCH="master"
chmod +x "$GETVERS_SCRIPT"
source "$GETVERS_SCRIPT"
echo "Look up of latest versions of Policy Framework images completed"


echo "Running tutorial . . ."
docker-compose -f docker-compose.yml up
echo "Tutorial run completed"

echo "Cleaning up . . ."
rm "$GETVERS_SCRIPT"
echo "Cleanup competed"
