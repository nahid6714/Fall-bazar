package com.example.ui.components

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.data.network.ApkUpdateDownloader
import com.example.data.network.AppUpdateInfo
import kotlinx.coroutines.launch
import java.io.File

/** UI state for the in-app update download shown inside [UpdateAvailableDialog]. */
private sealed class UpdateDownloadState {
    data object Idle : UpdateDownloadState()
    data class Downloading(val percent: Int, val downloadedMb: Double, val totalMb: Double) : UpdateDownloadState()
    data class Completed(val file: File) : UpdateDownloadState()
    data class Failed(val message: String) : UpdateDownloadState()
}

/**
 * Shows update details and downloads the new APK with a live progress bar inside this same
 * dialog (no system notification, no browser tab). Once the download finishes, the confirm
 * button switches to "ইন্সটল করুন", which hands the file straight to the system installer.
 */
@Composable
fun UpdateAvailableDialog(info: AppUpdateInfo, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var downloadState by remember { mutableStateOf<UpdateDownloadState>(UpdateDownloadState.Idle) }

    fun startDownload() {
        if (ApkUpdateDownloader.needsInstallPermission(context)) {
            context.startActivity(
                Intent(
                    android.provider.Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                    Uri.parse("package:${context.packageName}")
                )
            )
            return
        }
        downloadState = UpdateDownloadState.Downloading(0, 0.0, 0.0)
        coroutineScope.launch {
            try {
                val file = ApkUpdateDownloader.download(context, info.apkDownloadUrl) { percent, downloadedMb, totalMb ->
                    downloadState = UpdateDownloadState.Downloading(percent, downloadedMb, totalMb)
                }
                downloadState = UpdateDownloadState.Completed(file)
            } catch (e: Exception) {
                downloadState = UpdateDownloadState.Failed(e.localizedMessage ?: "অজানা সমস্যা")
            }
        }
    }

    val state = downloadState
    AlertDialog(
        onDismissRequest = {
            // Don't let a tap-outside close the dialog mid-download and lose progress.
            if (state !is UpdateDownloadState.Downloading) onDismiss()
        },
        title = { Text("নতুন আপডেট উপলব্ধ (Build #${info.buildNumber})") },
        text = {
            when (state) {
                is UpdateDownloadState.Idle -> {
                    Text(
                        info.releaseNotes.ifBlank {
                            "অ্যাপের একটি নতুন সংস্করণ পাওয়া গেছে। আপডেট করতে ডাউনলোড করুন।"
                        }
                    )
                }
                is UpdateDownloadState.Downloading -> {
                    Column {
                        Text("ডাউনলোড হচ্ছে... ${state.percent}%")
                        Spacer(modifier = Modifier.height(8.dp))
                        LinearProgressIndicator(
                            progress = { state.percent / 100f },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("%.1f".format(state.downloadedMb) + " / " + "%.1f".format(state.totalMb) + " MB")
                    }
                }
                is UpdateDownloadState.Completed -> {
                    Text("ডাউনলোড সম্পন্ন হয়েছে। এখন ইন্সটল করুন।")
                }
                is UpdateDownloadState.Failed -> {
                    Text("ডাউনলোড ব্যর্থ হয়েছে: ${state.message}")
                }
            }
        },
        confirmButton = {
            when (state) {
                is UpdateDownloadState.Idle -> {
                    TextButton(onClick = { startDownload() }) {
                        Text("ডাউনলোড করুন")
                    }
                }
                is UpdateDownloadState.Downloading -> {
                    TextButton(onClick = {}, enabled = false) {
                        Text("ডাউনলোড হচ্ছে...")
                    }
                }
                is UpdateDownloadState.Completed -> {
                    TextButton(onClick = {
                        ApkUpdateDownloader.installApk(context, state.file)
                        onDismiss()
                    }) {
                        Text("ইন্সটল করুন")
                    }
                }
                is UpdateDownloadState.Failed -> {
                    TextButton(onClick = { startDownload() }) {
                        Text("আবার চেষ্টা করুন")
                    }
                }
            }
        },
        dismissButton = {
            if (state !is UpdateDownloadState.Downloading) {
                TextButton(onClick = onDismiss) {
                    Text("পরে")
                }
            }
        }
    )
}
