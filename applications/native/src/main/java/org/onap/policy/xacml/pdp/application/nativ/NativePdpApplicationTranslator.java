/*-
 * ============LICENSE_START=======================================================
 * ONAP
 * ================================================================================
 * Copyright (C) 2020 AT&T Intellectual Property. All rights reserved.
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

import com.att.research.xacml.api.Decision;
import com.att.research.xacml.api.Request;
import com.att.research.xacml.api.Response;
import com.att.research.xacml.std.dom.DOMRequest;
import com.att.research.xacml.std.dom.DOMResponse;
import com.att.research.xacml.std.json.JsonRequestTranslator;
import com.att.research.xacml.std.json.JsonResponseTranslator;
import com.att.research.xacml.util.XACMLPolicyScanner;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import oasis.names.tc.xacml._3_0.core.schema.wd_17.PolicyType;
import org.apache.http.entity.ContentType;
import org.onap.policy.models.decisions.concepts.DecisionRequest;
import org.onap.policy.models.decisions.concepts.DecisionResponse;
import org.onap.policy.models.tosca.authorative.concepts.ToscaPolicy;
import org.onap.policy.pdp.xacml.application.common.ToscaPolicyConversionException;
import org.onap.policy.pdp.xacml.application.common.ToscaPolicyTranslator;
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
    private static final String POLICY = "policy";
    private static final String APPLICATION_XACML_XML = "application/xacml+xml";

    public NativePdpApplicationTranslator() {
        super();
    }

    @Override
    public PolicyType convertPolicy(ToscaPolicy toscaPolicy) throws ToscaPolicyConversionException {

        // Extract the URL encoded policy xml string and decode it
        String encodedXacmlPolicy = getNativeXacmlPolicy(toscaPolicy);
        String decodedXacmlPolicy;
        try {
            decodedXacmlPolicy = URLDecoder.decode(encodedXacmlPolicy, StandardCharsets.UTF_8.toString());
        } catch (UnsupportedEncodingException exc) {
            throw new ToscaPolicyConversionException("error on URL decoding the native policy", exc);
        }

        LOGGER.debug("Decoded xacml policy {}",decodedXacmlPolicy);

        // Scan the string and convert to xacml PolicyType
        try (InputStream is = new ByteArrayInputStream(decodedXacmlPolicy.getBytes(StandardCharsets.UTF_8))) {
            return (PolicyType) XACMLPolicyScanner.readPolicy(is);
        } catch (IOException exc) {
            throw new ToscaPolicyConversionException("Failed to read policy", exc);
        }
    }

    /**
     * Converts the incoming request string to a xacml request.
     * @param contentType the content type of the request
     * @param incomingRequeString the incoming request string
     * @return the xacml request
     */
    public Request convertRequest(ContentType contentType, String incomingRequeString) {

        LOGGER.info("Converting request {}", incomingRequeString);
        if (contentType.getMimeType().equalsIgnoreCase(ContentType.APPLICATION_JSON.getMimeType())) {
            return JsonRequestTranslator.load(incomingRequeString);
        } else if (contentType.getMimeType().equalsIgnoreCase(APPLICATION_XACML_XML)) {
            return DOMRequest.load(incomingRequeString);
        }
    }

    /**
     * Converts the xacml response to a pretty print string based on the content type.
     * @param contentType the content type of the response
     * @param xacmlResponse the xacml native response
     * @return the response in a pretty print string
     */
    public String convertResponse(ContentType contentType, Response xacmlResponse) {

        LOGGER.info("Converting response {}", xacmlResponse);
        if (contentType.getMimeType().equalsIgnoreCase(ContentType.APPLICATION_JSON.getMimeType())) {
            return JsonResponseTranslator.toString(xacmlResponse, true);
        } else if (contentType.getMimeType().equalsIgnoreCase(APPLICATION_XACML_XML)) {
            return DOMResponse.toString(xacmlResponse, true);
        }
    }

    private String getNativeXacmlPolicy(ToscaPolicy toscaPolicy) throws ToscaPolicyConversionException {

        Map<String, Object> propertyMap = toscaPolicy.getProperties();
        if (propertyMap.isEmpty() || !propertyMap.containsKey(POLICY)) {
            throw new ToscaPolicyConversionException("no xacml native policy found in the tosca policy");
        }

        String nativePolicyString = (String) propertyMap.get(POLICY);
        LOGGER.debug("URL encoded native xacml policy {}", nativePolicyString);
        return nativePolicyString;
    }

    @Override
    public Request convertRequest(DecisionRequest request) {
        //
        // We do nothing to DecisionRequest for native xacml application
        //
        return null;
    }

    @Override
    public DecisionResponse convertResponse(Response xacmlResponse) {
        //
        // We do nothing to DecisionResponse for native xacml application
        //
        return null;
    }
}
