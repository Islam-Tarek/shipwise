CREATE TABLE orders (
    id           BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    customer_id  BIGINT NOT NULL REFERENCES customers (id),
    status       VARCHAR(30) NOT NULL DEFAULT 'PENDING'
                 CHECK (status IN ('PENDING', 'CONFIRMED', 'CANCELLED')),
    total_amount NUMERIC(12, 2) NOT NULL,
    created_at   TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at   TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_orders_customer_id ON orders (customer_id);
CREATE INDEX idx_orders_status ON orders (status);

CREATE TABLE order_items (
    id         BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    order_id   BIGINT NOT NULL REFERENCES orders (id) ON DELETE CASCADE,
    product_id BIGINT NOT NULL REFERENCES products (id),
    quantity   INT NOT NULL CHECK (quantity > 0),
    unit_price NUMERIC(12, 2) NOT NULL
);

CREATE INDEX idx_order_items_order_id ON order_items (order_id);
