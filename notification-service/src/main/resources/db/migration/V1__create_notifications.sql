CREATE TABLE notifications (
    id         BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    type       VARCHAR(50) NOT NULL DEFAULT 'GENERIC',
    recipient  VARCHAR(255) NOT NULL,
    message    TEXT NOT NULL,
    channel    VARCHAR(20) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_notifications_recipient ON notifications (recipient);