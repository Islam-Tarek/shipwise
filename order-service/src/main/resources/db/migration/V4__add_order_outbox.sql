CREATE TABLE order_outbox (
    id         BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    order_id   BIGINT NOT NULL REFERENCES orders (id),
    event_type VARCHAR(50) NOT NULL,
    payload    JSONB NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    sent_at    TIMESTAMPTZ,
    attempts   INT NOT NULL DEFAULT 0
);

CREATE INDEX idx_order_outbox_unsent ON order_outbox (id) WHERE sent_at IS NULL;