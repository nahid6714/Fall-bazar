package com.example.data.network

import com.example.data.local.SessionManager
import com.example.data.model.Admin
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

class SupabaseClient(private val sessionManager: SessionManager) {

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .writeTimeout(20, TimeUnit.SECONDS)
        .build()

    private val jsonMediaType = "application/json; charset=utf-8".toMediaTypeOrNull()

    // Prevents multiple parallel requests from each firing their own token refresh.
    private val refreshMutex = Mutex()

    fun isConfigured(): Boolean {
        val url = sessionManager.getSupabaseUrl()
        val key = sessionManager.getSupabaseAnonKey()
        return url.startsWith("https://") && !url.contains("your-project-id") && !url.contains("xyzcompany") && key.length > 20
    }

    /**
     * Admin login with email & password via Supabase Auth
     * Verifies that the user exists in `public.admins` and `is_active == true` using authenticated user UUID
     */
    suspend fun login(email: String, pass: String): Result<Admin> = withContext(Dispatchers.IO) {
        val url = sessionManager.getSupabaseUrl().trim().removeSuffix("/")
        val key = sessionManager.getSupabaseAnonKey().trim()

        if (!url.startsWith("https://") || url.contains("your-project-id") || url.contains("xyzcompany")) {
            return@withContext Result.failure(
                Exception("Supabase URL কনফিগার করা হয়নি বা অবৈধ ($url)। সেটিংস থেকে সঠিক Supabase URL দিন।")
            )
        }
        if (key.isBlank() || key.contains("example_anon_key") || key.length < 10) {
            return@withContext Result.failure(
                Exception("Supabase Anon Key কনফিগার করা হয়নি। সেটিংস থেকে সঠিক Publishable/Anon Key দিন।")
            )
        }

        // Offline demo mode: only when Supabase itself is not properly configured, and only
        // with a fixed, explicit demo credential pair (never based on loosely matching the
        // typed email, which previously allowed ANY "admin@..." email + 6-char password to
        // sign in as a super_admin).
        if (!isConfigured()) {
            if (email.trim().equals(DEMO_EMAIL, ignoreCase = true) && pass == DEMO_PASSWORD) {
                val demoAdmin = Admin(
                    id = "demo-admin-01",
                    authUserId = "auth-demo-01",
                    email = DEMO_EMAIL,
                    name = "টার্মিনাল অ্যাডমিন (ডেমো)",
                    role = "super_admin",
                    active = true,
                    createdAt = "2026-01-01T00:00:00Z"
                )
                sessionManager.saveSession("demo_access_token", "demo_refresh_token", demoAdmin, expiresInSeconds = 0L)
                return@withContext Result.success(demoAdmin)
            } else {
                return@withContext Result.failure(
                    Exception("Supabase কনফিগার করা নেই। ডেমো মোডে ঢুকতে ইমেইল '$DEMO_EMAIL' ও নির্দিষ্ট ডেমো পাসওয়ার্ড ব্যবহার করুন, অথবা Settings-এ সঠিক Supabase ক্রেডেনশিয়াল দিন।")
                )
            }
        }

        try {
            // Step 1: Call Supabase Auth email/password sign-in (GoTrue endpoint)
            val authUrl = "$url/auth/v1/token?grant_type=password"
            val authPayload = JSONObject().apply {
                put("email", email.trim())
                put("password", pass)
            }

            val authRequest = Request.Builder()
                .url(authUrl)
                .header("apikey", key)
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")
                .post(authPayload.toString().toRequestBody(jsonMediaType))
                .build()

            val authResponse = httpClient.newCall(authRequest).execute()
            val authResponseBody = authResponse.body?.string().orEmpty()

            if (!authResponse.isSuccessful) {
                val errorMsg = try {
                    val errJson = JSONObject(authResponseBody)
                    val desc = errJson.optString("error_description", "")
                    val msg = errJson.optString("msg", "")
                    val message = errJson.optString("message", "")
                    val code = errJson.optString("error", errJson.optString("code", ""))
                    when {
                        desc.isNotBlank() -> "$desc${if (code.isNotBlank()) " ($code)" else ""}"
                        msg.isNotBlank() -> "$msg${if (code.isNotBlank()) " ($code)" else ""}"
                        message.isNotBlank() -> "$message${if (code.isNotBlank()) " ($code)" else ""}"
                        else -> "ইমেইল বা পাসওয়ার্ড সঠিক নয় (HTTP ${authResponse.code})"
                    }
                } catch (_: Exception) {
                    "লগইন প্রমাণীকরণ ব্যর্থ হয়েছে (HTTP ${authResponse.code})"
                }
                return@withContext Result.failure(Exception(errorMsg))
            }

            // Step 2: Parse session tokens and authenticated user object
            val authJson = JSONObject(authResponseBody)
            val accessToken = authJson.optString("access_token")
            val refreshToken = authJson.optString("refresh_token")
            val expiresIn = authJson.optLong("expires_in", SessionManager.DEFAULT_TOKEN_TTL_SECONDS)
            val userObj = authJson.optJSONObject("user")
            val authUserId = userObj?.optString("id").orEmpty()

            if (authUserId.isBlank()) {
                return@withContext Result.failure(
                    Exception("Supabase Auth সেশন থেকে ব্যবহারকারীর UUID পাওয়া যায়নি (User UUID is empty)")
                )
            }
            if (accessToken.isBlank()) {
                return@withContext Result.failure(
                    Exception("Supabase Auth সেশন থেকে Access Token পাওয়া যায়নি")
                )
            }

            // Step 3: Query public.admins strictly using the authenticated user UUID
            // Equivalent to:
            // SELECT * FROM public.admins WHERE id = authenticatedUser.id AND is_active = true LIMIT 1;
            val adminCheckUrl = "$url/rest/v1/admins?id=eq.$authUserId&select=*&limit=1"
            val adminRequest = Request.Builder()
                .url(adminCheckUrl)
                .header("apikey", key)
                .header("Authorization", "Bearer $accessToken")
                .header("Accept", "application/json")
                .get()
                .build()

            val adminResponse = httpClient.newCall(adminRequest).execute()
            val adminResponseBody = adminResponse.body?.string().orEmpty()

            // Step 4: Validate HTTP response and provide detailed diagnostics
            if (!adminResponse.isSuccessful) {
                var postgrestCode = ""
                var postgrestMessage = ""
                var postgrestDetails = ""
                var postgrestHint = ""
                try {
                    val errObj = JSONObject(adminResponseBody)
                    postgrestCode = errObj.optString("code", "")
                    postgrestMessage = errObj.optString("message", errObj.optString("msg", ""))
                    postgrestDetails = errObj.optString("details", "")
                    postgrestHint = errObj.optString("hint", "")
                } catch (_: Exception) {
                    postgrestMessage = adminResponseBody.take(250)
                }

                val diagnosticMsg = when (adminResponse.code) {
                    401 -> "মেয়াদোত্তীর্ণ বা অবৈধ JWT সেশন (HTTP 401: $postgrestMessage)"
                    403 -> "RLS পারমিশন এরর: public.admins টেবিলে এক্সেস অনুমতি নেই (HTTP 403${if (postgrestCode.isNotBlank()) ", Code: $postgrestCode" else ""}${if (postgrestMessage.isNotBlank()) ", Msg: $postgrestMessage" else ""})"
                    404 -> "public.admins টেবিল বা এন্ডপয়েন্ট পাওয়া যায়নি (HTTP 404: $postgrestMessage)"
                    else -> "PostgREST ত্রুটি (HTTP ${adminResponse.code}${if (postgrestCode.isNotBlank()) ", Code: $postgrestCode" else ""}): $postgrestMessage${if (postgrestDetails.isNotBlank() && postgrestDetails != "null") " [Details: $postgrestDetails]" else ""}${if (postgrestHint.isNotBlank() && postgrestHint != "null") " [Hint: $postgrestHint]" else ""}"
                }
                return@withContext Result.failure(Exception(diagnosticMsg))
            }

            // Step 5: Verify the admin row exists
            val adminArray = JSONArray(adminResponseBody)
            if (adminArray.length() == 0) {
                val notFoundMsg = "অ্যাডমিন রেকর্ড পাওয়া যায়নি: এই ইউজার 'public.admins' টেবিলে নথিভুক্ত নেই বা RLS পলিসির কারণে দৃশ্যমান নয়"
                return@withContext Result.failure(Exception(notFoundMsg))
            }

            // Step 6: Parse the admin record and verify is_active = true
            val adminJson = adminArray.getJSONObject(0)
            val admin = Admin.fromJson(adminJson)

            val isActive = when {
                adminJson.has("is_active") -> adminJson.optBoolean("is_active", false)
                adminJson.has("active") -> adminJson.optBoolean("active", false)
                else -> admin.active
            }

            if (!isActive) {
                val inactiveMsg = "অ্যাডমিন অ্যাকাউন্টটি নিষ্ক্রিয় (is_active = false)। মূল অ্যাডমিনের সাথে যোগাযোগ করুন।"
                return@withContext Result.failure(Exception(inactiveMsg))
            }

            // Save authenticated session and return admin
            sessionManager.saveSession(accessToken, refreshToken, admin, expiresInSeconds = expiresIn)
            Result.success(admin)
        } catch (e: java.net.UnknownHostException) {
            Result.failure(Exception("নেটওয়ার্ক ত্রুটি: Supabase হোস্ট খুঁজে পাওয়া যায়নি (${e.message})। ইন্টারনেট সংযোগ এবং Supabase URL যাচাই করুন।"))
        } catch (e: java.net.SocketTimeoutException) {
            Result.failure(Exception("নেটওয়ার্ক টাইমআউট: Supabase সার্ভার থেকে সাড়া পেতে বিলম্ব হচ্ছে। ইন্টারনেট সংযোগ পরীক্ষা করুন।"))
        } catch (e: java.io.IOException) {
            Result.failure(Exception("নেটওয়ার্ক সংযোগ ত্রুটি (${e.javaClass.simpleName}): ${e.localizedMessage ?: "সংযোগ বিচ্ছিন্ন"}"))
        } catch (e: Exception) {
            Result.failure(Exception("লগইন যাচাই ত্রুটি: ${e.localizedMessage ?: e.javaClass.simpleName}"))
        }
    }

    /**
     * Attempts to exchange the stored refresh token for a new access token.
     * Returns true if the session was refreshed successfully.
     */
    private suspend fun refreshAccessToken(): Boolean = refreshMutex.withLock {
        val url = sessionManager.getSupabaseUrl().trim().removeSuffix("/")
        val key = sessionManager.getSupabaseAnonKey().trim()
        val refreshToken = sessionManager.getRefreshToken()

        if (refreshToken.isBlank() || refreshToken == "demo_refresh_token") return@withLock false

        return@withLock try {
            val refreshUrl = "$url/auth/v1/token?grant_type=refresh_token"
            val payload = JSONObject().apply { put("refresh_token", refreshToken) }
            val request = Request.Builder()
                .url(refreshUrl)
                .header("apikey", key)
                .header("Content-Type", "application/json")
                .post(payload.toString().toRequestBody(jsonMediaType))
                .build()

            val response = httpClient.newCall(request).execute()
            val body = response.body?.string().orEmpty()
            if (!response.isSuccessful) return@withLock false

            val json = JSONObject(body)
            val newAccessToken = json.optString("access_token")
            val newRefreshToken = json.optString("refresh_token", refreshToken)
            val expiresIn = json.optLong("expires_in", SessionManager.DEFAULT_TOKEN_TTL_SECONDS)
            if (newAccessToken.isBlank()) return@withLock false

            sessionManager.updateTokens(newAccessToken, newRefreshToken, expiresIn)
            true
        } catch (_: Exception) {
            false
        }
    }

    /** Ensures we have a live access token before making an authenticated request. */
    private suspend fun ensureFreshToken() {
        if (sessionManager.isAccessTokenExpired()) {
            refreshAccessToken()
        }
    }

    suspend fun get(table: String, query: String = "select=*"): Result<JSONArray> = withContext(Dispatchers.IO) {
        val url = sessionManager.getSupabaseUrl().trim().removeSuffix("/")
        val key = sessionManager.getSupabaseAnonKey()

        if (!isConfigured()) {
            // Genuinely unconfigured (no Supabase project set up yet): serve the local demo dataset.
            return@withContext Result.success(MockDataStore.getTable(table))
        }

        ensureFreshToken()

        suspend fun attempt(): okhttp3.Response {
            val token = sessionManager.getAccessToken()
            val fullUrl = "$url/rest/v1/$table?$query"
            val request = Request.Builder()
                .url(fullUrl)
                .header("apikey", key)
                .apply {
                    if (token.isNotBlank()) header("Authorization", "Bearer $token")
                }
                .get()
                .build()
            return httpClient.newCall(request).execute()
        }

        try {
            var response = attempt()
            if (response.code == 401 && refreshAccessToken()) {
                response = attempt()
            }
            val body = response.body?.string().orEmpty()

            if (response.isSuccessful) {
                Result.success(JSONArray(body))
            } else {
                // Real, honest error — we deliberately do NOT fall back to mock data here.
                // Silently showing fake data when Supabase is actually configured would hide
                // real connectivity/permission problems from the admin.
                Result.failure(IOException("PostgREST Error: ${response.code} - ${body.take(300)}"))
            }
        } catch (e: Exception) {
            Result.failure(Exception("নেটওয়ার্ক ত্রুটি: ডেটা লোড করা যায়নি (${e.localizedMessage ?: e.javaClass.simpleName})"))
        }
    }

    suspend fun insert(table: String, json: JSONObject): Result<JSONObject> = withContext(Dispatchers.IO) {
        val url = sessionManager.getSupabaseUrl().trim().removeSuffix("/")
        val key = sessionManager.getSupabaseAnonKey()

        if (!isConfigured()) {
            val inserted = MockDataStore.insert(table, json)
            return@withContext Result.success(inserted)
        }

        ensureFreshToken()

        suspend fun attempt(): okhttp3.Response {
            val token = sessionManager.getAccessToken()
            val fullUrl = "$url/rest/v1/$table"
            val request = Request.Builder()
                .url(fullUrl)
                .header("apikey", key)
                .apply {
                    if (token.isNotBlank()) header("Authorization", "Bearer $token")
                }
                .header("Prefer", "return=representation")
                .header("Content-Type", "application/json")
                .post(json.toString().toRequestBody(jsonMediaType))
                .build()
            return httpClient.newCall(request).execute()
        }

        try {
            var response = attempt()
            if (response.code == 401 && refreshAccessToken()) {
                response = attempt()
            }
            val body = response.body?.string().orEmpty()

            if (response.isSuccessful) {
                val array = JSONArray(body)
                if (array.length() > 0) {
                    Result.success(array.getJSONObject(0))
                } else {
                    Result.success(json)
                }
            } else {
                Result.failure(IOException("ডাটা যোগ করতে ব্যর্থ: ${response.code} - ${body.take(300)}"))
            }
        } catch (e: Exception) {
            Result.failure(Exception("নেটওয়ার্ক ত্রুটি: ডাটা যোগ করা যায়নি (${e.localizedMessage ?: e.javaClass.simpleName})"))
        }
    }

    suspend fun update(table: String, id: String, json: JSONObject): Result<JSONObject> = withContext(Dispatchers.IO) {
        val url = sessionManager.getSupabaseUrl().trim().removeSuffix("/")
        val key = sessionManager.getSupabaseAnonKey()

        if (!isConfigured()) {
            val updated = MockDataStore.update(table, id, json)
            return@withContext Result.success(updated)
        }

        ensureFreshToken()

        suspend fun attempt(): okhttp3.Response {
            val token = sessionManager.getAccessToken()
            val fullUrl = "$url/rest/v1/$table?id=eq.$id"
            val request = Request.Builder()
                .url(fullUrl)
                .header("apikey", key)
                .apply {
                    if (token.isNotBlank()) header("Authorization", "Bearer $token")
                }
                .header("Prefer", "return=representation")
                .header("Content-Type", "application/json")
                .patch(json.toString().toRequestBody(jsonMediaType))
                .build()
            return httpClient.newCall(request).execute()
        }

        try {
            var response = attempt()
            if (response.code == 401 && refreshAccessToken()) {
                response = attempt()
            }
            val body = response.body?.string().orEmpty()

            if (response.isSuccessful) {
                val array = JSONArray(body)
                if (array.length() > 0) {
                    Result.success(array.getJSONObject(0))
                } else {
                    Result.success(json)
                }
            } else {
                Result.failure(IOException("আপডেট ব্যর্থ: ${response.code} - ${body.take(300)}"))
            }
        } catch (e: Exception) {
            Result.failure(Exception("নেটওয়ার্ক ত্রুটি: আপডেট করা যায়নি (${e.localizedMessage ?: e.javaClass.simpleName})"))
        }
    }

    suspend fun delete(table: String, id: String): Result<Boolean> = withContext(Dispatchers.IO) {
        val url = sessionManager.getSupabaseUrl().trim().removeSuffix("/")
        val key = sessionManager.getSupabaseAnonKey()

        if (!isConfigured()) {
            MockDataStore.delete(table, id)
            return@withContext Result.success(true)
        }

        ensureFreshToken()

        suspend fun attempt(): okhttp3.Response {
            val token = sessionManager.getAccessToken()
            val fullUrl = "$url/rest/v1/$table?id=eq.$id"
            val request = Request.Builder()
                .url(fullUrl)
                .header("apikey", key)
                .apply {
                    if (token.isNotBlank()) header("Authorization", "Bearer $token")
                }
                .delete()
                .build()
            return httpClient.newCall(request).execute()
        }

        try {
            var response = attempt()
            if (response.code == 401 && refreshAccessToken()) {
                response = attempt()
            }
            if (response.isSuccessful) {
                Result.success(true)
            } else {
                Result.failure(IOException("মুছে ফেলতে ব্যর্থ: ${response.code}"))
            }
        } catch (e: Exception) {
            Result.failure(Exception("নেটওয়ার্ক ত্রুটি: মুছে ফেলা যায়নি (${e.localizedMessage ?: e.javaClass.simpleName})"))
        }
    }

    /**
     * Connection health check with three distinct outcomes rather than collapsing
     * "connected" and "connected but blocked by RLS" into one generic success message.
     */
    suspend fun testConnection(): Result<String> = withContext(Dispatchers.IO) {
        val url = sessionManager.getSupabaseUrl().trim().removeSuffix("/")
        val key = sessionManager.getSupabaseAnonKey()
        val token = sessionManager.getAccessToken()

        try {
            val testUrl = "$url/rest/v1/districts?select=id&limit=1"
            val request = Request.Builder()
                .url(testUrl)
                .header("apikey", key)
                .apply {
                    if (token.isNotBlank()) header("Authorization", "Bearer $token")
                }
                .get()
                .build()

            val response = httpClient.newCall(request).execute()
            val code = response.code
            val body = response.body?.string().orEmpty()
            when {
                response.isSuccessful ->
                    Result.success("✅ সফলভাবে Supabase ডেটাবেজের সাথে সংযোগ স্থাপন করা হয়েছে এবং ডেটা পড়া যাচ্ছে।")
                code == 403 || body.contains("42501") || body.contains("permission denied") ->
                    Result.success("⚠️ Supabase সার্ভারের সাথে সংযোগ হয়েছে, কিন্তু Row Level Security (RLS) পলিসি এই টেবিলে এক্সেস আটকে দিচ্ছে। RLS পলিসি যাচাই করুন।")
                code == 401 ->
                    Result.failure(Exception("সংযোগ ব্যর্থ: অবৈধ বা মেয়াদোত্তীর্ণ API Key/সেশন (HTTP 401)"))
                else ->
                    Result.failure(Exception("সার্ভার রেসপন্স কোড: $code (${body.take(120)})"))
            }
        } catch (e: Exception) {
            Result.failure(Exception("সংযোগ ত্রুটি: ${e.localizedMessage}"))
        }
    }

    companion object {
        const val DEMO_EMAIL = "demo@busterminalbd.com"
        const val DEMO_PASSWORD = "Demo@12345"
    }
}
