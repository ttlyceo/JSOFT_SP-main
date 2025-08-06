CREATE TABLE IF NOT EXISTS `messages` (
  `messageId` INT NOT NULL DEFAULT 0,
  `senderId` INT NOT NULL DEFAULT 0,
  `receiverId` INT NOT NULL DEFAULT 0,
  `subject` TINYTEXT,
  `content` TEXT,
  `expiration` bigint(13) unsigned NOT NULL DEFAULT '0',
  `reqAdena` BIGINT NOT NULL DEFAULT 0,
  `hasAttachments` tinyint(1) NOT NULL DEFAULT 0,
  `isUnread` tinyint(1) NOT NULL DEFAULT 1,
  `isDeletedBySender` tinyint(1) NOT NULL DEFAULT 0,
  `isDeletedByReceiver` tinyint(1) NOT NULL DEFAULT 0,
  `isLocked` tinyint(1) NOT NULL DEFAULT 0,
  `sendBySystem` tinyint(1) NOT NULL DEFAULT 0,
  `isReturned` tinyint(1) NOT NULL DEFAULT 0,
  PRIMARY KEY (`messageId`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;