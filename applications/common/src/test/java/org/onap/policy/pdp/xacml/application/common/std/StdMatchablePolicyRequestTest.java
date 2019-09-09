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

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

import com.att.research.xacml.api.DataTypeException;
import com.att.research.xacml.api.Request;
import com.att.research.xacml.api.RequestAttributes;
import com.att.research.xacml.api.XACML3;
import com.att.research.xacml.std.IdentifierImpl;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.onap.policy.models.decisions.concepts.DecisionRequest;
import org.onap.policy.pdp.xacml.application.common.ToscaDictionary;

public class StdMatchablePolicyRequestTest {
    private static final String ACTION = "my-action";
    private static final String ONAP_NAME = "my-name";
    private static final String ONAP_INSTANCE = "my-instance";
    private static final String ONAP_COMPONENT = "my-component";
    private static final String RESOURCE1 = "my-scope";
    private static final String RESOURCE2 = "my-service";
    private static final String RESOURCE3 = "my-geography1";
    private static final String RESOURCE4 = "my-geography2";

    @Mock
    private DecisionRequest decreq;

    private Map<String, Object> resources;

    private Request stdreq;

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
    public void testCreateInstance() throws IllegalAccessException, DataTypeException {
        resources.put("resource1", RESOURCE1);
        resources.put("resource2", RESOURCE2);
        resources.put("resource3", Arrays.asList(RESOURCE3, RESOURCE4));

        stdreq = StdMatchablePolicyRequest.createInstance(decreq);

        assertNotNull(stdreq);

        assertTrue(stdreq.getRequestAttributes(XACML3.ID_ATTRIBUTE_CATEGORY_ACTION).hasNext());
        assertTrue(stdreq.getRequestAttributes(XACML3.ID_SUBJECT_CATEGORY_ACCESS_SUBJECT).hasNext());


        Iterator<RequestAttributes> iterResources = stdreq.getRequestAttributes(XACML3.ID_ATTRIBUTE_CATEGORY_RESOURCE);
        assertTrue(iterResources.hasNext());
        while (iterResources.hasNext()) {
            RequestAttributes attrs = iterResources.next();
            assertTrue(attrs.hasAttributes(new IdentifierImpl(ToscaDictionary.ID_RESOURCE_MATCHABLE + "resource1")));
        }

    }

    /*
    @Test
    public void testCreateInstance_StringValues() {
        resources.put(StdMatchablePolicyRequest.POLICY_SCOPE_KEY, POLICY_SCOPE);
        resources.put(StdMatchablePolicyRequest.POLICY_SCOPE_KEY + "-x", "unused value");
        resources.put(StdMatchablePolicyRequest.POLICY_TYPE_KEY, POLICY_TYPE);

        stdreq = StdMatchablePolicyRequest.createInstance(decreq);

        Collection<String> res = stdreq.getPolicyScopes();
        assertFalse(res.isEmpty());
        assertEquals(POLICY_SCOPE, res.iterator().next());

        res = stdreq.getPolicyTypes();
        assertFalse(res.isEmpty());
        assertEquals(POLICY_TYPE, res.iterator().next());
    }

    @Test
    public void testCreateInstance_Collections() {
        resources.put(StdMatchablePolicyRequest.POLICY_SCOPE_KEY, Collections.singleton(POLICY_SCOPE));
        resources.put(StdMatchablePolicyRequest.POLICY_TYPE_KEY, Collections.singleton(POLICY_TYPE));

        stdreq = StdMatchablePolicyRequest.createInstance(decreq);

        Collection<String> res = stdreq.getPolicyScopes();
        assertFalse(res.isEmpty());
        assertEquals(POLICY_SCOPE, res.iterator().next());

        res = stdreq.getPolicyTypes();
        assertFalse(res.isEmpty());
        assertEquals(POLICY_TYPE, res.iterator().next());
    }
*/

}
