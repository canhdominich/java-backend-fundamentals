create table users (
    id bigint auto_increment primary key,
    email varchar(255) not null,
    password_hash varchar(100) not null,
    role varchar(20) not null,
    created_at datetime(6) not null,
    constraint uk_users_email unique (email)
) engine = InnoDB;

create table products (
    id bigint auto_increment primary key,
    sku varchar(40) not null,
    name varchar(200) not null,
    price_cents bigint not null,
    stock int not null,
    active boolean not null default true,
    version bigint not null default 0,
    created_at datetime(6) not null,
    updated_at datetime(6) not null,
    constraint uk_products_sku unique (sku),
    constraint ck_products_price check (price_cents >= 0),
    constraint ck_products_stock check (stock >= 0)
) engine = InnoDB;

create table orders (
    id bigint auto_increment primary key,
    user_id bigint not null,
    status varchar(20) not null,
    total_cents bigint not null,
    idempotency_key varchar(80),
    created_at datetime(6) not null,
    constraint fk_orders_user foreign key (user_id) references users (id),
    constraint uk_orders_user_key unique (user_id, idempotency_key)
) engine = InnoDB;

create index ix_orders_user_created on orders (user_id, created_at);

create table order_lines (
    id bigint auto_increment primary key,
    order_id bigint not null,
    product_id bigint not null,
    quantity int not null,
    unit_price_cents bigint not null,
    constraint fk_lines_order foreign key (order_id) references orders (id) on delete cascade,
    constraint fk_lines_product foreign key (product_id) references products (id),
    constraint ck_lines_quantity check (quantity > 0)
) engine = InnoDB;

create index ix_lines_order on order_lines (order_id);
