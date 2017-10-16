CREATE TABLE IF NOT EXISTS `topic_mappings` (
    `service_code` CHAR(255) NOT NULL,
    `service_edition_code` CHAR(255) NOT NULL,
    `topic` CHAR(255) NOT NULL,
    `enabled` BOOL NOT NULL,
    PRIMARY KEY(`service_code`, `service_edition_code`)
);

CREATE TABLE IF NOT EXISTS `topic_mapping_log` (
    `service_code` CHAR(255) NOT NULL,
    `service_edition_code` CHAR(255) NOT NULL,
    `topic` CHAR(255) NOT NULL,
    `enabled` BOOL NOT NULL,
    `comment` TEXT NOT NULL,
    `updated_date` TIMESTAMP NOT NULL,
    `updated_by` CHAR(255) NOT NULL
);

