-- ----------------------------
-- Table structure for daily_tasks
-- ----------------------------
DROP TABLE IF EXISTS `daily_tasks`;
CREATE TABLE `daily_tasks`  (
  `obj_Id` int(11) NOT NULL DEFAULT 0,
  `taskId` int(10) NOT NULL,
  `type` varchar(15) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL,
  `params` int(10) NOT NULL,
  `status` int(1) NOT NULL,
  `rewarded` int(1) NOT NULL
) ENGINE = InnoDB CHARACTER SET = utf8 COLLATE = utf8_general_ci ROW_FORMAT = Dynamic;

