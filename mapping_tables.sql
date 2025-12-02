
USE vehicle_mapping;

CREATE TABLE vehicle_mapping_2w(
	id INT(11) NOT NULL AUTO_INCREMENT,
    VEHICLE_MODEL_STRING VARCHAR(255) NOT NULL,
    REWARD_VEHICLE_TYPE VARCHAR(255) NOT NULL,
    product_type VARCHAR(255) NOT NULL,
    ic VARCHAR(255) NOT NULL,
    PRIMARY KEY (ID)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE vehicle_mapping_4w(
	id INT(11) NOT NULL AUTO_INCREMENT,
    VEHICLE_MODEL_STRING VARCHAR(255) NOT NULL,
    REWARD_VEHICLE_TYPE VARCHAR(255) NOT NULL,
    product_type VARCHAR(255) NOT NULL,
    ic VARCHAR(255) NOT NULL,
    PRIMARY KEY (ID)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE vehicle_mapping_cv(
	id INT(11) NOT NULL AUTO_INCREMENT,
    VEHICLE_MODEL_STRING VARCHAR(255) NOT NULL,
    REWARD_VEHICLE_TYPE VARCHAR(255) NOT NULL,
    product_type VARCHAR(255),
    ic VARCHAR(255) NOT NULL,
    PRIMARY KEY (ID)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

ALTER TABLE vehicle_mapping_cv
ADD COLUMN vehicle_fuel VARCHAR(100);

ALTER TABLE vehicle_mapping_cv
ADD COLUMN reward_model VARCHAR(100);

ALTER TABLE vehicle_mapping_cv
ADD COLUMN vehicle_power_bi VARCHAR(100);

CREATE INDEX idx_vehicle_mapping_2w ON vehicle_mapping_2w(vehicle_model_string);
CREATE INDEX idx_vehicle_mapping_4w ON vehicle_mapping_4w(vehicle_model_string);
CREATE INDEX idx_vehicle_mapping_cv_ic ON vehicle_mapping_cv(vehicle_model_string, ic);

SELECT * FROM vehicle_mapping_2w WHERE ic = "DIGIT";
SELECT * FROM vehicle_mapping_4w WHERE ic = "ICICI";
SELECT * FROM vehicle_mapping_cv WHERE ic = "TATA";


-- for vehicle model and  make code (our master codes)
CREATE TABLE `master_car_model` (
  `id` int NOT NULL,
  `make_id` int NOT NULL,
  `make_name` varchar(255) CHARACTER SET utf8mb3 NOT NULL,
  `name` varchar(255) CHARACTER SET utf8mb3 NOT NULL,
  `status` tinyint NOT NULL DEFAULT '1',
  `ic_type` varchar(50) CHARACTER SET utf8mb3 DEFAULT NULL,
  `is_deleted` tinyint NOT NULL DEFAULT '0',
  `created_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `model_code` varchar(255) CHARACTER SET utf8mb3 NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
 
CREATE TABLE `master_car_make` (
  `id` int NOT NULL,
  `name` varchar(255) CHARACTER SET utf8mb3 NOT NULL,
  `orderby` tinyint NOT NULL DEFAULT '0',
  `status` tinyint NOT NULL DEFAULT '1',
  `ic_type` varchar(50) CHARACTER SET utf8mb3 DEFAULT NULL,
  `is_deleted` tinyint NOT NULL DEFAULT '0',
  `created_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `make_code` varchar(255) CHARACTER SET utf8mb3 NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;


