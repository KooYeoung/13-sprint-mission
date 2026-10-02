ALTER TABLE read_statuses
    ADD notification_enabled boolean NOT NULL DEFAULT false;

UPDATE read_statuses r
SET notification_enabled = (
    SELECT CASE
               WHEN c.type = 'PRIVATE' THEN true
               ELSE false
           END
    FROM channels c
    WHERE c.id = r.channel_id
);

ALTER TABLE read_statuses
    ALTER COLUMN notification_enabled DROP DEFAULT;

CREATE TABLE notifications (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    receiver_id uuid NOT NULL,
    title varchar(255) NOT NULL,
    content varchar(255) NOT NULL,
    created_at timestamptz NOT NULL DEFAULT now(),

    CONSTRAINT fk_user_notifications
        FOREIGN KEY (receiver_id) REFERENCES users (id) ON DELETE CASCADE
);
