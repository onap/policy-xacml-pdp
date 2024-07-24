/*-
 * ============LICENSE_START=======================================================
 * ONAP
 * ================================================================================
 * Copyright (C) 2021 AT&T Intellectual Property. All rights reserved.
 * Modifications Copyright (C) 2023-2024 Nordix Foundation.
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TestXacmlPdpServiceFilter {

    // pick an arbitrary service
    private static final String PERM_SVC = XacmlPdpServiceFilter.PERMANENT_SERVICES.iterator().next();

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    private FilterChain filterChain;

    private XacmlPdpServiceFilter filter;


    /**
     * Initializes the fields.
     */
    @BeforeEach
    void setUp() {
        XacmlPdpServiceFilter.disableApi();

        filterChain = (req, resp) -> {
            HttpServletResponse resp2 = (HttpServletResponse) resp;
            resp2.setStatus(HttpServletResponse.SC_OK);
        };

        filter = new XacmlPdpServiceFilter();
    }

    @Test
    void testDoFilter() throws Exception {
        XacmlPdpServiceFilter.enableApi();
        lenient().when(request.getRequestURI()).thenReturn("/other");
        assertThat(getFilterResponse()).isEqualTo(HttpServletResponse.SC_OK);
    }

    /**
     * Tests doFilter() when the API is disabled, but a permanent service is requested.
     */
    @Test
    void testDoFilter_DisabledPermanentServiceReq() throws Exception {
        XacmlPdpServiceFilter.disableApi();
        when(request.getRequestURI()).thenReturn(PERM_SVC);
        assertThat(getFilterResponse()).isEqualTo(HttpServletResponse.SC_OK);
    }

    /**
     * Tests doFilter() when the API is disabled, but a permanent service is requested, with a leading slash.
     */
    @Test
    void testDoFilter_DisabledPermanentServiceReqLeadingSlash() throws Exception {
        XacmlPdpServiceFilter.disableApi();
        when(request.getRequestURI()).thenReturn("/" + PERM_SVC);
        assertThat(getFilterResponse()).isEqualTo(HttpServletResponse.SC_OK);
    }

    /**
     * Tests doFilter() when the API is disabled, but a permanent service is requested, with extra URI prefix.
     */
    @Test
    void testDoFilter_DisabledPermanentServiceReqExtraUri() throws Exception {
        XacmlPdpServiceFilter.disableApi();
        when(request.getRequestURI()).thenReturn("/some/stuff/" + PERM_SVC);
        assertThat(getFilterResponse()).isEqualTo(HttpServletResponse.SC_OK);
    }

    /**
     * Tests doFilter() when the API is disabled, but a permanent service is requested, with extra characters before
     * the service name.
     */
    @Test
    void testDoFilter_DisabledPermanentServiceReqExtraChars() throws Exception {
        XacmlPdpServiceFilter.disableApi();
        when(request.getRequestURI()).thenReturn("/ExtraStuff" + PERM_SVC);
        assertThat(getFilterResponse()).isEqualTo(HttpServletResponse.SC_CONFLICT);
    }

    /**
     * Tests doFilter() when the API is disabled and an API service is requested.
     */
    @Test
    void testDoFilter_DisabledApiReq() throws Exception {
        XacmlPdpServiceFilter.disableApi();
        when(request.getRequestURI()).thenReturn("/other");
        assertThat(getFilterResponse()).isEqualTo(HttpServletResponse.SC_CONFLICT);
    }

    @Test
    void testEnableApi_testDisableApi_testIsApiEnabled() {

        XacmlPdpServiceFilter.enableApi();
        assertThat(XacmlPdpServiceFilter.isApiEnabled()).isTrue();

        XacmlPdpServiceFilter.disableApi();
        assertThat(XacmlPdpServiceFilter.isApiEnabled()).isFalse();
    }

    /**
     * Invokes doFilter().
     *
     * @return the response code set by the filter
     */
    private int getFilterResponse() throws Exception {
        filter.doFilter(request, response, filterChain);

        // should only be called once
        var responseCode = ArgumentCaptor.forClass(Integer.class);
        verify(response).setStatus(responseCode.capture());

        return responseCode.getValue();
    }
}
