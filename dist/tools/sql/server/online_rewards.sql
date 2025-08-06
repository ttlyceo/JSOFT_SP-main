-- ----------------------------
-- Table structure for online_rewards
-- ----------------------------
DROP TABLE IF EXISTS `online_rewards`;
CREATE TABLE `online_rewards`  (
  `charInfo` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL DEFAULT '',
  `list` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL DEFAULT '',
  INDEX `charInfo`(`charInfo`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8 COLLATE = utf8_general_ci ROW_FORMAT = Dynamic;



