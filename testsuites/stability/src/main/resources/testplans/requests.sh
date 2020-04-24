#!/bin/bash
#
# ============LICENSE_START=======================================================
#  Copyright (C) 2020 AT&T Intellectual Property. All rights reserved.
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
# Computes the number of requests per second processed during the test.
#
# Reads the jmeter log associated with the "Decision" requests, taking
# the difference between the latency ($15) and the connect time ($17),
# which yields the server processing time.  Averages it and then converts
# from ms/request to request/sec.
#

awk -F, 'NR > 1 { count++; sum += $15 - $17; } END { print 1000*count/sum; }' \
    /tmp/pdpx_stability_decisions.log
