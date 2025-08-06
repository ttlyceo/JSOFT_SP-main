DROP TABLE IF EXISTS `clan_variables`;
CREATE TABLE `clan_variables` (
  `obj_id` int(11) NOT NULL DEFAULT '0',
  `type` varchar(86) NOT NULL DEFAULT '0',
  `name` varchar(86) NOT NULL DEFAULT '0',
  `value` text NOT NULL,
  `expire_time` decimal(20,0) NOT NULL DEFAULT '0',
  UNIQUE KEY `prim` (`obj_id`,`type`,`name`),
  KEY `obj_id` (`obj_id`),
  KEY `type` (`type`),
  KEY `name` (`name`),
  KEY `expire_time` (`expire_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;