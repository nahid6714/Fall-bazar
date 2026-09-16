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


## Automatic release fix

The GitHub Actions workflow uses Android SDK command-line tools without requesting the
removed legacy `tools` package. Every successful push to `main` builds the release APK,
uploads an Actions artifact, and creates a GitHub Release such as `v1.0.12` with the APK attached.


### Release workflow note

The workflow does not assume the APK filename is `app-release.apk`.
It searches the release output directory for the actual APK produced by Gradle,
copies it to a stable `FolBazar-Admin-vX.Y.Z.apk` filename, then publishes that file.


### Release workflow v4

The publish step uses the APK path and release tag exported by the previous step.
It does not reference an undefined `runNumber` shell variable.


## Automatic Release v5

Every successful push to `main` creates a unique release tag using the GitHub Actions
run number, for example `v1.0.15`. The workflow passes the tag through an environment
variable, so it does not use an undefined `runNumber` shell variable.


## Installable APK note

The release APK is signed with a generated Android debug signing key in CI so this
starter/demo APK can be installed directly on Android. This is intentionally not a
production signing key. Before public production distribution or Play Store release,
replace it with a persistent private release keystore stored in GitHub Actions Secrets.


## Stable CI build

The release workflow includes Gradle/Maven HTTP timeouts, retry settings, and five
build attempts with increasing waits. This protects the build from temporary Maven
Central `429 Too Many Requests` responses. It does not treat a network rate-limit
as an application-code failure.


## APK signing fix v8

The CI workflow explicitly runs `zipalign` and `apksigner sign` after Gradle builds
the APK, then verifies the final APK with `apksigner verify`. This avoids publishing
an unsigned/invalid APK and removes the previous META-INF verification failure.


## Auth URL fix v9

The release build now has safe Supabase defaults so a downloaded APK does not build
an invalid URL such as `/auth/v1/...` when GitHub Actions Secrets are missing.
CI secrets still override the defaults when provided. Supabase Auth uses the project's
real HTTPS URL for email/password sign-in.


## v10 final build fix

The CI failure was traced directly to `MainActivity.kt`: `Session` was imported twice
and initialized twice. The duplicate import/initialization has been removed. A separate
Kotlin compilation check now runs before the full release APK build so source-level
compilation errors are detected early.


## v11 customer-table fix

The Supabase schema uses `public.profiles` for customer accounts; there is no
`public.customers` table. The Admin app now reads customer profiles from
`profiles` instead of requesting the nonexistent `customers` table.
