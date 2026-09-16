package com.example.data.network

import com.example.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.concurrent.TimeUnit

data class AppUpdateInfo(
    val buildNumber: Int,
    val releaseName: String,
    val releaseNotes: String,
    val apkDownloadUrl: String
)

/**
 * Checks the project's GitHub Releases (published automatically by
 * .github/workflows/release-apk.yml on every push) for a newer signed APK build than the
 * one currently installed, so the app can prompt the admin to update without needing the
 * Play Store.
 *
 * Compares BuildConfig.APP_BUILD_NUMBER (set at build time from `-PappBuildNumber=<run
 * number>`) against the numeric suffix of the latest release's "build-<run number>" tag.
 */
object UpdateChecker {

    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()

    /** Returns update info if a newer build is published on GitHub, else null. Never throws. */
    suspend fun checkForUpdate(): AppUpdateInfo? = withContext(Dispatchers.IO) {
        try {
            val url = "https://api.github.com/repos/${BuildConfig.GITHUB_REPO}/releases/latest"
            val request = Request.Builder()
                .url(url)
                .header("Accept", "application/vnd.github+json")
                .build()

            val response = client.newCall(request).execute()
            val body = response.body?.string().orEmpty()
            if (!response.isSuccessful) return@withContext null

            val json = JSONObject(body)
            val tagName = json.optString("tag_name", "")
            // Tag format published by release-apk.yml: "build-<run_number>"
            val remoteBuildNumber = tagName.substringAfter("build-", "").toIntOrNull()
                ?: return@withContext null
            val currentBuildNumber = BuildConfig.APP_BUILD_NUMBER.toIntOrNull() ?: 0

            if (remoteBuildNumber <= currentBuildNumber) return@withContext null

            val assets = json.optJSONArray("assets") ?: return@withContext null
            var apkUrl: String? = null
            for (i in 0 until assets.length()) {
                val asset = assets.getJSONObject(i)
                val name = asset.optString("name", "")
                if (name.endsWith(".apk", ignoreCase = true)) {
                    apkUrl = asset.optString("browser_download_url", "")
                    break
                }
            }
            if (apkUrl.isNullOrBlank()) return@withContext null

            AppUpdateInfo(
                buildNumber = remoteBuildNumber,
                releaseName = json.optString("name", tagName),
                releaseNotes = json.optString("body", ""),
                apkDownloadUrl = apkUrl
            )
        } catch (_: Exception) {
            // Network hiccup, rate limit, malformed response, etc. — never let an update
            // check crash or block the app; just silently skip it this launch.
            null
        }
    }
}
