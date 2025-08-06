DROP TABLE IF EXISTS `votereward_records`;
CREATE TABLE `votereward_records` (
  `site` VARCHAR(45) NOT NULL,
  `identifier` varchar(300) NOT NULL default '',
  `votes` SMALLINT NOT NULL DEFAULT 0,
  `lastvotedate` INT NOT NULL DEFAULT -1,
  PRIMARY KEY (`site`,`identifier`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;