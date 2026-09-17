package com.example.data.remote.api

import android.content.Context
import com.example.BuildConfig
import com.example.data.local.SessionManager
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Response
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.concurrent.TimeUnit

class AuthInterceptor(private val sessionManager: SessionManager) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val original = chain.request()
        val token = runBlocking { sessionManager.getAccessToken() }

        val request = if (!token.isNullOrBlank()) {
            original.newBuilder()
                .header("Authorization", "Bearer $token")
                .header("Content-Type", "application/json")
                .build()
        } else {
            original.newBuilder()
                .header("Content-Type", "application/json")
                .build()
        }

        return chain.proceed(request)
    }
}

object ApiClient {
    private var apiInstance: GroupTrackApi? = null
    private var currentBaseUrl: String = getInitialBaseUrl()

    private fun getInitialBaseUrl(): String {
        return try {
            val url = BuildConfig.BACKEND_BASE_URL
            if (url.isNotBlank()) {
                if (url.endsWith("/")) url else "$url/"
            } else {
                "http://10.0.2.2:3000/"
            }
        } catch (e: Exception) {
            "http://10.0.2.2:3000/"
        }
    }

    fun getApi(context: Context, sessionManager: SessionManager): GroupTrackApi {
        return apiInstance ?: synchronized(this) {
            apiInstance ?: createApi(context, sessionManager, currentBaseUrl).also { apiInstance = it }
        }
    }

    fun updateBaseUrl(newUrl: String, context: Context, sessionManager: SessionManager) {
        val sanitized = if (newUrl.endsWith("/")) newUrl else "$newUrl/"
        currentBaseUrl = sanitized
        apiInstance = createApi(context, sessionManager, sanitized)
    }

    fun getBaseUrl(): String = currentBaseUrl

    fun isNetworkAvailable(context: Context): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? android.net.ConnectivityManager
            ?: return false
        val activeNetwork = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(activeNetwork) ?: return false
        return capabilities.hasCapability(android.net.NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    private fun createApi(context: Context, sessionManager: SessionManager, baseUrl: String): GroupTrackApi {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }

        val okHttpClient = OkHttpClient.Builder()
            .addInterceptor(AuthInterceptor(sessionManager))
            .addInterceptor(logging)
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .writeTimeout(15, TimeUnit.SECONDS)
            .build()

        val moshi = Moshi.Builder()
            .add(KotlinJsonAdapterFactory())
            .build()

        return Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(GroupTrackApi::class.java)
    }
}
