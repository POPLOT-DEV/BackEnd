CREATE TABLE qr_ticket_key (
    key_id VARCHAR(64) PRIMARY KEY,
    status VARCHAR(20) NOT NULL,
    activated_at TIMESTAMP(6) NOT NULL,
    retired_at TIMESTAMP(6),
    CONSTRAINT ck_qr_ticket_key_status CHECK (status IN ('ACTIVE','RETIRED')),
    CONSTRAINT ck_qr_ticket_key_retired_at CHECK ((status = 'RETIRED') = (retired_at IS NOT NULL))
);

CREATE UNIQUE INDEX uk_qr_ticket_key_active ON qr_ticket_key(status)
WHERE status = 'ACTIVE';

ALTER TABLE qr_ticket
    ADD COLUMN revoked_reason VARCHAR(30);

ALTER TABLE qr_ticket
    ADD CONSTRAINT ck_qr_ticket_revoked_reason CHECK (revoked_reason IN ('KEY_ROTATED'));
