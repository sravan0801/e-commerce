-- Deterministic catalogue data so the app is usable straight after startup.
-- User accounts are seeded in code (they need the BCrypt encoder) -- see
-- com.example.storefront.bootstrap.AccountSeeder.

insert into category (name, slug) values
    ('Books', 'books'),
    ('Electronics', 'electronics'),
    ('Home & Kitchen', 'home-kitchen');

insert into product (sku, name, description, price_cents, currency, stock_quantity, category_id, active) values
    ('BK-1001', 'Designing Data-Intensive Applications', 'Reference on modern data systems.', 4599, 'USD', 50,
        (select id from category where slug = 'books'), true),
    ('BK-1002', 'Release It!', 'Patterns for resilient production software.', 3799, 'USD', 50,
        (select id from category where slug = 'books'), true),
    ('EL-2001', 'Mechanical Keyboard', 'Hot-swappable, 75% layout.', 10900, 'USD', 200,
        (select id from category where slug = 'electronics'), true),
    ('EL-2002', 'USB-C Hub', '7-in-1 aluminium hub.', 3499, 'USD', 50,
        (select id from category where slug = 'electronics'), true),
    ('HK-3001', 'Pour-Over Coffee Set', 'Glass dripper with reusable filter.', 2899, 'USD', 50,
        (select id from category where slug = 'home-kitchen'), true),
    ('HK-3002', 'Cast Iron Skillet', 'Pre-seasoned 10-inch skillet.', 3999, 'USD', 50,
        (select id from category where slug = 'home-kitchen'), true);
