ALTER TABLE binary_contents
    ADD status varchar(20) NOT NULL DEFAULT 'SUCCESS';

ALTER TABLE binary_contents
    ADD updated_at timestamptz;

ALTER TABLE binary_contents
    ALTER COLUMN status DROP DEFAULT;