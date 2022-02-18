/*-
 * ============LICENSE_START=======================================================
 * Copyright (C) 2019, 2021 AT&T Intellectual Property. All rights reserved.
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

package org.onap.policy.pdpx.main.comm;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.startsWith;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.onap.policy.common.endpoints.event.comm.client.TopicSinkClient;
import org.onap.policy.models.pdp.concepts.PdpStatus;
import org.onap.policy.models.pdp.concepts.PdpUpdate;
import org.onap.policy.models.tosca.authorative.concepts.ToscaConceptIdentifier;
import org.onap.policy.models.tosca.authorative.concepts.ToscaPolicy;
import org.onap.policy.pdp.xacml.application.common.XacmlApplicationException;
import org.onap.policy.pdp.xacml.application.common.XacmlApplicationServiceProvider;
import org.onap.policy.pdpx.main.XacmlState;
import org.onap.policy.pdpx.main.rest.XacmlPdpApplicationManager;
import org.onap.policy.pdpx.main.rest.XacmlPdpStatisticsManager;


/**
 * Initializes objects, including the publisher.
 */
@RunWith(MockitoJUnitRunner.class)
public class XacmlPdpUpdatePublisherTest {

    private static final int NEW_COUNT = 5;

    @Mock
    private TopicSinkClient client;

    @Mock
    private XacmlState state;

    @Mock
    private PdpStatus status;

    @Mock
    private XacmlPdpApplicationManager appmgr;

    @Mock
    private ToscaPolicy deployed1;

    @Mock
    private ToscaPolicy deployed2;

    @Mock
    private ToscaPolicy deployed3;

    @Mock
    private ToscaPolicy deployed4;

    @Mock
    private ToscaPolicy deployed5;

    @Mock
    private ToscaPolicy added1;

    @Mock
    private ToscaPolicy added2;

    @Mock
    private ToscaPolicy failPolicy1;

    @Mock
    private ToscaPolicy failPolicy2;

    @Mock
    private PdpUpdate update;

    @Mock
    private PdpUpdate failurePdpUpdate;

    private XacmlPdpUpdatePublisher publisher;
    private XacmlPdpStatisticsManager statmgr;


    /**
     * Initializes objects, including the publisher.
     */
    @Before
    public void setUp() {
        ToscaConceptIdentifier deployedId1 = new ToscaConceptIdentifier("deployed-1", "1.0.0");
        ToscaConceptIdentifier deployedId2 = new ToscaConceptIdentifier("deployed-2", "1.0.0");
        ToscaConceptIdentifier deployedId4 = new ToscaConceptIdentifier("deployed-4", "1.0.0");
        ToscaConceptIdentifier deployedId5 = new ToscaConceptIdentifier("deployed-5", "1.0.0");
        ToscaConceptIdentifier addedId1 = new ToscaConceptIdentifier("added-1", "1.0.0");
        ToscaConceptIdentifier addedId2 = new ToscaConceptIdentifier("added-2", "1.0.0");

        when(deployed1.getIdentifier()).thenReturn(deployedId1);
        when(deployed2.getIdentifier()).thenReturn(deployedId2);
        when(deployed3.getIdentifier()).thenReturn(new ToscaConceptIdentifier("deployed-3", "1.0.0"));
        when(deployed4.getIdentifier()).thenReturn(deployedId4);
        when(deployed5.getIdentifier()).thenReturn(deployedId5);
        when(added1.getIdentifier()).thenReturn(addedId1);
        when(added2.getIdentifier()).thenReturn(addedId2);
        when(failPolicy1.getIdentifier()).thenReturn(new ToscaConceptIdentifier("failPolicy-1", "1.0.0"));
        when(failPolicy2.getIdentifier()).thenReturn(new ToscaConceptIdentifier("failPolicy-2", "1.0.0"));

        Map<ToscaPolicy, XacmlApplicationServiceProvider> deployedPolicies = new HashMap<>();
        deployedPolicies.put(deployed1, null);
        deployedPolicies.put(deployed2, null);
        deployedPolicies.put(deployed3, null);
        deployedPolicies.put(deployed4, null);
        deployedPolicies.put(deployed5, null);
        when(appmgr.getToscaPolicies()).thenReturn(deployedPolicies);

        // update includes one overlap with existing and one overlap between the two
        when(update.getPoliciesToBeDeployed()).thenReturn(List.of(added1, deployed2, deployed5, added2));
        when(update.getPoliciesToBeUndeployed()).thenReturn(List.of(addedId1, deployedId1, deployedId5, deployedId4));

        when(failurePdpUpdate.getPoliciesToBeDeployed())
                        .thenReturn(List.of(added1, deployed2, deployed5, failPolicy1, failPolicy2));
        when(failurePdpUpdate.getPoliciesToBeUndeployed())
                        .thenReturn(List.of(addedId1, deployedId1, deployedId5, deployedId4));

        when(appmgr.getPolicyCount()).thenReturn(NEW_COUNT);

        when(state.updateInternalState(any(), any())).thenReturn(status);

        when(client.send(any())).thenReturn(true);

        publisher = new XacmlPdpUpdatePublisher(client, state, appmgr);

        statmgr = new XacmlPdpStatisticsManager();
        XacmlPdpStatisticsManager.setCurrent(statmgr);
    }

    @Test
    public void testHandlePdpUpdate() throws XacmlApplicationException {
        publisher.handlePdpUpdate(update);

        // two removed
        verify(appmgr).removeUndeployedPolicy(deployed1);
        verify(appmgr).removeUndeployedPolicy(deployed4);

        // two added
        verify(appmgr).loadDeployedPolicy(added1);
        verify(appmgr).loadDeployedPolicy(added2);

        // three untouched
        verify(appmgr, never()).removeUndeployedPolicy(deployed2);
        verify(appmgr, never()).removeUndeployedPolicy(deployed3);
        verify(appmgr, never()).removeUndeployedPolicy(deployed5);
        verify(appmgr, never()).loadDeployedPolicy(deployed2);
        verify(appmgr, never()).loadDeployedPolicy(deployed3);
        verify(appmgr, never()).loadDeployedPolicy(deployed5);

        assertEquals(NEW_COUNT, statmgr.getTotalPoliciesCount());

        verify(client).send(status);
    }

    @Test
    public void testHandlePdpUpdate_Deploy() throws XacmlApplicationException {
        when(update.getPoliciesToBeUndeployed()).thenReturn(null);

        publisher.handlePdpUpdate(update);

        // none removed
        verify(appmgr, never()).removeUndeployedPolicy(any());

        // two added
        verify(appmgr).loadDeployedPolicy(added1);
        verify(appmgr).loadDeployedPolicy(added2);

        // three untouched
        verify(appmgr, never()).loadDeployedPolicy(deployed2);
        verify(appmgr, never()).loadDeployedPolicy(deployed3);
        verify(appmgr, never()).loadDeployedPolicy(deployed5);
    }

    @Test
    public void testHandlePdpUpdate_Undeploy() throws XacmlApplicationException {
        when(update.getPoliciesToBeDeployed()).thenReturn(null);

        publisher.handlePdpUpdate(update);

        // three removed
        verify(appmgr).removeUndeployedPolicy(deployed1);
        verify(appmgr).removeUndeployedPolicy(deployed4);
        verify(appmgr).removeUndeployedPolicy(deployed5);

        // none added
        verify(appmgr, never()).loadDeployedPolicy(any());

        // two untouched
        verify(appmgr, never()).removeUndeployedPolicy(deployed2);
        verify(appmgr, never()).removeUndeployedPolicy(deployed3);
    }

    @Test
    public void testHandlePdpUpdate_LoadPolicyFailed() throws XacmlApplicationException {
        // Set loadPolicy to fail
        doThrow(new XacmlApplicationException()).when(appmgr).loadDeployedPolicy(failPolicy1);
        doThrow(new XacmlApplicationException()).when(appmgr).loadDeployedPolicy(failPolicy2);

        publisher.handlePdpUpdate(failurePdpUpdate);

        // two removed
        verify(appmgr).removeUndeployedPolicy(deployed1);
        verify(appmgr).removeUndeployedPolicy(deployed4);

        // one untouched
        verify(appmgr, never()).removeUndeployedPolicy(deployed5);

        verify(state).updateInternalState(any(), startsWith("Failed to load policy"));
        verify(client).send(status);
    }

    @Test
    public void testHandlePdpUpdate_NullPolicies() throws XacmlApplicationException {
        when(update.getPoliciesToBeDeployed()).thenReturn(null);
        when(update.getPoliciesToBeUndeployed()).thenReturn(null);

        publisher.handlePdpUpdate(update);

        // none removed
        verify(appmgr, never()).removeUndeployedPolicy(any());

        // none added
        verify(appmgr, never()).loadDeployedPolicy(any());

        verify(client).send(status);
    }

    @Test
    public void testHandlePdpUpdate_NullStats() {
        XacmlPdpStatisticsManager.setCurrent(null);

        // should work without throwing an exception
        publisher.handlePdpUpdate(update);

        verify(client).send(status);
    }

    @Test
    public void testHandlePdpUpdate_SendFail() {

        when(client.send(any())).thenReturn(false);

        publisher.handlePdpUpdate(update);

        verify(client).send(status);
    }

}
