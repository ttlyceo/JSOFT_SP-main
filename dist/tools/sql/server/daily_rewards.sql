-- ----------------------------
-- Table structure for daily_rewards
-- ----------------------------
DROP TABLE IF EXISTS `daily_rewards`;
CREATE TABLE `daily_rewards`  (
  `charInfo` varchar(300) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL DEFAULT '',
  `value` varchar(300) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL DEFAULT '',
  `last_day` int(2) NOT NULL DEFAULT 1,
  INDEX `charInfo`(`charInfo`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8 COLLATE = utf8_general_ci ROW_FORMAT = Dynamic;



