package com.charleshartmann.grocyfridge.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class BugReport(
    val number: Int,
    val title: String,
    val status: String,
    @SerialName("created_at") val createdAt: String,
    @SerialName("html_url") val htmlUrl: String
)

@Serializable
data class CreateIssueRequest(
    val title: String,
    val body: String
)

@Serializable
data class GithubIssue(
    val number: Int,
    val title: String,
    val state: String,
    @SerialName("html_url") val htmlUrl: String,
    @SerialName("created_at") val createdAt: String,
    val body: String? = null
)

@Serializable
data class GithubUser(
    val login: String
)

@Serializable
data class GithubComment(
    val id: Long,
    val body: String,
    @SerialName("created_at") val createdAt: String,
    val user: GithubUser
)

@Serializable
data class PostCommentRequest(
    val body: String
)

@Serializable
data class UploadAssetRequest(
    val filename: String,
    val contentBase64: String
)

@Serializable
data class UploadAssetContent(
    @SerialName("download_url") val downloadUrl: String
)

@Serializable
data class UploadAssetResponse(
    val content: UploadAssetContent
)
