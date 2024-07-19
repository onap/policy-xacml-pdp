-- ============LICENSE_START=======================================================
-- Copyright (C) 2022, 2024 Nordix Foundation. All rights reserved.
-- ================================================================================
-- Licensed under the Apache License, Version 2.0 (the "License");
-- you may not use this file except in compliance with the License.
-- You may obtain a copy of the License at
--
--      http://www.apache.org/licenses/LICENSE-2.0
--
-- Unless required by applicable law or agreed to in writing, software
-- distributed under the License is distributed on an "AS IS" BASIS,
-- WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
-- See the License for the specific language governing permissions and
-- limitations under the License.
-- ============LICENSE_END=========================================================

\c operationshistory;

CREATE TABLE IF NOT EXISTS operationshistory
(
    id             SERIAL PRIMARY KEY,
    closedLoopName VARCHAR(255) NOT NULL,
    requestId      VARCHAR(50),
    actor          VARCHAR(50)  NOT NULL,
    operation      VARCHAR(50)  NOT NULL,
    target         VARCHAR(50)  NOT NULL,
    starttime      TIMESTAMP    NOT NULL,
    outcome        VARCHAR(50)  NOT NULL,
    message        VARCHAR(255),
    subrequestId   VARCHAR(50),
    endtime        TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);

create index if not exists operationshistory_clreqid_index on
    operationshistory (requestId, closedLoopName);

create index if not exists operationshistory_target_index on
    operationshistory (target, operation, actor, endtime);
