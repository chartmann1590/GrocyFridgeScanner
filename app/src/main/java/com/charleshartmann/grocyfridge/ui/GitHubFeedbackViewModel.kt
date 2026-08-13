package com.charleshartmann.grocyfridge.ui

import android.app.Application
import android.net.Uri
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.charleshartmann.grocyfridge.BuildConfig
import com.charleshartmann.grocyfridge.ai.ModelManager
import com.charleshartmann.grocyfridge.data.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class GitHubFeedbackViewModel(application: Application) : AndroidViewModel(application) {
    private val bugReportRepo = BugReportRepo(application)
    private val modelManager = ModelManager(application)
    private val settingsStore = SettingsStore(application)
    
    // Talks to the cloudflare-worker/ feedback relay — see GithubClient.create.
    private val githubApi: GithubApi = GithubClient.create()

    private var customServerUrl: String? = null

    val reports: StateFlow<List<BugReport>> = bugReportRepo.reports.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = emptyList()
    )

    var selectedScreenshotUri = MutableStateFlow<Uri?>(null)
        private set

    private val _isSubmitting = MutableStateFlow(false)
    val isSubmitting: StateFlow<Boolean> = _isSubmitting.asStateFlow()

    private val _submissionError = MutableStateFlow<String?>(null)
    val submissionError: StateFlow<String?> = _submissionError.asStateFlow()

    private val _submissionSuccess = MutableStateFlow(false)
    val submissionSuccess: StateFlow<Boolean> = _submissionSuccess.asStateFlow()

    private val _activeIssue = MutableStateFlow<GithubIssue?>(null)
    val activeIssue: StateFlow<GithubIssue?> = _activeIssue.asStateFlow()

    private val _comments = MutableStateFlow<List<GithubComment>>(emptyList())
    val comments: StateFlow<List<GithubComment>> = _comments.asStateFlow()

    private val _isLoadingComments = MutableStateFlow(false)
    val isLoadingComments: StateFlow<Boolean> = _isLoadingComments.asStateFlow()

    private val _commentsError = MutableStateFlow<String?>(null)
    val commentsError: StateFlow<String?> = _commentsError.asStateFlow()

    private val _isPostingComment = MutableStateFlow(false)
    val isPostingComment: StateFlow<Boolean> = _isPostingComment.asStateFlow()

    var commentScreenshotUri = MutableStateFlow<Uri?>(null)
        private set

    init {
        viewModelScope.launch {
            settingsStore.settings.collect { settings ->
                customServerUrl = settings.grocyUrl
            }
        }
        syncIssueStatuses()
    }

    fun selectScreenshot(uri: Uri?) {
        selectedScreenshotUri.value = uri
    }

    fun selectCommentScreenshot(uri: Uri?) {
        commentScreenshotUri.value = uri
    }

    fun clearSubmissionState() {
        _isSubmitting.value = false
        _submissionError.value = null
        _submissionSuccess.value = false
        selectedScreenshotUri.value = null
    }

    fun syncIssueStatuses() {
        viewModelScope.launch {
            try {
                val currentList = reports.value
                val updatedList = currentList.map { report ->
                    try {
                        val remote = githubApi.getIssue(report.number)
                        report.copy(status = remote.state)
                    } catch (e: Exception) {
                        report
                    }
                }
                bugReportRepo.updateBugReports(updatedList)
            } catch (e: Exception) {
                Log.e("GitHubFeedbackVM", "Failed to sync issue statuses", e)
            }
        }
    }

    fun submitReport(
        title: String,
        description: String,
        name: String,
        email: String,
        includeDiagnostics: Boolean
    ) {
        viewModelScope.launch {
            _isSubmitting.value = true
            _submissionError.value = null
            try {
                var finalBody = description
                if (name.isNotBlank() || email.isNotBlank()) {
                    finalBody += "\n\n---\n**Reporter Info:**"
                    if (name.isNotBlank()) finalBody += "\n- **Name:** $name"
                    if (email.isNotBlank()) finalBody += "\n- **Email:** $email"
                }

                if (includeDiagnostics) {
                    val isModelReady = modelManager.isModelReady()
                    val diagnostics = DiagnosticsHelper.gatherDiagnostics(
                        context = getApplication(),
                        modelName = BuildConfig.MODEL_FILE,
                        isModelOnDevice = true,
                        isModelReady = isModelReady,
                        customServerUrl = customServerUrl
                    )
                    finalBody += "\n\n$diagnostics"
                }

                val uri = selectedScreenshotUri.value
                if (uri != null) {
                    val base64 = DiagnosticsHelper.uriToBase64(getApplication(), uri)
                    if (base64 != null) {
                        val filename = "feedback_${System.currentTimeMillis()}.jpg"
                        val uploadResponse = githubApi.uploadAsset(
                            UploadAssetRequest(filename = filename, contentBase64 = base64)
                        )
                        val imageUrl = uploadResponse.content.downloadUrl
                        finalBody += "\n\n![Screenshot]($imageUrl)"
                    } else {
                        Log.w("GitHubFeedbackVM", "Could not convert URI to base64: $uri")
                    }
                }

                val issueResponse = githubApi.createIssue(CreateIssueRequest(title, finalBody))

                val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
                val newReport = BugReport(
                    number = issueResponse.number,
                    title = issueResponse.title,
                    status = issueResponse.state,
                    createdAt = dateFormat.format(Date()),
                    htmlUrl = issueResponse.htmlUrl
                )
                bugReportRepo.saveBugReport(newReport)
                _submissionSuccess.value = true
                selectedScreenshotUri.value = null
            } catch (e: Exception) {
                Log.e("GitHubFeedbackVM", "Failed to submit bug report", e)
                _submissionError.value = e.message ?: "Failed to submit feedback."
            } finally {
                _isSubmitting.value = false
            }
        }
    }

    fun openIssueThread(issueNumber: Int) {
        viewModelScope.launch {
            _isLoadingComments.value = true
            _commentsError.value = null
            _activeIssue.value = null
            _comments.value = emptyList()
            try {
                val issue = githubApi.getIssue(issueNumber)
                _activeIssue.value = issue
                val commentsList = githubApi.getComments(issueNumber)
                _comments.value = commentsList
            } catch (e: Exception) {
                Log.e("GitHubFeedbackVM", "Failed to load comments", e)
                _commentsError.value = e.message ?: "Failed to load discussion."
            } finally {
                _isLoadingComments.value = false
            }
        }
    }

    fun postComment(body: String) {
        val active = _activeIssue.value ?: return
        viewModelScope.launch {
            _isPostingComment.value = true
            try {
                var finalBody = "**[User Reply from App]**\n\n$body"

                val uri = commentScreenshotUri.value
                if (uri != null) {
                    val base64 = DiagnosticsHelper.uriToBase64(getApplication(), uri)
                    if (base64 != null) {
                        val filename = "feedback_${System.currentTimeMillis()}.jpg"
                        val uploadResponse = githubApi.uploadAsset(
                            UploadAssetRequest(filename = filename, contentBase64 = base64)
                        )
                        val imageUrl = uploadResponse.content.downloadUrl
                        finalBody += "\n\n![Screenshot]($imageUrl)"
                    }
                }

                githubApi.postComment(active.number, PostCommentRequest(finalBody))
                commentScreenshotUri.value = null
                val commentsList = githubApi.getComments(active.number)
                _comments.value = commentsList
            } catch (e: Exception) {
                Log.e("GitHubFeedbackVM", "Failed to post comment", e)
            } finally {
                _isPostingComment.value = false
            }
        }
    }
}
