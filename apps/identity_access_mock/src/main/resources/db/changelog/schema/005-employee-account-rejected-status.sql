--liquibase formatted sql

-- Admin approval flow (AuthController approve/reject) needs a third terminal status — the
-- original CHECK (001-employee-account.sql) only allowed ACTIVE/PENDING_APPROVAL. Split per dbms
-- because the original CHECK has no explicit name: Postgres defaults an unnamed inline CHECK to
-- "<table>_<column>_check", but H2 (used by tests, see test/resources/application.properties)
-- auto-generates an opaque name (CONSTRAINT_xx) that has to be looked up at runtime.

--changeset robert_neat:employee_account-005-rejected-status-postgresql dbms:postgresql
ALTER TABLE identity_access.employee_account DROP CONSTRAINT employee_account_status_check;
ALTER TABLE identity_access.employee_account
    ADD CONSTRAINT employee_account_status_check CHECK (status IN ('ACTIVE', 'PENDING_APPROVAL', 'REJECTED'));

--changeset robert_neat:employee_account-005-rejected-status-h2 dbms:h2
EXECUTE IMMEDIATE 'ALTER TABLE identity_access.employee_account DROP CONSTRAINT "' ||
    (SELECT constraint_name FROM information_schema.table_constraints
     WHERE LOWER(table_schema) = 'identity_access' AND LOWER(table_name) = 'employee_account'
       AND constraint_type = 'CHECK') || '"';
ALTER TABLE identity_access.employee_account ADD CONSTRAINT employee_account_status_check
    CHECK (status IN ('ACTIVE', 'PENDING_APPROVAL', 'REJECTED'));
