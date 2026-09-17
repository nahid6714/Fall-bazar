# Fol Bazar Admin — Banner Image Editor

- Width/Height editing controls have been removed from the Banner Editor.
- Admin keeps the normal/default banner frame for preview.
- Preview uses crop behavior matching the existing website frame.
- Admin can change the banner image and save it without changing stored banner dimensions.
- Existing Supabase `width_percent` and `height_px` values are preserved.
- New banners rely on the database's normal/default dimension values.
- Website source files are not changed by this Admin-only update.
