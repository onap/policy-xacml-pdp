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

package org.onap.policy.pdp.xacml.application.common.operationshistory;

import com.att.research.xacml.api.XACML3;
import com.att.research.xacml.api.pip.PIPException;
import com.att.research.xacml.api.pip.PIPFinder;
import com.att.research.xacml.api.pip.PIPRequest;
import com.att.research.xacml.api.pip.PIPResponse;
import com.att.research.xacml.std.pip.StdMutablePIPResponse;
import com.att.research.xacml.std.pip.StdPIPResponse;
import com.google.common.base.Strings;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collection;
import java.util.Properties;

import javax.persistence.Persistence;

import org.onap.policy.pdp.xacml.application.common.ToscaDictionary;
import org.onap.policy.pdp.xacml.application.common.std.StdOnapPip;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class CountRecentOperationsPip extends StdOnapPip {
    public static final String ISSUER_NAME = "count-recent-operations";
    private static Logger logger = LoggerFactory.getLogger(CountRecentOperationsPip.class);

    public CountRecentOperationsPip() {
        super();
    }

    @Override
    public Collection<PIPRequest> attributesRequired() {
        return Arrays.asList(PIP_REQUEST_ACTOR, PIP_REQUEST_RECIPE, PIP_REQUEST_TARGET);
    }

    @Override
    public void configure(String id, Properties properties) throws PIPException {
        super.configure(id, properties);
        //
        // Create our entity manager
        //
        em = null;
        try {
            //
            // In case there are any overloaded properties for the JPA
            //
            Properties emProperties = new Properties();
            emProperties.putAll(properties);
            
            //
            // Need to decode the password before creating the EntityManager
            //
            String decodedPassword = new String(Base64.getDecoder()
                    .decode(emProperties.getProperty("javax.persistence.jdbc.password")));
            emProperties.setProperty("javax.persistence.jdbc.password", decodedPassword);
            
            //
            // Create the entity manager factory
            //
            em = Persistence.createEntityManagerFactory(
                    properties.getProperty(ISSUER_NAME + ".persistenceunit"),
                    emProperties).createEntityManager();
        } catch (Exception e) {
            logger.error("Persistence failed {} operations history db {}", e.getLocalizedMessage(), e);
        }
    }

    /**
     * getAttributes.
     *
     * @param pipRequest the request
     * @param pipFinder the pip finder
     * @return PIPResponse
     */
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
        if (! pipRequest.getIssuer().startsWith(ToscaDictionary.GUARD_ISSUER_PREFIX)) {
            logger.debug("Issuer does not start with guard");
            //
            // We only respond to ourself as the issuer
            //
            return StdPIPResponse.PIP_RESPONSE_EMPTY;
        }
        //
        // Parse out the issuer which denotes the time window
        // Eg: any-prefix:tw:10:minute
        //
        String[] s1 = pipRequest.getIssuer().split("tw:");
        String[] s2 = s1[1].split(":");
        int timeWindowVal = Integer.parseInt(s2[0]);
        String timeWindowScale = s2[1];
        //
        // Grab other attribute values
        //
        String actor = getAttribute(pipFinder, PIP_REQUEST_ACTOR);
        String operation = getAttribute(pipFinder, PIP_REQUEST_RECIPE);
        String target = getAttribute(pipFinder, PIP_REQUEST_TARGET);
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
        long operationCount = doDatabaseQuery(actor, operation, target, timeWindowVal, timeWindowScale);
        //
        // Create and return PipResponse
        //
        StdMutablePIPResponse pipResponse    = new StdMutablePIPResponse();
        this.addLongAttribute(pipResponse,
                XACML3.ID_ATTRIBUTE_CATEGORY_RESOURCE,
                ToscaDictionary.ID_RESOURCE_GUARD_OPERATIONCOUNT,
                operationCount,
                pipRequest);
        return new StdPIPResponse(pipResponse);
    }

    private long doDatabaseQuery(String actor, String operation, String target, int timeWindowVal,
            String timeWindowScale) {
        logger.info("Querying operations history for {} {} {} {} {}",
                actor, operation, target, timeWindowVal, timeWindowScale);
        //
        // Only can query if we have an EntityManager
        //
        if (em == null) {
            logger.error("No EntityManager available");
            return -1;
        }
        //
        // Do the query
        //
        try {
            //
            // We are expecting a single result
            //
            return em.createQuery("select count(e) from Dbao e"
                                  + " where e.outcome<>'Failure_Guard'"
                                  + " and e.actor= ?1"
                                  + " and e.operation= ?2"
                                  + " and e.target= ?3"
                                  + " and e.endtime between"
                                  + " ?4 and CURRENT_TIMESTAMP",
                                  Long.class)
                .setParameter(1, actor)
                .setParameter(2, operation)
                .setParameter(3, target)
                .setParameter(4, Timestamp.from(Instant.now()
                                                .minus(timeWindowVal,
                                                       stringToChronoUnit(timeWindowScale))))
                .getSingleResult();
        } catch (Exception e) {
            logger.error("Typed query failed ", e);
            return -1;
        }
    }

    private ChronoUnit stringToChronoUnit(String scale) {
        //
        // Compute the time window
        //
        switch (scale.toLowerCase()) {
            case "second":
                return ChronoUnit.SECONDS;
            case "minute":
                return ChronoUnit.MINUTES;
            case "hour":
                return ChronoUnit.HOURS;
            case "day":
                return ChronoUnit.DAYS;
            case "week":
                return ChronoUnit.WEEKS;
            case "month":
                return ChronoUnit.MONTHS;
            case "year":
                return ChronoUnit.YEARS;
            default:
                //
                // Unsupported
                //
                logger.error("Unsupported time window scale value {}", scale);
        }
        return null;
    }

}
