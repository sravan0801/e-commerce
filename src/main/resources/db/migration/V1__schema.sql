-- Whole schema in one file. Flyway applies it once and records it in
-- flyway_schema_history; later changes go in new V2, V3, ... files.
-- Money is stored as integer minor units (cents); the currency sits next to it.

-- ---- accounts -------------------------------------------------------------
create table app_user (
    id            bigserial primary key,
    email         varchar(255) not null unique,
    password_hash varchar(255) not null,
    full_name     varchar(255),
    enabled       boolean      not null default true,
    created_at    timestamptz  not null default now(),
    updated_at    timestamptz  not null default now()
);

create table app_user_role (
    user_id bigint      not null references app_user (id) on delete cascade,
    role    varchar(32) not null,
    primary key (user_id, role)
);

-- ---- catalogue -----------------------------------------------------------
create table category (
    id   bigserial primary key,
    name varchar(128) not null unique,
    slug varchar(128) not null unique
);

create table product (
    id             bigserial primary key,
    sku            varchar(64)  not null unique,
    name           varchar(255) not null,
    description    text,
    price_cents    bigint       not null check (price_cents >= 0),
    currency       varchar(3)   not null default 'USD',
    stock_quantity integer      not null default 0 check (stock_quantity >= 0),
    category_id    bigint       references category (id),
    active         boolean      not null default true,
    version        bigint       not null default 0,   -- JPA optimistic locking
    created_at     timestamptz  not null default now(),
    updated_at     timestamptz  not null default now()
);

create index idx_product_active on product (active);

-- ---- orders ------------------------------------------------------------
-- "orders" (plural) because ORDER is a reserved SQL word. A line snapshots the
-- sku and unit price at purchase time so later catalogue edits never rewrite
-- history.
create table orders (
    id             bigserial primary key,
    order_ref      varchar(32)  not null unique,
    user_id        bigint       not null references app_user (id),
    status         varchar(16)  not null,   -- PENDING | CONFIRMED | REJECTED
    total_cents    bigint       not null check (total_cents >= 0),
    currency       varchar(3)   not null default 'USD',
    rejection_note varchar(255),
    created_at     timestamptz  not null default now(),
    updated_at     timestamptz  not null default now()
);

create table order_line (
    id               bigserial primary key,
    order_id         bigint      not null references orders (id) on delete cascade,
    product_id       bigint      not null references product (id),
    sku              varchar(64) not null,
    unit_price_cents bigint      not null check (unit_price_cents >= 0),
    quantity         integer     not null check (quantity > 0)
);

create index idx_orders_user on orders (user_id);
create index idx_order_line_order on order_line (order_id);
