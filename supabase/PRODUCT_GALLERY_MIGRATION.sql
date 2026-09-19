-- Adds a column to store multiple product images (gallery).
-- Run this once in the Supabase SQL editor. The first URL in the array is
-- usually the same as image_url (the "main" image), the rest are extra photos.

alter table if exists public.products
  add column if not exists gallery_urls jsonb not null default '[]'::jsonb;

-- Optional: backfill existing single image_url into the new gallery column
-- so older products already show their one image inside the gallery too.
update public.products
set gallery_urls = jsonb_build_array(image_url)
where gallery_urls = '[]'::jsonb and image_url is not null and image_url <> '';
