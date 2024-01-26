CREATE TABLE `settings` (
  `app_name` varchar(50) PRIMARY KEY NOT NULL,
  `datasource_mode` boolean DEFAULT FALSE,
  `tools_folder` varchar(255) NOT NULL,
  `max_upload_size` int NOT NULL,
  `concurrent_users` int NOT NULL,
  `corrector_filename` varchar(50) NOT NULL,
  `default_css_path` varchar(255) DEFAULT NULL,
  `notice` varchar DEFAULT NULL
);

CREATE TABLE `mgmt_user` (
  `sid` integer PRIMARY KEY AUTO_INCREMENT,
  `username` varchar(50) UNIQUE NOT NULL,
  `name_full` varchar(255),
  `email` varchar(255),
  `password` varchar(255) NOT NULL,
  `type` int NOT NULL DEFAULT 2,
  `is_local` boolean,
  `exe_restrictions` varchar(255),
  `created` datetime NOT NULL,
  `updated` datetime NOT NULL
);

CREATE TABLE `tool` (
  `sid` integer PRIMARY KEY AUTO_INCREMENT,
  `name` varchar(50) UNIQUE NOT NULL,
  `description` varchar(200) DEFAULT NULL,
  `deliveryPassword` varchar(200),
  `enabled` boolean DEFAULT 0,
  `enabled_from` datetime DEFAULT NULL,
  `enabled_until` datetime DEFAULT NULL,
  `outcome` tinyint(1) DEFAULT 0,
  `extra_args` varchar(200),
  `type` int NOT NULL DEFAULT 0,
  `json_config` varchar(255),
  `created` datetime NOT NULL,
  `updated` datetime NOT NULL
);

CREATE TABLE `tool_counter` (
  `tool_sid` integer PRIMARY KEY,
  `counter` int NOT NULL DEFAULT 0
);

CREATE TABLE `tool_user` (
  `tool_sid` integer NOT NULL,
  `user_sid` integer NOT NULL,
  `type` int NOT NULL DEFAULT 2,
  PRIMARY KEY (`tool_sid`, `user_sid`)
);

CREATE TABLE `consumer` (
  `sid` integer PRIMARY KEY AUTO_INCREMENT,
  `guid` varchar(255) UNIQUE NOT NULL,
  `lti_version` varchar(12) DEFAULT NULL,
  `name` varchar(255) DEFAULT NULL,
  `version` varchar(255) DEFAULT NULL,
  `css_path` varchar(255) DEFAULT NULL,
  `created` datetime NOT NULL,
  `updated` datetime NOT NULL
);

CREATE TABLE `tool_key` (
  `sid` integer PRIMARY KEY AUTO_INCREMENT,
  `tool_sid` integer NOT NULL,
  `consumer_sid` integer,
  `context_sid` integer,
  `resource_link_sid` integer,
  `key` varchar(50) UNIQUE NOT NULL,
  `secret` varchar(255) NOT NULL,
  `enabled` boolean DEFAULT 1,
  `created` datetime NOT NULL,
  `updated` datetime NOT NULL
);

CREATE TABLE `nonce` (
  `key_sid` integer NOT NULL,
  `consumer_sid` integer NOT NULL,
  `value` varchar(255) NOT NULL,
  `ts` datetime NOT NULL,
  `expires` datetime NOT NULL,
  PRIMARY KEY (`key_sid`, `consumer_sid`, `ts`, `value`)
);

CREATE TABLE `context` (
  `sid` integer PRIMARY KEY AUTO_INCREMENT,
  `consumer_sid` integer,
  `context_id` varchar(255) DEFAULT NULL,
  `label` varchar(255) NOT NULL,
  `title` varchar(255) NOT NULL,
  `created` datetime NOT NULL,
  `updated` datetime NOT NULL
);

CREATE TABLE `resource_link` (
  `sid` integer PRIMARY KEY AUTO_INCREMENT,
  `tool_sid` integer,
  `context_sid` integer,
  `resource_id` varchar(255) DEFAULT NULL,
  `title` varchar(255) NOT NULL,
  `custom_properties` text,
  `outcome_service_url` varchar(255) DEFAULT NULL,
  `tool_key_sid` integer DEFAULT NULL,
  `created` datetime NOT NULL,
  `updated` datetime NOT NULL
);

CREATE TABLE `lti_user` (
  `sid` integer PRIMARY KEY AUTO_INCREMENT,
  `consumer_sid` integer NOT NULL,
  `lti_user_id` varchar(255) NOT NULL,
  `source_id` varchar(255),
  `name_given` varchar(255),
  `name_family` varchar(255),
  `name_full` varchar(255),
  `email` varchar(255),
  `created` datetime NOT NULL,
  `updated` datetime NOT NULL
);

CREATE TABLE `resource_user` (
  `sid` integer PRIMARY KEY AUTO_INCREMENT,
  `resource_sid` integer NOT NULL,
  `lti_user_sid` integer NOT NULL,
  `lti_result_sourcedid` varchar(255),
  `created` datetime NOT NULL,
  `updated` datetime NOT NULL
);

CREATE TABLE `attempt` (
  `sid` integer PRIMARY KEY AUTO_INCREMENT,
  `resource_user_sid` integer NOT NULL,
  `original_ru_sid` integer NOT NULL,
  `epoch_seconds` integer NOT NULL,
  `nanoseconds` integer NOT NULL,
  `fileSaved` boolean DEFAULT 0,
  `outputSaved` boolean DEFAULT 0,
  `filename` varchar(255) NOT NULL,
  `storage_type` integer NOT NULL,
  `score` integer NOT NULL,
  `errorCode` integer NOT NULL,
  UNIQUE(`resource_user_sid`, `epoch_seconds`, `nanoseconds`),
  FOREIGN KEY (`resource_user_sid`) REFERENCES `resource_user` (`sid`),
  FOREIGN KEY (`original_ru_sid`) REFERENCES `resource_user` (`sid`)
);

CREATE UNIQUE INDEX `context_index_0` ON `context` (`consumer_id`, `context_id`);

CREATE UNIQUE INDEX `resource_link_index_1` ON `resource_link` (`tool_sid`, `context_sid`, `resource_id`);

CREATE UNIQUE INDEX `lti_user_index_2` ON `lti_user` (`consumer_sid`, `lti_user_id`);

CREATE UNIQUE INDEX `resource_user_index_3` ON `resource_user` (`resource_sid`, `lti_user_sid`);

ALTER TABLE `tool_counter` ADD FOREIGN KEY (`tool_sid`) REFERENCES `tool` (`sid`);

ALTER TABLE `tool_user` ADD FOREIGN KEY (`tool_sid`) REFERENCES `tool` (`sid`);

ALTER TABLE `tool_user` ADD FOREIGN KEY (`user_sid`) REFERENCES `mgmt_user` (`sid`);

ALTER TABLE `tool_key` ADD FOREIGN KEY (`tool_sid`) REFERENCES `tool` (`sid`);

ALTER TABLE `tool_key` ADD FOREIGN KEY (`consumer_sid`) REFERENCES `consumer` (`sid`);

ALTER TABLE `tool_key` ADD FOREIGN KEY (`context_sid`) REFERENCES `context` (`sid`);

ALTER TABLE `tool_key` ADD FOREIGN KEY (`resource_link_sid`) REFERENCES `resource_link` (`sid`);

ALTER TABLE `nonce` ADD FOREIGN KEY (`key_sid`) REFERENCES `tool_key` (`sid`);

ALTER TABLE `nonce` ADD FOREIGN KEY (`consumer_sid`) REFERENCES `consumer` (`sid`);

ALTER TABLE `context` ADD FOREIGN KEY (`consumer_sid`) REFERENCES `consumer` (`sid`);

ALTER TABLE `resource_link` ADD FOREIGN KEY (`tool_sid`) REFERENCES `tool` (`sid`);

ALTER TABLE `resource_link` ADD FOREIGN KEY (`tool_key_sid`) REFERENCES `tool_key` (`sid`);

ALTER TABLE `resource_link` ADD FOREIGN KEY (`context_sid`) REFERENCES `context` (`sid`);

ALTER TABLE `lti_user` ADD FOREIGN KEY (`consumer_sid`) REFERENCES `consumer` (`sid`);

ALTER TABLE `resource_user` ADD FOREIGN KEY (`resource_sid`) REFERENCES `resource_link` (`sid`);

ALTER TABLE `resource_user` ADD FOREIGN KEY (`lti_user_sid`) REFERENCES `lti_user` (`sid`);
