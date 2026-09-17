# Fol Bazar Admin v37 — Unified Image Control

This version updates image handling in Banner, Product, and Category editors.

## Included
- One unified image area with URL input, live preview, upload, and remove.
- Banner editor has a bordered Website Banner Frame.
- Banner width (50–100%) and height (120–500px) update the live frame immediately.
- Product main image has the same unified control.
- Product gallery thumbnails have a remove button.
- Category image has the same unified control.
- Removing an assigned image clears the database field when the record is saved.

## Supabase
No new SQL migration is required beyond the existing `BANNER_SIZE_MIGRATION.sql` already run for `site_banners`.

## Cloudinary deletion note
The app removes the image assignment/URL from the record. It does not expose a Cloudinary API secret inside the APK. Physical Cloudinary asset deletion requires a secure server-side endpoint and is intentionally not done client-side.
