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

package org.onap.policy.pdp.xacml.application.common.std;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.TreeMap;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.onap.policy.models.decisions.concepts.DecisionRequest;

public class StdCombinedPolicyRequestTest {
    private static final String ACTION = "my-action";
    private static final String ONAP_NAME = "my-name";
    private static final String ONAP_INSTANCE = "my-instance";
    private static final String ONAP_COMPONENT = "my-component";
    private static final String POLICY_ID = "my-policy";
    private static final String POLICY_TYPE = "my-type";

    @Mock
    private DecisionRequest decreq;

    private Map<String, Object> resources;

    private StdCombinedPolicyRequest stdreq;

    /**
     * Initializes objects.
     */
    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        resources = new TreeMap<>();

        when(decreq.getResource()).thenReturn(resources);
        when(decreq.getAction()).thenReturn(ACTION);
        when(decreq.getOnapComponent()).thenReturn(ONAP_COMPONENT);
        when(decreq.getOnapInstance()).thenReturn(ONAP_INSTANCE);
        when(decreq.getOnapName()).thenReturn(ONAP_NAME);
    }

    @Test
    public void testCreateInstance() {
        resources.put(StdCombinedPolicyRequest.POLICY_ID_KEY, 100);
        resources.put(StdCombinedPolicyRequest.POLICY_TYPE_KEY, 101);

        stdreq = StdCombinedPolicyRequest.createInstance(decreq);

        assertNotNull(stdreq);

        assertEquals(ACTION, stdreq.getAction());
        assertEquals(ONAP_COMPONENT, stdreq.getOnapComponent());
        assertEquals(ONAP_INSTANCE, stdreq.getOnapInstance());
        assertEquals(ONAP_NAME, stdreq.getOnapName());

        assertTrue(stdreq.getResource().isEmpty());
        assertTrue(stdreq.getResourcePolicyType().isEmpty());
    }

    @Test
    public void testCreateInstance_StringValues() {
        resources.put(StdCombinedPolicyRequest.POLICY_ID_KEY, POLICY_ID);
        resources.put(StdCombinedPolicyRequest.POLICY_ID_KEY + "-x", "unused value");
        resources.put(StdCombinedPolicyRequest.POLICY_TYPE_KEY, POLICY_TYPE);

        stdreq = StdCombinedPolicyRequest.createInstance(decreq);

        Collection<String> res = stdreq.getResource();
        assertFalse(res.isEmpty());
        assertEquals(POLICY_ID, res.iterator().next());

        res = stdreq.getResourcePolicyType();
        assertFalse(res.isEmpty());
        assertEquals(POLICY_TYPE, res.iterator().next());
    }

    @Test
    public void testCreateInstance_Collections() {
        resources.put(StdCombinedPolicyRequest.POLICY_ID_KEY, Collections.singleton(POLICY_ID));
        resources.put(StdCombinedPolicyRequest.POLICY_TYPE_KEY, Collections.singleton(POLICY_TYPE));

        stdreq = StdCombinedPolicyRequest.createInstance(decreq);

        Collection<String> res = stdreq.getResource();
        assertFalse(res.isEmpty());
        assertEquals(POLICY_ID, res.iterator().next());

        res = stdreq.getResourcePolicyType();
        assertFalse(res.isEmpty());
        assertEquals(POLICY_TYPE, res.iterator().next());
    }

}
