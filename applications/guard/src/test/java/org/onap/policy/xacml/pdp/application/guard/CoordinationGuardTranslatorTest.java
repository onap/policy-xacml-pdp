/*-
 * ============LICENSE_START=======================================================
 * ONAP
 * ================================================================================
 * Copyright (C) 2020 AT&T Intellectual Property. All rights reserved.
 * Modifications Copyright (C) 2024 Nordix Foundation.
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

package org.onap.policy.xacml.pdp.application.guard;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import org.junit.jupiter.api.Test;
import org.onap.policy.pdp.xacml.application.common.ToscaPolicyConversionException;

class CoordinationGuardTranslatorTest {

    @Test
    void testUnsupportedMethods() {
        CoordinationGuardTranslator translator = new CoordinationGuardTranslator();

        assertThatExceptionOfType(ToscaPolicyConversionException.class)
            .isThrownBy(() -> translator.convertRequest(null))
            .withMessageContaining("this convertRequest shouldn't be used");

        assertThat(translator.convertResponse(null)).isNull();
    }

    @Test
    void testLoadingDirectives() {
        assertThat(CoordinationGuardTranslator.loadCoordinationDirectiveFromFile(null)).isNull();

        assertThat(CoordinationGuardTranslator.loadCoordinationDirectiveFromFile("nonexistent.yaml")).isNull();

        CoordinationDirective directive = CoordinationGuardTranslator
            .loadCoordinationDirectiveFromFile("src/test/resources/test-directive.yaml");
        assertThat(directive).isNotNull();
        assertThat(directive.getCoordinationFunction()).isEqualTo("whatisthisvaluesupposedtobe");
        assertThat(directive.getControlLoop()).hasSize(2);
        assertThat(directive.getControlLoop()).contains("cl1", "cl2");
    }

    @Test
    void testGeneratingXacml() {
        CoordinationDirective directive = CoordinationGuardTranslator
            .loadCoordinationDirectiveFromFile("src/test/resources/test-directive.yaml");

        assertThatExceptionOfType(ToscaPolicyConversionException.class)
            .isThrownBy(() -> CoordinationGuardTranslator
                .generateXacmlFromCoordinationDirective(directive, "idontexist.yaml"))
            .withMessageContaining("Unable to find prototype ");
    }

}
