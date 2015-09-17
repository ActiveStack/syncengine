CREATE DATABASE IF NOT EXISTS as_example;

DROP TABLE IF EXISTS `update_table`;
CREATE TABLE `update_table` (
  `ID` int(11) NOT NULL AUTO_INCREMENT,
  `tableName` varchar(255) DEFAULT NULL,
  `rowID` varchar(255) DEFAULT NULL,
  `type` enum('INSERT','UPDATE','DELETE') NOT NULL DEFAULT 'UPDATE',
  `lockID` int(11) DEFAULT NULL,
  `lockDate` datetime DEFAULT NULL,
  `timestamp` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`ID`),
  KEY `tableName` (`tableName`),
  KEY `lockID` (`lockID`)
);