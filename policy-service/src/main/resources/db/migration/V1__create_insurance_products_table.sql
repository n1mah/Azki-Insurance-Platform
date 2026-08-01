CREATE TABLE insurance_products (
    id                 BIGINT AUTO_INCREMENT NOT NULL,
    name               VARCHAR(150)   NOT NULL,
    base_premium_rate  DECIMAL(12,2)  NOT NULL,
    coverage_type      VARCHAR(100)   NOT NULL,
    PRIMARY KEY (id)
);