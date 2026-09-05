CREATE TABLE order_items (
    id           BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    order_id     BIGINT NOT NULL REFERENCES orders (id),
    product_id   BIGINT NOT NULL,
    product_name VARCHAR(255) NOT NULL,
    unit_price   NUMERIC(12, 2) NOT NULL,
    quantity     INT NOT NULL CHECK (quantity > 0)
);

CREATE INDEX idx_order_items_order_id ON order_items (order_id);