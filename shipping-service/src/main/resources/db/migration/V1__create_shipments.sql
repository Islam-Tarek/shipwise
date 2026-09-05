CREATE TABLE shipments (
    id              BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    order_id        BIGINT NOT NULL,
    address         VARCHAR(500) NOT NULL,
    carrier         VARCHAR(100) NOT NULL,
    tracking_number VARCHAR(50) NOT NULL UNIQUE,
    status          VARCHAR(20) NOT NULL,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_shipments_order_id ON shipments (order_id);