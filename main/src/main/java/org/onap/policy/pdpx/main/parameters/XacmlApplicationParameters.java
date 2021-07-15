/*-
 * ============LICENSE_START=======================================================
 * Copyright (C) 2021 AT&T Intellectual Property. All rights reserved.
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
 *
 * SPDX-License-Identifier: Apache-2.0
 * ============LICENSE_END=========================================================
 */

package org.onap.policy.pdpx.main.parameters;

import java.util.List;
import lombok.Getter;
import lombok.NonNull;
import org.onap.policy.common.parameters.ParameterGroupImpl;
import org.onap.policy.common.parameters.annotations.NotBlank;
import org.onap.policy.common.parameters.annotations.NotNull;

@NotBlank
@Getter
public class XacmlApplicationParameters extends ParameterGroupImpl {

    @NotNull
    private String applicationPath;

    private List<String> exclusions;

    public XacmlApplicationParameters() {
        super(XacmlApplicationParameters.class.getSimpleName());
    }

    /**
     * Looks for an application class that has been configured
     * as excluded.
     *
     * @param canonicalName The classname
     * @return true if excluded
     */
    public boolean isExcluded(@NonNull String canonicalName) {
        if (exclusions == null) {
            return false;
        }
        return exclusions.contains(canonicalName);
    }

}
