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

