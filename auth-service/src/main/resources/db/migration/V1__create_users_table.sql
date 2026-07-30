CREATE TABLE users (
    id            BINARY(16)   NOT NULL,
    username      VARCHAR(100) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    role          VARCHAR(20)  NOT NULL,
    created_at    TIMESTAMP    NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uq_users_username (username)
);