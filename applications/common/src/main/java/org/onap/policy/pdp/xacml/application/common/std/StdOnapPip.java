/*-
 * ============LICENSE_START=======================================================
 * Copyright (C) 2019-2021 AT&T Intellectual Property. All rights reserved.
 * Modifications Copyright (C) 2020 Bell Canada. All rights reserved.
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

import com.att.research.xacml.api.Attribute;
import com.att.research.xacml.api.AttributeValue;
import com.att.research.xacml.api.DataTypeException;
import com.att.research.xacml.api.Identifier;
import com.att.research.xacml.api.XACML3;
import com.att.research.xacml.api.pip.PIPException;
import com.att.research.xacml.api.pip.PIPFinder;
import com.att.research.xacml.api.pip.PIPRequest;
import com.att.research.xacml.api.pip.PIPResponse;
import com.att.research.xacml.std.StdMutableAttribute;
import com.att.research.xacml.std.datatypes.DataTypes;
import com.att.research.xacml.std.pip.StdMutablePIPResponse;
import com.att.research.xacml.std.pip.StdPIPRequest;
import com.att.research.xacml.std.pip.engines.StdConfigurableEngine;
import java.math.BigInteger;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.Properties;
import javax.persistence.EntityManager;
import javax.persistence.Persistence;
import org.onap.policy.pdp.xacml.application.common.ToscaDictionary;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public abstract class StdOnapPip extends StdConfigurableEngine {
    protected static Logger logger = LoggerFactory.getLogger(StdOnapPip.class);

    protected static final PIPRequest PIP_REQUEST_ACTOR   = new StdPIPRequest(
            XACML3.ID_ATTRIBUTE_CATEGORY_RESOURCE,
            ToscaDictionary.ID_RESOURCE_GUARD_ACTOR,
            XACML3.ID_DATATYPE_STRING);

    protected static final PIPRequest PIP_REQUEST_RECIPE  = new StdPIPRequest(
            XACML3.ID_ATTRIBUTE_CATEGORY_RESOURCE,
            ToscaDictionary.ID_RESOURCE_GUARD_RECIPE,
            XACML3.ID_DATATYPE_STRING);

    protected static final PIPRequest PIP_REQUEST_TARGET  = new StdPIPRequest(
            XACML3.ID_ATTRIBUTE_CATEGORY_RESOURCE,
            ToscaDictionary.ID_RESOURCE_GUARD_TARGETID,
            XACML3.ID_DATATYPE_STRING);

    protected Properties properties;
    protected EntityManager em;
    protected String issuer;
    protected boolean shutdown = false;

    protected StdOnapPip() {
        super();
    }

    @Override
    public Collection<PIPRequest> attributesProvided() {
        return Collections.emptyList();
    }

    @Override
    public synchronized void configure(String id, Properties properties) throws PIPException {
        //
        // This most likely will never get called since configure is called
        // upon startup.
        //
        if (this.shutdown) {
            throw new PIPException("Engine is shutdown.");
        }
        super.configure(id, properties);
        logger.info("Configuring historyDb PIP {}", properties);
        this.properties = properties;
        //
        // Create our entity manager
        //
        em = null;
        try {
            //
            // In case there are any overloaded properties for the JPA
            //
            var emProperties = new Properties();
            emProperties.putAll(properties);

            //
            // Create the entity manager factory
            //
            em = Persistence.createEntityManagerFactory(
                    properties.getProperty(this.issuer + ".persistenceunit"),
                    emProperties).createEntityManager();
        } catch (Exception e) {
            logger.error("Persistence failed {} operations history db", e.getLocalizedMessage(), e);
        }
    }

    @Override
    public synchronized void shutdown() {
        if (this.em != null) {
            this.em.close();
            this.em = null;
        }
        this.shutdown = true;
    }

    protected String getAttribute(PIPFinder pipFinder, PIPRequest pipRequest) {
        //
        // Get the actor value
        //
        var pipResponse = this.getAttribute(pipRequest, pipFinder);
        if (pipResponse == null) {
            logger.error("Need actor attribute which is not found");
            return null;
        }
        //
        // Find the actor
        //
        return findFirstAttributeValue(pipResponse);
    }

    protected PIPResponse getAttribute(PIPRequest pipRequest, PIPFinder pipFinder) {
        PIPResponse pipResponse = null;
        try {
            pipResponse = pipFinder.getMatchingAttributes(pipRequest, this);
            if (pipResponse.getStatus() != null && !pipResponse.getStatus().isOk()) {
                logger.info("get attribute error retrieving {}: {}", pipRequest.getAttributeId(),
                                pipResponse.getStatus());
                pipResponse = null;
            }
            if (pipResponse != null && pipResponse.getAttributes().isEmpty()) {
                logger.info("No value for {}", pipRequest.getAttributeId());
                pipResponse = null;
            }
        } catch (PIPException ex) {
            logger.error("PIPException getting subject-id attribute", ex);
        }
        return pipResponse;
    }

    protected String findFirstAttributeValue(PIPResponse pipResponse) {
        for (Attribute attribute: pipResponse.getAttributes()) {
            Iterator<AttributeValue<String>> iterAttributeValues    = attribute.findValues(DataTypes.DT_STRING);
            while (iterAttributeValues.hasNext()) {
                String value   = iterAttributeValues.next().getValue();
                if (value != null) {
                    return value;
                }
            }
        }
        return null;
    }

    protected void addIntegerAttribute(StdMutablePIPResponse stdPipResponse, Identifier category,
            Identifier attributeId, int value, PIPRequest pipRequest) {
        AttributeValue<BigInteger> attributeValue   = null;
        try {
            attributeValue  = makeInteger(value);
        } catch (Exception e) {
            logger.error("Failed to convert {} to integer", value, e);
        }
        if (attributeValue != null) {
            stdPipResponse.addAttribute(new StdMutableAttribute(category, attributeId, attributeValue,
                    pipRequest.getIssuer(), false));
        }
    }

    protected void addLongAttribute(StdMutablePIPResponse stdPipResponse, Identifier category,
            Identifier attributeId, long value, PIPRequest pipRequest) {
        AttributeValue<BigInteger> attributeValue   = null;
        try {
            attributeValue  = makeLong(value);
        } catch (Exception e) {
            logger.error("Failed to convert {} to long", value, e);
        }
        if (attributeValue != null) {
            stdPipResponse.addAttribute(new StdMutableAttribute(category, attributeId, attributeValue,
                    pipRequest.getIssuer(), false));
        }
    }

    protected void addStringAttribute(StdMutablePIPResponse stdPipResponse, Identifier category, Identifier attributeId,
            String value, PIPRequest pipRequest) {
        AttributeValue<String> attributeValue = null;
        try {
            attributeValue = makeString(value);
        } catch (Exception ex) {
            logger.error("Failed to convert {} to an AttributeValue<String>", value, ex);
        }
        if (attributeValue != null) {
            stdPipResponse.addAttribute(new StdMutableAttribute(category, attributeId, attributeValue,
                    pipRequest.getIssuer(), false));
        }
    }

    // these may be overridden by junit tests

    protected AttributeValue<BigInteger> makeInteger(int value) throws DataTypeException {
        return DataTypes.DT_INTEGER.createAttributeValue(value);
    }

    protected AttributeValue<BigInteger> makeLong(long value) throws DataTypeException {
        return DataTypes.DT_INTEGER.createAttributeValue(value);
    }

    protected AttributeValue<String> makeString(String value) throws DataTypeException {
        return DataTypes.DT_STRING.createAttributeValue(value);
    }

}
