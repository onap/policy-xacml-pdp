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

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import java.util.Arrays;
import java.util.Collections;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.onap.policy.common.utils.network.NetworkUtil;
import org.onap.policy.models.pdp.concepts.PdpMessage;
import org.onap.policy.models.pdp.concepts.PdpStatus;
import org.onap.policy.models.pdp.concepts.PdpUpdate;
import org.onap.policy.models.pdp.enums.PdpHealthStatus;
import org.onap.policy.models.pdp.enums.PdpState;
import org.onap.policy.models.tosca.authorative.concepts.ToscaPolicyTypeIdentifier;
import org.onap.policy.pdpx.main.rest.XacmlPdpApplicationManager;
import org.onap.policy.pdpx.main.startstop.XacmlPdpActivator;
import org.powermock.reflect.Whitebox;

public class XacmlStateTest {
    private static final String PDP_TYPE = "xacml";

    @Mock
    private XacmlPdpApplicationManager appmgr;

    private ToscaPolicyTypeIdentifier ident1;
    private ToscaPolicyTypeIdentifier ident2;

    private XacmlState state;

    /**
     * Initializes objects, including the state.
     */
    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        ident1 = new ToscaPolicyTypeIdentifier("nameA", "typeA");
        ident2 = new ToscaPolicyTypeIdentifier("nameB", "typeB");

        when(appmgr.getToscaPolicyTypeIdents()).thenReturn(Arrays.asList(ident1, ident2));

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
        XacmlPdpActivator act = mock(XacmlPdpActivator.class);
        XacmlPdpActivator.setCurrent(act);

        // not healthy
        PdpStatus status = state.genHeartbeat();
        assertEquals(PdpHealthStatus.NOT_HEALTHY, status.getHealthy());
        assertEquals(NetworkUtil.getHostname(), status.getName());
        assertEquals(PDP_TYPE, status.getPdpType());
        assertEquals(PdpState.PASSIVE, status.getState());
        assertEquals("[ToscaPolicyTypeIdentifier(name=nameA, version=typeA), " + "ToscaPolicyTypeIdentifier(name=nameB, version=typeB)]", status.getSupportedPolicyTypes().toString());
        assertTrue(status.getPolicies().isEmpty());

        // healthy
        when(act.isAlive()).thenReturn(true);

        status = state.genHeartbeat();
        assertEquals(PdpHealthStatus.HEALTHY, status.getHealthy());
    }

    @Test
    public void testUpdateInternalStatePdpStateChange() {
        fail("Not yet implemented");
    }

    @Test
    public void testUpdateInternalStatePdpUpdate() {
        fail("Not yet implemented");
    }

    @Test
    public void testTerminatePdpMessage() {
        fail("Not yet implemented");
    }

    @Test
    public void testMakeResponse() {

    }

}
