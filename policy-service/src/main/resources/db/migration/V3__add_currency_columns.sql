ALTER TABLE insurance_products
    CHANGE COLUMN base_premium_rate base_premium_rate DECIMAL(12,2) NOT NULL,
    ADD COLUMN base_premium_currency VARCHAR(10) NOT NULL DEFAULT 'Toman';

ALTER TABLE policies
    CHANGE COLUMN premium_amount premium_amount DECIMAL(12,2) NOT NULL,
    ADD COLUMN premium_currency VARCHAR(10) NOT NULL DEFAULT 'Toman';