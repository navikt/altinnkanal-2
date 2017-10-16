CREATE TABLE IF NOT EXISTS `topic_mappings` (
    `service_code` CHAR(255) NOT NULL,
    `service_edition_code` CHAR(255) NOT NULL,
    `topic` CHAR(255) NOT NULL,
    `enabled` BOOL NOT NULL,
    `updated_by` CHAR(255) NOT NULL,
    `updated_date` TIMESTAMP NOT NULL,
    `comment` TEXT NOT NULL,
    PRIMARY KEY(`service_code`, `service_edition_code`)
);

CREATE TABLE IF NOT EXISTS `topic_mapping_log` (
    `service_code` CHAR(255) NOT NULL,
    `service_edition_code` CHAR(255) NOT NULL,
    `old_topic` CHAR(255),
    `new_topic` CHAR(255) NOT NULL,
    `log_event_type` ENUM('UPDATE', 'CREATE', 'DELETE') NOT NULL,
    `updated_date` TIMESTAMP NOT NULL,
    `updated_by` CHAR(255) NOT NULL
);

