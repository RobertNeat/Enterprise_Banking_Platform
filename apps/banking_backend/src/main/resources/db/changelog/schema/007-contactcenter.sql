--liquibase formatted sql

--changeset robert_neat:service_disposition-001
CREATE TABLE service_disposition (
    id VARCHAR(10) PRIMARY KEY,
    customer_case_id VARCHAR(10) NOT NULL REFERENCES customer_case (id),
    type_key VARCHAR(50) NOT NULL,
    status VARCHAR(15) NOT NULL CHECK (status IN ('new', 'inProgress', 'escalated', 'resolved')),
    occurred_at DATE NOT NULL
);

--changeset robert_neat:contact_history_entry-001
CREATE TABLE contact_history_entry (
    id VARCHAR(10) PRIMARY KEY,
    customer_id VARCHAR(20) NOT NULL REFERENCES customer (id),
    channel_key VARCHAR(30) NOT NULL,
    occurred_at DATE NOT NULL,
    summary VARCHAR(500) NOT NULL
);
