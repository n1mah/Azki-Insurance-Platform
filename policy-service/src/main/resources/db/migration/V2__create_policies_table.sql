CREATE TABLE policies (
    id              BINARY(16)     NOT NULL,
    user_id         BINARY(16)     NOT NULL,
    product_id      BIGINT         NOT NULL,
    status          VARCHAR(20)    NOT NULL,
    premium_amount  DECIMAL(12,2)  NOT NULL,
    start_date      DATE           NOT NULL,
    end_date        DATE           NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_policies_product
        FOREIGN KEY (product_id) REFERENCES insurance_products (id)
);