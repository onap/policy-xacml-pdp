/*-
 * ============LICENSE_START=======================================================
 * guard
 * ================================================================================
 * Copyright (C) 2019-2020 AT&T Intellectual Property. All rights reserved.
 * ================================================================================
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * ============LICENSE_END=========================================================
 */

package org.onap.policy.xacml.pdp.application.guard;

import java.io.Serializable;
import java.util.List;
import lombok.Data;

@Data
public class CoordinationDirective implements Serializable {
    private static final long serialVersionUID = 6897293694639777548L;
    private List<String> controlLoop;
    private String coordinationFunction;

    /**
     * gets the ith control loop.
     *
     * @param index the control loop's index
     * @return the CoordinationDirective's string representation
     */
    public String getControlLoop(int index) {
        return controlLoop.get(index);
    }
}
