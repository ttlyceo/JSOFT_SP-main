CREATE TABLE IF NOT EXISTS `auction_bid` (
  `id` INT NOT NULL default 0,
  `auctionId` INT NOT NULL default 0,
  `bidderId` INT NOT NULL default 0,
  `bidderName` varchar(50) NOT NULL,
  `clanId` INT NOT NULL DEFAULT 0,
  `maxBid` BIGINT UNSIGNED NOT NULL default 0,
  `time_bid` bigint(13) unsigned NOT NULL DEFAULT '0',
  PRIMARY KEY  (`auctionId`,`bidderId`),
  KEY `id` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;