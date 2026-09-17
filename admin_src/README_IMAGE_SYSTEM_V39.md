# Fol Bazar Admin v39 — Live Website Banner Preview

- Banner list thumbnails use the same 250px clipped/cropped viewport as the current live mobile hero renderer.
- Banner editor preview uses a fixed 250px viewport and `ContentScale.Crop`, matching the current live website behavior.
- Existing Supabase `width_percent` and `height_px` fields are preserved and still saved.
- Website source is not modified by this package.
- Note: the current website renderer uses a fixed mobile hero viewport, so changing `height_px` in Admin does not change the live website until the website renderer is intentionally changed.
