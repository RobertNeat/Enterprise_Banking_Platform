--liquibase formatted sql

--changeset robert_neat:seed-device-001 context:mock-data
-- Sample mobile devices previously seeded in DeviceService's constructor (seedDevices()).
INSERT INTO identity_access.device (device_id, owner_id, model, last_seen_at, trusted) VALUES
('DEV-0001', 'PRC-30678', 'iPhone 14', CURRENT_TIMESTAMP, TRUE),
('DEV-0002', 'EB-1001', 'Samsung Galaxy S23', CURRENT_TIMESTAMP, TRUE),
('DEV-0003', 'EB-1004', 'Xiaomi Redmi Note 12', CURRENT_TIMESTAMP, FALSE);
