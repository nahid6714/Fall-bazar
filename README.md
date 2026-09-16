# ফল বাজার Admin

Production-oriented Android Admin starter for the ফল বাজার website.

Included now: Jetpack Compose UI, email/password login (Supabase Auth), dashboard, full product CRUD (add/edit/delete + Cloudinary image upload), order status management, customer list, and automatic GitHub Release APK workflow.

## Required Supabase setup

1. **Auth user** — Create at least one admin user under Authentication → Users (email + password). The app logs in with this and uses the user's own access token for all API calls, so Row Level Security policies are enforced correctly.
2. **Row Level Security** — Make sure your `products`, `orders`, and `customers` tables have RLS policies that allow the `authenticated` role to select/insert/update/delete (or scope it to an `is_admin` claim/table, however you manage admin access).
3. **Expected columns** (rename in `Repository.kt` if yours differ):
   - `products`: id, title, price, old_price, stock, image_url, category, is_active, created_at
   - `orders`: id, customer_name, total, status, created_at
   - `customers`: id, name, phone, address, created_at
4. **Cloudinary** — create an **unsigned** upload preset (Settings → Upload → Upload presets) so the app can upload product photos without embedding your API secret.

Fill real values into `local.properties` (copy from `local.properties.example`) for local builds, and into the repo's Actions secrets for release builds.

## GitHub Release

Push a version tag:

`git tag v1.0.0 && git push origin v1.0.0`

GitHub Actions builds the release APK and attaches it to a GitHub Release.

Repository Actions secrets:
- SUPABASE_URL
- SUPABASE_PUBLISHABLE_KEY
- CLOUDINARY_CLOUD_NAME
- CLOUDINARY_UPLOAD_PRESET

Never put a Supabase service-role/secret key in the Android app.
