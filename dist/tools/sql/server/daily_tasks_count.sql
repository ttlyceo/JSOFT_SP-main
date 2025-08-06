-- ----------------------------
-- Table structure for daily_tasks_count
-- ----------------------------
DROP TABLE IF EXISTS `daily_tasks_count`;
CREATE TABLE `daily_tasks_count`  (
  `hwid` varchar(48) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL DEFAULT '',
  `ip` varchar(15) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL,
  `dailyCount` int(3) NOT NULL,
  `weeklyCount` int(3) NOT NULL,
  `monthCount` int(3) NOT NULL,
  INDEX `hwid`(`hwid`) USING BTREE,
  INDEX `ip`(`ip`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8 COLLATE = utf8_general_ci ROW_FORMAT = Dynamic;


