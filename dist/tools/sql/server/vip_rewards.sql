DROP TABLE IF EXISTS `vip_rewards`;
CREATE TABLE `vip_rewards` (
  `value` varchar(255) NOT NULL,
  `time` bigint(20) NOT NULL DEFAULT '0'
)ENGINE=InnoDB DEFAULT CHARSET=utf8;