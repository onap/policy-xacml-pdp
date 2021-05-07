/*-
 * ============LICENSE_START=======================================================
 * Copyright (C) 2020-2021 AT&T Intellectual Property. All rights reserved.
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

package org.onap.policy.pdpx.main.startstop;

import java.util.Properties;
import org.onap.policy.common.endpoints.http.server.JsonExceptionMapper;
import org.onap.policy.common.endpoints.http.server.RestServer;
import org.onap.policy.common.endpoints.http.server.YamlExceptionMapper;
import org.onap.policy.common.endpoints.http.server.YamlMessageBodyHandler;
import org.onap.policy.common.endpoints.http.server.aaf.AafAuthFilter;
import org.onap.policy.common.endpoints.parameters.RestServerParameters;
import org.onap.policy.common.endpoints.properties.PolicyEndPointProperties;
import org.onap.policy.common.gson.GsonMessageBodyHandler;
import org.onap.policy.pdpx.main.rest.serialization.XacmlJsonExceptionMapper;
import org.onap.policy.pdpx.main.rest.serialization.XacmlJsonMessageBodyHandler;
import org.onap.policy.pdpx.main.rest.serialization.XacmlXmlExceptionMapper;
import org.onap.policy.pdpx.main.rest.serialization.XacmlXmlMessageBodyHandler;

/**
 * Class to manage life cycle of a rest server that is particularly used by xacml pdp.
 *
 * @author Chenfei Gao (cgao@research.att.com)
 */
public class XacmlPdpRestServer extends RestServer {

    /**
     * Constructs the object.
     *
     * @param restServerParameters the rest server parameters
     * @param aafFilter class of object to use to filter AAF requests, or {@code null}
     * @param jaxrsProviders classes providing the services
     */
    public XacmlPdpRestServer(final RestServerParameters restServerParameters,
            Class<? extends AafAuthFilter> aafFilter, Class<?>... jaxrsProviders) {

        super(restServerParameters, aafFilter, jaxrsProviders);
    }

    @Override
    protected Properties getServerProperties(RestServerParameters restServerParameters, String names) {

        final var props = super.getServerProperties(restServerParameters, names);
        final String svcpfx =
                        PolicyEndPointProperties.PROPERTY_HTTP_SERVER_SERVICES + "." + restServerParameters.getName();

        props.setProperty(svcpfx + PolicyEndPointProperties.PROPERTY_HTTP_SERIALIZATION_PROVIDER,
                String.join(",", GsonMessageBodyHandler.class.getName(), YamlMessageBodyHandler.class.getName(),
                                JsonExceptionMapper.class.getName(), YamlExceptionMapper.class.getName(),
                                XacmlJsonMessageBodyHandler.class.getName(), XacmlJsonExceptionMapper.class.getName(),
                                XacmlXmlMessageBodyHandler.class.getName(), XacmlXmlExceptionMapper.class.getName()));
        return props;
    }
}
