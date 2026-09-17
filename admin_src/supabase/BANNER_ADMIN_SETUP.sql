-- Fol Bazar Admin: website banner management
-- Run this once in Supabase SQL Editor if site_banners does not exist yet.
create table if not exists public.site_banners (
  id uuid not null default gen_random_uuid(),
  banner_type text not null default 'hero',
  title text null,
  alt_text text not null default 'ফল বাজার ব্যানার',
  image_url text not null,
  link_url text null,
  sort_order integer not null default 0,
  is_active boolean not null default true,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now(),
  constraint site_banners_pkey primary key (id),
  constraint site_banners_type_check check (banner_type in ('hero','promo'))
);
create index if not exists site_banners_active_order_idx on public.site_banners (banner_type, is_active, sort_order);
alter table public.site_banners enable row level security;
drop policy if exists "Public can read active banners" on public.site_banners;
create policy "Public can read active banners" on public.site_banners for select to anon, authenticated using (is_active = true);
drop policy if exists "Admins can manage banners" on public.site_banners;
create policy "Admins can manage banners" on public.site_banners for all to authenticated using ((select public.is_admin())) with check ((select public.is_admin()));
grant select on public.site_banners to anon, authenticated;
grant insert, update, delete on public.site_banners to authenticated;
notify pgrst, 'reload schema';

-- Banner display size controls (used by Admin App and Website)
alter table public.site_banners
  add column if not exists width_percent integer not null default 100,
  add column if not exists height_px integer not null default 180;

alter table public.site_banners
  drop constraint if exists site_banners_width_percent_check;
alter table public.site_banners
  add constraint site_banners_width_percent_check check (width_percent between 50 and 100);
alter table public.site_banners
  drop constraint if exists site_banners_height_px_check;
alter table public.site_banners
  add constraint site_banners_height_px_check check (height_px between 120 and 500);
