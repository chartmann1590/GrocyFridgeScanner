package com.charleshartmann.grocyfridge.ui.components

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.charleshartmann.grocyfridge.ui.GitHubFeedbackViewModel
import com.charleshartmann.grocyfridge.data.BugReport
import java.text.SimpleDateFormat
import java.util.Locale

@Composable
fun ReportFormDialog(
    viewModel: GitHubFeedbackViewModel,
    onDismiss: () -> Unit
) {
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var includeDiagnostics by remember { mutableStateOf(true) }

    val isSubmitting by viewModel.isSubmitting.collectAsState()
    val submissionError by viewModel.submissionError.collectAsState()
    val submissionSuccess by viewModel.submissionSuccess.collectAsState()
    val selectedScreenshot by viewModel.selectedScreenshotUri.collectAsState()

    val pickMedia = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            viewModel.selectScreenshot(uri)
        }
    }

    LaunchedEffect(submissionSuccess) {
        if (submissionSuccess) {
            onDismiss()
            viewModel.clearSubmissionState()
        }
    }

    Dialog(
        onDismissRequest = { if (!isSubmitting) onDismiss() },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
            ),
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .fillMaxHeight(0.85f)
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        Icons.Filled.BugReport,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(Modifier.width(10.dp))
                    Text(
                        "Report a Problem",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.weight(1f))
                    IconButton(
                        onClick = onDismiss,
                        enabled = !isSubmitting
                    ) {
                        Icon(Icons.Filled.Close, contentDescription = "Close")
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f)
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Filled.Warning,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(Modifier.width(10.dp))
                            Text(
                                "Warning: Anything submitted here will be publicly visible on the GitHub repository issues list.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }

                    if (submissionError != null) {
                        Text(
                            submissionError ?: "",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text("Title / Subject *") },
                        placeholder = { Text("Describe the bug briefly") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )

                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        label = { Text("Description *") },
                        placeholder = { Text("What happened, and how can we reproduce it?") },
                        minLines = 4,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )

                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Your Name (Optional)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )

                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it },
                        label = { Text("Your Email (Optional)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { includeDiagnostics = !includeDiagnostics }
                            .padding(vertical = 4.dp)
                    ) {
                        Checkbox(
                            checked = includeDiagnostics,
                            onCheckedChange = { includeDiagnostics = it }
                        )
                        Spacer(Modifier.width(8.dp))
                        Column {
                            Text(
                                "Include system diagnostics & models info",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                "Adds brand/model, memory, locale & AI model statuses. No personal data or keys.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    if (selectedScreenshot != null) {
                        Box(
                            modifier = Modifier
                                .size(120.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(12.dp))
                        ) {
                            AsyncImage(
                                model = selectedScreenshot,
                                contentDescription = "Screenshot preview",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                            IconButton(
                                onClick = { viewModel.selectScreenshot(null) },
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(4.dp)
                                    .size(24.dp)
                                    .background(Color.Black.copy(alpha = 0.6f), CircleShape)
                            ) {
                                Icon(
                                    Icons.Filled.Close,
                                    contentDescription = "Remove screenshot",
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    } else {
                        OutlinedButton(
                            onClick = {
                                pickMedia.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                            },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Filled.PhotoCamera, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("Attach Screenshot")
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        enabled = !isSubmitting,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Cancel")
                    }

                    Button(
                        onClick = {
                            viewModel.submitReport(
                                title = title.trim(),
                                description = description.trim(),
                                name = name.trim(),
                                email = email.trim(),
                                includeDiagnostics = includeDiagnostics
                            )
                        },
                        enabled = title.isNotBlank() && description.isNotBlank() && !isSubmitting,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        if (isSubmitting) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        } else {
                            Text("Submit")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun IssueDetailDialog(
    viewModel: GitHubFeedbackViewModel,
    issueNumber: Int,
    onDismiss: () -> Unit
) {
    val activeIssue by viewModel.activeIssue.collectAsState()
    val comments by viewModel.comments.collectAsState()
    val isLoadingComments by viewModel.isLoadingComments.collectAsState()
    val commentsError by viewModel.commentsError.collectAsState()
    val isPostingComment by viewModel.isPostingComment.collectAsState()
    val commentScreenshot by viewModel.commentScreenshotUri.collectAsState()

    val uriHandler = LocalUriHandler.current
    var replyText by remember { mutableStateOf("") }

    val pickCommentMedia = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            viewModel.selectCommentScreenshot(uri)
        }
    }

    LaunchedEffect(issueNumber) {
        viewModel.openIssueThread(issueNumber)
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
            ),
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .fillMaxHeight(0.85f)
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            activeIssue?.title ?: "Bug Report #$issueNumber",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(Modifier.height(2.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            val state = activeIssue?.state ?: "open"
                            val isClosed = state.lowercase() == "closed"
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(
                                        if (isClosed) MaterialTheme.colorScheme.errorContainer
                                        else Color(0xFFE2F3E8)
                                    )
                                    .padding(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    state.replaceFirstChar { it.uppercase() },
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isClosed) MaterialTheme.colorScheme.onErrorContainer
                                    else Color(0xFF1B5E20)
                                )
                            }
                        }
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Filled.Close, contentDescription = "Close")
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp))

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) {
                    if (isLoadingComments) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator()
                        }
                    } else if (commentsError != null) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                commentsError ?: "Failed to load thread.",
                                color = MaterialTheme.colorScheme.error,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(16.dp)
                            )
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            activeIssue?.let { issue ->
                                item {
                                    CommentBubble(
                                        author = "Reporter",
                                        body = issue.body ?: "",
                                        createdAt = issue.createdAt,
                                        isUserReply = false
                                    )
                                }
                            }

                            items(comments) { comment ->
                                val isUserReply = comment.body.startsWith("**[User Reply from App]**")
                                val cleanedBody = if (isUserReply) {
                                    comment.body.removePrefix("**[User Reply from App]**").trim()
                                } else {
                                    comment.body
                                }
                                CommentBubble(
                                    author = comment.user.login,
                                    body = cleanedBody,
                                    createdAt = comment.createdAt,
                                    isUserReply = isUserReply
                                )
                            }
                        }
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp))

                if (commentScreenshot != null) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(50.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(8.dp))
                        ) {
                            AsyncImage(
                                model = commentScreenshot,
                                contentDescription = "Comment attachment preview",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                            IconButton(
                                onClick = { viewModel.selectCommentScreenshot(null) },
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(2.dp)
                                    .size(16.dp)
                                    .background(Color.Black.copy(alpha = 0.6f), CircleShape)
                            ) {
                                Icon(
                                    Icons.Filled.Close,
                                    contentDescription = "Remove screenshot",
                                    tint = Color.White,
                                    modifier = Modifier.size(10.dp)
                                )
                            }
                        }
                        Spacer(Modifier.width(10.dp))
                        Text(
                            "Image attached",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    IconButton(
                        onClick = {
                            pickCommentMedia.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                        },
                        enabled = !isPostingComment
                    ) {
                        Icon(Icons.Filled.AttachFile, contentDescription = "Attach Screenshot")
                    }

                    OutlinedTextField(
                        value = replyText,
                        onValueChange = { replyText = it },
                        placeholder = { Text("Write a reply...") },
                        singleLine = false,
                        maxLines = 3,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(20.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                        )
                    )

                    Spacer(Modifier.width(8.dp))

                    IconButton(
                        onClick = {
                            if (replyText.isNotBlank() || commentScreenshot != null) {
                                viewModel.postComment(replyText.trim())
                                replyText = ""
                            }
                        },
                        enabled = (replyText.isNotBlank() || commentScreenshot != null) && !isPostingComment,
                        modifier = Modifier
                            .background(
                                if (replyText.isNotBlank() || commentScreenshot != null) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.surfaceVariant,
                                CircleShape
                            )
                            .size(40.dp)
                    ) {
                        if (isPostingComment) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        } else {
                            Icon(
                                Icons.Filled.Send,
                                contentDescription = "Send",
                                tint = if (replyText.isNotBlank() || commentScreenshot != null) MaterialTheme.colorScheme.onPrimary
                                else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }

                Spacer(Modifier.height(12.dp))

                activeIssue?.let { issue ->
                    OutlinedButton(
                        onClick = { uriHandler.openUri(issue.htmlUrl) },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Filled.OpenInBrowser, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("View on GitHub")
                    }
                }
            }
        }
    }
}

@Composable
private fun CommentBubble(
    author: String,
    body: String,
    createdAt: String,
    isUserReply: Boolean
) {
    val bubbleBg = if (isUserReply) {
        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
    } else {
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
    }
    val alignEnd = isUserReply

    val dateFormatted = try {
        val parser = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US)
        val date = parser.parse(createdAt)
        if (date != null) {
            SimpleDateFormat("MM/dd HH:mm", Locale.getDefault()).format(date)
        } else {
            createdAt.take(16)
        }
    } catch (e: Exception) {
        createdAt.take(16)
    }

    Column(
        horizontalAlignment = if (alignEnd) Alignment.End else Alignment.Start,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 4.dp)
        ) {
            Text(
                text = author,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.width(6.dp))
            Text(
                text = dateFormatted,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
            )
        }
        Spacer(Modifier.height(4.dp))
        Box(
            modifier = Modifier
                .clip(
                    RoundedCornerShape(
                        topStart = 16.dp,
                        topEnd = 16.dp,
                        bottomStart = if (alignEnd) 16.dp else 4.dp,
                        bottomEnd = if (alignEnd) 4.dp else 16.dp
                    )
                )
                .background(bubbleBg)
                .padding(12.dp)
                .widthIn(max = 280.dp)
        ) {
            val imageRegex = Regex("""!\[Screenshot]\((https://[^)]+)\)""")
            val match = imageRegex.find(body)
            if (match != null) {
                val imageUrl = match.groupValues[1]
                val cleanBody = body.replace(imageRegex, "").trim()
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (cleanBody.isNotBlank()) {
                        Text(
                            text = cleanBody,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                    AsyncImage(
                        model = imageUrl,
                        contentDescription = "Comment attachment",
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 160.dp)
                            .clip(RoundedCornerShape(8.dp))
                    )
                }
            } else {
                Text(
                    text = body,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}
