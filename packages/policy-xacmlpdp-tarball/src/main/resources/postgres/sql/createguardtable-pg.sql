-- ============LICENSE_START=======================================================
-- Copyright (C) 2022 Nordix Foundation. All rights reserved.
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

create table if not exists operationshistory (
    id int(11) SERIAL PRIMARY KEY,
    closedLoopName varchar(255) not null,
    requestId varchar(50),
    actor varchar(50) not null,
    operation varchar(50) not null,
    target varchar(50) not null,
    starttime timestamp not null,
    outcome varchar(50) not null,
    message varchar(255),
    subrequestId varchar(50),
    endtime timestamp not null default current_timestamp
);

create index if not exists operationshistory_clreqid_index on
    operationshistory(requestId, closedLoopName);

create index if not exists operationshistory_target_index on
    operationshistory(target, operation, actor, endtime);
