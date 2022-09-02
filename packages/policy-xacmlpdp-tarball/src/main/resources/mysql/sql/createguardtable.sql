-- ============LICENSE_START=======================================================
-- Copyright (C) 2019-2022 AT&T Intellectual Property. All rights reserved.
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

USE operationshistory;

CREATE TABLE IF NOT EXISTS operationshistory (
    id INT(11) NOT NULL AUTO_INCREMENT,
    closedLoopName VARCHAR(255) NOT NULL,
    requestId VARCHAR(50),
    actor VARCHAR(50) NOT NULL,
    operation VARCHAR(50) NOT NULL,
    target VARCHAR(50) NOT NULL,
    starttime timestamp NOT NULL,
    outcome VARCHAR(50) NOT NULL,
    message VARCHAR(255),
    subrequestId VARCHAR(50),
    endtime timestamp NULL DEFAULT current_timestamp,
    PRIMARY KEY (id)
);

DROP PROCEDURE IF EXISTS create_clreqid_index;

\d $$
CREATE PROCEDURE create_clreqid_index()
BEGIN
    DECLARE index_count INT DEFAULT 1;

    SELECT count(index_name) INTO index_count FROM information_schema.statistics
    WHERE table_schema=DATABASE() AND table_name='operationshistory' AND index_name='operationshistory_clreqid_index';

    IF index_count = 0 THEN
        CREATE INDEX operationshistory_clreqid_index ON operationshistory(requestId, closedLoopName);
    END IF;
END
$$

\d ;

CALL create_clreqid_index();

DROP PROCEDURE IF EXISTS create_target_index;

\d $$
CREATE PROCEDURE create_target_index()
BEGIN
    DECLARE index_count INT DEFAULT 1;

    SELECT count(index_name) INTO index_count FROM information_schema.statistics
    WHERE table_schema=DATABASE() AND table_name='operationshistory' AND index_name='operationshistory_target_index';

    IF index_count = 0 THEN
        CREATE INDEX operationshistory_target_index ON operationshistory(target, operation, actor, endtime);
    END IF;
END
$$

CALL create_target_index();
\d ;
