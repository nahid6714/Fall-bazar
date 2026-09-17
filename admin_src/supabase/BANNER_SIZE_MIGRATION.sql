-- Fol Bazar: Banner size controls
-- Run this once in Supabase SQL Editor after site_banners exists.

alter table public.site_banners
  add column if not exists width_percent integer not null default 100,
  add column if not exists height_px integer not null default 180;

update public.site_banners
set width_percent = coalesce(width_percent, 100),
    height_px = coalesce(height_px, 180);

alter table public.site_banners
  drop constraint if exists site_banners_width_percent_check;

alter table public.site_banners
  add constraint site_banners_width_percent_check
  check (width_percent between 50 and 100);

alter table public.site_banners
  drop constraint if exists site_banners_height_px_check;

alter table public.site_banners
  add constraint site_banners_height_px_check
  check (height_px between 120 and 500);

notify pgrst, 'reload schema';
