/*-
 * ============LICENSE_START=======================================================
 * Copyright (C) 2019, 2021 AT&T Intellectual Property. All rights reserved.
 * Modifications Copyright (C) 2019, 2023-2024 Nordix Foundation.
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.nio.file.Path;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.onap.policy.common.parameters.ValidationResult;
import org.onap.policy.common.parameters.rest.RestClientParameters;
import org.onap.policy.common.parameters.rest.RestServerParameters;
import org.onap.policy.common.parameters.topic.TopicParameterGroup;

/**
 * Class to perform unit test of XacmlPdpParameterGroup.
 */
class TestXacmlPdpParameterGroup {
    private static File applicationPath;
    private static final CommonTestData testData = new CommonTestData();

    @TempDir
    static Path applicationFolder;

    @BeforeEach
    void setupPath() {
        applicationPath = applicationFolder.toFile();
    }

    @Test
    void testXacmlPdpParameterGroup() {
        final RestServerParameters restServerParameters =
            testData.toObject(testData.getRestServerParametersMap(false), RestServerParameters.class);
        final RestClientParameters policyApiParameters =
            testData.toObject(testData.getPolicyApiParametersMap(false), RestClientParameters.class);
        final TopicParameterGroup topicParameterGroup =
            testData.toObject(testData.getTopicParametersMap(false), TopicParameterGroup.class);
        final XacmlApplicationParameters xacmlApplicationParameters =
            testData.toObject(testData.getXacmlapplicationParametersMap(false,
                applicationPath.getAbsolutePath()), XacmlApplicationParameters.class);
        final XacmlPdpParameterGroup pdpxParameters =
            new XacmlPdpParameterGroup(CommonTestData.PDPX_PARAMETER_GROUP_NAME,
                CommonTestData.PDPX_GROUP, "flavor", restServerParameters, policyApiParameters,
                topicParameterGroup, xacmlApplicationParameters);
        final ValidationResult validationResult = pdpxParameters.validate();
        assertThat(validationResult.getResult()).isNull();
        assertTrue(validationResult.isValid());
        assertEquals(restServerParameters.getHost(), pdpxParameters.getRestServerParameters().getHost());
        assertEquals(restServerParameters.getPort(), pdpxParameters.getRestServerParameters().getPort());
        assertEquals(restServerParameters.getUserName(), pdpxParameters.getRestServerParameters().getUserName());
        assertEquals(restServerParameters.getPassword(), pdpxParameters.getRestServerParameters().getPassword());
        assertEquals(CommonTestData.PDPX_PARAMETER_GROUP_NAME, pdpxParameters.getName());
        assertEquals("flavor", pdpxParameters.getPdpType());
        assertFalse(pdpxParameters.getRestServerParameters().isHttps());
        assertThat(pdpxParameters.getApplicationParameters().getExclusions()).isEmpty();
    }

    @Test
    void testXacmlPdpParameterGroup_NullParameterGroupName() {
        final RestServerParameters restServerParameters =
            testData.toObject(testData.getRestServerParametersMap(false), RestServerParameters.class);
        final RestClientParameters policyApiParameters =
            testData.toObject(testData.getPolicyApiParametersMap(false), RestClientParameters.class);
        final TopicParameterGroup topicParameterGroup =
            testData.toObject(testData.getTopicParametersMap(false), TopicParameterGroup.class);
        final XacmlApplicationParameters xacmlApplicationParameters =
            testData.toObject(testData.getXacmlapplicationParametersMap(false,
                applicationPath.getAbsolutePath()), XacmlApplicationParameters.class);
        final XacmlPdpParameterGroup pdpxParameters = new XacmlPdpParameterGroup(null, CommonTestData.PDPX_GROUP,
            null, restServerParameters, policyApiParameters, topicParameterGroup, xacmlApplicationParameters);
        final ValidationResult validationResult = pdpxParameters.validate();
        assertFalse(validationResult.isValid());
        assertNull(pdpxParameters.getName());
        assertThat(validationResult.getResult()).contains("\"name\" value \"null\" INVALID, is null");
    }

    @Test
    void testXacmlPdpParameterGroup_EmptyParameterGroupName() {
        final RestServerParameters restServerParameters =
            testData.toObject(testData.getRestServerParametersMap(false), RestServerParameters.class);
        final RestClientParameters policyApiParameters =
            testData.toObject(testData.getPolicyApiParametersMap(false), RestClientParameters.class);
        final TopicParameterGroup topicParameterGroup =
            testData.toObject(testData.getTopicParametersMap(false), TopicParameterGroup.class);
        final XacmlApplicationParameters xacmlApplicationParameters =
            testData.toObject(testData.getXacmlapplicationParametersMap(false,
                applicationPath.getAbsolutePath()), XacmlApplicationParameters.class);
        final XacmlPdpParameterGroup pdpxParameters = new XacmlPdpParameterGroup("", CommonTestData.PDPX_GROUP,
            CommonTestData.PDPX_TYPE, restServerParameters, policyApiParameters, topicParameterGroup,
            xacmlApplicationParameters);
        final ValidationResult validationResult = pdpxParameters.validate();
        assertFalse(validationResult.isValid());
        assertEquals("", pdpxParameters.getName());
        assertThat(validationResult.getResult()).contains("\"name\" value \"\" INVALID, is blank");
    }

    @Test
    void testXacmlPdpParameterGroup_NullPdpGroup() {
        final RestServerParameters restServerParameters =
            testData.toObject(testData.getRestServerParametersMap(false), RestServerParameters.class);
        final RestClientParameters policyApiParameters =
            testData.toObject(testData.getPolicyApiParametersMap(false), RestClientParameters.class);
        final TopicParameterGroup topicParameterGroup =
            testData.toObject(testData.getTopicParametersMap(false), TopicParameterGroup.class);
        final XacmlApplicationParameters xacmlApplicationParameters =
            testData.toObject(testData.getXacmlapplicationParametersMap(false,
                applicationPath.getAbsolutePath()), XacmlApplicationParameters.class);
        final XacmlPdpParameterGroup pdpxParameters =
            new XacmlPdpParameterGroup(CommonTestData.PDPX_PARAMETER_GROUP_NAME, null, null, restServerParameters,
                policyApiParameters, topicParameterGroup, xacmlApplicationParameters);
        final ValidationResult validationResult = pdpxParameters.validate();
        assertFalse(validationResult.isValid());
        assertNull(pdpxParameters.getPdpGroup());
        assertThat(validationResult.getResult()).contains("\"pdpGroup\" value \"null\" INVALID, is null");
    }

    @Test
    void testXacmlPdpParameterGroup_EmptyPdpGroup() {
        final RestServerParameters restServerParameters =
            testData.toObject(testData.getRestServerParametersMap(false), RestServerParameters.class);
        final RestClientParameters policyApiParameters =
            testData.toObject(testData.getPolicyApiParametersMap(false), RestClientParameters.class);
        final TopicParameterGroup topicParameterGroup =
            testData.toObject(testData.getTopicParametersMap(false), TopicParameterGroup.class);
        final XacmlApplicationParameters xacmlApplicationParameters =
            testData.toObject(testData.getXacmlapplicationParametersMap(false,
                applicationPath.getAbsolutePath()), XacmlApplicationParameters.class);
        final XacmlPdpParameterGroup pdpxParameters =
            new XacmlPdpParameterGroup(CommonTestData.PDPX_PARAMETER_GROUP_NAME, "", null, restServerParameters,
                policyApiParameters, topicParameterGroup, xacmlApplicationParameters);
        final ValidationResult validationResult = pdpxParameters.validate();
        assertFalse(validationResult.isValid());
        assertEquals("", pdpxParameters.getPdpGroup());
        assertThat(validationResult.getResult()).contains("\"pdpGroup\" value \"\" INVALID, is blank");
    }

    @Test
    void testXacmlPdpParameterGroup_EmptyRestServerParameters() {
        final RestServerParameters restServerParameters =
            testData.toObject(testData.getRestServerParametersMap(true), RestServerParameters.class);
        final RestClientParameters policyApiParameters =
            testData.toObject(testData.getPolicyApiParametersMap(false), RestClientParameters.class);
        final TopicParameterGroup topicParameterGroup =
            testData.toObject(testData.getTopicParametersMap(false), TopicParameterGroup.class);
        final XacmlApplicationParameters xacmlApplicationParameters =
            testData.toObject(testData.getXacmlapplicationParametersMap(false,
                applicationPath.getAbsolutePath()), XacmlApplicationParameters.class);
        final XacmlPdpParameterGroup pdpxParameters =
            new XacmlPdpParameterGroup(CommonTestData.PDPX_PARAMETER_GROUP_NAME, CommonTestData.PDPX_GROUP,
                null, restServerParameters, policyApiParameters,
                topicParameterGroup, xacmlApplicationParameters);
        final ValidationResult validationResult = pdpxParameters.validate();
        assertFalse(validationResult.isValid());
        assertThat(validationResult.getResult()).contains("\"RestServerParameters\"");
    }

    @Test
    void testXacmlPdpParameterGroup_EmptyPolicyApiParameters() {
        final RestServerParameters restServerParameters =
            testData.toObject(testData.getRestServerParametersMap(false), RestServerParameters.class);
        final RestClientParameters policyApiParameters =
            testData.toObject(testData.getPolicyApiParametersMap(true), RestClientParameters.class);
        final TopicParameterGroup topicParameterGroup =
            testData.toObject(testData.getTopicParametersMap(false), TopicParameterGroup.class);
        final XacmlApplicationParameters xacmlApplicationParameters =
            testData.toObject(testData.getXacmlapplicationParametersMap(false,
                applicationPath.getAbsolutePath()), XacmlApplicationParameters.class);
        final XacmlPdpParameterGroup pdpxParameters =
            new XacmlPdpParameterGroup(CommonTestData.PDPX_PARAMETER_GROUP_NAME, CommonTestData.PDPX_GROUP,
                null, restServerParameters, policyApiParameters,
                topicParameterGroup, xacmlApplicationParameters);
        final ValidationResult validationResult = pdpxParameters.validate();
        assertFalse(validationResult.isValid());
        assertThat(validationResult.getResult()).contains("\"policyApiParameters\"");
    }

    @Test
    void testXacmlPdpParameterGroup_EmptyTopicParameterGroup() {
        final RestServerParameters restServerParameters =
            testData.toObject(testData.getRestServerParametersMap(false), RestServerParameters.class);
        final RestClientParameters policyApiParameters =
            testData.toObject(testData.getPolicyApiParametersMap(false), RestClientParameters.class);
        final TopicParameterGroup topicParameterGroup =
            testData.toObject(testData.getTopicParametersMap(true), TopicParameterGroup.class);
        final XacmlApplicationParameters xacmlApplicationParameters =
            testData.toObject(testData.getXacmlapplicationParametersMap(false,
                applicationPath.getAbsolutePath()), XacmlApplicationParameters.class);
        final XacmlPdpParameterGroup pdpxParameters =
            new XacmlPdpParameterGroup(CommonTestData.PDPX_PARAMETER_GROUP_NAME, CommonTestData.PDPX_GROUP,
                null, restServerParameters, policyApiParameters,
                topicParameterGroup, xacmlApplicationParameters);
        final ValidationResult validationResult = pdpxParameters.validate();
        assertFalse(validationResult.isValid());
        assertThat(validationResult.getResult()).contains("\"TopicParameterGroup\"");
    }

    @Test
    void testXacmlPdpParameterGroup_EmptyApplicationParameterGroup() {
        final RestServerParameters restServerParameters =
            testData.toObject(testData.getRestServerParametersMap(false), RestServerParameters.class);
        final RestClientParameters policyApiParameters =
            testData.toObject(testData.getPolicyApiParametersMap(false), RestClientParameters.class);
        final TopicParameterGroup topicParameterGroup =
            testData.toObject(testData.getTopicParametersMap(false), TopicParameterGroup.class);
        final XacmlApplicationParameters xacmlApplicationParameters =
            testData.toObject(testData.getXacmlapplicationParametersMap(true,
                applicationPath.getAbsolutePath()), XacmlApplicationParameters.class);
        final XacmlPdpParameterGroup pdpxParameters =
            new XacmlPdpParameterGroup(CommonTestData.PDPX_PARAMETER_GROUP_NAME, CommonTestData.PDPX_GROUP,
                null, restServerParameters, policyApiParameters,
                topicParameterGroup, xacmlApplicationParameters);
        final ValidationResult validationResult = pdpxParameters.validate();
        assertFalse(validationResult.isValid());
        assertThat(validationResult.getResult()).contains("\"XacmlApplicationParameters\"");
    }
}
