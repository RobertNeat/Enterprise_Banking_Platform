--liquibase formatted sql

--changeset robert_neat:customer-001
CREATE TABLE customer (
    id VARCHAR(20) PRIMARY KEY,
    full_name VARCHAR(200) NOT NULL,
    segment VARCHAR(20) NOT NULL CHECK (segment IN ('retail', 'business')),
    tax_id VARCHAR(30),
    risk_rating VARCHAR(10)
);

--changeset robert_neat:employee-001
CREATE TABLE employee (
    id VARCHAR(10) PRIMARY KEY,
    first_name VARCHAR(100) NOT NULL,
    last_name VARCHAR(100) NOT NULL,
    email VARCHAR(150) NOT NULL,
    phone VARCHAR(30),
    department VARCHAR(100),
    branch VARCHAR(100),
    position VARCHAR(100),
    supervisor_id VARCHAR(10) REFERENCES employee (id),
    role VARCHAR(50) NOT NULL,
    business_lines VARCHAR(200),
    start_date DATE NOT NULL,
    status VARCHAR(20) NOT NULL CHECK (status IN ('PENDING_APPROVAL', 'ACTIVE'))
);

--changeset robert_neat:bank_account-001
CREATE TABLE bank_account (
    id VARCHAR(20) PRIMARY KEY,
    customer_id VARCHAR(20) NOT NULL REFERENCES customer (id),
    masked_number VARCHAR(34) NOT NULL,
    balance NUMERIC(19, 2) NOT NULL,
    blocked_amount NUMERIC(19, 2) NOT NULL
);
