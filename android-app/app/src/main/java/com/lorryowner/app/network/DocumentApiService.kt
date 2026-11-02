package com.lorryowner.app.network

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Log
import androidx.core.content.FileProvider
import com.lorryowner.app.security.SessionManager
import org.json.JSONArray
import org.json.JSONObject
import java.io.*
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.*

data class DocumentUiItem(
    val id: Long,
    val title: String,
    val originalFilename: String,
    val contentType: String,
    val fileSize: Long,
    val fileSizeFormatted: String,
    val uploadedAtFormatted: String,
    val uploadedByName: String?,
    val downloadUrl: String
)

data class StagedDocument(
    val uri: Uri? = null,
    val originalFilename: String,
    val title: String,
    val mimeType: String,
    val sizeBytes: Long
)

object DocumentApiService {

    private const val TAG = "DocumentApi"
    private const val CONNECT_TIMEOUT_MS = 15000
    private const val READ_TIMEOUT_MS = 30000

    fun formatFileSize(bytes: Long): String {
        if (bytes <= 0) return "0 B"
        val kb = bytes / 1024.0
        val mb = kb / 1024.0
        return when {
            mb >= 1.0 -> String.format(Locale.getDefault(), "%.1f MB", mb)
            kb >= 1.0 -> String.format(Locale.getDefault(), "%.1f KB", kb)
            else -> "$bytes B"
        }
    }

    fun formatIsoTimestamp(isoString: String?): String {
        if (isoString.isNullOrBlank()) return "Recently"
        return try {
            val isoFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault()).apply {
                timeZone = TimeZone.getTimeZone("UTC")
            }
            val date = isoFormat.parse(isoString.take(19))
            if (date != null) {
                val outFormat = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())
                outFormat.format(date)
            } else isoString
        } catch (e: Exception) {
            isoString.take(10)
        }
    }

    fun extractFileInfoFromUri(context: Context, uri: Uri): Pair<String, Long> {
        var filename = "document"
        var size: Long = 0
        try {
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                if (cursor.moveToFirst()) {
                    if (nameIndex != -1) filename = cursor.getString(nameIndex) ?: "document"
                    if (sizeIndex != -1) size = cursor.getLong(sizeIndex)
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Could not resolve file info from URI", e)
        }
        return Pair(filename, size)
    }

    /**
     * Lists documents from a given endpoint (Owner or Driver path).
     */
    fun fetchDocuments(sessionManager: SessionManager?, endpointPath: String): Result<List<DocumentUiItem>> {
        val token = sessionManager?.getAccessToken()
            ?: return Result.failure(IllegalStateException("Session token missing"))

        val candidateUrls = ApiConfig.getCandidateBaseUrls(sessionManager)
        var lastEx: Exception? = null

        for (baseUrl in candidateUrls) {
            val endpointUrl = "$baseUrl$endpointPath"
            var connection: HttpURLConnection? = null
            try {
                val url = URL(endpointUrl)
                connection = (url.openConnection(java.net.Proxy.NO_PROXY) as HttpURLConnection).apply {
                    requestMethod = "GET"
                    connectTimeout = CONNECT_TIMEOUT_MS
                    readTimeout = READ_TIMEOUT_MS
                    setRequestProperty("Accept", "application/json")
                    setRequestProperty("Authorization", "Bearer $token")
                }

                if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                    val rawJson = connection.inputStream.bufferedReader().use(BufferedReader::readText)
                    val arr = JSONArray(rawJson)
                    val list = mutableListOf<DocumentUiItem>()
                    for (i in 0 until arr.length()) {
                        val obj = arr.getJSONObject(i)
                        val size = obj.optLong("fileSize", 0)
                        val uploadedAtStr = obj.optString("uploadedAt", "")
                        list.add(
                            DocumentUiItem(
                                id = obj.optLong("id"),
                                title = obj.optString("documentName", "Document"),
                                originalFilename = obj.optString("originalFilename", "file"),
                                contentType = obj.optString("contentType", "application/octet-stream"),
                                fileSize = size,
                                fileSizeFormatted = formatFileSize(size),
                                uploadedAtFormatted = formatIsoTimestamp(uploadedAtStr),
                                uploadedByName = if (obj.has("uploadedByName") && !obj.isNull("uploadedByName")) obj.optString("uploadedByName") else null,
                                downloadUrl = obj.optString("downloadUrl", "")
                            )
                        )
                    }
                    ApiConfig.onUrlSuccess(sessionManager, baseUrl)
                    return Result.success(list)
                } else {
                    lastEx = Exception("Server returned ${connection.responseCode}")
                }
            } catch (e: Exception) {
                lastEx = e
            } finally {
                connection?.disconnect()
            }
        }
        return Result.failure(lastEx ?: Exception("Failed to load documents"))
    }

    /**
     * Uploads a document via multipart/form-data.
     */
    fun uploadDocument(
        context: Context,
        sessionManager: SessionManager?,
        endpointPath: String,
        documentTitle: String,
        fileUri: Uri
    ): Result<DocumentUiItem> {
        val token = sessionManager?.getAccessToken()
            ?: return Result.failure(IllegalStateException("Session token missing"))

        val (filename, size) = extractFileInfoFromUri(context, fileUri)
        val mimeType = context.contentResolver.getType(fileUri) ?: "application/octet-stream"

        val boundary = "==BHP_BOUNDARY_" + System.currentTimeMillis() + "=="
        val lineEnd = "\r\n"
        val twoHyphens = "--"

        val candidateUrls = ApiConfig.getCandidateBaseUrls(sessionManager)
        var lastEx: Exception? = null

        for (baseUrl in candidateUrls) {
            val endpointUrl = "$baseUrl$endpointPath"
            var connection: HttpURLConnection? = null
            try {
                val url = URL(endpointUrl)
                connection = (url.openConnection(java.net.Proxy.NO_PROXY) as HttpURLConnection).apply {
                    requestMethod = "POST"
                    connectTimeout = CONNECT_TIMEOUT_MS
                    readTimeout = READ_TIMEOUT_MS
                    doOutput = true
                    doInput = true
                    useCaches = false
                    setRequestProperty("Connection", "Keep-Alive")
                    setRequestProperty("Content-Type", "multipart/form-data; boundary=$boundary")
                    setRequestProperty("Authorization", "Bearer $token")
                    setRequestProperty("Accept", "application/json")
                }

                connection.outputStream.use { out ->
                    val writer = DataOutputStream(out)

                    // 1. Part: documentName
                    writer.writeBytes(twoHyphens + boundary + lineEnd)
                    writer.writeBytes("Content-Disposition: form-data; name=\"documentName\"" + lineEnd)
                    writer.writeBytes("Content-Type: text/plain; charset=UTF-8" + lineEnd)
                    writer.writeBytes(lineEnd)
                    writer.write(documentTitle.toByteArray(Charsets.UTF_8))
                    writer.writeBytes(lineEnd)

                    // 2. Part: file
                    writer.writeBytes(twoHyphens + boundary + lineEnd)
                    writer.writeBytes("Content-Disposition: form-data; name=\"file\"; filename=\"$filename\"" + lineEnd)
                    writer.writeBytes("Content-Type: $mimeType" + lineEnd)
                    writer.writeBytes(lineEnd)

                    context.contentResolver.openInputStream(fileUri)?.use { input ->
                        val buffer = ByteArray(8192)
                        var bytesRead: Int
                        while (input.read(buffer).also { bytesRead = it } != -1) {
                            writer.write(buffer, 0, bytesRead)
                        }
                    }
                    writer.writeBytes(lineEnd)

                    // Closing boundary
                    writer.writeBytes(twoHyphens + boundary + twoHyphens + lineEnd)
                    writer.flush()
                }

                val responseCode = connection.responseCode
                if (responseCode == HttpURLConnection.HTTP_CREATED || responseCode == HttpURLConnection.HTTP_OK) {
                    val rawJson = connection.inputStream.bufferedReader().use(BufferedReader::readText)
                    val obj = JSONObject(rawJson)
                    val fileSize = obj.optLong("fileSize", size)
                    val item = DocumentUiItem(
                        id = obj.optLong("id"),
                        title = obj.optString("documentName", documentTitle),
                        originalFilename = obj.optString("originalFilename", filename),
                        contentType = obj.optString("contentType", mimeType),
                        fileSize = fileSize,
                        fileSizeFormatted = formatFileSize(fileSize),
                        uploadedAtFormatted = formatIsoTimestamp(obj.optString("uploadedAt")),
                        uploadedByName = if (obj.has("uploadedByName") && !obj.isNull("uploadedByName")) obj.optString("uploadedByName") else null,
                        downloadUrl = obj.optString("downloadUrl", "")
                    )
                    ApiConfig.onUrlSuccess(sessionManager, baseUrl)
                    return Result.success(item)
                } else {
                    val err = connection.errorStream?.bufferedReader()?.use(BufferedReader::readText)
                    lastEx = Exception("Upload failed ($responseCode): ${err ?: ""}")
                }
            } catch (e: Exception) {
                lastEx = e
            } finally {
                connection?.disconnect()
            }
        }
        return Result.failure(lastEx ?: Exception("Upload failed"))
    }

    /**
     * Deletes a document from the server.
     */
    fun deleteDocument(sessionManager: SessionManager?, endpointPath: String): Result<Unit> {
        val token = sessionManager?.getAccessToken()
            ?: return Result.failure(IllegalStateException("Session token missing"))

        val candidateUrls = ApiConfig.getCandidateBaseUrls(sessionManager)
        var lastEx: Exception? = null

        for (baseUrl in candidateUrls) {
            val endpointUrl = "$baseUrl$endpointPath"
            var connection: HttpURLConnection? = null
            try {
                val url = URL(endpointUrl)
                connection = (url.openConnection(java.net.Proxy.NO_PROXY) as HttpURLConnection).apply {
                    requestMethod = "DELETE"
                    connectTimeout = CONNECT_TIMEOUT_MS
                    readTimeout = READ_TIMEOUT_MS
                    setRequestProperty("Authorization", "Bearer $token")
                }

                if (connection.responseCode == HttpURLConnection.HTTP_NO_CONTENT || connection.responseCode == HttpURLConnection.HTTP_OK) {
                    ApiConfig.onUrlSuccess(sessionManager, baseUrl)
                    return Result.success(Unit)
                } else {
                    lastEx = Exception("Delete failed with code ${connection.responseCode}")
                }
            } catch (e: Exception) {
                lastEx = e
            } finally {
                connection?.disconnect()
            }
        }
        return Result.failure(lastEx ?: Exception("Delete failed"))
    }

    /**
     * Downloads file content to local cache directory and opens with Intent.ACTION_VIEW.
     */
    fun downloadAndOpenDocument(
        context: Context,
        sessionManager: SessionManager?,
        downloadPath: String,
        originalFilename: String,
        contentType: String
    ): Result<Unit> {
        val token = sessionManager?.getAccessToken()
            ?: return Result.failure(IllegalStateException("Session token missing"))

        val candidateUrls = ApiConfig.getCandidateBaseUrls(sessionManager)
        var lastEx: Exception? = null

        for (baseUrl in candidateUrls) {
            val endpointUrl = if (downloadPath.startsWith("http")) downloadPath else "$baseUrl$downloadPath"
            var connection: HttpURLConnection? = null
            try {
                val url = URL(endpointUrl)
                connection = (url.openConnection(java.net.Proxy.NO_PROXY) as HttpURLConnection).apply {
                    requestMethod = "GET"
                    connectTimeout = CONNECT_TIMEOUT_MS
                    readTimeout = READ_TIMEOUT_MS
                    setRequestProperty("Authorization", "Bearer $token")
                }

                if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                    // Create documents cache directory
                    val docsDir = File(context.cacheDir, "documents")
                    if (!docsDir.exists()) docsDir.mkdirs()

                    // Ensure safe filename with appropriate extension
                    val sanitized = originalFilename.replace(Regex("[^a-zA-Z0-9._-]"), "_")
                    val targetFile = File(docsDir, sanitized)

                    connection.inputStream.use { input ->
                        FileOutputStream(targetFile).use { output ->
                            input.copyTo(output)
                        }
                    }

                    // Open via FileProvider
                    val authority = "${context.packageName}.fileprovider"
                    val contentUri = FileProvider.getUriForFile(context, authority, targetFile)

                    val intent = Intent(Intent.ACTION_VIEW).apply {
                        setDataAndType(contentUri, contentType)
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }

                    context.startActivity(Intent.createChooser(intent, "Open Document").apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    })

                    ApiConfig.onUrlSuccess(sessionManager, baseUrl)
                    return Result.success(Unit)
                } else {
                    lastEx = Exception("Download failed with HTTP ${connection.responseCode}")
                }
            } catch (e: Exception) {
                lastEx = e
            } finally {
                connection?.disconnect()
            }
        }
        return Result.failure(lastEx ?: Exception("Failed to download document"))
    }
}
