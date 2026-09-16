-- This file is only needed if your live products table is from the older app schema.
-- The current admin app uses the base Mousum Bazar schema:
-- name, slug, category_id, stock_quantity, is_active, etc.
-- Run only the statements for columns that are actually missing.

alter table if exists public.products add column if not exists name text;
alter table if exists public.products add column if not exists slug text;
alter table if exists public.products add column if not exists category_id uuid references public.categories(id) on delete set null;
alter table if exists public.products add column if not exists description text;
alter table if exists public.products add column if not exists image_url text;
alter table if exists public.products add column if not exists old_price numeric(12,2);
alter table if exists public.products add column if not exists stock_quantity integer not null default 0;
alter table if exists public.products add column if not exists sold_quantity integer not null default 0;
alter table if exists public.products add column if not exists discount_percent numeric(5,2);
alter table if exists public.products add column if not exists is_active boolean not null default true;
alter table if exists public.products add column if not exists is_featured boolean not null default false;
alter table if exists public.products add column if not exists is_flash_sale boolean not null default false;
alter table if exists public.products add column if not exists is_hot_deal boolean not null default false;
alter table if exists public.products add column if not exists sort_order integer not null default 0;

-- If old rows use title/stock, copy values after adding the new columns.
-- Only execute these updates if the old columns exist in your project.
-- update public.products set name = coalesce(name, title) where name is null;
-- update public.products set stock_quantity = coalesce(stock_quantity, stock) where stock_quantity is null;
