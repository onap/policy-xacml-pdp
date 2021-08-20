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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import javax.servlet.FilterChain;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class TestXacmlPdpServiceFilter {

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
    @Before
    public void setUp() {
        XacmlPdpServiceFilter.disableApi();

        when(request.getMethod()).thenReturn("GET");
        when(request.isUserInRole(any())).thenReturn(true);

        filterChain = (req, resp) -> {
            HttpServletResponse resp2 = (HttpServletResponse) resp;
            resp2.setStatus(HttpServletResponse.SC_OK);
        };

        filter = new XacmlPdpServiceFilter();
    }

    @Test
    public void testDoFilter() throws Exception {
        XacmlPdpServiceFilter.enableApi();
        when(request.getRequestURI()).thenReturn("/other");
        assertThat(getFilterResponse()).isEqualTo(HttpServletResponse.SC_OK);
    }

    /**
     * Tests doFilter() when the API is disabled, but a permanent service is requested.
     */
    @Test
    public void testDoFilter_DisabledPermanentServiceReq() throws Exception {
        XacmlPdpServiceFilter.disableApi();
        when(request.getRequestURI()).thenReturn(PERM_SVC);
        assertThat(getFilterResponse()).isEqualTo(HttpServletResponse.SC_OK);
    }

    /**
     * Tests doFilter() when the API is disabled, but a permanent service is requested, with a leading slash.
     */
    @Test
    public void testDoFilter_DisabledPermanentServiceReqLeadingSlash() throws Exception {
        XacmlPdpServiceFilter.disableApi();
        when(request.getRequestURI()).thenReturn("/" + PERM_SVC);
        assertThat(getFilterResponse()).isEqualTo(HttpServletResponse.SC_OK);
    }

    /**
     * Tests doFilter() when the API is disabled, but a permanent service is requested, with extra URI prefix.
     */
    @Test
    public void testDoFilter_DisabledPermanentServiceReqExtraUri() throws Exception {
        XacmlPdpServiceFilter.disableApi();
        when(request.getRequestURI()).thenReturn("/some/stuff/" + PERM_SVC);
        assertThat(getFilterResponse()).isEqualTo(HttpServletResponse.SC_OK);
    }

    /**
     * Tests doFilter() when the API is disabled, but a permanent service is requested, with extra characters before
     * the service name.
     */
    @Test
    public void testDoFilter_DisabledPermanentServiceReqExtraChars() throws Exception {
        XacmlPdpServiceFilter.disableApi();
        when(request.getRequestURI()).thenReturn("/ExtraStuff" + PERM_SVC);
        assertThat(getFilterResponse()).isEqualTo(HttpServletResponse.SC_CONFLICT);
    }

    /**
     * Tests doFilter() when the API is disabled and an API service is requested.
     */
    @Test
    public void testDoFilter_DisabledApiReq() throws Exception {
        XacmlPdpServiceFilter.disableApi();
        when(request.getRequestURI()).thenReturn("/other");
        assertThat(getFilterResponse()).isEqualTo(HttpServletResponse.SC_CONFLICT);
    }

    /**
     * Tests doFilter() when the API is disabled and an API service is requested.
     */
    @Test
    public void testDoFilter_EnabledApiReq() throws Exception {
        XacmlPdpServiceFilter.enableApi();
        when(request.getRequestURI()).thenReturn("/other");
        assertThat(getFilterResponse()).isEqualTo(HttpServletResponse.SC_OK);
    }

    @Test
    public void testEnableApi_testDisableApi_testIsApiEnabled() {

        XacmlPdpServiceFilter.enableApi();
        assertThat(XacmlPdpServiceFilter.isApiEnabled()).isTrue();

        XacmlPdpServiceFilter.disableApi();
        assertThat(XacmlPdpServiceFilter.isApiEnabled()).isFalse();
    }

    /**
     * Invokes doFilter().
     * @return the response code set by the filter
     */
    private int getFilterResponse()  throws Exception {
        filter.doFilter(request, response, filterChain);

        // should only be called once
        var responseCode = ArgumentCaptor.forClass(Integer.class);
        verify(response).setStatus(responseCode.capture());

        return responseCode.getValue();
    }
}
