CREATE DATABASE IF NOT EXISTS auth_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS policy_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

CREATE USER IF NOT EXISTS 'azki_migration'@'%' IDENTIFIED BY 'migration_password';
GRANT ALL PRIVILEGES ON auth_db.* TO 'azki_migration'@'%';
GRANT ALL PRIVILEGES ON policy_db.* TO 'azki_migration'@'%';

CREATE USER IF NOT EXISTS 'azki_app'@'%' IDENTIFIED BY 'app_password';
GRANT SELECT, INSERT, UPDATE, DELETE ON auth_db.* TO 'azki_app'@'%';
GRANT SELECT, INSERT, UPDATE, DELETE ON policy_db.* TO 'azki_app'@'%';

FLUSH PRIVILEGES;