# ফল বাজার Admin — Full Control Panel v13

This version turns the Android app into the main admin control panel for the Supabase-backed fruit shop.

## Included
- Supabase Auth + `profiles.role == admin` login gate
- Products: add / edit / delete, stock, price, old price, description, category, Cloudinary image, active, featured, flash-sale, hot-deal
- Categories: add / edit / delete / active state
- Product variants table support in the repository
- Orders: all orders, customer/address/payment details, order status and payment status
- Customers: profiles list and role management
- Complaints: status + admin note
- Coupons: percent/fixed discount, minimum order, maximum discount, usage limit, active state
- Wishlist summary: product-wise wishlist counts
- Dashboard: product/order/customer/pending/delivered-sales/complaint counts
- No Supabase service-role/secret key in the APK

## Important: database setup
Run `supabase/ADMIN_SETUP.sql` in the Supabase SQL Editor once. It adds the missing admin authorization policies and the coupon/wishlist/site-settings tables.

The current product schema is the schema from `mousum-bazar-supabase-schema.sql`:
- `products.name`
- `products.slug`
- `products.category_id`
- `products.stock_quantity`
- `products.image_url`
- `products.is_active`
- `products.is_featured`
- `products.is_flash_sale`
- `products.is_hot_deal`

The previous Admin v12 app was querying the old names `category`, `stock`, `title`; that caused the 400 schema-cache error. v13 now uses the current schema.

Only run `supabase/PRODUCT_SCHEMA_FIX.sql` if your live database is actually missing the new product columns. Do not run it blindly on an already-correct schema.

## Admin account
1. Create/sign in the user in Supabase Auth.
2. Make that user's `public.profiles.role` equal to `admin`.
3. Run `ADMIN_SETUP.sql` before using CRUD.

The app uses the user's JWT for every database request, and the database RLS policies are the final authorization layer.

## Cloudinary
The APK uses an unsigned upload preset. No Cloudinary API secret is stored in the app.

## GitHub Actions
The existing release workflow can build and publish the APK. Required values remain:
- `SUPABASE_URL`
- `SUPABASE_PUBLISHABLE_KEY`
- `CLOUDINARY_CLOUD_NAME`
- `CLOUDINARY_UPLOAD_PRESET`

For Supabase schema changes, prefer migration files / version-controlled SQL rather than repeatedly editing the production database manually.
