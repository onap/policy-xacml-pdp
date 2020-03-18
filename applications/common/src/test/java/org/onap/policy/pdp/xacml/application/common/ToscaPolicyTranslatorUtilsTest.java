/*-
 * ============LICENSE_START=======================================================
 * ONAP
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
 *
 * SPDX-License-Identifier: Apache-2.0
 * ============LICENSE_END=========================================================
 */

package org.onap.policy.pdp.xacml.application.common;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertTrue;

import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;
import oasis.names.tc.xacml._3_0.core.schema.wd_17.AllOfType;
import oasis.names.tc.xacml._3_0.core.schema.wd_17.AnyOfType;
import oasis.names.tc.xacml._3_0.core.schema.wd_17.MatchType;
import oasis.names.tc.xacml._3_0.core.schema.wd_17.TargetType;
import org.junit.Test;

public class ToscaPolicyTranslatorUtilsTest {

    @Test
    public void test() throws NoSuchMethodException, SecurityException {
        final Constructor<ToscaPolicyTranslatorUtils> constructor
            = ToscaPolicyTranslatorUtils.class.getDeclaredConstructor();
        assertTrue(Modifier.isPrivate(constructor.getModifiers()));

    }

    @Test
    public void testTimeInRange() {
        assertThat(ToscaPolicyTranslatorUtils.generateTimeInRange("T00:00:00Z", "T08:00:00Z")).isNotNull();
    }

    @Test
    public void testBuildAndAppend() {
        assertThat(ToscaPolicyTranslatorUtils.buildAndAppendAllof(null, new MatchType())).isInstanceOf(AnyOfType.class);
        assertThat(ToscaPolicyTranslatorUtils.buildAndAppendAllof(null, new AllOfType())).isInstanceOf(AnyOfType.class);
        assertThat(ToscaPolicyTranslatorUtils.buildAndAppendAllof(null, new String())).isNull();

        assertThat(ToscaPolicyTranslatorUtils.buildAndAppendTarget(new TargetType(),
                new AnyOfType()).getAnyOf()).hasSize(1);
        assertThat(ToscaPolicyTranslatorUtils.buildAndAppendTarget(new TargetType(),
                new MatchType()).getAnyOf()).hasSize(1);
        assertThat(ToscaPolicyTranslatorUtils.buildAndAppendTarget(new TargetType(),
                new String()).getAnyOf()).isEmpty();
    }

    @Test
    public void testInteger() {
        assertThat(ToscaPolicyTranslatorUtils.parseInteger("foo")).isNull();
        assertThat(ToscaPolicyTranslatorUtils.parseInteger("1")).isEqualTo(1);
        assertThat(ToscaPolicyTranslatorUtils.parseInteger("1.0")).isEqualTo(1);
    }
}
