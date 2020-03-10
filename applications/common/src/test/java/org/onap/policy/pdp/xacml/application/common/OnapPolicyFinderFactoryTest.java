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
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import java.io.FileInputStream;
import java.util.Properties;
import org.junit.Test;

public class OnapPolicyFinderFactoryTest {

    @Test
    public void testNoUseConstructor() throws Exception {
        assertThatExceptionOfType(OnapPolicyFinderFactoryException.class).isThrownBy(() -> {
            new OnapPolicyFinderFactory();
        }).withMessageContaining("Please use the constructor with Properties object.");
    }

    @Test
    public void testFinder() throws Exception {
        //
        // Load our test properties to use
        //
        Properties properties = new Properties();
        try (FileInputStream is = new FileInputStream("src/test/resources/finder.test.properties")) {
            properties.load(is);
        }
        OnapPolicyFinderFactory finder = new OnapPolicyFinderFactory(properties);
        assertThat(finder).isNotNull();

        assertThat(finder.getPolicyFinder()).isNotNull();
        assertThat(finder.getPolicyFinder(properties)).isNotNull();
    }

    @Test
    public void testFinderWithCombiningAlgorithm() throws Exception {
        //
        // Load our test properties to use
        //
        Properties properties = new Properties();
        try (FileInputStream is = new FileInputStream("src/test/resources/finder.test.properties")) {
            properties.load(is);
        }
        //
        // Set a combining algorithm
        //
        properties.put("xacml.att.policyFinderFactory.combineRootPolicies",
                "urn:com:att:xacml:3.0:policy-combining-algorithm:combined-permit-overrides");
        OnapPolicyFinderFactory finder = new OnapPolicyFinderFactory(properties);
        assertThat(finder).isNotNull();
    }

}
