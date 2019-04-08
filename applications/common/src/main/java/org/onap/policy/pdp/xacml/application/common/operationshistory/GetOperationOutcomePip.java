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

import java.util.Arrays;
import java.util.Collection;
import java.util.Properties;

import javax.persistence.NoResultException;
import javax.persistence.Persistence;

import org.onap.policy.pdp.xacml.application.common.ToscaDictionary;
import org.onap.policy.pdp.xacml.application.common.std.StdOnapPip;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class GetOperationOutcomePip extends StdOnapPip {
    public static final String ISSUER_NAME = "get-operation-outcome";
    private static Logger logger = LoggerFactory.getLogger(GetOperationOutcomePip.class);

    public GetOperationOutcomePip() {
        super();
    }

    @Override
    public Collection<PIPRequest> attributesRequired() {
        return Arrays.asList(PIP_REQUEST_TARGET);
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
            Properties emProperties = new Properties(properties);
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
        // Eg: any-prefix:clname:some-controlloop-name
        //
        String[] s1 = pipRequest.getIssuer().split("clname:");
        String clname = s1[1];
        String target = null;
        target = getAttribute(pipFinder, PIP_REQUEST_TARGET);

        logger.debug("Going to query DB about: clname={}, target={}", clname, target);
        String outcome = doDatabaseQuery(clname, target);
        logger.debug("Query result is: {}", outcome);

        StdMutablePIPResponse pipResponse = new StdMutablePIPResponse();
        this.addStringAttribute(pipResponse,
                XACML3.ID_ATTRIBUTE_CATEGORY_RESOURCE,
                ToscaDictionary.ID_RESOURCE_GUARD_OPERATIONOUTCOME,
                outcome,
                pipRequest);
        return new StdPIPResponse(pipResponse);
    }

    private String doDatabaseQuery(String clname, String target) {
        logger.info("Querying operations history for {} {}", clname, target);
        //
        // Do the query
        //
        try {
            //
            // We are expecting a single result
            //
            return em.createQuery("select e.outcome from Dbao e"
                                  + " where e.closedLoopName= ?1"
                                  + " and e.target= ?2"
                                  + " order by e.endtime desc", String.class)
                .setParameter(1, clname)
                .setParameter(2, target)
                .setMaxResults(1)
                .getSingleResult();
        } catch (NoResultException ex) {
            logger.debug("NoResultException for getSingleResult()", ex);
        } catch (Exception ex) {
            logger.error("doDatabaseQuery threw an exception", ex);
        }
        return null;
    }
}
