CREATE TABLE users (
    id            BIGSERIAL PRIMARY KEY,
    email         VARCHAR(120) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    full_name     VARCHAR(120) NOT NULL,
    role          VARCHAR(20)  NOT NULL,
    created_at    TIMESTAMPTZ  NOT NULL,
    updated_at    TIMESTAMPTZ  NOT NULL
);

CREATE TABLE refresh_tokens (
    id         BIGSERIAL PRIMARY KEY,
    user_id    BIGINT      NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    token_hash VARCHAR(64) NOT NULL UNIQUE,
    expires_at TIMESTAMPTZ NOT NULL,
    revoked    BOOLEAN     NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL
);
CREATE INDEX idx_refresh_tokens_user ON refresh_tokens (user_id);

CREATE TABLE categories (
    id   BIGSERIAL PRIMARY KEY,
    name VARCHAR(80) NOT NULL UNIQUE,
    slug VARCHAR(80) NOT NULL UNIQUE
);

CREATE TABLE products (
    id          BIGSERIAL PRIMARY KEY,
    sku         VARCHAR(40)    NOT NULL UNIQUE,
    name        VARCHAR(150)   NOT NULL,
    description TEXT,
    price       NUMERIC(12, 2) NOT NULL CHECK (price > 0),
    stock       INTEGER        NOT NULL CHECK (stock >= 0),
    active      BOOLEAN        NOT NULL DEFAULT TRUE,
    category_id BIGINT         NOT NULL REFERENCES categories (id),
    image_url   VARCHAR(500),
    version     BIGINT         NOT NULL DEFAULT 0,
    created_at  TIMESTAMPTZ    NOT NULL,
    updated_at  TIMESTAMPTZ    NOT NULL
);
CREATE INDEX idx_products_category ON products (category_id);

CREATE TABLE carts (
    id         BIGSERIAL PRIMARY KEY,
    user_id    BIGINT      NOT NULL UNIQUE REFERENCES users (id) ON DELETE CASCADE,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL
);

CREATE TABLE cart_items (
    id         BIGSERIAL PRIMARY KEY,
    cart_id    BIGINT  NOT NULL REFERENCES carts (id) ON DELETE CASCADE,
    product_id BIGINT  NOT NULL REFERENCES products (id),
    quantity   INTEGER NOT NULL CHECK (quantity > 0),
    UNIQUE (cart_id, product_id)
);

CREATE TABLE orders (
    id             BIGSERIAL PRIMARY KEY,
    order_number   VARCHAR(30)    NOT NULL UNIQUE,
    user_id        BIGINT         NOT NULL REFERENCES users (id),
    status         VARCHAR(20)    NOT NULL,
    subtotal       NUMERIC(12, 2) NOT NULL,
    shipping_cost  NUMERIC(12, 2) NOT NULL,
    total          NUMERIC(12, 2) NOT NULL,
    ship_full_name VARCHAR(120)   NOT NULL,
    ship_line1     VARCHAR(200)   NOT NULL,
    ship_district  VARCHAR(80)    NOT NULL,
    ship_city      VARCHAR(80)    NOT NULL,
    ship_phone     VARCHAR(20)    NOT NULL,
    paid_at        TIMESTAMPTZ,
    version        BIGINT         NOT NULL DEFAULT 0,
    created_at     TIMESTAMPTZ    NOT NULL,
    updated_at     TIMESTAMPTZ    NOT NULL
);
CREATE INDEX idx_orders_user ON orders (user_id);
CREATE INDEX idx_orders_status_created ON orders (status, created_at);
CREATE INDEX idx_orders_paid_at ON orders (paid_at);

CREATE TABLE order_items (
    id           BIGSERIAL PRIMARY KEY,
    order_id     BIGINT         NOT NULL REFERENCES orders (id) ON DELETE CASCADE,
    product_id   BIGINT         NOT NULL,
    sku          VARCHAR(40)    NOT NULL,
    product_name VARCHAR(150)   NOT NULL,
    unit_price   NUMERIC(12, 2) NOT NULL,
    quantity     INTEGER        NOT NULL CHECK (quantity > 0),
    line_total   NUMERIC(12, 2) NOT NULL
);
CREATE INDEX idx_order_items_order ON order_items (order_id);

CREATE TABLE order_status_history (
    id          BIGSERIAL PRIMARY KEY,
    order_id    BIGINT       NOT NULL REFERENCES orders (id) ON DELETE CASCADE,
    from_status VARCHAR(20),
    to_status   VARCHAR(20)  NOT NULL,
    changed_by  VARCHAR(120) NOT NULL,
    note        VARCHAR(255),
    changed_at  TIMESTAMPTZ  NOT NULL
);
CREATE INDEX idx_status_history_order ON order_status_history (order_id);

CREATE TABLE payments (
    id                      BIGSERIAL PRIMARY KEY,
    order_id                BIGINT         NOT NULL REFERENCES orders (id),
    idempotency_key         VARCHAR(100)   NOT NULL UNIQUE,
    amount                  NUMERIC(12, 2) NOT NULL,
    status                  VARCHAR(20)    NOT NULL,
    provider_transaction_id VARCHAR(100),
    failure_reason          VARCHAR(255),
    created_at              TIMESTAMPTZ    NOT NULL
);
CREATE INDEX idx_payments_order ON payments (order_id);

CREATE TABLE notifications (
    id         BIGSERIAL PRIMARY KEY,
    recipient  VARCHAR(120) NOT NULL,
    type       VARCHAR(40)  NOT NULL,
    subject    VARCHAR(200) NOT NULL,
    body       TEXT         NOT NULL,
    status     VARCHAR(20)  NOT NULL,
    error      VARCHAR(500),
    created_at TIMESTAMPTZ  NOT NULL
);
CREATE INDEX idx_notifications_recipient ON notifications (recipient);
