/*-
 * ============LICENSE_START=======================================================
 * ONAP
 * ================================================================================
 * Copyright (C) 2020-2021 AT&T Intellectual Property. All rights reserved.
 * Modifications Copyright (C) 2020 Nordix Foundation.
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

package org.onap.policy.xacml.pdp.application.nativ;

import com.att.research.xacml.api.Request;
import com.att.research.xacml.api.Response;
import com.att.research.xacml.util.XACMLPolicyScanner;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import lombok.Getter;
import org.onap.policy.common.parameters.annotations.NotBlank;
import org.onap.policy.common.parameters.annotations.NotNull;
import org.onap.policy.models.decisions.concepts.DecisionRequest;
import org.onap.policy.models.decisions.concepts.DecisionResponse;
import org.onap.policy.models.tosca.authorative.concepts.ToscaPolicy;
import org.onap.policy.pdp.xacml.application.common.ToscaPolicyConversionException;
import org.onap.policy.pdp.xacml.application.common.ToscaPolicyTranslator;
import org.onap.policy.pdp.xacml.application.common.ToscaPolicyTranslatorUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class implements one translator that interprets TOSCA policy and decision API request/response payload.
 *
 * @author Chenfei Gao (cgao@research.att.com)
 *
 */
public class NativePdpApplicationTranslator implements ToscaPolicyTranslator {

    private static final Logger LOGGER = LoggerFactory.getLogger(NativePdpApplicationTranslator.class);

    public NativePdpApplicationTranslator() {
        super();
    }

    @Override
    public Object convertPolicy(ToscaPolicy toscaPolicy) throws ToscaPolicyConversionException {
        //
        // Extract the Base64 encoded policy xml string and decode it
        //
        String encodedXacmlPolicy = getNativeXacmlPolicy(toscaPolicy);
        String decodedXacmlPolicy;
        try {
            decodedXacmlPolicy = new String(Base64.getDecoder().decode(encodedXacmlPolicy), StandardCharsets.UTF_8);
        } catch (IllegalArgumentException exc) {
            throw new ToscaPolicyConversionException("error on Base64 decoding the native policy", exc);
        }
        LOGGER.debug("Decoded xacml policy {}", decodedXacmlPolicy);
        //
        // Scan the string and convert to xacml PolicyType
        //
        try (var is = new ByteArrayInputStream(decodedXacmlPolicy.getBytes(StandardCharsets.UTF_8))) {
            //
            // Read the Policy In
            //
            Object policy = XACMLPolicyScanner.readPolicy(is);
            if (policy == null) {
                throw new ToscaPolicyConversionException("Invalid XACML Policy");
            }
            return policy;
        } catch (IOException exc) {
            throw new ToscaPolicyConversionException("Failed to read policy", exc);
        }
    }

    private String getNativeXacmlPolicy(ToscaPolicy toscaPolicy) throws ToscaPolicyConversionException {

        NativeDefinition nativeDefinition = ToscaPolicyTranslatorUtils.decodeProperties(toscaPolicy.getProperties(),
                        NativeDefinition.class);

        LOGGER.debug("Base64 encoded native xacml policy {}", nativeDefinition.getPolicy());
        return nativeDefinition.getPolicy();
    }

    @Override
    public Request convertRequest(DecisionRequest request) throws ToscaPolicyConversionException {
        throw new ToscaPolicyConversionException("Do not call native convertRequest");
    }

    @Override
    public DecisionResponse convertResponse(Response xacmlResponse) {
        //
        // We do nothing to DecisionResponse for native xacml application
        //
        return null;
    }

    @Getter
    public static class NativeDefinition {
        @NotNull
        @NotBlank
        private String policy;
    }
}
