-- V4__booking_flow_updates.sql
CREATE TABLE IF NOT EXISTS idempotency_records (
    idempotency_key VARCHAR(255) PRIMARY KEY,
    response_status INTEGER NOT NULL,
    response_body TEXT,
    created_at TIMESTAMP NOT NULL
);
