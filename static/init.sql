CREATE TABLE `companies` (
    `id` varchar(40) DEFAULT (uuid()) NOT NULL,
    `createdAt` timestamp DEFAULT current_timestamp NOT NULL,
    `name` varchar(50) NOT NULL,
    `address` varchar(100),
    `emailAddress` varchar(100),
    `phoneNumber` varchar(12),
    `websiteUrl` varchar(150),
    PRIMARY KEY (`id`)
);

CREATE TABLE `carparks` (
    `id` varchar(40) DEFAULT (uuid()),
    `createdAt` timestamp DEFAULT current_timestamp NOT NULL,
    `name` varchar(50) NOT NULL,
    `payByPhoneCode` varchar(10),
    `ownerId` varchar(40),
    `type` varchar(20) NOT NULL,
    `surfaceType` varchar(20) NOT NULL,
    `multiStorey` boolean DEFAULT 0 NOT NULL,
    `latitude` decimal(10, 8),
    `longitude` decimal(11, 8),
    `spaces` int NOT NULL,
    `disabledSpaces` int DEFAULT 0 NOT NULL,
    `parentChildSpaces` int DEFAULT 0 NOT NULL,
    `electricChargingSpaces` int DEFAULT 0 NOT NULL,
    `liveTrackingCode` varchar(30),
    `notes` text,
    FOREIGN KEY (`ownerId`) REFERENCES `companies`(`id`) ON DELETE SET NULL,
    PRIMARY KEY (`id`)
);

CREATE TABLE `carparkPaymentMethods` (
    `id` int auto_increment NOT NULL,
    `carparkId` varchar(40) NOT NULL,
    `paymentMethod` varchar(20) NOT NULL,
    FOREIGN KEY (`carparkId`) REFERENCES `carparks`(`id`) ON DELETE CASCADE,
    PRIMARY KEY (`id`)
);

CREATE TABLE `liveParkingSpaces` (
    `id` int auto_increment NOT NULL,
    `createdAt` timestamp NOT NULL,
    `name` varchar(30) NOT NULL,
    `code` varchar(20) NOT NULL,
    `spaces` int NOT NULL,
    `status` varchar(20) NOT NULL,
    `open` boolean NOT NULL,
    PRIMARY KEY (`id`)
);

CREATE TABLE `vehicles` (
    `id` int auto_increment NOT NULL,
    `firstRegisteredAt` date NOT NULL,
    `firstRegisteredInJerseyAt` date NOT NULL,
    `make` varchar(40) NOT NULL,
    `model` varchar(40),
    `color` varchar(40),
    `cylinderCapacity` int,
    `weight` varchar(30),
    `co2Emissions` int,
    `fuelType` varchar(30) NOT NULL,
    PRIMARY KEY (`id`)
);

ALTER TABLE `vehicles` ADD INDEX `vehicle_hash_index` (`hash`);

CREATE TABLE `recyclingCentres` (
    `id` varchar(40) DEFAULT (uuid()),
    `createdAt` timestamp DEFAULT current_timestamp NOT NULL,
    `location` varchar(80) NOT NULL,
    `parish` varchar(30) NOT NULL,
    `latitude` decimal(10, 8),
    `longitude` decimal(11, 8),
    `notes` text,
    PRIMARY KEY (`id`)
);

CREATE TABLE `recyclingCentreServices` (
    `id` int auto_increment NOT NULL,
    `recyclingCentreId` varchar(40) NOT NULL,
    `service` varchar(50) NOT NULL, 
    FOREIGN KEY (`recyclingCentreId`) REFERENCES `recyclingCentres`(`id`) ON DELETE CASCADE,
    PRIMARY KEY (`id`)
);

CREATE TABLE `publicToilets` (
    `id` varchar(40) DEFAULT (uuid()),
    `createdAt` timestamp DEFAULT current_timestamp NOT NULL,
    `name` varchar(120) NOT NULL,
    `parish` varchar(30) NOT NULL,
    `latitude` decimal(10, 8),
    `longitude` decimal(11, 8),
    `tenure` varchar(20) NOT NULL,
    `ownerId` varchar(40),
    `female` boolean NOT NULL,
    `femaleCubicles` int,
    `femaleHandDryers` int,
    `femaleSinks` int,
    `male` boolean NOT NULL,
    `maleCubicles` int,
    `maleUrinals` int,
    `maleHandDryers` int,
    `maleSinks` int,
    FOREIGN KEY (`ownerId`) REFERENCES `companies`(`id`) ON DELETE SET NULL,
    PRIMARY KEY (`id`)
);

CREATE TABLE `publicToiletFacilities` (
    `id` int auto_increment NOT NULL,
    `facility` varchar(30) NOT NULL,
    `publicToiletId` varchar(40) NOT NULL,
    FOREIGN KEY (`publicToiletId`) REFERENCES `publicToilets`(`id`) ON DELETE CASCADE,
    PRIMARY KEY (`id`)
);

CREATE TABLE `publicToiletPeriodProducts` (
    `id` int auto_increment NOT NULL,
    `product` varchar(30) NOT NULL,
    `type` varchar(20) NOT NULL, /* enum */
    `publicToiletId` varchar(40) NOT NULL,
    FOREIGN KEY (`publicToiletId`) REFERENCES `publicToilets`(`id`) ON DELETE CASCADE,
    PRIMARY KEY (`id`)
);

CREATE TABLE `busStops` (
    `id` varchar(40) NOT NULL,
    `createdAt` timestamp DEFAULT current_timestamp NOT NULL,
    `name` varchar(50) NOT NULL,
    `stopNumber` varchar(4) NOT NULL,
    `latitude` decimal(10, 8),
    `longitude` decimal(11, 8),
    `shelter` boolean NOT NULL,
    PRIMARY KEY (`id`)
);

CREATE TABLE `productRecalls` (
    `id` int NOT NULL,
    `title` varchar(150) NOT NULL,
    `imageUrl` varchar(250),
    `brand` varchar(120) NOT NULL,
    `recallDate` timestamp NOT NULL,
    `packSize` varchar(220),
    `batchCodes` text,
    `problem` text,
    `furtherInformation` text,
    `websiteUrl` varchar(250),
    PRIMARY KEY (`id`)
);

CREATE TABLE `liveClsQueuesData` (
    `id` int auto_increment NOT NULL,
    `createdAt` timestamp NOT NULL,
    `name` varchar(50) NOT NULL,
    `queueId` int NOT NULL,
    `color` varchar(7) NOT NULL,
    `waiting` int NOT NULL,
    `waitingMinutes` int NOT NULL,
    `open` boolean NOT NULL,
    PRIMARY KEY (`id`)
);

CREATE TABLE `foiRequests` (
    `id` int NOT NULL,
    `publishDate` timestamp NOT NULL,
    `title` varchar(150) NOT NULL,
    `producer` varchar(250) NOT NULL,
    `author` varchar(250) NOT NULL,
    `requestText` mediumtext,
    `responseText` mediumtext,
    PRIMARY KEY (`id`)
);

CREATE TABLE `magistratesCourtHearings` (
    `id` int auto_increment NOT NULL,
    `appearanceDate` timestamp,
    `courtRoom` varchar(100),
    `hearingPurpose` varchar(255),
    `defendant` varchar(255),
    PRIMARY KEY (`id`)
);

ALTER TABLE `magistratesCourtHearings` ADD UNIQUE KEY `unique_magistrateCourt_hearing` (`appearanceDate`, `courtRoom`, `hearingPurpose`, `defendant`);

CREATE TABLE `magistratesCourtResults` (
     `id` int auto_increment NOT NULL,
     `appearanceDate` timestamp,
     `video` varchar(50),
     `hearingPurpose` varchar(255),
     `result` TEXT,
     `remandedOrBailed` varchar(50),
     `nextAppearanceDate` timestamp,
     `courtRoom` varchar(100),
     `lawOfficer` varchar(255),
     `defendant` varchar(255),
     `magistrate` varchar(255),
     PRIMARY KEY (`id`)
 );

ALTER TABLE `magistratesCourtResults` ADD UNIQUE KEY `unique_magistratesCourt_result` (`appearanceDate`, `courtRoom`, `hearingPurpose`, `defendant`, `video`, `remandedOrBailed`);

CREATE TABLE magistratesCourtResultOffences (
    `id` int auto_increment NOT NULL,
    `resultId` int NOT NULL,
    `offence` varchar(255),
    PRIMARY KEY(`id`),
    UNIQUE KEY `unique_offence` (`resultId`, `offence`)
);

CREATE TABLE `users` (
    `id` varchar(40) NOT NULL,
    `createdAt` timestamp DEFAULT current_timestamp NOT NULL,
    `updatedAt` timestamp DEFAULT current_timestamp ON UPDATE current_timestamp,
    `email` varchar(200) NOT NULL,
    `password` varchar(60) NOT NULL,
    `emailVerificationToken` varchar(40),
    `passwordResetToken` varchar(40),
    `emailVerified` boolean DEFAULT 0 NOT NULL,
    `siteAdmin` boolean DEFAULT 0 NOT NULL,
    PRIMARY KEY (`id`)
);

CREATE TABLE `apiTokens` (
    `id` varchar(40) NOT NULL,
    `createdAt` timestamp DEFAULT current_timestamp NOT NULL,
    `userId` varchar(40) NOT NULL,
    `token` varchar(40) NOT NULL,
    `summary` varchar(200),
    FOREIGN KEY (`userId`) REFERENCES `users`(`id`) ON DELETE CASCADE,
    PRIMARY KEY (`id`)
);

CREATE TABLE `apiRequests` (
    `id` varchar(40) NOT NULL,
    `createdAt` timestamp DEFAULT current_timestamp NOT NULL,
    `method` varchar(10) NOT NULL,
    `path` varchar(200) NOT NULL,
    `statusCode` int NOT NULL,
    `ipAddress` varchar(45) NOT NULL,
    `userAgent` varchar(255),
    `apiTokenId` varchar(40),
    FOREIGN KEY (`apiTokenId`) REFERENCES `apiTokens`(`id`) ON DELETE SET NULL,
    PRIMARY KEY (`id`)
);

CREATE TABLE `petitions` (
    `id` bigint NOT NULL,
    `createdAt` datetime DEFAULT current_timestamp NOT NULL,
    `updatedAt` datetime,
    `openedAt` datetime,
    `closedAt` datetime,
    `rejectedAt` datetime,
    `moderationThresholdReachedAt` datetime,
    `responseThresholdReachedAt` datetime,
    `debateThresholdReachedAt` datetime,
    `scheduledDebateDate` datetime,
    `debateOutcomeAt` datetime,
    `state` varchar(50),
    `creatorName` varchar(150),
    `title` text NOT NULL,
    `summary` text NOT NULL,
    `description` text,
    `signatureCount` int,
    `rejectionCode` varchar(50),
    `rejectionDetails` text,
    `fullyProcessedClosed` boolean DEFAULT FALSE NOT NULL,
    PRIMARY KEY (`id`)
);

CREATE TABLE `petitionMinistersResponses` (
    `id` int AUTO_INCREMENT NOT NULL,
    `createdAt` datetime NOT NULL,
    `updatedAt` datetime NOT NULL,
    `publishedOn` date NOT NULL,
    `petitionId` bigint NOT NULL,
    `summary` text,
    `description` text,
    FOREIGN KEY (`petitionId`) REFERENCES `petitions`(`id`) ON DELETE CASCADE,
    PRIMARY KEY (`id`)
);

CREATE TABLE `petitionDebates` (
    `id` int AUTO_INCREMENT NOT NULL,
    `debatedOn` date NOT NULL,
    `petitionId` bigint NOT NULL,
    `transcriptUrl` varchar(255),
    `videoUrl` varchar(255),
    `debatePackUrl` varchar(255),
    `overview` text,
    FOREIGN KEY (`petitionId`) REFERENCES `petitions`(`id`) ON DELETE CASCADE,
    PRIMARY KEY (`id`)
);

CREATE TABLE `petitionSignaturesByParish` (
    `id` int AUTO_INCREMENT NOT NULL,
    `petitionId` bigint NOT NULL,
    `parishName` varchar(50),
    `signatureCount` int,
    FOREIGN KEY (`petitionId`) REFERENCES `petitions`(`id`) ON DELETE CASCADE,
    PRIMARY KEY (`id`)
);
