package com.lorryowner.app.ui.components

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lorryowner.app.network.DocumentApiService
import com.lorryowner.app.network.DocumentUiItem
import com.lorryowner.app.network.StagedDocument
import com.lorryowner.app.security.SessionManager
import com.lorryowner.app.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Clean Document Card displaying uploaded document metadata, file type badge, and Open action.
 */
@Composable
fun DocumentCard(
    document: DocumentUiItem,
    sessionManager: SessionManager?,
    canDelete: Boolean = false,
    onDelete: (DocumentUiItem) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var isDownloading by remember { mutableStateOf(false) }
    var downloadError by remember { mutableStateOf<String?>(null) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    val isPdf = document.contentType.contains("pdf", ignoreCase = true) ||
            document.originalFilename.endsWith(".pdf", ignoreCase = true)

    val badgeBg = if (isPdf) Color(0xFFFEF2F2) else Color(0xFFEFF6FF)
    val badgeColor = if (isPdf) Color(0xFFDC2626) else Color(0xFF2563EB)
    val badgeLabel = if (isPdf) "PDF" else "IMG"

    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = CardDefaults.outlinedCardBorder(),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Type icon badge
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(badgeBg),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = badgeLabel,
                        fontWeight = FontWeight.Bold,
                        color = badgeColor,
                        fontSize = 12.sp
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                // Metadata
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = document.title,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.SemiBold,
                            color = BhpTextPrimary,
                            fontSize = 14.sp
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "${document.originalFilename} • ${document.fileSizeFormatted}",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = BhpTextSecondary,
                            fontSize = 11.sp
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "Uploaded: ${document.uploadedAtFormatted}",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = Color(0xFF94A3B8),
                            fontSize = 10.sp
                        )
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Actions: Open / View
                Button(
                    onClick = {
                        if (isDownloading) return@Button
                        isDownloading = true
                        downloadError = null
                        coroutineScope.launch {
                            val result = withContext(Dispatchers.IO) {
                                DocumentApiService.downloadAndOpenDocument(
                                    context = context,
                                    sessionManager = sessionManager,
                                    downloadPath = document.downloadUrl,
                                    originalFilename = document.originalFilename,
                                    contentType = document.contentType
                                )
                            }
                            isDownloading = false
                            if (result.isFailure) {
                                downloadError = result.exceptionOrNull()?.message ?: "Failed to open document"
                            }
                        }
                    },
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = BhpDarkForestGreen,
                        contentColor = Color.White
                    ),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    modifier = Modifier.height(34.dp)
                ) {
                    if (isDownloading) {
                        CircularProgressIndicator(
                            color = Color.White,
                            modifier = Modifier.size(14.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text("View", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }

                // Delete button (owner only)
                if (canDelete) {
                    Spacer(modifier = Modifier.width(6.dp))
                    IconButton(
                        onClick = { showDeleteConfirm = true },
                        modifier = Modifier.size(34.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete Document",
                            tint = Color(0xFF94A3B8),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            // Error alert if download fails
            if (downloadError != null) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = downloadError!!,
                    color = BhpErrorRed,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Delete Document?", fontWeight = FontWeight.Bold) },
            text = { Text("Are you sure you want to remove '${document.title}'? This action cannot be undone.") },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteConfirm = false
                        onDelete(document)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = BhpErrorRed)
                ) {
                    Text("Delete", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("Cancel", color = BhpTextSecondary)
                }
            }
        )
    }
}

/**
 * Section for staging and uploading documents while creating a Trip or registering a Lorry.
 */
@Composable
fun DocumentUploadStagingSection(
    title: String,
    subtitle: String,
    stagedDocuments: List<StagedDocument>,
    onAddStagedDocument: (StagedDocument) -> Unit,
    onRemoveStagedDocument: (Int) -> Unit,
    suggestedDocumentTypes: List<String> = emptyList(),
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var showAddDialog by remember { mutableStateOf(false) }
    var selectedUri by remember { mutableStateOf<Uri?>(null) }
    var selectedFilename by remember { mutableStateOf("") }
    var selectedSize by remember { mutableStateOf(0L) }
    var selectedMimeType by remember { mutableStateOf("") }
    var customDocTitle by remember { mutableStateOf("") }
    var dialogError by remember { mutableStateOf<String?>(null) }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            val (fname, fsize) = DocumentApiService.extractFileInfoFromUri(context, uri)
            val mime = context.contentResolver.getType(uri) ?: "application/octet-stream"

            // Check file size (15MB max)
            if (fsize > 15 * 1024 * 1024) {
                dialogError = "Selected file exceeds 15MB limit"
                return@rememberLauncherForActivityResult
            }

            selectedUri = uri
            selectedFilename = fname
            selectedSize = fsize
            selectedMimeType = mime
            if (customDocTitle.isBlank()) {
                customDocTitle = fname.substringBeforeLast(".")
            }
            dialogError = null
        }
    }

    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = CardDefaults.outlinedCardBorder(),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = BhpTextPrimary,
                            fontSize = 15.sp
                        )
                    )
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = BhpTextSecondary,
                            fontSize = 12.sp
                        )
                    )
                }

                OutlinedButton(
                    onClick = {
                        selectedUri = null
                        selectedFilename = ""
                        selectedSize = 0L
                        selectedMimeType = ""
                        customDocTitle = ""
                        dialogError = null
                        showAddDialog = true
                    },
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = BhpDarkForestGreen),
                    border = androidx.compose.foundation.BorderStroke(1.dp, BhpDarkForestGreen),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                    modifier = Modifier.height(34.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Add Document",
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Add Document", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }
            }

            if (stagedDocuments.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                stagedDocuments.forEachIndexed { index, staged ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFFF8FAFC))
                            .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(8.dp))
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = staged.title,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 13.sp,
                                color = BhpTextPrimary
                            )
                            Text(
                                text = "${staged.originalFilename} • ${DocumentApiService.formatFileSize(staged.sizeBytes)}",
                                fontSize = 11.sp,
                                color = BhpTextSecondary
                            )
                        }

                        IconButton(
                            onClick = { onRemoveStagedDocument(index) },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Remove",
                                tint = BhpErrorRed,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            } else {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Optional. You can add documents now or upload them later.",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = Color(0xFF94A3B8),
                        fontSize = 11.sp
                    )
                )
            }
        }
    }

    if (showAddDialog) {
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            containerColor = Color.White,
            title = { Text("Add Document", fontWeight = FontWeight.Bold, fontSize = 17.sp) },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Suggested title chips
                    if (suggestedDocumentTypes.isNotEmpty()) {
                        Text("Suggested Document Types:", fontSize = 12.sp, color = BhpTextSecondary, fontWeight = FontWeight.Medium)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            suggestedDocumentTypes.take(3).forEach { suggestion ->
                                FilterChip(
                                    selected = customDocTitle == suggestion,
                                    onClick = { customDocTitle = suggestion },
                                    label = { Text(suggestion, fontSize = 11.sp) }
                                )
                            }
                        }
                    }

                    OutlinedTextField(
                        value = customDocTitle,
                        onValueChange = {
                            customDocTitle = it
                            dialogError = null
                        },
                        label = { Text("Document Name / Title *") },
                        placeholder = { Text("e.g. E-Way Bill, RC, Tax Invoice") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp)
                    )

                    // File picker trigger
                    OutlinedButton(
                        onClick = {
                            filePickerLauncher.launch("*/*")
                        },
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = Icons.Default.AttachFile,
                            contentDescription = "Pick File",
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (selectedFilename.isNotBlank()) selectedFilename else "Select PDF or Image",
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    if (selectedFilename.isNotBlank()) {
                        Text(
                            text = "Size: ${DocumentApiService.formatFileSize(selectedSize)}",
                            fontSize = 11.sp,
                            color = BhpTextSecondary
                        )
                    }

                    if (dialogError != null) {
                        Text(
                            text = dialogError!!,
                            color = BhpErrorRed,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (customDocTitle.trim().isBlank()) {
                            dialogError = "Please enter a document name"
                            return@Button
                        }
                        val uri = selectedUri
                        if (uri == null) {
                            dialogError = "Please select a document file"
                            return@Button
                        }
                        onAddStagedDocument(
                            StagedDocument(
                                uri = uri,
                                originalFilename = selectedFilename,
                                title = customDocTitle.trim(),
                                mimeType = selectedMimeType,
                                sizeBytes = selectedSize
                            )
                        )
                        showAddDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = BhpDarkForestGreen)
                ) {
                    Text("Add", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) {
                    Text("Cancel", color = BhpTextSecondary)
                }
            }
        )
    }
}

/**
 * Empty state container when no documents are uploaded.
 */
@Composable
fun EmptyDocumentsView(
    message: String = "No documents uploaded yet.",
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = CardDefaults.outlinedCardBorder(),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFF1F5F9)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Description,
                    contentDescription = null,
                    tint = Color(0xFF94A3B8),
                    modifier = Modifier.size(24.dp)
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = BhpTextSecondary,
                    fontWeight = FontWeight.Medium,
                    fontSize = 13.sp
                )
            )
        }
    }
}
