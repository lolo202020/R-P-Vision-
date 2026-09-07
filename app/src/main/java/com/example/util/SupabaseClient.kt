package com.example.util

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/**
 * Singleton client for managing Supabase connection,
 * providing direct access to PostgreSQL (PostgREST), Authentication, and Storage.
 */
class SupabaseClient(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("supabase_sync_prefs", Context.MODE_PRIVATE)

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .writeTimeout(20, TimeUnit.SECONDS)
        .build()

    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    var supabaseUrl: String
        get() {
            val saved = prefs.getString("supabase_url", "") ?: ""
            if (saved.isNotBlank()) return saved
            return getEnvBuildConfig("SUPABASE_URL")
        }
        set(value) {
            prefs.edit().putString("supabase_url", value.trim().removeSuffix("/")).apply()
        }

    var supabaseKey: String
        get() {
            val saved = prefs.getString("supabase_key", "") ?: ""
            if (saved.isNotBlank()) return saved
            return getEnvBuildConfig("SUPABASE_KEY")
        }
        set(value) {
            prefs.edit().putString("supabase_key", value.trim()).apply()
        }

    private fun getEnvBuildConfig(fieldName: String): String {
        return try {
            val buildConfigClass = Class.forName("com.example.BuildConfig")
            val field = buildConfigClass.getField(fieldName)
            (field.get(null) as? String ?: "").trim().removeSuffix("/")
        } catch (e: Exception) {
            ""
        }
    }

    fun isConfigured(): Boolean = supabaseUrl.isNotBlank() && supabaseKey.isNotBlank()

    fun updateCredentials(url: String, key: String) {
        supabaseUrl = url
        supabaseKey = key
    }

    // =========================================================================
    // 1. POSTGRESQL DATABASE REST API (PostgREST)
    // =========================================================================

    suspend fun queryDatabase(
        table: String,
        select: String = "*",
        filterQuery: String? = null
    ): Result<JSONArray> = withContext(Dispatchers.IO) {
        if (!isConfigured()) return@withContext Result.failure(IllegalStateException("Supabase not configured"))

        try {
            var url = "$supabaseUrl/rest/v1/$table?select=$select"
            if (!filterQuery.isNullOrBlank()) {
                url += "&$filterQuery"
            }

            val request = Request.Builder()
                .url(url)
                .addHeader("apikey", supabaseKey)
                .addHeader("Authorization", "Bearer $supabaseKey")
                .get()
                .build()

            val response = client.newCall(request).execute()
            val bodyString = response.body?.string() ?: "[]"

            if (response.isSuccessful) {
                Result.success(JSONArray(bodyString))
            } else {
                Result.failure(Exception("PostgreSQL query failed [${response.code}]: $bodyString"))
            }
        } catch (e: Exception) {
            Log.e("SupabaseClient", "Database query error: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun insertDatabase(
        table: String,
        jsonData: JSONObject
    ): Result<String> = withContext(Dispatchers.IO) {
        if (!isConfigured()) return@withContext Result.failure(IllegalStateException("Supabase not configured"))

        try {
            val request = Request.Builder()
                .url("$supabaseUrl/rest/v1/$table")
                .addHeader("apikey", supabaseKey)
                .addHeader("Authorization", "Bearer $supabaseKey")
                .addHeader("Prefer", "resolution=merge-duplicates,return=representation")
                .post(jsonData.toString().toRequestBody(jsonMediaType))
                .build()

            val response = client.newCall(request).execute()
            val bodyString = response.body?.string() ?: ""

            if (response.isSuccessful || response.code == 201) {
                Result.success(bodyString)
            } else {
                Result.failure(Exception("PostgreSQL insert failed [${response.code}]: $bodyString"))
            }
        } catch (e: Exception) {
            Log.e("SupabaseClient", "Database insert error: ${e.message}", e)
            Result.failure(e)
        }
    }

    // =========================================================================
    // 2. AUTHENTICATION API (GoTrue)
    // =========================================================================

    suspend fun signUpWithEmail(email: String, pass: String): Result<JSONObject> = withContext(Dispatchers.IO) {
        if (!isConfigured()) return@withContext Result.failure(IllegalStateException("Supabase not configured"))

        try {
            val payload = JSONObject().apply {
                put("email", email)
                put("password", pass)
            }

            val request = Request.Builder()
                .url("$supabaseUrl/auth/v1/signup")
                .addHeader("apikey", supabaseKey)
                .post(payload.toString().toRequestBody(jsonMediaType))
                .build()

            val response = client.newCall(request).execute()
            val bodyString = response.body?.string() ?: "{}"

            if (response.isSuccessful) {
                Result.success(JSONObject(bodyString))
            } else {
                Result.failure(Exception("Auth signup failed [${response.code}]: $bodyString"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun signInWithEmail(email: String, pass: String): Result<JSONObject> = withContext(Dispatchers.IO) {
        if (!isConfigured()) return@withContext Result.failure(IllegalStateException("Supabase not configured"))

        try {
            val payload = JSONObject().apply {
                put("email", email)
                put("password", pass)
            }

            val request = Request.Builder()
                .url("$supabaseUrl/auth/v1/token?grant_type=password")
                .addHeader("apikey", supabaseKey)
                .post(payload.toString().toRequestBody(jsonMediaType))
                .build()

            val response = client.newCall(request).execute()
            val bodyString = response.body?.string() ?: "{}"

            if (response.isSuccessful) {
                Result.success(JSONObject(bodyString))
            } else {
                Result.failure(Exception("Auth signin failed [${response.code}]: $bodyString"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // =========================================================================
    // 3. STORAGE API
    // =========================================================================

    suspend fun uploadFile(
        bucketName: String,
        path: String,
        bytes: ByteArray,
        mimeType: String = "image/jpeg"
    ): Result<String> = withContext(Dispatchers.IO) {
        if (!isConfigured()) return@withContext Result.failure(IllegalStateException("Supabase not configured"))

        try {
            val uploadUrl = "$supabaseUrl/storage/v1/object/$bucketName/$path"
            val requestBody = bytes.toRequestBody(mimeType.toMediaType())

            val request = Request.Builder()
                .url(uploadUrl)
                .addHeader("apikey", supabaseKey)
                .addHeader("Authorization", "Bearer $supabaseKey")
                .addHeader("x-upsert", "true")
                .post(requestBody)
                .build()

            val response = client.newCall(request).execute()
            val bodyString = response.body?.string() ?: ""

            if (response.isSuccessful) {
                val publicUrl = "$supabaseUrl/storage/v1/object/public/$bucketName/$path"
                Result.success(publicUrl)
            } else {
                Result.failure(Exception("Storage upload failed [${response.code}]: $bodyString"))
            }
        } catch (e: Exception) {
            Log.e("SupabaseClient", "Storage upload error: ${e.message}", e)
            Result.failure(e)
        }
    }

    fun getPublicUrl(bucketName: String, path: String): String {
        return "$supabaseUrl/storage/v1/object/public/$bucketName/$path"
    }

    companion object {
        @Volatile
        private var instance: SupabaseClient? = null

        fun getInstance(context: Context): SupabaseClient {
            return instance ?: synchronized(this) {
                instance ?: SupabaseClient(context.applicationContext).also { instance = it }
            }
        }
    }
}
