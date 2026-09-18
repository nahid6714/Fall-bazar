# Fol Bazar Admin Desktop v1.1.0

Cross-platform desktop rebuild of the Fol Bazar Admin workflow.

## Added in this build
- Full CRUD editor improvements for products, categories, coupons and banners
- Product category selector
- Product image preview + Cloudinary upload
- Product variant manager using `product_variants`
- Order detail modal with `order_items`
- Order status/payment status controls
- CSV export for filtered orders
- Better desktop table actions and responsive modal layout
- Windows NSIS, Linux AppImage/deb and macOS dmg packaging targets

## Run on Windows
1. Install Node.js 20+.
2. Open this folder in PowerShell.
3. Run `npm install`.
4. Run `npm run dev` to test.
5. Run `npm run dist` to create the Windows installer.

The installer is generated under `release/`.

## Backend
Use the same Supabase project and Cloudinary unsigned upload preset as the existing Admin app. Do not place any Supabase service-role key or Cloudinary API secret in the desktop app.
