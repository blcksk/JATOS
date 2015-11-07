# Initial table creation 

# --- !Ups
 
CREATE TABLE `Component` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `active` tinyint(1) NOT NULL,
  `comments` longtext,
  `date` datetime DEFAULT NULL,
  `htmlFilePath` varchar(255) DEFAULT NULL,
  `jsonData` longtext,
  `reloadable` tinyint(1) NOT NULL,
  `title` varchar(255) DEFAULT NULL,
  `uuid` varchar(255) NOT NULL,
  `study_id` bigint(20) DEFAULT NULL,
  `componentList_order` int(11) DEFAULT NULL,
  PRIMARY KEY (`id`)
) DEFAULT CHARSET=utf8;
 

CREATE TABLE `ComponentResult` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `componentState` int(11) DEFAULT NULL,
  `data` longtext,
  `endDate` datetime DEFAULT NULL,
  `errorMsg` varchar(255) DEFAULT NULL,
  `startDate` datetime DEFAULT NULL,
  `component_id` bigint(20) DEFAULT NULL,
  `studyResult_id` bigint(20) DEFAULT NULL,
  `componentResultList_order` int(11) DEFAULT NULL,
  PRIMARY KEY (`id`)
) DEFAULT CHARSET=utf8;

CREATE TABLE `Groupp` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `messaging` tinyint(1) NOT NULL,
  `minActiveMemberSize` int(11) NOT NULL,
  `maxActiveMemberSize` int(11) NOT NULL,
  `maxTotalMemberSize` int(11) NOT NULL,
  `study_id` bigint(20) DEFAULT NULL,
  `title` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`id`)
) DEFAULT CHARSET=utf8;

CREATE TABLE `GroupResult` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `endDate` datetime DEFAULT NULL,
  `groupState` int(11) DEFAULT NULL,
  `startDate` datetime DEFAULT NULL,
  `group_id` bigint(20) DEFAULT NULL,
  PRIMARY KEY (`id`)
) DEFAULT CHARSET=utf8;

CREATE TABLE `Study` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `comments` longtext ,
  `date` datetime DEFAULT NULL,
  `description` longtext ,
  `dirName` varchar(255) DEFAULT NULL,
  `jsonData` longtext ,
  `locked` tinyint(1) NOT NULL,
  `title` varchar(255) DEFAULT NULL,
  `uuid` varchar(255) NOT NULL,
  PRIMARY KEY (`id`)
) DEFAULT CHARSET=utf8;

CREATE TABLE `StudyResult` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `abortMsg` varchar(255) DEFAULT NULL,
  `confirmationCode` varchar(255) DEFAULT NULL,
  `endDate` datetime DEFAULT NULL,
  `errorMsg` varchar(255) DEFAULT NULL,
  `startDate` datetime DEFAULT NULL,
  `studySessionData` longtext ,
  `studyState` int(11) DEFAULT NULL,
  `groupResult_id` bigint(20) DEFAULT NULL,
  `study_id` bigint(20) DEFAULT NULL,
  `worker_id` bigint(20) DEFAULT NULL,
  `groupResultHistory_id` bigint(20) DEFAULT NULL,
  `studyResultList_order` int(11) DEFAULT NULL,
  PRIMARY KEY (`id`)
) DEFAULT CHARSET=utf8;

CREATE TABLE `StudyUserMap` (
  `study_id` bigint(20) NOT NULL,
  `user_email` varchar(255) NOT NULL,
  PRIMARY KEY (`study_id`,`user_email`)
) DEFAULT CHARSET=utf8;

CREATE TABLE `Study_allowedWorkerTypeList` (
  `Study_id` bigint(20) NOT NULL,
  `allowedWorkerTypeList` varchar(255) DEFAULT NULL
) DEFAULT CHARSET=utf8;

CREATE TABLE `User` (
  `email` varchar(255) NOT NULL,
  `name` varchar(255) DEFAULT NULL,
  `passwordHash` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`email`)
) DEFAULT CHARSET=utf8;

CREATE TABLE `Worker` (
  `workerType` varchar(31) NOT NULL,
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `mtWorkerId` varchar(255) DEFAULT NULL,
  `comment` varchar(255) DEFAULT NULL,
  `user_email` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`id`)
) DEFAULT CHARSET=utf8;


ALTER TABLE `Component` ADD KEY `FK_hv7ffe3qq68l092inojua2sxa` (`study_id`);
ALTER TABLE `Component` ADD CONSTRAINT `FK_hv7ffe3qq68l092inojua2sxa` FOREIGN KEY (`study_id`) REFERENCES `Study` (`id`);

ALTER TABLE `ComponentResult` ADD KEY `FK_eevh468dxdqmcwsu8cqm4i2et` (`studyResult_id`);
ALTER TABLE `ComponentResult` ADD KEY `FK_qxb7hfq7d4vaf9r5vjvnxpuwm` (`component_id`);
ALTER TABLE `ComponentResult` ADD CONSTRAINT `FK_eevh468dxdqmcwsu8cqm4i2et` FOREIGN KEY (`studyResult_id`) REFERENCES `StudyResult` (`id`);
ALTER TABLE `ComponentResult` ADD CONSTRAINT `FK_qxb7hfq7d4vaf9r5vjvnxpuwm` FOREIGN KEY (`component_id`) REFERENCES `Component` (`id`);

ALTER TABLE `Groupp` ADD KEY `FK_80kwrl4v39mbxsw13mg09vnbi` (`study_id`);
ALTER TABLE `Groupp` ADD CONSTRAINT `FK_80kwrl4v39mbxsw13mg09vnbi` FOREIGN KEY (`study_id`) REFERENCES `Study` (`id`);

ALTER TABLE `GroupResult` ADD KEY `FK_g1hsnkt6f7jp8ulpne7h87pi1` (`group_id`);
ALTER TABLE `GroupResult` ADD CONSTRAINT `FK_g1hsnkt6f7jp8ulpne7h87pi1` FOREIGN KEY (`group_id`) REFERENCES `Groupp` (`id`);

ALTER TABLE `Study` ADD UNIQUE KEY `UK_k65c7qp8ndhaqllkeeianjrpc` (`uuid`);

ALTER TABLE `StudyResult` ADD KEY `FK_2vbvsrpwxwnqbd0rud8kfr9ur` (`groupResult_id`);
ALTER TABLE `StudyResult` ADD KEY `FK_iiln24n58g3b1mxx3vupmg36h` (`study_id`);
ALTER TABLE `StudyResult` ADD KEY `FK_dggkq2gf4lsibvfxqrc25r8m6` (`worker_id`);
ALTER TABLE `StudyResult` ADD KEY `FK_7052aavudt8sm5b6a3lhqn4uu` (`groupResultHistory_id`);
ALTER TABLE `StudyResult` ADD CONSTRAINT `FK_7052aavudt8sm5b6a3lhqn4uu` FOREIGN KEY (`groupResultHistory_id`) REFERENCES `GroupResult` (`id`);
ALTER TABLE `StudyResult` ADD CONSTRAINT `FK_2vbvsrpwxwnqbd0rud8kfr9ur` FOREIGN KEY (`groupResult_id`) REFERENCES `GroupResult` (`id`);
ALTER TABLE `StudyResult` ADD CONSTRAINT `FK_dggkq2gf4lsibvfxqrc25r8m6` FOREIGN KEY (`worker_id`) REFERENCES `Worker` (`id`);
ALTER TABLE `StudyResult` ADD CONSTRAINT `FK_iiln24n58g3b1mxx3vupmg36h` FOREIGN KEY (`study_id`) REFERENCES `Study` (`id`);

ALTER TABLE `StudyUserMap` ADD KEY `FK_d3uknug3vjrsetf527b7uplcd` (`user_email`);
ALTER TABLE `StudyUserMap` ADD KEY `FK_povwnfi99xfcfiyloh0ufv7hb` (`study_id`);
ALTER TABLE `StudyUserMap` ADD CONSTRAINT `FK_povwnfi99xfcfiyloh0ufv7hb` FOREIGN KEY (`study_id`) REFERENCES `Study` (`id`);
ALTER TABLE `StudyUserMap` ADD CONSTRAINT `FK_d3uknug3vjrsetf527b7uplcd` FOREIGN KEY (`user_email`) REFERENCES `User` (`email`);
  
ALTER TABLE `Study_allowedWorkerTypeList` ADD KEY `FK_kwj5qdspmur6iqdgtb7kjvdg2` (`Study_id`);
ALTER TABLE `Study_allowedWorkerTypeList` ADD CONSTRAINT `FK_kwj5qdspmur6iqdgtb7kjvdg2` FOREIGN KEY (`Study_id`) REFERENCES `Study` (`id`);

ALTER TABLE `Worker` ADD KEY `FK_rvmm2rl58o8ui2tsq774o8rij` (`user_email`);
ALTER TABLE `Worker` ADD CONSTRAINT `FK_rvmm2rl58o8ui2tsq774o8rij` FOREIGN KEY (`user_email`) REFERENCES `User` (`email`);


# --- !Downs
 
DROP TABLE IF EXISTS `Component`;
DROP TABLE IF EXISTS `ComponentResult`;
DROP TABLE IF EXISTS `Group`;
DROP TABLE IF EXISTS `GroupResult`;
DROP TABLE IF EXISTS `Study`;
DROP TABLE IF EXISTS `StudyResult`;
DROP TABLE IF EXISTS `StudyUserMap`;
DROP TABLE IF EXISTS `Study_allowedWorkerTypeList`;
DROP TABLE IF EXISTS `User`;
DROP TABLE IF EXISTS `Worker`;


