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

package org.onap.policy.pdp.xacml.application.common;

import com.att.research.xacml.api.Attribute;
import com.att.research.xacml.api.AttributeValue;
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
import com.att.research.xacml.std.pip.StdPIPResponse;
import com.att.research.xacml.std.pip.engines.StdConfigurableEngine;
import com.google.common.base.Strings;

import java.math.BigInteger;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.Properties;

import javax.persistence.EntityManager;
import javax.persistence.Persistence;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OnapOperationsHistoryPipEngine extends StdConfigurableEngine {
    private static Logger logger = LoggerFactory.getLogger(OnapOperationsHistoryPipEngine.class);

    private static final PIPRequest PIP_REQUEST_ACTOR   = new StdPIPRequest(
            XACML3.ID_ATTRIBUTE_CATEGORY_RESOURCE,
            ToscaDictionary.ID_RESOURCE_GUARD_ACTOR,
            XACML3.ID_DATATYPE_STRING);

    private static final PIPRequest PIP_REQUEST_RECIPE  = new StdPIPRequest(
            XACML3.ID_ATTRIBUTE_CATEGORY_RESOURCE,
            ToscaDictionary.ID_RESOURCE_GUARD_RECIPE,
            XACML3.ID_DATATYPE_STRING);

    private static final PIPRequest PIP_REQUEST_TARGET  = new StdPIPRequest(
            XACML3.ID_ATTRIBUTE_CATEGORY_RESOURCE,
            ToscaDictionary.ID_RESOURCE_GUARD_TARGETID,
            XACML3.ID_DATATYPE_STRING);

    private Properties properties;

    public OnapOperationsHistoryPipEngine() {
        super();
    }

    @Override
    public Collection<PIPRequest> attributesRequired() {
        return Collections.emptyList();
    }

    @Override
    public Collection<PIPRequest> attributesProvided() {
        return Collections.emptyList();
    }

    @Override
    public PIPResponse getAttributes(PIPRequest pipRequest, PIPFinder pipFinder) throws PIPException {
        logger.debug("getAttributes requesting attribute {} of type {} for issuer {}",
                pipRequest.getAttributeId(), pipRequest.getDataTypeId(), pipRequest.getIssuer());
        //
        // Determine if the issuer is correct
        //
        if (Strings.isNullOrEmpty(pipRequest.getIssuer())) {
            logger.debug("issuer is null - returning empty response");
            //
            // We only respond to ourself as the issuer
            //
            return StdPIPResponse.PIP_RESPONSE_EMPTY;
        }
        if (! pipRequest.getIssuer().startsWith(ToscaDictionary.GUARD_ISSUER)) {
            logger.debug("Issuer does not start with guard");
            //
            // We only respond to ourself as the issuer
            //
            return StdPIPResponse.PIP_RESPONSE_EMPTY;
        }
        //
        // Parse out the issuer which denotes the time window
        //
        // Eg: urn:org:onapxacml:guard:historydb:tw:10:minute
        //
        String[] s1 = pipRequest.getIssuer().split("tw:");
        String[] s2 = s1[1].split(":");
        int timeWindowVal = Integer.parseInt(s2[0]);
        String timeWindowScale = s2[1];
        //
        // Grab other attribute values
        //
        String actor = getActor(pipFinder);
        String operation = getRecipe(pipFinder);
        String target = getTarget(pipFinder);
        String timeWindow = timeWindowVal + " " + timeWindowScale;
        logger.info("Going to query DB about: actor {} operation {} target {} time window {}",
                actor, operation, target, timeWindow);
        //
        // Sanity check
        //
        if (actor == null || operation == null || target == null) {
            //
            // See if we have all the values
            //
            logger.error("missing attributes return empty");
            return StdPIPResponse.PIP_RESPONSE_EMPTY;
        }
        //
        // Ok do the database query
        //
        int operationCount = doDatabaseQuery(actor, operation, target, timeWindowVal, timeWindowScale);
        //
        // Right now return empty
        //
        StdMutablePIPResponse stdPipResponse    = new StdMutablePIPResponse();
        this.addIntegerAttribute(stdPipResponse,
                XACML3.ID_ATTRIBUTE_CATEGORY_RESOURCE,
                ToscaDictionary.ID_RESOURCE_GUARD_OPERATIONCOUNT,
                operationCount,
                pipRequest);
        return new StdPIPResponse(stdPipResponse);
    }

    @Override
    public void configure(String id, Properties properties) throws PIPException {
        super.configure(id, properties);
        logger.debug("Configuring historyDb PIP {}", properties);
        this.properties = properties;
    }

    private String getActor(PIPFinder pipFinder) {
        //
        // Get the actor value
        //
        PIPResponse pipResponse = this.getAttribute(PIP_REQUEST_ACTOR, pipFinder);
        if (pipResponse == null) {
            logger.error("Need actor attribute which is not found");
            return null;
        }
        //
        // Find the actor
        //
        return findFirstAttributeValue(pipResponse);
    }

    private String getRecipe(PIPFinder pipFinder) {
        //
        // Get the actor value
        //
        PIPResponse pipResponse = this.getAttribute(PIP_REQUEST_RECIPE, pipFinder);
        if (pipResponse == null) {
            logger.error("Need recipe attribute which is not found");
            return null;
        }
        //
        // Find the actor
        //
        return findFirstAttributeValue(pipResponse);
    }

    private String getTarget(PIPFinder pipFinder) {
        //
        // Get the actor value
        //
        PIPResponse pipResponse = this.getAttribute(PIP_REQUEST_TARGET, pipFinder);
        if (pipResponse == null) {
            logger.error("Need target attribute which is not found");
            return null;
        }
        //
        // Find the actor
        //
        return findFirstAttributeValue(pipResponse);
    }

    private PIPResponse getAttribute(PIPRequest pipRequest, PIPFinder pipFinder) {
        PIPResponse pipResponse = null;
        try {
            pipResponse = pipFinder.getMatchingAttributes(pipRequest, this);
            if (pipResponse.getStatus() != null && !pipResponse.getStatus().isOk()) {
                if (logger.isInfoEnabled()) {
                    logger.info("get attribute error retrieving {}: {}", pipRequest.getAttributeId().stringValue(),
                        pipResponse.getStatus());
                }
                pipResponse = null;
            }
            if (pipResponse != null && pipResponse.getAttributes().isEmpty()) {
                if (logger.isInfoEnabled()) {
                    logger.info("No value for {}", pipRequest.getAttributeId().stringValue());
                }
                pipResponse = null;
            }
        } catch (PIPException ex) {
            logger.error("PIPException getting subject-id attribute: " + ex.getMessage(), ex);
        }
        return pipResponse;
    }

    private String findFirstAttributeValue(PIPResponse pipResponse) {
        for (Attribute attribute: pipResponse.getAttributes()) {
            Iterator<AttributeValue<String>> iterAttributeValues    = attribute.findValues(DataTypes.DT_STRING);
            if (iterAttributeValues != null) {
                while (iterAttributeValues.hasNext()) {
                    String value   = iterAttributeValues.next().getValue();
                    if (value != null) {
                        return value;
                    }
                }
            }
        }
        return null;
    }

    private void addIntegerAttribute(StdMutablePIPResponse stdPipResponse, Identifier category,
            Identifier attributeId, int value, PIPRequest pipRequest) {
        AttributeValue<BigInteger> attributeValue   = null;
        try {
            attributeValue  = DataTypes.DT_INTEGER.createAttributeValue(value);
        } catch (Exception e) {
            logger.error("Failed to convert {} to integer {}", value, e);
        }
        if (attributeValue != null) {
            stdPipResponse.addAttribute(new StdMutableAttribute(category, attributeId, attributeValue,
                    pipRequest.getIssuer(), false));
        }
    }

    private int doDatabaseQuery(String actor, String operation, String target, int timeWindowVal,
            String timeWindowScale) {
        logger.info("Querying operations history for {} {} {} {} {}",
                actor, operation, target, timeWindowVal, timeWindowScale);
        //
        // Create our entity manager
        //
        EntityManager em;
        try {
            Properties emProperties = new Properties(properties);
            em = Persistence.createEntityManagerFactory(properties.getProperty("historydb.persistenceunit"),
                    emProperties).createEntityManager();
        } catch (Exception e) {
            logger.error("Persistence failed {} operations history db {}", e.getLocalizedMessage(), e);
            return -1;
        }
        //
        // Compute the time window
        //
        if (! "minute".equalsIgnoreCase(timeWindowScale)
            || "hour".equalsIgnoreCase(timeWindowScale)
            || "day".equalsIgnoreCase(timeWindowScale)
            || "week".equalsIgnoreCase(timeWindowScale)
            || "month".equalsIgnoreCase(timeWindowScale)
            || "year".equalsIgnoreCase(timeWindowScale)) {
            //
            // Unsupported
            //
            logger.error("Unsupported time window scale value {}", timeWindowScale);
            //
            // Throw an exception instead?
            //
            return -1;
        }
        //
        // Do the query
        //
        Object result = null;
        try {
            //
            //
            //
            String strQuery = "select count(*) as numops from operationshistory"
                    + " where outcome<>'Failure_Guard'"
                    + " and actor=?"
                    + " and operation=?"
                    + " and target=?"
                    + " and endtime between TIMESTAMPDIFF("
                    + timeWindowScale.toUpperCase()
                    + ", ?, CURRENT_DATE)"
                    + " and CURRENT_DATE";
            //
            // We are expecting a single result
            //
            result = em.createNativeQuery(strQuery)
                .setParameter(1, actor)
                .setParameter(2, operation)
                .setParameter(3, target)
                .setParameter(4, timeWindowVal)
                .getSingleResult();
        } catch (Exception e) {
            logger.error("Named query failed ", e);
        }
        //
        // Check our query results
        //
        if (result != null) {
            //
            // Success let's see what JPA returned to us
            //
            logger.info("operations query returned {} class {}", result, result.getClass());
            //
            // Should get back a long
            //
            if (result instanceof Long) {
                return ((Long) result).intValue();
            }
            //
            // We shouldn't really get this result, but just
            // in case we'll do the dirty work of parsing the
            // string representation of the object.
            //
            return Integer.parseInt(result.toString());
        }
        //
        // We get here if we didn't get a result. Should
        // we propagate back an exception?
        //
        return -1;
    }

}
