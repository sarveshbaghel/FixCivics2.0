package com.civicfix.app.data.api

import android.os.Build
import android.util.Log
import com.civicfix.app.BuildConfig
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.IOException
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.util.concurrent.TimeUnit

object RetrofitClient {

    private val isEmulator: Boolean
        get() = (Build.BRAND.startsWith("generic") && Build.DEVICE.startsWith("generic"))
                || Build.FINGERPRINT.startsWith("generic")
                || Build.FINGERPRINT.startsWith("unknown")
                || Build.HARDWARE.contains("goldfish")
                || Build.HARDWARE.contains("ranchu")
                || Build.MODEL.contains("google_sdk")
                || Build.MODEL.contains("Emulator")
                || Build.MODEL.contains("Android SDK built for x86")
                || Build.MANUFACTURER.contains("Genymotion")
                || Build.PRODUCT.contains("sdk_google")
                || Build.PRODUCT.contains("google_sdk")
                || Build.PRODUCT.contains("sdk")
                || Build.PRODUCT.contains("sdk_x86")
                || Build.PRODUCT.contains("sdk_gphone64_arm64")
                || Build.PRODUCT.contains("vbox86p")
                || Build.PRODUCT.contains("emulator")
                || Build.PRODUCT.contains("simulator")

    // Dynamically select URL based on environment
    val BASE_URL = if (isEmulator) {
        BuildConfig.EMULATOR_API_BASE_URL
    } else {
        BuildConfig.DEVICE_API_BASE_URL
    } + "/"


    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    private val connectivityInterceptor = Interceptor { chain ->
        val request = chain.request()
        var exception: Exception? = null
        var response: okhttp3.Response? = null
        var tryCount = 0
        val maxRetries = 3

        while (tryCount < maxRetries && (response == null || !response.isSuccessful)) {
            try {
                response?.close() // Ensure previous response is closed before retrying
                response = chain.proceed(request)
                if (response.isSuccessful) {
                    return@Interceptor response
                }
            } catch (e: Exception) {
                exception = e
            }
            
            tryCount++
            if (tryCount < maxRetries) {
                Log.w("RetrofitClient", "Network request failed. Retrying $tryCount/$maxRetries...")
                try { Thread.sleep(1000) } catch (e: InterruptedException) { }
            }
        }

        if (response != null) {
            return@Interceptor response // Return the response even if unsucessful HTTP code, let Retrofit handle 400s/500s
        }

        // If all retries exhausted with no response (network exception), throw detailed IOException
        when (exception) {
            is SocketTimeoutException -> {
                Log.e("RetrofitClient", "Connection timeout to server on $BASE_URL", exception)
                throw IOException("Backend server is not reachable (Timeout). Expected at: $BASE_URL")
            }
            is ConnectException -> {
                Log.e("RetrofitClient", "Failed to connect to server on $BASE_URL", exception)
                throw IOException("Backend server is not reachable. Expected at: $BASE_URL")
            }
            else -> {
                Log.e("RetrofitClient", "Network error: ${exception?.message}", exception)
                throw exception ?: IOException("Unknown network error occurred")
            }
        }
    }

    private val httpClient = OkHttpClient.Builder()
        .addInterceptor(loggingInterceptor)
        .addInterceptor(connectivityInterceptor)
        .connectTimeout(15, TimeUnit.SECONDS) // reduced timeout so it fails faster and retries
        .readTimeout(15, TimeUnit.SECONDS)
        .writeTimeout(15, TimeUnit.SECONDS)
        .build()

    private val retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .client(httpClient)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    val api: CivicFixApi = retrofit.create(CivicFixApi::class.java)
}
