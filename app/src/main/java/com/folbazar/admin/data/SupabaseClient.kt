package com.folbazar.admin.data

import com.folbazar.admin.BuildConfig
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.contentOrNull

/** Thin REST wrapper around Supabase (PostgREST + GoTrue auth). No SDK needed. */
class SupabaseClient {
    private val client = OkHttpClient()
    private val jsonMedia = "application/json; charset=utf-8".toMediaType()
    private val supabaseUrl: String
        get() {
            val raw = BuildConfig.SUPABASE_URL.trim()
            return when {
                raw.startsWith("https://") || raw.startsWith("http://") -> raw.trimEnd('/')
                raw.isBlank() -> "https://jqaswzjeyuyhtwcjnusr.supabase.co"
                else -> "https://$raw".trimEnd('/')
            }
        }

    private val restBase get() = "$supabaseUrl/rest/v1"
    private val authBase get() = "$supabaseUrl/auth/v1"
    private val anonKey = BuildConfig.SUPABASE_PUBLISHABLE_KEY

    // Once a user logs in we call the API as them (so RLS policies apply); otherwise fall back to the anon key.
    private val bearer get() = Session.accessToken ?: anonKey

    private fun Request.Builder.withAuth(): Request.Builder =
        addHeader("apikey", anonKey).addHeader("Authorization", "Bearer $bearer")

    private fun execute(request: Request): String {
        client.newCall(request).execute().use { resp ->
            val text = resp.body?.string() ?: ""
            if (resp.code == 401 && Session.refreshToken != null && request.url.encodedPath != "/auth/v1/token") {
                val refreshed = refreshSession()
                if (refreshed) {
                    val retry = request.newBuilder()
                        .header("Authorization", "Bearer ${Session.accessToken}")
                        .build()
                    client.newCall(retry).execute().use { retryResp ->
                        val retryText = retryResp.body?.string() ?: ""
                        if (!retryResp.isSuccessful) throw IOException("Supabase ${retryResp.code}: $retryText")
                        return retryText.ifBlank { "[]" }
                    }
                }
            }
            if (!resp.isSuccessful) throw IOException("Supabase ${resp.code}: $text")
            return text.ifBlank { "[]" }
        }
    }

    /** Refreshes an expired access token once, then REST calls retry automatically. */
    private fun refreshSession(): Boolean {
        val refresh = Session.refreshToken ?: return false
        return try {
            val body = "{\"refresh_token\":${jsonString(refresh)}}"
            val request = Request.Builder().url("$authBase/token?grant_type=refresh_token")
                .addHeader("apikey", anonKey)
                .addHeader("Content-Type", "application/json")
                .post(body.toRequestBody(jsonMedia)).build()
            client.newCall(request).execute().use { resp ->
                val text = resp.body?.string() ?: ""
                if (!resp.isSuccessful) return false
                val root = Json.parseToJsonElement(text).jsonObject
                val token = root["access_token"]?.jsonPrimitive?.contentOrNull ?: return false
                val newRefresh = root["refresh_token"]?.jsonPrimitive?.contentOrNull
                Session.updateAccessToken(token, newRefresh)
                true
            }
        } catch (_: Exception) {
            false
        }
    }

    fun get(table: String, query: String = "?select=*"): String {
        val r = Request.Builder().url("$restBase/$table$query").withAuth().get().build()
        return execute(r)
    }

    /** GET with the just-issued Auth access token, so profile RLS applies. */
    fun getWithBearer(table: String, query: String, accessToken: String): String {
        val r = Request.Builder().url("$restBase/$table$query")
            .addHeader("apikey", anonKey)
            .addHeader("Authorization", "Bearer $accessToken")
            .get()
            .build()
        return execute(r)
    }

    fun post(table: String, jsonBody: String): String {
        val r = Request.Builder().url("$restBase/$table").withAuth()
            .addHeader("Content-Type", "application/json")
            .addHeader("Prefer", "return=representation")
            .post(jsonBody.toRequestBody(jsonMedia)).build()
        return execute(r)
    }

    fun patch(table: String, filter: String, jsonBody: String): String {
        val r = Request.Builder().url("$restBase/$table?$filter").withAuth()
            .addHeader("Content-Type", "application/json")
            .addHeader("Prefer", "return=representation")
            .patch(jsonBody.toRequestBody(jsonMedia)).build()
        return execute(r)
    }

    fun delete(table: String, filter: String): String {
        val r = Request.Builder().url("$restBase/$table?$filter").withAuth().delete().build()
        return execute(r)
    }

    /** Email/password sign-in against Supabase Auth (GoTrue). Returns the raw JSON response. */
    fun signIn(email: String, password: String): String {
        val body = """{"email":${jsonString(email)},"password":${jsonString(password)}}"""
        val r = Request.Builder().url("$authBase/token?grant_type=password")
            .addHeader("apikey", anonKey)
            .addHeader("Content-Type", "application/json")
            .post(body.toRequestBody(jsonMedia)).build()
        return execute(r)
    }

    private fun jsonString(s: String) = "\"" + s.replace("\\", "\\\\").replace("\"", "\\\"") + "\""
}
