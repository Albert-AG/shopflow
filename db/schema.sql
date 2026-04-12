-- ShopFlow Database Schema
-- PostgreSQL 16
--
-- MATERIAL DIDÁCTICO — Tema 8: MCP
-- Este esquema es la "fuente de verdad" de la base de datos.
-- El ejercicio T08 consiste en usar el MCP de PostgreSQL para que la IA
-- lea este schema y genere automáticamente las entidades JPA correspondientes.
--
-- Tipos y decisiones de diseño a destacar:
--   UUID         → IDs generados por la BD (gen_random_uuid())
--   DECIMAL(19,4)→ Cantidades monetarias (evita errores de punto flotante)
--   JSONB        → Datos semiestructurados (shipping_address, metadata)
--   TIMESTAMPTZ  → Timestamps con zona horaria (siempre UTC)
--   CHECK        → Invariantes a nivel de BD (stock no negativo, estados válidos)
--   REFERENCES   → Integridad referencial
--   INDEX        → Índices en columnas de filtrado frecuente

-- ============================================================
-- Customers (referencia — gestionado por otro bounded context)
-- ============================================================
CREATE TABLE IF NOT EXISTS customers (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    email       VARCHAR(255) NOT NULL UNIQUE,
    name        VARCHAR(255) NOT NULL,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now()
);

-- ============================================================
-- Products (referencia — gestionado por catalog service)
-- ============================================================
CREATE TABLE IF NOT EXISTS products (
    id          UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    sku         VARCHAR(100) NOT NULL UNIQUE,
    name        VARCHAR(255) NOT NULL,
    price       DECIMAL(19, 4) NOT NULL CHECK (price >= 0),
    stock       INTEGER      NOT NULL DEFAULT 0 CHECK (stock >= 0),
    active      BOOLEAN      NOT NULL DEFAULT true,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now()
);

-- ============================================================
-- Orders (core of the orders bounded context)
-- ============================================================
CREATE TABLE IF NOT EXISTS orders (
    id               UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    customer_id      UUID         NOT NULL REFERENCES customers(id),
    status           VARCHAR(20)  NOT NULL DEFAULT 'PENDING'
                         CHECK (status IN ('PENDING', 'CONFIRMED', 'SHIPPED', 'DELIVERED', 'CANCELLED')),
    total_amount     DECIMAL(19, 4) NOT NULL CHECK (total_amount >= 0),
    discount_code    VARCHAR(50),
    shipping_address JSONB,
    notes            TEXT,
    created_at       TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at       TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_orders_customer_id ON orders(customer_id);
CREATE INDEX IF NOT EXISTS idx_orders_status      ON orders(status);
CREATE INDEX IF NOT EXISTS idx_orders_created_at  ON orders(created_at DESC);

-- ============================================================
-- Order Items
-- ============================================================
CREATE TABLE IF NOT EXISTS order_items (
    id           UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    order_id     UUID         NOT NULL REFERENCES orders(id) ON DELETE CASCADE,
    product_id   UUID         NOT NULL REFERENCES products(id),
    quantity     INTEGER      NOT NULL CHECK (quantity >= 1),
    unit_price   DECIMAL(19, 4) NOT NULL CHECK (unit_price >= 0),
    subtotal     DECIMAL(19, 4) GENERATED ALWAYS AS (quantity * unit_price) STORED
);

CREATE INDEX IF NOT EXISTS idx_order_items_order_id ON order_items(order_id);

-- ============================================================
-- Outbox Events (T13 — Event-Driven Architecture)
-- Implementación del Outbox Pattern para envío confiable de eventos.
-- El agente lee esta tabla y publica a Kafka de forma transaccional.
-- ============================================================
CREATE TABLE IF NOT EXISTS outbox_events (
    id           UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    aggregate_id UUID         NOT NULL,
    event_type   VARCHAR(100) NOT NULL,
    payload      JSONB        NOT NULL,
    created_at   TIMESTAMPTZ  NOT NULL DEFAULT now(),
    published_at TIMESTAMPTZ  -- NULL = pendiente de publicar
);

CREATE INDEX IF NOT EXISTS idx_outbox_unpublished
    ON outbox_events(created_at)
    WHERE published_at IS NULL;

-- ============================================================
-- Inventory Reservations (T10 — Microservices)
-- ============================================================
CREATE TABLE IF NOT EXISTS inventory_reservations (
    id           UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    order_id     UUID        NOT NULL REFERENCES orders(id),
    product_id   UUID        NOT NULL REFERENCES products(id),
    quantity     INTEGER     NOT NULL CHECK (quantity >= 1),
    status       VARCHAR(20) NOT NULL DEFAULT 'RESERVED'
                     CHECK (status IN ('RESERVED', 'CONFIRMED', 'RELEASED')),
    created_at   TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- ============================================================
-- Seed data for demos (minimal)
-- ============================================================
INSERT INTO customers (id, email, name) VALUES
    ('a0000000-0000-0000-0000-000000000001', 'alice@example.com', 'Alice Corp'),
    ('a0000000-0000-0000-0000-000000000002', 'bob@example.com',   'Bob Industries')
ON CONFLICT DO NOTHING;

INSERT INTO products (id, sku, name, price, stock) VALUES
    ('b0000000-0000-0000-0000-000000000001', 'PROD-001', 'Widget Pro',  29.9900, 100),
    ('b0000000-0000-0000-0000-000000000002', 'PROD-002', 'Gadget Plus', 49.9900,  50),
    ('b0000000-0000-0000-0000-000000000003', 'PROD-003', 'Super Tool',   9.9900, 200)
ON CONFLICT DO NOTHING;
