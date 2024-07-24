/*-
 * ============LICENSE_START=======================================================
 * Copyright (C) 2019, 2021-2022 AT&T Intellectual Property. All rights reserved.
 * Modifications Copyright (C) 2021, 2023-2024 Nordix Foundation.
 * Modifications Copyright (C) 2023 Bell Canada.
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

package org.onap.policy.pdpx.main;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.onap.policy.models.pdp.concepts.PdpResponseDetails;
import org.onap.policy.models.pdp.concepts.PdpStateChange;
import org.onap.policy.models.pdp.concepts.PdpStatus;
import org.onap.policy.models.pdp.concepts.PdpUpdate;
import org.onap.policy.models.pdp.enums.PdpHealthStatus;
import org.onap.policy.models.pdp.enums.PdpResponseStatus;
import org.onap.policy.models.pdp.enums.PdpState;
import org.onap.policy.pdpx.main.rest.XacmlPdpApplicationManager;
import org.onap.policy.pdpx.main.startstop.XacmlPdpActivator;

@ExtendWith(MockitoExtension.class)
class XacmlStateTest {
    private static final String PDP_TYPE = "xacml-flavor";
    private static final String GROUP = "my-group";
    private static final String SUBGROUP = "my-subgroup";
    private static final PdpState STATE = PdpState.SAFE;

    @Mock
    private XacmlPdpApplicationManager appmgr;

    @Mock
    private XacmlPdpActivator act;

    private String pdpName;

    private XacmlState state;

    /**
     * Initializes objects, including the state.
     */
    @BeforeEach
    void setUp() {
        pdpName = XacmlState.PDP_NAME;

        XacmlPdpActivator.setCurrent(act);
        state = new XacmlState(appmgr, GROUP, PDP_TYPE);
    }

    @AfterAll
    static void tearDownAfterClass() {
        XacmlPdpActivator.setCurrent(null);
    }

    @Test
    void testShouldHandle() {
        PdpUpdate msg = new PdpUpdate();
        assertFalse(state.shouldHandle(msg));

        msg.setName(XacmlState.PDP_NAME);
        assertTrue(state.shouldHandle(msg));
    }

    @Test
    void testGenHeartbeat() {
        // not healthy
        PdpStatus status = state.genHeartbeat();
        assertEquals(PdpHealthStatus.NOT_HEALTHY, status.getHealthy());
        assertEquals(pdpName, status.getName());
        assertEquals(GROUP, status.getPdpGroup());
        assertEquals(PDP_TYPE, status.getPdpType());
        assertEquals(PdpState.PASSIVE, status.getState());
        assertTrue(status.getPolicies().isEmpty());

        // healthy
        when(act.isAlive()).thenReturn(true);

        status = state.genHeartbeat();
        assertEquals(PdpHealthStatus.HEALTHY, status.getHealthy());
    }

    @Test
    void testUpdateInternalStatePdpStateChange() {
        PdpStateChange req = new PdpStateChange();
        req.setName(pdpName);
        req.setPdpGroup("wrong-pdp-group");
        req.setPdpSubgroup(SUBGROUP);
        req.setState(STATE);

        PdpStatus status = state.updateInternalState(req);
        assertEquals(PdpState.SAFE, status.getState());
        assertEquals(GROUP, status.getPdpGroup());

        PdpResponseDetails resp = status.getResponse();
        assertNotNull(resp);
        assertEquals(req.getRequestId(), resp.getResponseTo());
        assertEquals(PdpResponseStatus.SUCCESS, resp.getResponseStatus());

        // ensure info was saved
        status = state.genHeartbeat();
        assertEquals(PdpState.SAFE, status.getState());

        req.setState(PdpState.ACTIVE);
        status = state.updateInternalState(req);
        assertEquals(PdpState.ACTIVE, status.getState());
        verify(act).enableApi();

        req.setState(PdpState.PASSIVE);
        status = state.updateInternalState(req);
        assertEquals(PdpState.PASSIVE, status.getState());
        verify(act).disableApi();
    }

    @Test
    void testUpdateInternalStatePdpUpdate() {
        PdpUpdate req = new PdpUpdate();
        req.setPdpGroup("wrong-pdp-group");
        req.setPdpSubgroup(SUBGROUP);

        PdpStatus status = state.updateInternalState(req, "");

        PdpResponseDetails resp = status.getResponse();
        assertNotNull(resp);
        assertEquals(req.getRequestId(), resp.getResponseTo());
        assertEquals(PdpResponseStatus.SUCCESS, resp.getResponseStatus());
        assertNull(resp.getResponseMessage());

        // ensure info was saved
        status = state.genHeartbeat();
        assertEquals(GROUP, status.getPdpGroup());
        assertEquals(SUBGROUP, status.getPdpSubgroup());

        status = state.updateInternalState(req, "Failed to load policy: failLoadPolicy1: null");
        assertEquals("Failed to load policy: failLoadPolicy1: null", status.getResponse().getResponseMessage());
        assertEquals(PdpResponseStatus.FAIL, status.getResponse().getResponseStatus());
        assertEquals(GROUP, status.getPdpGroup());
    }

    @Test
    void testTerminatePdpMessage() {
        PdpStatus status = state.terminatePdpMessage();
        assertEquals(PdpState.TERMINATED, status.getState());
    }
}
