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

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import org.junit.Test;
import org.onap.policy.pdpx.main.PolicyXacmlPdpException;
import org.onap.policy.pdpx.main.startstop.XacmlPdpCommandLineArguments;

/**
 * Class to perform unit test of XacmlPdpParameterHandler.
 *
 */
public class TestXacmlPdpParameterHandler {
    @Test
    public void testParameterHandlerNoParameterFile() throws PolicyXacmlPdpException {
        final String[] noArgumentString = {"-c", "parameters/NoParameterFile.json"};

        final XacmlPdpCommandLineArguments noArguments = new XacmlPdpCommandLineArguments();
        noArguments.parse(noArgumentString);

        assertThatThrownBy(() -> new XacmlPdpParameterHandler().getParameters(noArguments))
                .isInstanceOf(PolicyXacmlPdpException.class);
    }

    @Test
    public void testParameterHandlerEmptyParameters() throws PolicyXacmlPdpException {
        final String[] emptyArgumentString = {"-c", "parameters/EmptyParameters.json"};

        final XacmlPdpCommandLineArguments emptyArguments = new XacmlPdpCommandLineArguments();
        emptyArguments.parse(emptyArgumentString);

        assertThatThrownBy(() -> new XacmlPdpParameterHandler().getParameters(emptyArguments))
                .hasMessage("no parameters found in \"parameters/EmptyParameters.json\"");
    }

    @Test
    public void testParameterHandlerBadParameters() throws PolicyXacmlPdpException {
        final String[] badArgumentString = {"-c", "parameters/BadParameters.json"};

        final XacmlPdpCommandLineArguments badArguments = new XacmlPdpCommandLineArguments();
        badArguments.parse(badArgumentString);

        assertThatThrownBy(() -> new XacmlPdpParameterHandler().getParameters(badArguments))
                .hasMessage("error reading parameters from \"parameters/BadParameters.json\"\n"
                        + "(JsonSyntaxException):java.lang.IllegalStateException: "
                        + "Expected a string but was BEGIN_ARRAY at line 2 column 14 path $.name");

    }

    @Test
    public void testParameterHandlerInvalidParameters() throws PolicyXacmlPdpException {
        final String[] invalidArgumentString = {"-c", "parameters/InvalidParameters.json"};

        final XacmlPdpCommandLineArguments invalidArguments = new XacmlPdpCommandLineArguments();
        invalidArguments.parse(invalidArgumentString);

        assertThatThrownBy(() -> new XacmlPdpParameterHandler().getParameters(invalidArguments))
                .hasMessage("error reading parameters from \"parameters/InvalidParameters.json\"\n"
                        + "(JsonSyntaxException):java.lang.IllegalStateException: "
                        + "Expected a string but was BEGIN_ARRAY at line 2 column 14 path $.name");
    }

    @Test
    public void testParameterHandlerNoParameters() throws PolicyXacmlPdpException {
        final String[] noArgumentString = {"-c", "parameters/NoParameters.json"};

        final XacmlPdpCommandLineArguments noArguments = new XacmlPdpCommandLineArguments();
        noArguments.parse(noArgumentString);

        assertThatThrownBy(() -> new XacmlPdpParameterHandler().getParameters(noArguments))
                .hasMessage("validation error(s) on parameters from \"parameters/NoParameters.json\"\n"
                        + "parameter group \"null\" type "
                        + "\"org.onap.policy.pdpx.main.parameters.XacmlPdpParameterGroup\""
                        + " INVALID, parameter group has status INVALID\n"
                        + "  field \"name\" type \"java.lang.String\" value \"null\" INVALID, "
                        + "must be a non-blank string\n"
                        + "  field \"pdpGroup\" type \"java.lang.String\" value \"null\" INVALID, "
                        + "must be a non-blank string\n"
                        + "  field \"applicationPath\" type \"java.lang.String\" value \"null\" INVALID, "
                        + "must have application path for applications to store policies and data.\n");
    }

    @Test
    public void testParameterHandlerMinumumParameters() throws PolicyXacmlPdpException {
        final String[] minArgumentString = {"-c", "parameters/MinimumParameters.json"};

        final XacmlPdpCommandLineArguments minArguments = new XacmlPdpCommandLineArguments();
        minArguments.parse(minArgumentString);

        final XacmlPdpParameterGroup parGroup = new XacmlPdpParameterHandler().getParameters(minArguments);
        assertEquals(CommonTestData.PDPX_PARAMETER_GROUP_NAME, parGroup.getName());
        assertEquals(CommonTestData.PDPX_GROUP, parGroup.getPdpGroup());
    }

    @Test
    public void testXacmlPdpParameterGroup() throws PolicyXacmlPdpException {
        final String[] xacmlPdpConfigParameters = {"-c", "parameters/XacmlPdpConfigParameters.json"};

        final XacmlPdpCommandLineArguments arguments = new XacmlPdpCommandLineArguments();
        arguments.parse(xacmlPdpConfigParameters);

        final XacmlPdpParameterGroup parGroup = new XacmlPdpParameterHandler().getParameters(arguments);
        assertTrue(arguments.checkSetConfigurationFilePath());
        assertEquals(CommonTestData.PDPX_PARAMETER_GROUP_NAME, parGroup.getName());
        assertEquals(CommonTestData.PDPX_GROUP, parGroup.getPdpGroup());
    }

    @Test
    public void testXacmlPdpParameterGroup_InvalidName() throws PolicyXacmlPdpException {
        final String[] xacmlPdpConfigParameters = {"-c", "parameters/XacmlPdpConfigParameters_InvalidName.json"};

        final XacmlPdpCommandLineArguments arguments = new XacmlPdpCommandLineArguments();
        arguments.parse(xacmlPdpConfigParameters);

        assertThatThrownBy(() -> new XacmlPdpParameterHandler().getParameters(arguments)).hasMessageContaining(
                "field \"name\" type \"java.lang.String\" value \" \" INVALID, must be a non-blank string");
    }

    @Test
    public void testXacmlPdpParameterGroup_InvalidPdpGroup() throws PolicyXacmlPdpException {
        final String[] xacmlPdpConfigParameters = {"-c", "parameters/XacmlPdpConfigParameters_InvalidPdpGroup.json"};

        final XacmlPdpCommandLineArguments arguments = new XacmlPdpCommandLineArguments();
        arguments.parse(xacmlPdpConfigParameters);

        assertThatThrownBy(() -> new XacmlPdpParameterHandler().getParameters(arguments)).hasMessageContaining(
                "field \"pdpGroup\" type \"java.lang.String\" value \" \" INVALID, must be a non-blank string");
    }

    @Test
    public void testXacmlPdpParameterGroup_InvalidRestServerParameters() throws PolicyXacmlPdpException, IOException {
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
    public void testXacmlPdpVersion() throws PolicyXacmlPdpException {
        final String[] xacmlPdpConfigParameters = {"-v"};
        final XacmlPdpCommandLineArguments arguments = new XacmlPdpCommandLineArguments();
        final String version = arguments.parse(xacmlPdpConfigParameters);
        assertTrue(version.startsWith("ONAP Policy Framework Xacml PDP Service"));
    }

    @Test
    public void testXacmlPdpHelp() throws PolicyXacmlPdpException {
        final String[] xacmlPdpConfigParameters = {"-h"};
        final XacmlPdpCommandLineArguments arguments = new XacmlPdpCommandLineArguments();
        final String help = arguments.parse(xacmlPdpConfigParameters);
        assertTrue(help.startsWith("usage:"));
    }

    @Test
    public void testXacmlPdpInvalidOption() throws PolicyXacmlPdpException {
        final String[] xacmlPdpConfigParameters = {"-d"};
        final XacmlPdpCommandLineArguments arguments = new XacmlPdpCommandLineArguments();
        try {
            arguments.parse(xacmlPdpConfigParameters);
        } catch (final Exception exp) {
            assertTrue(exp.getMessage().startsWith("invalid command line arguments specified"));
        }
    }
}
