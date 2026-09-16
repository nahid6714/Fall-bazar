# ফল বাজার Admin

Android Admin App for the ফল বাজার e-commerce project.

## Included

- Jetpack Compose Admin UI
- Supabase Auth email/password login
- Dashboard
- Products / stock module
- Orders module
- Customer module
- Coupons / Wishlist / Settings module structure
- Supabase REST client with the logged-in user's access token (RLS applies)
- Cloudinary configuration hooks
- Automatic GitHub Release APK workflow

## Automatic Release APK

Every successful push to `main` automatically:

1. Builds the release APK.
2. Verifies that the APK exists.
3. Uploads the APK as a GitHub Actions artifact.
4. Creates a new GitHub Release.
5. Attaches the APK to that Release.
6. Marks the newest release as the latest release.

Example generated tag:

```text
v1.0.27
```

No manual `git tag` command is required for normal releases.

You can also start the same workflow manually from **GitHub → Actions → Android Release APK → Run workflow**.

## GitHub Actions secrets

Add these under **Repository → Settings → Secrets and variables → Actions → New repository secret**:

- `SUPABASE_URL`
- `SUPABASE_PUBLISHABLE_KEY`
- `CLOUDINARY_CLOUD_NAME`
- `CLOUDINARY_UPLOAD_PRESET`

The Supabase service-role/secret key must never be placed inside the Android app.

## Local setup

1. Open the project in Android Studio.
2. Copy `local.properties.example` to `local.properties`.
3. Fill in the local Supabase and Cloudinary values.
4. Sync Gradle and run the app.

`local.properties` and keystores are ignored by Git.
