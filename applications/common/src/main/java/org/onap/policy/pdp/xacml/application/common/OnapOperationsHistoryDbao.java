/*-
 * ============LICENSE_START=======================================================
 * ONAP
 * ================================================================================
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

import java.sql.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.Table;

import lombok.Data;

@Entity
@Table(name = "operationshistory10")
@Inheritance(strategy = InheritanceType.TABLE_PER_CLASS)
@Data
public class OnapOperationsHistoryDbao {

    public OnapOperationsHistoryDbao() {
        super();
    }

    @Column(name = "CLNAME", length = 255)
    private String clName;

    @Column(name = "requestID", length = 100)
    private String requestId;

    @Column(name = "actor", length = 50)
    private String actor;

    @Column(name = "operation", length = 50)
    private String operation;

    @Column(name = "target", length = 50)
    private String target;

    @Column(name = "starttime")
    private Date starttime;

    @Column(name = "outcome", length = 50)
    private String outcome;

    @Column(name = "message", length = 255)
    private String message;

    @Column(name = "subrequestId", length = 100)
    private String subrequestId;

    @Column(name = "endtime")
    private Date endtime;

}
