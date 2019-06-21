use operationshistory;

create table if not exists operationshistory (
    id int(11) not null auto_increment,
    closedLoopName varchar(255) not null,
    requestId varchar(50),
    actor varchar(50) not null,
    operation varchar(50) not null,
    target varchar(50) not null,
    starttime timestamp not null,
    outcome varchar(50) not null,
    message varchar(255),
    subrequestId varchar(50),
    endtime timestamp not null default current_timestamp,
    PRIMARY KEY (id)
);

insert into operationshistory (`closedLoopName`,`requestId`,`actor`,`operation`,`target`,`starttime`,`outcome`,`message`,`subrequestId`,`endtime`)
VALUES('ControlLoop-vDNS-6f37f56d-a87d-4b85-b6a9-cc953cf779b3','test001122','SO','VF Module Create','.*',NOW() - INTERVAL 1 HOUR,'SUCCESS',null,null,Now());
