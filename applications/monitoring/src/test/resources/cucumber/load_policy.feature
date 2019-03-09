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
  When a TOSCA Policy is received, convert it
  to a XACML policy and then load it into the XACML PDP engine.
  
  Scenario: No Policies Loaded
    Given Initialization
    When Decision Requested
    Then Decision Permit 0 Obligations
  
  Scenario: Load New Policy
    Given Initialization
    When The application gets new Tosca Policy
    Then Load Policy
    And Save Configuration
