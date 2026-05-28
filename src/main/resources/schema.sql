CREATE TABLE IF NOT EXISTS users (
    id BIGSERIAL PRIMARY KEY,
    login VARCHAR(100) NOT NULL UNIQUE,
    password_hash VARCHAR(100) NOT NULL,
    role VARCHAR(20) NOT NULL CHECK (role IN ('ADMIN', 'USER')),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE IF NOT EXISTS otp_config (
    id BOOLEAN PRIMARY KEY DEFAULT TRUE,
    code_length INTEGER NOT NULL CHECK (code_length BETWEEN 4 AND 12),
    ttl_seconds INTEGER NOT NULL CHECK (ttl_seconds BETWEEN 30 AND 86400),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT one_otp_config_row CHECK (id)
);

INSERT INTO otp_config (id, code_length, ttl_seconds)
VALUES (TRUE, 6, 300)
ON CONFLICT (id) DO NOTHING;

CREATE TABLE IF NOT EXISTS otp_codes (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    operation_id VARCHAR(150) NOT NULL,
    code VARCHAR(20) NOT NULL,
    status VARCHAR(20) NOT NULL CHECK (status IN ('ACTIVE', 'EXPIRED', 'USED')),
    channel VARCHAR(20) NOT NULL,
    destination VARCHAR(255) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    expires_at TIMESTAMPTZ NOT NULL,
    used_at TIMESTAMPTZ
);

CREATE UNIQUE INDEX IF NOT EXISTS ux_otp_codes_active_operation
    ON otp_codes (user_id, operation_id)
    WHERE status = 'ACTIVE';

CREATE INDEX IF NOT EXISTS idx_otp_codes_expiration
    ON otp_codes (status, expires_at);

CREATE INDEX IF NOT EXISTS idx_otp_codes_user
    ON otp_codes (user_id);
