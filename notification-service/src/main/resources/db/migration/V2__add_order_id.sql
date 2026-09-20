ALTER TABLE notifications ADD COLUMN order_id BIGINT;

CREATE UNIQUE INDEX idx_notifications_order_id
    ON notifications (order_id)
    WHERE order_id IS NOT NULL;