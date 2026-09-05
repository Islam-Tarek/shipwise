CREATE TABLE payments (
    id             BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    order_id       BIGINT NOT NULL,
    amount         NUMERIC(12, 2) NOT NULL CHECK (amount > 0),
    currency       VARCHAR(3) NOT NULL DEFAULT 'USD',
    status         VARCHAR(20) NOT NULL,
    failure_reason VARCHAR(255),
    created_at     TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at     TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_payments_order_id ON payments (order_id);