-- ----------------------------
-- Table structure for hwid_list
-- ----------------------------
DROP TABLE IF EXISTS `hwid_list`;
CREATE TABLE `hwid_list`  (
  `hwid` varchar(32) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL DEFAULT '',
  `windows_count` int(10) UNSIGNED NOT NULL DEFAULT 1,
  `login` varchar(45) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL DEFAULT '',
  PRIMARY KEY (`hwid`) USING BTREE
) ENGINE = MyISAM CHARACTER SET = utf8 COLLATE = utf8_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for hwid_bans
-- ----------------------------
DROP TABLE IF EXISTS `hwid_bans`;
CREATE TABLE `hwid_bans`  (
   `hwid` varchar(32) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL,
   `comment` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT '',
   `ban_type` enum('PLAYER_BAN','ACCOUNT_BAN','NONE') CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL DEFAULT 'NONE',
   `name` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT '',
   UNIQUE INDEX `hwid`(`hwid`) USING BTREE
) ENGINE = MyISAM CHARACTER SET = utf8 COLLATE = utf8_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for protection_info_list
-- ----------------------------
DROP TABLE IF EXISTS `protection_info_list`;
CREATE TABLE `protection_info_list`  (
   `login` varchar(45) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL DEFAULT '',
   `buf` varchar(1024) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT '',
   PRIMARY KEY (`login`) USING BTREE
) ENGINE = MyISAM CHARACTER SET = utf8 COLLATE = utf8_general_ci ROW_FORMAT = Dynamic;

