package com.example.data.network

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Environment
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * Downloads a release .apk itself (instead of handing it to Android's DownloadManager or a
 * browser) so the app can show live progress in its own UI, and hands the finished file
 * straight to the system package installer — no notification shade, no browser tab, no
 * leaving the app.
 */
object ApkUpdateDownloader {

    private const val FILE_NAME = "bus-terminal-bd-admin-update.apk"

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()

    /** True if the user still needs to grant "install unknown apps" for this app first. */
    fun needsInstallPermission(context: Context): Boolean =
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
            !context.packageManager.canRequestPackageInstalls()

    /**
     * Downloads [apkUrl] to app-private storage, reporting progress as it goes.
     * [onProgress] is invoked on the Main dispatcher, safe to use directly from Compose state.
     * Returns the downloaded file on success; throws on any failure (network, HTTP, IO).
     */
    suspend fun download(
        context: Context,
        apkUrl: String,
        onProgress: (percent: Int, downloadedMb: Double, totalMb: Double) -> Unit
    ): File = withContext(Dispatchers.IO) {
        val destinationDir = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
            ?: context.filesDir
        val destinationFile = File(destinationDir, FILE_NAME)
        if (destinationFile.exists()) destinationFile.delete()

        val request = Request.Builder().url(apkUrl).build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw IOException("সার্ভার এরর (HTTP ${response.code})")
            }
            val body = response.body ?: throw IOException("খালি রেসপন্স পাওয়া গেছে")
            val totalBytes = body.contentLength()
            var downloadedBytes = 0L
            var lastReportedPercent = -1

            body.byteStream().use { input ->
                FileOutputStream(destinationFile).use { output ->
                    val buffer = ByteArray(8 * 1024)
                    while (true) {
                        val read = input.read(buffer)
                        if (read == -1) break
                        output.write(buffer, 0, read)
                        downloadedBytes += read

                        if (totalBytes > 0) {
                            val percent = ((downloadedBytes * 100) / totalBytes).toInt()
                            if (percent != lastReportedPercent) {
                                lastReportedPercent = percent
                                withContext(Dispatchers.Main) {
                                    onProgress(
                                        percent,
                                        downloadedBytes / 1024.0 / 1024.0,
                                        totalBytes / 1024.0 / 1024.0
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        if (!destinationFile.exists() || destinationFile.length() == 0L) {
            throw IOException("ডাউনলোড করা ফাইল খালি")
        }
        destinationFile
    }

    /** Hands the downloaded APK to the system installer via FileProvider. */
    fun installApk(context: Context, file: File) {
        val apkUri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
        val installIntent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(apkUri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(installIntent)
    }
}
