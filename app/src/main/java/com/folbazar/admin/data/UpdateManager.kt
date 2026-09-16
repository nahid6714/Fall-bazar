package com.folbazar.admin.data

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.core.content.FileProvider
import com.folbazar.admin.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL

data class AppUpdateInfo(
    val versionCode: Int,
    val versionName: String,
    val releaseName: String,
    val downloadUrl: String,
    val releaseUrl: String
)

sealed class UpdateResult {
    data object UpToDate : UpdateResult()
    data class Available(val info: AppUpdateInfo) : UpdateResult()
    data class Error(val message: String) : UpdateResult()
}

data class DownloadState(
    val running: Boolean = false,
    val progress: Int = 0,
    val downloadedBytes: Long = 0L,
    val totalBytes: Long = -1L,
    val file: File? = null,
    val error: String? = null
)

object UpdateManager {
    private const val REPO = "nahid6714/Fall-bazar"
    private const val LATEST_URL = "https://api.github.com/repos/$REPO/releases/latest"

    suspend fun checkForUpdate(): UpdateResult = withContext(Dispatchers.IO) {
        try {
            val conn = (URL(LATEST_URL).openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 15_000
                readTimeout = 20_000
                setRequestProperty("Accept", "application/vnd.github+json")
                setRequestProperty("User-Agent", "FolBazar-Admin/${BuildConfig.VERSION_NAME}")
            }

            try {
                if (conn.responseCode !in 200..299) {
                    return@withContext UpdateResult.Error("GitHub update check failed: HTTP ${conn.responseCode}")
                }

                val body = conn.inputStream.bufferedReader().use { it.readText() }
                val root = Json.parseToJsonElement(body).jsonObject
                val tag = root["tag_name"]?.jsonPrimitive?.content.orEmpty()
                val releaseName = root["name"]?.jsonPrimitive?.content ?: tag
                val releaseUrl = root["html_url"]?.jsonPrimitive?.content.orEmpty()
                val assets = root["assets"]?.jsonArray ?: return@withContext UpdateResult.Error("Release-এ APK পাওয়া যায়নি")

                val apk = assets.firstOrNull {
                    val name = it.jsonObject["name"]?.jsonPrimitive?.content.orEmpty()
                    name.endsWith(".apk", ignoreCase = true) &&
                        name.startsWith("FolBazar-Admin-", ignoreCase = true)
                }?.jsonObject ?: return@withContext UpdateResult.Error("Latest release-এ Fol Bazar APK পাওয়া যায়নি")

                val assetName = apk["name"]?.jsonPrimitive?.content.orEmpty()
                val downloadUrl = apk["browser_download_url"]?.jsonPrimitive?.content.orEmpty()
                val versionCode = Regex("""FolBazar-Admin-(\d+)\.apk""", RegexOption.IGNORE_CASE)
                    .find(assetName)?.groupValues?.getOrNull(1)?.toIntOrNull()
                    ?: Regex("""(\d+)""").find(tag)?.value?.toIntOrNull()
                    ?: 0
                val versionName = Regex("""v?(\d+\.\d+(?:\.\d+)?)""").find(tag)?.groupValues?.getOrNull(1)
                    ?: tag.removePrefix("v")

                if (downloadUrl.isBlank() || versionCode <= 0) {
                    return@withContext UpdateResult.Error("Latest release-এর version/APK তথ্য সঠিক নয়")
                }

                if (versionCode > BuildConfig.VERSION_CODE) {
                    UpdateResult.Available(
                        AppUpdateInfo(versionCode, versionName, releaseName, downloadUrl, releaseUrl)
                    )
                } else {
                    UpdateResult.UpToDate
                }
            } finally {
                conn.disconnect()
            }
        } catch (e: Exception) {
            UpdateResult.Error(e.message ?: "Update check করা যায়নি")
        }
    }

    suspend fun downloadUpdate(
        context: Context,
        info: AppUpdateInfo,
        onProgress: (DownloadState) -> Unit
    ): Result<File> = withContext(Dispatchers.IO) {
        try {
            val updatesDir = File(context.filesDir, "updates").apply { mkdirs() }
            val safeName = "FolBazar-Admin-${info.versionCode}.apk"
            val target = File(updatesDir, safeName)
            if (target.exists() && target.length() > 0L) {
                onProgress(DownloadState(progress = 100, downloadedBytes = target.length(), totalBytes = target.length(), file = target))
                return@withContext Result.success(target)
            }

            val conn = (URL(info.downloadUrl).openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 20_000
                readTimeout = 60_000
                instanceFollowRedirects = true
                setRequestProperty("User-Agent", "FolBazar-Admin/${BuildConfig.VERSION_NAME}")
                setRequestProperty("Accept", "application/octet-stream")
            }

            try {
                if (conn.responseCode !in 200..299) {
                    return@withContext Result.failure(Exception("APK download failed: HTTP ${conn.responseCode}"))
                }

                val total = conn.contentLengthLong
                var done = 0L
                onProgress(DownloadState(running = true, totalBytes = total))

                FileOutputStream(target).use { output ->
                    conn.inputStream.use { input ->
                        val buffer = ByteArray(64 * 1024)
                        while (true) {
                            val read = input.read(buffer)
                            if (read < 0) break
                            output.write(buffer, 0, read)
                            done += read
                            val percent = if (total > 0) ((done * 100) / total).toInt().coerceIn(0, 100) else 0
                            onProgress(DownloadState(running = true, progress = percent, downloadedBytes = done, totalBytes = total))
                        }
                    }
                }

                onProgress(DownloadState(progress = 100, downloadedBytes = done, totalBytes = total, file = target))
                Result.success(target)
            } finally {
                conn.disconnect()
            }
        } catch (e: Exception) {
            onProgress(DownloadState(error = e.message ?: "APK download করা যায়নি"))
            Result.failure(e)
        }
    }

    fun canInstallPackages(context: Context): Boolean =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.packageManager.canRequestPackageInstalls()
        } else true

    fun openUnknownSourcesSettings(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val intent = Intent(
                android.provider.Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                Uri.parse("package:${context.packageName}")
            ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        }
    }

    fun installApk(context: Context, file: File): Result<Unit> {
        return try {
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/vnd.android.package-archive")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(intent)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
