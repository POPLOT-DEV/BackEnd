ALTER TABLE waitlist RENAME COLUMN canceled_at TO declined_at;

UPDATE waitlist
SET status = 'DECLINED'
WHERE status = 'CANCELED';

ALTER TABLE waitlist DROP CONSTRAINT ck_waitlist_status;

ALTER TABLE waitlist
    ADD CONSTRAINT ck_waitlist_status
        CHECK (status IN ('WAITING','OFFERED','ACCEPTED','DECLINED','EXPIRED'));
