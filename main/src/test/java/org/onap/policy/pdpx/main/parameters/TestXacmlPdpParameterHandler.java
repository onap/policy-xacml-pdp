/*-
 * ============LICENSE_START=======================================================
 * Copyright (C) 2019-2021 AT&T Intellectual Property. All rights reserved.
 * Modifications Copyright (C) 2020, 2024 Nordix Foundation
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
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import org.junit.jupiter.api.Test;
import org.onap.policy.pdpx.main.PolicyXacmlPdpException;
import org.onap.policy.pdpx.main.startstop.XacmlPdpCommandLineArguments;

/**
 * Class to perform unit test of XacmlPdpParameterHandler.
 */
class TestXacmlPdpParameterHandler {
    @Test
    void testParameterHandlerNoParameterFile() throws PolicyXacmlPdpException {
        final String[] noArgumentString = {"-c", "parameters/NoParameterFile.json"};

        final XacmlPdpCommandLineArguments noArguments = new XacmlPdpCommandLineArguments();
        noArguments.parse(noArgumentString);

        assertThatThrownBy(() -> new XacmlPdpParameterHandler().getParameters(noArguments))
            .isInstanceOf(PolicyXacmlPdpException.class);
    }

    @Test
    void testParameterHandlerEmptyParameters() throws PolicyXacmlPdpException {
        final String[] emptyArgumentString = {"-c", "parameters/EmptyParameters.json"};

        final XacmlPdpCommandLineArguments emptyArguments = new XacmlPdpCommandLineArguments();
        emptyArguments.parse(emptyArgumentString);

        assertThatThrownBy(() -> new XacmlPdpParameterHandler().getParameters(emptyArguments))
            .hasMessage("no parameters found in \"parameters/EmptyParameters.json\"");
    }

    @Test
    void testParameterHandlerBadParameters() throws PolicyXacmlPdpException {
        final String[] badArgumentString = {"-c", "parameters/BadParameters.json"};

        final XacmlPdpCommandLineArguments badArguments = new XacmlPdpCommandLineArguments();
        badArguments.parse(badArgumentString);

        assertThatThrownBy(() -> new XacmlPdpParameterHandler().getParameters(badArguments))
            .hasMessageContaining("error reading parameters from", "parameters/BadParameters.json",
                "JsonSyntaxException", "java.lang.IllegalStateException",
                "Expected a string but was BEGIN_ARRAY at line 2 column 14 path $.name");

    }

    @Test
    void testParameterHandlerInvalidParameters() throws PolicyXacmlPdpException {
        final String[] invalidArgumentString = {"-c", "parameters/InvalidParameters.json"};

        final XacmlPdpCommandLineArguments invalidArguments = new XacmlPdpCommandLineArguments();
        invalidArguments.parse(invalidArgumentString);

        assertThatThrownBy(() -> new XacmlPdpParameterHandler().getParameters(invalidArguments))
            .hasMessageContaining("error reading parameters from", "parameters/InvalidParameters.json",
                "JsonSyntaxException", "java.lang.IllegalStateException",
                "Expected a string but was BEGIN_ARRAY at line 2 column 14 path $.name");
    }

    @Test
    void testParameterHandlerNoParameters() throws PolicyXacmlPdpException {
        final String[] noArgumentString = {"-c", "parameters/NoParameters.json"};

        final XacmlPdpCommandLineArguments noArguments = new XacmlPdpCommandLineArguments();
        noArguments.parse(noArgumentString);

        assertThatThrownBy(() -> new XacmlPdpParameterHandler().getParameters(noArguments))
            .hasMessageContaining("validation error(s) on parameters from \"parameters/NoParameters.json\"",
                "\"XacmlPdpParameterGroup\" INVALID, item has status INVALID",
                "\"name\" value \"null\" INVALID, is null",
                "\"pdpGroup\" value \"null\" INVALID, is null",
                "\"pdpType\" value \"null\" INVALID, is null",
                "\"applicationPath\" value \"null\" INVALID, is null");
    }

    @Test
    void testParameterHandlerMinumumParameters() throws PolicyXacmlPdpException {
        final String[] minArgumentString = {"-c", "parameters/MinimumParameters.json"};

        final XacmlPdpCommandLineArguments minArguments = new XacmlPdpCommandLineArguments();
        minArguments.parse(minArgumentString);

        final XacmlPdpParameterGroup parGroup = new XacmlPdpParameterHandler().getParameters(minArguments);
        assertEquals(CommonTestData.PDPX_PARAMETER_GROUP_NAME, parGroup.getName());
        assertEquals(CommonTestData.PDPX_GROUP, parGroup.getPdpGroup());
        assertEquals(CommonTestData.PDPX_TYPE, parGroup.getPdpType());
    }

    @Test
    void testXacmlPdpParameterGroup() throws PolicyXacmlPdpException {
        final String[] xacmlPdpConfigParameters = {"-c", "parameters/XacmlPdpConfigParameters.json"};

        final XacmlPdpCommandLineArguments arguments = new XacmlPdpCommandLineArguments();
        arguments.parse(xacmlPdpConfigParameters);

        final XacmlPdpParameterGroup parGroup = new XacmlPdpParameterHandler().getParameters(arguments);
        assertTrue(arguments.checkSetConfigurationFilePath());
        assertEquals(CommonTestData.PDPX_PARAMETER_GROUP_NAME, parGroup.getName());
        assertEquals(CommonTestData.PDPX_GROUP, parGroup.getPdpGroup());
    }

    @Test
    void testXacmlPdpParameterGroup_Invalid() throws PolicyXacmlPdpException {
        final String[] xacmlPdpConfigParameters = {"-c", "parameters/XacmlPdpConfigParameters_InvalidName.json"};

        final XacmlPdpCommandLineArguments arguments = new XacmlPdpCommandLineArguments();
        arguments.parse(xacmlPdpConfigParameters);

        assertThatThrownBy(() -> new XacmlPdpParameterHandler().getParameters(arguments)).hasMessageContaining(
            "\"name\" value \" \" INVALID, is blank");
        xacmlPdpConfigParameters[1] = "parameters/XacmlPdpConfigParameters_InvalidPdpGroup.json";

        arguments.parse(xacmlPdpConfigParameters);

        assertThatThrownBy(() -> new XacmlPdpParameterHandler().getParameters(arguments)).hasMessageContaining(
            "\"pdpGroup\" value \" \" INVALID, is blank");

        xacmlPdpConfigParameters[1] = "parameters/XacmlPdpConfigParameters_InvalidPdpType.json";

        arguments.parse(xacmlPdpConfigParameters);

        assertThatThrownBy(() -> new XacmlPdpParameterHandler().getParameters(arguments)).hasMessageContaining(
            "\"pdpType\" value \"\" INVALID, is blank");
    }

    @Test
    void testXacmlPdpParameterGroup_InvalidRestServerParameters() throws PolicyXacmlPdpException, IOException {
        final String[] xacmlPdpConfigParameters =
            {"-c", "parameters/XacmlPdpConfigParameters_InvalidRestServerParameters.json"};

        final XacmlPdpCommandLineArguments arguments = new XacmlPdpCommandLineArguments();
        arguments.parse(xacmlPdpConfigParameters);

        new String(Files.readAllBytes(
            Paths.get("src/test/resources/expectedValidationResults/InvalidRestServerParameters.txt")));

        assertThatThrownBy(() -> new XacmlPdpParameterHandler().getParameters(arguments))
            .hasMessageContaining("validation error(s) on parameters from "
                + "\"parameters/XacmlPdpConfigParameters_InvalidRestServerParameters.json\"");
    }

    @Test
    void testXacmlPdpParameterGroup_Exclusions() throws PolicyXacmlPdpException {
        final String[] xacmlPdpConfigParameters = {"-c", "parameters/XacmlPdpConfigParameters_Exclusions.json"};

        final XacmlPdpCommandLineArguments arguments = new XacmlPdpCommandLineArguments();
        arguments.parse(xacmlPdpConfigParameters);

        final XacmlPdpParameterGroup parGroup = new XacmlPdpParameterHandler().getParameters(arguments);
        assertTrue(arguments.checkSetConfigurationFilePath());
        assertEquals(CommonTestData.PDPX_PARAMETER_GROUP_NAME, parGroup.getName());
        assertEquals(CommonTestData.PDPX_GROUP, parGroup.getPdpGroup());
        assertThat(parGroup.getApplicationParameters().getExclusions()).hasSize(2);
    }

    @Test
    void testXacmlPdpVersion() throws PolicyXacmlPdpException {
        final String[] xacmlPdpConfigParameters = {"-v"};
        final XacmlPdpCommandLineArguments arguments = new XacmlPdpCommandLineArguments();
        final String version = arguments.parse(xacmlPdpConfigParameters);
        assertTrue(version.startsWith("ONAP Policy Framework Xacml PDP Service"));
    }

    @Test
    void testXacmlPdpHelp() throws PolicyXacmlPdpException {
        final String[] xacmlPdpConfigParameters = {"-h"};
        final XacmlPdpCommandLineArguments arguments = new XacmlPdpCommandLineArguments();
        final String help = arguments.parse(xacmlPdpConfigParameters);
        assertTrue(help.startsWith("usage:"));
    }

    @Test
    void testXacmlPdpInvalidOption() {
        final String[] xacmlPdpConfigParameters = {"-d"};
        final XacmlPdpCommandLineArguments arguments = new XacmlPdpCommandLineArguments();
        assertThatThrownBy(() ->
            arguments.parse(xacmlPdpConfigParameters)
        ).isInstanceOf(PolicyXacmlPdpException.class)
            .hasMessageContaining("invalid command line arguments specified");
    }
}
