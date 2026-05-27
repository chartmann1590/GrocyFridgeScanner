package com.charleshartmann.grocyfridge.data

import android.util.Log
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import java.util.concurrent.TimeUnit

object GrocyClientFactory {
    @OptIn(kotlinx.serialization.ExperimentalSerializationApi::class)
    private val json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
        coerceInputValues = true
    }

    fun create(baseUrl: String, apiKey: String): GrocyApi {
        val normalizedUrl = baseUrl.trim().trimEnd('/') + "/"
        val logger = HttpLoggingInterceptor { message ->
            Log.d("GrocyHttp", message)
        }.apply { level = HttpLoggingInterceptor.Level.BODY }

        val client = OkHttpClient.Builder()
            .connectTimeout(20, TimeUnit.SECONDS)
            .readTimeout(45, TimeUnit.SECONDS)
            .addInterceptor { chain ->
                chain.proceed(
                    chain.request().newBuilder()
                        .header("GROCY-API-KEY", apiKey)
                        .build()
                )
            }
            .addNetworkInterceptor { chain ->
                val request = chain.request()
                if (request.body != null) {
                    chain.proceed(
                        request.newBuilder()
                            .header("Content-Type", "application/json")
                            .build()
                    )
                } else {
                    chain.proceed(request)
                }
            }
            .addInterceptor(logger)
            .build()

        return Retrofit.Builder()
            .baseUrl(normalizedUrl)
            .client(client)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(GrocyApi::class.java)
    }
}
