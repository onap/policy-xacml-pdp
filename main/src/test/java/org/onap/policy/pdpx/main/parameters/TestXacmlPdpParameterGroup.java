/*-
 * ============LICENSE_START=======================================================
 * Copyright (C) 2019 AT&T Intellectual Property. All rights reserved.
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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.onap.policy.common.parameters.GroupValidationResult;

/**
 * Class to perform unit test of XacmlPdpParameterGroup.
 *
 */
public class TestXacmlPdpParameterGroup {
    CommonTestData commonTestData = new CommonTestData();

    @Test
    public void testXacmlPdpParameterGroup() {
        final RestServerParameters restServerParameters = commonTestData.getRestServerParameters(false);
        final XacmlPdpParameterGroup pdpxParameters =
                new XacmlPdpParameterGroup(CommonTestData.PDPX_GROUP_NAME, restServerParameters);
        final GroupValidationResult validationResult = pdpxParameters.validate();
        assertTrue(validationResult.isValid());
        assertEquals(restServerParameters.getHost(), pdpxParameters.getRestServerParameters().getHost());
        assertEquals(restServerParameters.getPort(), pdpxParameters.getRestServerParameters().getPort());
        assertEquals(restServerParameters.getUserName(), pdpxParameters.getRestServerParameters().getUserName());
        assertEquals(restServerParameters.getPassword(), pdpxParameters.getRestServerParameters().getPassword());
        assertEquals(CommonTestData.PDPX_GROUP_NAME, pdpxParameters.getName());
        assertFalse(pdpxParameters.getRestServerParameters().isHttps());
        assertFalse(pdpxParameters.getRestServerParameters().isAaf());
    }

    @Test
    public void testXacmlPdpParameterGroup_NullName() {
        final RestServerParameters restServerParameters = commonTestData.getRestServerParameters(false);
        final XacmlPdpParameterGroup pdpxParameters = new XacmlPdpParameterGroup(null, restServerParameters);
        final GroupValidationResult validationResult = pdpxParameters.validate();
        assertFalse(validationResult.isValid());
        assertEquals(null, pdpxParameters.getName());
        assertTrue(validationResult.getResult().contains(
                "field \"name\" type \"java.lang.String\" value \"null\" INVALID, " + "must be a non-blank string"));
    }

    @Test
    public void testXacmlPdpParameterGroup_EmptyName() {
        final RestServerParameters restServerParameters = commonTestData.getRestServerParameters(false);

        final XacmlPdpParameterGroup pdpxParameters = new XacmlPdpParameterGroup("", restServerParameters);
        final GroupValidationResult validationResult = pdpxParameters.validate();
        assertFalse(validationResult.isValid());
        assertEquals("", pdpxParameters.getName());
        assertTrue(validationResult.getResult().contains(
                "field \"name\" type \"java.lang.String\" value \"\" INVALID, " + "must be a non-blank string"));
    }

    @Test
    public void testXacmlPdpParameterGroup_EmptyRestServerParameters() {
        final RestServerParameters restServerParameters = commonTestData.getRestServerParameters(true);

        final XacmlPdpParameterGroup pdpxParameters =
                new XacmlPdpParameterGroup(CommonTestData.PDPX_GROUP_NAME, restServerParameters);
        final GroupValidationResult validationResult = pdpxParameters.validate();
        assertFalse(validationResult.isValid());
        assertTrue(validationResult.getResult()
                .contains("\"org.onap.policy.pdpx.main.parameters.RestServerParameters\" INVALID, "
                        + "parameter group has status INVALID"));
    }
}
