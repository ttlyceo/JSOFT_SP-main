-- ----------------------------
-- Table structure for protection_info_list
-- ----------------------------
DROP TABLE IF EXISTS `protection_info_list`;
CREATE TABLE `protection_info_list`  (
  `login` varchar(45) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL DEFAULT '',
  `buf` varchar(1024) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT '',
  PRIMARY KEY (`login`) USING BTREE
) ENGINE = MyISAM CHARACTER SET = utf8 COLLATE = utf8_general_ci ROW_FORMAT = Dynamic;

