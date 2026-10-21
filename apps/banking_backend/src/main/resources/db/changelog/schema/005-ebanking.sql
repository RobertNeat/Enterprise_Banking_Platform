--liquibase formatted sql

--changeset robert_neat:ebanking_user-001
CREATE TABLE ebanking_user (
    id VARCHAR(10) PRIMARY KEY,
    customer_id VARCHAR(20) NOT NULL REFERENCES customer (id),
    username VARCHAR(100) NOT NULL,
    channel VARCHAR(10) NOT NULL CHECK (channel IN ('web', 'mobile')),
    access_status VARCHAR(15) NOT NULL CHECK (access_status IN ('active', 'locked', 'pendingReset')),
    daily_limit NUMERIC(19, 2) NOT NULL,
    last_login TIMESTAMP WITH TIME ZONE,
    permissions VARCHAR(500)
);

--changeset robert_neat:ebanking_user-002
-- username is the e-banking login — must be unique across all customers (data_model.md /
-- banking_backend.md item 19: previously unenforced at schema level, relied only on seed data
-- happening to be unique).
ALTER TABLE ebanking_user ADD CONSTRAINT uq_ebanking_user_username UNIQUE (username);
