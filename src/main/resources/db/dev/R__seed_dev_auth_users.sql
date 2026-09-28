INSERT INTO users (public_id, email, name, role, status, created_at, updated_at)
VALUES ('00000000-0000-0000-0000-000000000001', 'dev-user@poplot.local', 'Dev User', 'USER', 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
       ('00000000-0000-0000-0000-000000000002', 'dev-staff@poplot.local', 'Dev Staff', 'STAFF', 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
       ('00000000-0000-0000-0000-000000000003', 'dev-admin@poplot.local', 'Dev Admin', 'ADMIN', 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
ON CONFLICT (public_id) DO NOTHING;
