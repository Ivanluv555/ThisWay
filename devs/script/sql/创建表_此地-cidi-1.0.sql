CREATE TABLE IF NOT EXISTS `User` (
	`userID` INTEGER NOT NULL AUTO_INCREMENT UNIQUE,
	`Wechat_UnionID` VARCHAR(255) NOT NULL,
	`nickname` VARCHAR(255) NOT NULL,
	`tel_number` VARCHAR(255) NOT NULL,
	`signup_time` DATETIME NOT NULL,
	`is_ban` BOOLEAN NOT NULL,
	PRIMARY KEY(`userID`, `Wechat_UnionID`)
);


CREATE TABLE IF NOT EXISTS `Location` (
	`locaID` INTEGER NOT NULL AUTO_INCREMENT UNIQUE,
	`is_open` BOOLEAN,
	`price` DECIMAL,
	`name` VARCHAR(255) NOT NULL,
	`latitude` DECIMAL NOT NULL,
	`longitude` DECIMAL NOT NULL,
	`hot_temp` DECIMAL NOT NULL,
	`rating` DECIMAL NOT NULL,
	`post_time` INTEGER NOT NULL,
	PRIMARY KEY(`locaID`)
);


CREATE TABLE IF NOT EXISTS `Admin` (
	`adminID` INTEGER NOT NULL AUTO_INCREMENT UNIQUE,
	`job` VARCHAR(255),
	`username` VARCHAR(255) NOT NULL,
	`password_harsh` VARCHAR(255) NOT NULL,
	PRIMARY KEY(`adminID`)
);


CREATE TABLE IF NOT EXISTS `Post` (
	`postID` INTEGER NOT NULL AUTO_INCREMENT UNIQUE,
	`title` VARCHAR(255),
	`content` TEXT(65535),
	`userID` INTEGER,
	`post_time` DATETIME,
	`count_eye` INTEGER,
	`locaID` INTEGER,
	PRIMARY KEY(`postID`)
);


CREATE TABLE IF NOT EXISTS `User_History` (
	`hisID` INTEGER,
	`userID` INTEGER NOT NULL AUTO_INCREMENT UNIQUE,
	`postID` INTEGER,
	PRIMARY KEY(`hisID`)
);


CREATE TABLE IF NOT EXISTS `User_Auditlog` (
	`u_logID` VARCHAR(255) NOT NULL UNIQUE,
	`userID` INTEGER NOT NULL,
	`op_type` VARCHAR(255) NOT NULL,
	`op_time` DATETIME NOT NULL,
	`target_type` VARCHAR(255) NOT NULL,
	`IPaddress` VARCHAR(255),
	`targetID` INTEGER NOT NULL,
	`detail` VARCHAR(255),
	PRIMARY KEY(`u_logID`)
);


CREATE TABLE IF NOT EXISTS `Admin_Auditlog` (
	`a_logID` INTEGER NOT NULL AUTO_INCREMENT UNIQUE,
	`adminID` INTEGER NOT NULL,
	`op_type` VARCHAR(255) NOT NULL,
	`op_time` DATETIME NOT NULL,
	`target_type` VARCHAR(255) NOT NULL,
	`targetID` INTEGER NOT NULL,
	PRIMARY KEY(`a_logID`)
);


