#
# ============LICENSE_START=======================================================
# ONAP
# ================================================================================
# Copyright (C) 2019 AT&T Intellectual Property. All rights reserved.
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
Feature: Loading TOSCA Policies
  When a TOSCA Policy is received from PAP or loaded from install, convert it
  to a XACML policy and then load it into the XACML PDP engine.

  Scenario: Initialize Known XACML Policies
    Given The application is starting up
    When The application starts up
    Then Read Policies To Load
    And Load All Policies Into XACML PDP

  Scenario: Load TOSCA Policy
      Given Monitoring TOSCA Policy
      When New TOSCA Policy Appears
      Then I Convert TOSCA Policy to XACML
      And Load All Policies Into XACML PDP