DROP TABLE IF EXISTS `character_donate_rates`;
CREATE TABLE `character_donate_rates` (
  `charId` int(11) NOT NULL DEFAULT '0',
  `id` int(3) NOT NULL DEFAULT '0',
  `expire_time` decimal(20,0) NOT NULL DEFAULT '0',
  UNIQUE KEY `prim` (`charId`,`id`),
  KEY `charId` (`charId`),
  KEY `id` (`id`),
  KEY `expire_time` (`expire_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;