package com.folbazar.admin.data

import android.content.Context
import android.net.Uri
import com.folbazar.admin.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

/** Uploads a product photo to Cloudinary using an unsigned upload preset (no API secret in the app). */
class CloudinaryClient(private val context: Context) {
    private val client = OkHttpClient()

    suspend fun uploadImage(uri: Uri): Result<String> = runCatching {
        withContext(Dispatchers.IO) {
            val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                ?: throw IllegalStateException("ছবি পড়া যায়নি")
            val filePart = bytes.toRequestBody("image/*".toMediaType())
            val body = MultipartBody.Builder().setType(MultipartBody.FORM)
                .addFormDataPart("file", "product.jpg", filePart)
                .addFormDataPart("upload_preset", BuildConfig.CLOUDINARY_UPLOAD_PRESET)
                .build()
            val url = "https://api.cloudinary.com/v1_1/${BuildConfig.CLOUDINARY_CLOUD_NAME}/image/upload"
            val request = Request.Builder().url(url).post(body).build()
            client.newCall(request).execute().use { resp ->
                val text = resp.body?.string() ?: "{}"
                if (!resp.isSuccessful) throw IllegalStateException("Cloudinary আপলোড ব্যর্থ: $text")
                Json.parseToJsonElement(text).jsonObject["secure_url"]?.jsonPrimitive?.content
                    ?: throw IllegalStateException("secure_url পাওয়া যায়নি")
            }
        }
    }
}
