-- ফল বাজার Admin backend upgrade
-- Run this in Supabase SQL Editor AFTER the base schema is present.
-- Never put service_role/secret keys in the Android app.

create extension if not exists pgcrypto;

-- Extra admin-managed ecommerce tables
create table if not exists public.coupons (
  id uuid primary key default gen_random_uuid(),
  code text not null unique,
  title text,
  discount_type text not null default 'percent' check (discount_type in ('percent','fixed')),
  discount_value numeric(12,2) not null default 0 check (discount_value >= 0),
  min_order numeric(12,2) not null default 0 check (min_order >= 0),
  max_discount numeric(12,2),
  usage_limit integer,
  used_count integer not null default 0,
  is_active boolean not null default true,
  starts_at timestamptz,
  expires_at timestamptz,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now()
);

create table if not exists public.coupon_usages (
  id uuid primary key default gen_random_uuid(),
  coupon_id uuid not null references public.coupons(id) on delete cascade,
  user_id uuid references auth.users(id) on delete set null,
  order_id uuid references public.orders(id) on delete set null,
  used_at timestamptz not null default now(),
  unique(coupon_id, order_id)
);

create table if not exists public.wishlists (
  id uuid primary key default gen_random_uuid(),
  user_id uuid not null references auth.users(id) on delete cascade,
  product_id uuid not null references public.products(id) on delete cascade,
  created_at timestamptz not null default now(),
  unique(user_id, product_id)
);

create table if not exists public.site_settings (
  key text primary key,
  value jsonb not null default '{}'::jsonb,
  updated_at timestamptz not null default now()
);

create index if not exists wishlists_user_id_idx on public.wishlists(user_id);
create index if not exists wishlists_product_id_idx on public.wishlists(product_id);
create index if not exists coupons_active_idx on public.coupons(is_active);

-- Protect profile roles. Non-admin users may edit their own profile fields, but cannot
-- promote themselves. Admins may change customer/reseller/seller/admin roles.
create or replace function public.is_admin()
returns boolean
language sql
stable
security definer
set search_path = public
as $$
  select exists (
    select 1 from public.profiles p
    where p.id = auth.uid() and p.role = 'admin'
  );
$$;

revoke all on function public.is_admin() from public;
grant execute on function public.is_admin() to anon, authenticated;

create or replace function public.protect_profile_role()
returns trigger
language plpgsql
security definer
set search_path = public
as $$
begin
  if old.role is distinct from new.role and not public.is_admin() then
    raise exception 'Only an admin can change profile role';
  end if;
  return new;
end;
$$;

drop trigger if exists protect_profile_role_trigger on public.profiles;
create trigger protect_profile_role_trigger
before update on public.profiles
for each row execute function public.protect_profile_role();

-- Admin CRUD policies. The app signs in with Supabase Auth and every request uses
-- the user's JWT, so these policies are the real authorization boundary.

do $$
declare t text;
begin
  foreach t in array array['categories','products','product_variants','profiles','orders','order_items','complaints','coupons','coupon_usages','wishlists','site_settings'] loop
    execute format('alter table public.%I enable row level security', t);
  end loop;
end $$;

-- Categories
 drop policy if exists "admins manage categories" on public.categories;
 create policy "admins manage categories" on public.categories for all to authenticated using (public.is_admin()) with check (public.is_admin());

-- Products
 drop policy if exists "admins manage products" on public.products;
 create policy "admins manage products" on public.products for all to authenticated using (public.is_admin()) with check (public.is_admin());

-- Variants
 drop policy if exists "admins manage variants" on public.product_variants;
 create policy "admins manage variants" on public.product_variants for all to authenticated using (public.is_admin()) with check (public.is_admin());

-- Profiles: admins see/update everyone; users keep their own access.
 drop policy if exists "admins manage profiles" on public.profiles;
 create policy "admins manage profiles" on public.profiles for all to authenticated using (public.is_admin()) with check (public.is_admin());

-- Orders
 drop policy if exists "admins manage orders" on public.orders;
 create policy "admins manage orders" on public.orders for all to authenticated using (public.is_admin()) with check (public.is_admin());

-- Order items
 drop policy if exists "admins manage order items" on public.order_items;
 create policy "admins manage order items" on public.order_items for all to authenticated using (public.is_admin()) with check (public.is_admin());

-- Complaints
 drop policy if exists "admins manage complaints" on public.complaints;
 create policy "admins manage complaints" on public.complaints for all to authenticated using (public.is_admin()) with check (public.is_admin());

-- Coupons
 drop policy if exists "admins manage coupons" on public.coupons;
 create policy "admins manage coupons" on public.coupons for all to authenticated using (public.is_admin()) with check (public.is_admin());

-- Coupon usages
 drop policy if exists "admins manage coupon usages" on public.coupon_usages;
 create policy "admins manage coupon usages" on public.coupon_usages for all to authenticated using (public.is_admin()) with check (public.is_admin());

-- Wishlists: customers can access their own; admin can inspect/manage all.
 drop policy if exists "admins manage wishlists" on public.wishlists;
 create policy "admins manage wishlists" on public.wishlists for all to authenticated using (public.is_admin()) with check (public.is_admin());
drop policy if exists "users read own wishlists" on public.wishlists;
create policy "users read own wishlists" on public.wishlists for select to authenticated using (user_id = auth.uid());

-- Site settings are admin-only for now.
 drop policy if exists "admins manage site settings" on public.site_settings;
 create policy "admins manage site settings" on public.site_settings for all to authenticated using (public.is_admin()) with check (public.is_admin());

-- Existing customer profile policies are intentionally retained. This policy makes
-- the role-change trigger the final guard against self-promotion.

-- Optional starter settings
insert into public.site_settings(key,value)
values
 ('store', '{"name":"ফল বাজার","currency":"BDT"}'::jsonb),
 ('delivery', '{"default_charge":0}'::jsonb)
on conflict (key) do nothing;
