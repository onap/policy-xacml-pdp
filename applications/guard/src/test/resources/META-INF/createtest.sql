#
# Create the operations history table
#
CREATE TABLE `operationshistory` 
    (
    `id` bigint not null,
    `closedLoopName` varchar(255) not null,
    `requestId` varchar(50) not null, 
    `subrequestId` varchar(50) not null, 
    `actor` varchar(50) not null,
    `operation` varchar(50) not null, 
    `target` varchar(50) not null,
    `starttime` timestamp not null,
    `outcome` varchar(50) not null,
    `message` varchar(255) not null,
    `endtime` timestamp not null);
