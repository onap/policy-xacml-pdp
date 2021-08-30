/*-
 * ============LICENSE_START=======================================================
 * ONAP
 * ================================================================================
 * Copyright (C) 2021 AT&T Intellectual Property. All rights reserved.
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
 * ============LICENSE_END=========================================================
 */

package org.onap.policy.pdpx.main.rest;

import java.io.IOException;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Filter that verifies that the API services (i.e., decision services) are enabled
 * before allowing the request through.
 */
public class XacmlPdpServiceFilter implements Filter {

    /**
     * Services the are always available, even when the API is disabled.
     */
    public static final Set<String> PERMANENT_SERVICES = Set.of("healthcheck", "statistics", "metrics");


    private static final AtomicBoolean apiDisabled = new AtomicBoolean(true);


    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain)
                    throws IOException, ServletException {

        HttpServletRequest request = (HttpServletRequest) servletRequest;
        HttpServletResponse response = (HttpServletResponse) servletResponse;

        if (apiDisabled.get() && !PERMANENT_SERVICES.contains(getUriSuffix(request))) {
            response.setStatus(HttpServletResponse.SC_CONFLICT);
        } else {
            filterChain.doFilter(servletRequest, servletResponse);
        }
    }

    private String getUriSuffix(HttpServletRequest request) {
        String uri = request.getRequestURI();
        int index = uri.lastIndexOf('/');
        return (index < 0 ? uri : uri.substring(index + 1));
    }

    /**
     * Determines if API services are enabled.
     *
     * @return {@code true}, if API services are enabled
     */
    public static boolean isApiEnabled() {
        return !apiDisabled.get();
    }

    /**
     * Enables the API services.
     */
    public static void enableApi() {
        apiDisabled.set(false);
    }

    /**
     * Disables the API services.
     */
    public static void disableApi() {
        apiDisabled.set(true);
    }
}
