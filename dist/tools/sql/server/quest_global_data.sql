CREATE TABLE IF NOT EXISTS `quest_global_data` (
  `quest_name` VARCHAR(40) NOT NULL DEFAULT '',
  `var`  varchar(86) NOT NULL default '',
  `value` varchar(255) character set utf8 NOT NULL default '',
  PRIMARY KEY (`quest_name`,`var`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;