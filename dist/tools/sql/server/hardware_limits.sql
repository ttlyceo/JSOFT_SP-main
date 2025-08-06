DROP TABLE IF EXISTS `hardware_limits`;
CREATE TABLE `hardware_limits` (
  `hardware` varchar(255) NOT NULL,
  `windows_limit` smallint(5) NOT NULL,
  `limit_expire` decimal(20,0) NOT NULL DEFAULT '0',
  PRIMARY KEY (`hardware`)
)ENGINE=InnoDB DEFAULT CHARSET=utf8;