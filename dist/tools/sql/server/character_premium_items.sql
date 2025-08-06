CREATE TABLE IF NOT EXISTS `character_premium_items` (
  `charId` int(11) NOT NULL,
  `itemNum` int(11) NOT NULL,
  `itemId` int(11) NOT NULL,
  `itemCount` bigint(20) unsigned NOT NULL,
  `itemSender` varchar(50) NOT NULL,
  `status` int(1) NOT NULL DEFAULT '0',
  `time` varchar(86) NOT NULL default '',
  `recipient` varchar(300) NOT NULL default '',
  KEY `charId` (`charId`),
  KEY `itemNum` (`itemNum`),
  KEY `itemId` (`itemId`),
  KEY `status` (`status`),
  KEY `time` (`time`),
  KEY `recipient` (`recipient`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;