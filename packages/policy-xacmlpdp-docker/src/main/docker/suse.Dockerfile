#-------------------------------------------------------------------------------
# Dockerfile
# ============LICENSE_START=======================================================
#  Copyright (C) 2022 Nordix Foundation.
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
#-------------------------------------------------------------------------------
FROM opensuse/leap:15.3

LABEL maintainer="Policy Team"

ARG POLICY_LOGS=/var/log/onap/policy/pdpx

ENV POLICY_LOGS=$POLICY_LOGS
ENV POLICY_HOME=/opt/app/policy/pdpx
ENV LANG=en_US.UTF-8 LANGUAGE=en_US:en LC_ALL=en_US.UTF-8
ENV JAVA_HOME=/usr/lib64/jvm/java-11-openjdk-11

RUN zypper -n -q install --no-recommends gzip java-11-openjdk-headless mariadb-client netcat-openbsd postgresql tar && \
    zypper -n -q update && zypper -n -q clean --all && \
    groupadd --system policy && \
    useradd --system --shell /bin/sh -G policy policy && \
    mkdir -p $POLICY_LOGS $POLICY_HOME $POLICY_HOME/etc/ssl $POLICY_HOME/bin $POLICY_HOME/apps && \
    chown -R policy:policy $POLICY_HOME $POLICY_LOGS && \
    mkdir /packages

COPY /maven/* /packages
RUN tar xvfz /packages/policy-xacmlpdp.tar.gz --directory $POLICY_HOME && \
    rm /packages/policy-xacmlpdp.tar.gz

WORKDIR $POLICY_HOME
COPY policy-pdpx.sh  bin/.
COPY policy-pdpx-pg.sh  bin/.
RUN chown -R policy:policy * && chmod 755 bin/*.sh && chmod 755 mysql/bin/*.sh && chmod 755 postgres/bin/*.sh

USER policy
WORKDIR $POLICY_HOME/bin
ENTRYPOINT [ "./policy-pdpx.sh" ]
