package com.example.data.network

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import com.example.data.local.SessionManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.util.concurrent.TimeUnit

/** Exact Cloudinary folder paths used across the app's image/logo uploads. */
object CloudinaryFolders {
    const val BUS_IMAGES = "bus-terminal-bd/bus-images"
    const val COACH_IMAGES = "bus-terminal-bd/coach-images"
    const val COUNTER_IMAGES = "bus-terminal-bd/counter-images"
    const val OPERATOR_LOGOS = "bus-terminal-bd/operator-logos"
    const val TOUR_IMAGES = "bus-terminal-bd/tour-images"
}

class CloudinaryUploader(private val sessionManager: SessionManager) {

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()

    /**
     * Uploads [imageUri] to Cloudinary using the existing unsigned preset, placing it under
     * [folder] (e.g. [CloudinaryFolders.BUS_IMAGES]). Uses the unsigned upload API only — no
     * API secret involved, same as before.
     */
    suspend fun uploadImage(context: Context, imageUri: Uri, folder: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            val cloudName = sessionManager.getCloudinaryCloudName()
            val uploadPreset = sessionManager.getCloudinaryUploadPreset()

            // Read and compress image
            val compressedBytes = compressImage(context, imageUri)
                ?: return@withContext Result.failure(Exception("ছবি প্রস্তুত করতে সমস্যা হয়েছে"))

            val requestBody = MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("upload_preset", uploadPreset)
                .addFormDataPart("folder", folder)
                .addFormDataPart(
                    "file",
                    "btbd_${System.currentTimeMillis()}.jpg",
                    compressedBytes.toRequestBody("image/jpeg".toMediaTypeOrNull())
                )
                .build()

            val uploadUrl = "https://api.cloudinary.com/v1_1/$cloudName/image/upload"
            val request = Request.Builder()
                .url(uploadUrl)
                .post(requestBody)
                .build()

            val response = client.newCall(request).execute()
            val responseBody = response.body?.string().orEmpty()

            if (response.isSuccessful) {
                val json = JSONObject(responseBody)
                val secureUrl = json.optString("secure_url", json.optString("url", ""))
                if (secureUrl.isNotBlank()) {
                    Result.success(secureUrl)
                } else {
                    Result.failure(Exception("Cloudinary থেকে ইমেজ ইউআরএল পাওয়া যায়নি"))
                }
            } else {
                // If demo or unsigned preset not yet configured on Cloudinary, provide graceful fallback
                val errorMsg = try {
                    JSONObject(responseBody).optJSONObject("error")?.optString("message", "") ?: responseBody
                } catch (_: Exception) {
                    responseBody
                }
                Result.failure(Exception("ক্লাউডিনারি আপলোড ব্যর্থ ($errorMsg)"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun compressImage(context: Context, uri: Uri): ByteArray? {
        return try {
            val inputStream: InputStream? = context.contentResolver.openInputStream(uri)
            val originalBitmap = BitmapFactory.decodeStream(inputStream)
            inputStream?.close()

            if (originalBitmap == null) return null

            // Resize if too large (max dimension 1280px)
            val maxDimension = 1280
            val width = originalBitmap.width
            val height = originalBitmap.height
            val scale = if (width > maxDimension || height > maxDimension) {
                val ratio = width.toFloat() / height.toFloat()
                if (ratio > 1) {
                    maxDimension.toFloat() / width
                } else {
                    maxDimension.toFloat() / height
                }
            } else 1.0f

            val scaledBitmap = if (scale < 1.0f) {
                Bitmap.createScaledBitmap(
                    originalBitmap,
                    (width * scale).toInt(),
                    (height * scale).toInt(),
                    true
                )
            } else {
                originalBitmap
            }

            val outputStream = ByteArrayOutputStream()
            scaledBitmap.compress(Bitmap.CompressFormat.JPEG, 80, outputStream)
            outputStream.toByteArray()
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
