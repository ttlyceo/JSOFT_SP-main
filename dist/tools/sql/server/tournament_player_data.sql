-- ----------------------------
-- Table structure for tournament_player_data
-- ----------------------------
DROP TABLE IF EXISTS `tournament_player_data`;
CREATE TABLE `tournament_player_data`  (
  `obj_id` int(11) NULL DEFAULT NULL,
  `fight_type` varchar(45) CHARACTER SET latin1 COLLATE latin1_swedish_ci NULL DEFAULT '',
  `fights_done` int(11) NULL DEFAULT NULL,
  `victories` int(11) NULL DEFAULT NULL,
  `defeats` int(11) NULL DEFAULT NULL,
  `ties` int(11) NULL DEFAULT NULL,
  `kills` int(11) NULL DEFAULT NULL,
  `damage` int(11) NULL DEFAULT NULL,
  `wdt` varchar(11) CHARACTER SET latin1 COLLATE latin1_swedish_ci NULL DEFAULT '',
  `dpf` varchar(11) CHARACTER SET latin1 COLLATE latin1_swedish_ci NULL DEFAULT ''
) ENGINE = InnoDB CHARACTER SET = latin1 COLLATE = latin1_swedish_ci ROW_FORMAT = Dynamic;

