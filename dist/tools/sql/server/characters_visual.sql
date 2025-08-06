-- ----------------------------
-- Table structure for characters_visual
-- ----------------------------
DROP TABLE IF EXISTS `characters_visual`;
CREATE TABLE `characters_visual`  (
  `charId` int(10) NOT NULL,
  `skinType` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL,
  `skinId` int(10) NOT NULL,
  `active` int(1) NOT NULL,
  INDEX `charId`(`charId`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8 COLLATE = utf8_general_ci ROW_FORMAT = Dynamic;
