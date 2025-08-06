CREATE TABLE IF NOT EXISTS `clan_notices` (
  `clan_id` INT NOT NULL DEFAULT 0,
  `enabled` tinyint(1) NOT NULL DEFAULT '0',
  `notice` TEXT NOT NULL,
  PRIMARY KEY  (`clan_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;