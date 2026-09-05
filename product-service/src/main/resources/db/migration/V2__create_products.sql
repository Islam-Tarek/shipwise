CREATE TABLE products (
    id             BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    category_id    BIGINT NOT NULL REFERENCES categories (id),
    external_id    VARCHAR(50) UNIQUE,
    name           VARCHAR(255) NOT NULL,
    description    TEXT,
    price          NUMERIC(12, 2) NOT NULL,
    image_url      VARCHAR(500),
    stock_quantity INT NOT NULL DEFAULT 0 CHECK (stock_quantity >= 0),
    is_active      BOOLEAN NOT NULL DEFAULT true,
    created_at     TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at     TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_products_category_id ON products (category_id);
CREATE INDEX idx_products_name ON products (name);