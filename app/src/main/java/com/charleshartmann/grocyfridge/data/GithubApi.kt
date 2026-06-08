package com.charleshartmann.grocyfridge.data

import android.util.Log
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import java.util.concurrent.TimeUnit

interface GithubApi {
    @POST("repos/{owner}/{repo}/issues")
    suspend fun createIssue(
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Body request: CreateIssueRequest
    ): GithubIssue

    @GET("repos/{owner}/{repo}/issues/{number}")
    suspend fun getIssue(
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Path("number") number: Int
    ): GithubIssue

    @GET("repos/{owner}/{repo}/issues/{number}/comments")
    suspend fun getComments(
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Path("number") number: Int
    ): List<GithubComment>

    @POST("repos/{owner}/{repo}/issues/{number}/comments")
    suspend fun postComment(
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Path("number") number: Int,
        @Body request: PostCommentRequest
    ): GithubComment

    @PUT("repos/{owner}/{repo}/contents/feedback-assets/{filename}")
    suspend fun uploadAsset(
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Path("filename") filename: String,
        @Body request: UploadAssetRequest
    ): UploadAssetResponse
}

object GithubClient {
    private val json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
        coerceInputValues = true
    }

    fun create(token: String): GithubApi {
        val logger = HttpLoggingInterceptor { message ->
            Log.d("GitHubHttp", message)
        }.apply { level = HttpLoggingInterceptor.Level.BODY }

        val client = OkHttpClient.Builder()
            .connectTimeout(20, TimeUnit.SECONDS)
            .readTimeout(45, TimeUnit.SECONDS)
            .addInterceptor { chain ->
                val request = chain.request().newBuilder()
                    .header("Authorization", "Bearer $token")
                    .header("Accept", "application/vnd.github+json")
                    .header("X-GitHub-Api-Version", "2022-11-28")
                    .header("User-Agent", "Grocy Fridge Scanner-Android/0.1")
                    .build()
                chain.proceed(request)
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
            .baseUrl("https://api.github.com/")
            .client(client)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(GithubApi::class.java)
    }
}
