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

package org.onap.policy.pdpx.main;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.onap.policy.common.utils.network.NetworkUtil;
import org.onap.policy.models.pdp.concepts.PdpResponseDetails;
import org.onap.policy.models.pdp.concepts.PdpStateChange;
import org.onap.policy.models.pdp.concepts.PdpStatus;
import org.onap.policy.models.pdp.concepts.PdpUpdate;
import org.onap.policy.models.pdp.enums.PdpHealthStatus;
import org.onap.policy.models.pdp.enums.PdpResponseStatus;
import org.onap.policy.models.pdp.enums.PdpState;
import org.onap.policy.models.tosca.authorative.concepts.ToscaPolicyTypeIdentifier;
import org.onap.policy.pdpx.main.rest.XacmlPdpApplicationManager;
import org.onap.policy.pdpx.main.startstop.XacmlPdpActivator;

public class XacmlStateTest {
    private static final String PDP_TYPE = "xacml";
    private static final String GROUP = "my-group";
    private static final String SUBGROUP = "my-subgroup";
    private static final PdpState STATE = PdpState.SAFE;

    @Mock
    private XacmlPdpApplicationManager appmgr;

    @Mock
    private XacmlPdpActivator act;

    private ToscaPolicyTypeIdentifier ident1;
    private ToscaPolicyTypeIdentifier ident2;

    private String hostName;

    private XacmlState state;

    /**
     * Initializes objects, including the state.
     */
    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        hostName = NetworkUtil.getHostname();

        ident1 = new ToscaPolicyTypeIdentifier("nameA", "typeA");
        ident2 = new ToscaPolicyTypeIdentifier("nameB", "typeB");

        when(appmgr.getToscaPolicyTypeIdents()).thenReturn(Arrays.asList(ident1, ident2));

        XacmlPdpActivator.setCurrent(act);

        state = new XacmlState(appmgr);
    }

    @AfterClass
    public static void tearDownAfterClass() {
        XacmlPdpActivator.setCurrent(null);
    }

    @Test
    public void testShouldHandle() {
        PdpUpdate msg = new PdpUpdate();
        assertFalse(state.shouldHandle(msg));

        msg.setName(NetworkUtil.getHostname());
        assertTrue(state.shouldHandle(msg));
    }

    @Test
    public void testGenHeartbeat() {
        // not healthy
        PdpStatus status = state.genHeartbeat();
        assertEquals(PdpHealthStatus.NOT_HEALTHY, status.getHealthy());
        assertEquals(hostName, status.getName());
        assertEquals(PDP_TYPE, status.getPdpType());
        assertEquals(PdpState.PASSIVE, status.getState());
        assertEquals("[ToscaPolicyTypeIdentifier(name=nameA, version=typeA), "
                        + "ToscaPolicyTypeIdentifier(name=nameB, version=typeB)]",
                        status.getSupportedPolicyTypes().toString());
        assertTrue(status.getPolicies().isEmpty());

        // healthy
        when(act.isAlive()).thenReturn(true);

        status = state.genHeartbeat();
        assertEquals(PdpHealthStatus.HEALTHY, status.getHealthy());
    }

    @Test
    public void testUpdateInternalStatePdpStateChange() {
        PdpStateChange req = new PdpStateChange();
        req.setName(hostName);
        req.setPdpGroup(GROUP);
        req.setPdpSubgroup(SUBGROUP);
        req.setState(STATE);

        PdpStatus status = state.updateInternalState(req);
        assertEquals(PdpState.SAFE, status.getState());

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
        verify(act).startXacmlRestController();

        req.setState(PdpState.PASSIVE);
        status = state.updateInternalState(req);
        assertEquals(PdpState.PASSIVE, status.getState());
        verify(act).stopXacmlRestController();
    }

    @Test
    public void testUpdateInternalStatePdpUpdate() {
        PdpUpdate req = new PdpUpdate();
        req.setPdpGroup(GROUP);
        req.setPdpSubgroup(SUBGROUP);

        PdpStatus status = state.updateInternalState(req);

        PdpResponseDetails resp = status.getResponse();
        assertNotNull(resp);
        assertEquals(req.getRequestId(), resp.getResponseTo());
        assertEquals(PdpResponseStatus.SUCCESS, resp.getResponseStatus());
        assertNull(resp.getResponseMessage());

        // ensure info was saved
        status = state.genHeartbeat();
        assertEquals(GROUP, status.getPdpGroup());
        assertEquals(SUBGROUP, status.getPdpSubgroup());

        state.setErrorMessage("");
        status = state.updateInternalState(req);
        assertEquals(status.getResponse().getResponseStatus(), PdpResponseStatus.SUCCESS);
        assertNull(status.getResponse().getResponseMessage());

        state.setErrorMessage("Failed to load policy: failLoadPolicy1: null");
        status = state.updateInternalState(req);
        assertEquals(status.getResponse().getResponseMessage(), "Failed to load policy: failLoadPolicy1: null");
        assertEquals(status.getResponse().getResponseStatus(), PdpResponseStatus.FAIL);

    }

    @Test
    public void testTerminatePdpMessage() {
        PdpStatus status = state.terminatePdpMessage();
        assertEquals(PdpState.TERMINATED, status.getState());
    }
}
