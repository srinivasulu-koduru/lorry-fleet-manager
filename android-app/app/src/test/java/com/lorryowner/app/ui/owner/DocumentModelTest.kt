package com.lorryowner.app.ui.owner

import android.net.Uri
import com.lorryowner.app.network.DocumentApiService
import com.lorryowner.app.network.DocumentUiItem
import com.lorryowner.app.network.StagedDocument
import org.junit.Assert.*
import org.junit.Test

class DocumentModelTest {

    @Test
    fun testFormatFileSize_FormatsCorrectly() {
        assertEquals("0 B", DocumentApiService.formatFileSize(0))
        assertEquals("500 B", DocumentApiService.formatFileSize(500))
        assertEquals("1.0 KB", DocumentApiService.formatFileSize(1024))
        assertEquals("1.5 KB", DocumentApiService.formatFileSize(1536))
        assertEquals("1.0 MB", DocumentApiService.formatFileSize(1024 * 1024))
        assertEquals("2.5 MB", DocumentApiService.formatFileSize((2.5 * 1024 * 1024).toLong()))
    }

    @Test
    fun testDocumentUiItem_ModelCreation() {
        val doc = DocumentUiItem(
            id = 101L,
            title = "E-Way Bill",
            originalFilename = "eway_bill_101.pdf",
            contentType = "application/pdf",
            fileSize = 1024 * 1024,
            fileSizeFormatted = "1.0 MB",
            uploadedAtFormatted = "01 Nov 2026, 04:30 PM",
            uploadedByName = "Owner One",
            downloadUrl = "/api/owner/trips/1/documents/101/download"
        )

        assertEquals(101L, doc.id)
        assertEquals("E-Way Bill", doc.title)
        assertEquals("eway_bill_101.pdf", doc.originalFilename)
        assertEquals("application/pdf", doc.contentType)
        assertEquals(1024L * 1024L, doc.fileSize)
        assertEquals("1.0 MB", doc.fileSizeFormatted)
        assertEquals("01 Nov 2026, 04:30 PM", doc.uploadedAtFormatted)
        assertEquals("Owner One", doc.uploadedByName)
        assertEquals("/api/owner/trips/1/documents/101/download", doc.downloadUrl)
    }

    @Test
    fun testStagedDocument_ModelCreation() {
        val uri = Uri.EMPTY
        val staged = StagedDocument(
            uri = uri,
            originalFilename = "rc_book.jpg",
            title = "RC Card",
            mimeType = "image/jpeg",
            sizeBytes = 204800L
        )

        assertEquals("rc_book.jpg", staged.originalFilename)
        assertEquals("RC Card", staged.title)
        assertEquals("image/jpeg", staged.mimeType)
        assertEquals(204800L, staged.sizeBytes)
    }

    @Test
    fun testAddTripViewModel_StagedDocumentLifecycle() {
        val vm = AddTripViewModel(null, autoLoad = false)
        assertTrue(vm.uiState.stagedDocuments.isEmpty())

        val uri = Uri.EMPTY
        val doc1 = StagedDocument(uri, "eway.pdf", "E-Way Bill", "application/pdf", 1000L)
        val doc2 = StagedDocument(uri, "inv.pdf", "Tax Invoice", "application/pdf", 2000L)

        vm.onAddStagedDocument(doc1)
        vm.onAddStagedDocument(doc2)
        assertEquals(2, vm.uiState.stagedDocuments.size)
        assertEquals("E-Way Bill", vm.uiState.stagedDocuments[0].title)

        vm.onRemoveStagedDocument(0)
        assertEquals(1, vm.uiState.stagedDocuments.size)
        assertEquals("Tax Invoice", vm.uiState.stagedDocuments[0].title)
    }

    @Test
    fun testAddLorryViewModel_StagedDocumentLifecycle() {
        val vm = AddLorryViewModel(null, autoLoad = false)
        assertTrue(vm.uiState.stagedDocuments.isEmpty())

        val uri = Uri.EMPTY
        val doc1 = StagedDocument(uri, "rc.pdf", "RC Book", "application/pdf", 1500L)
        val doc2 = StagedDocument(uri, "ins.pdf", "Insurance", "application/pdf", 2500L)

        vm.onAddStagedDocument(doc1)
        vm.onAddStagedDocument(doc2)
        assertEquals(2, vm.uiState.stagedDocuments.size)
        assertEquals("RC Book", vm.uiState.stagedDocuments[0].title)

        vm.onRemoveStagedDocument(0)
        assertEquals(1, vm.uiState.stagedDocuments.size)
        assertEquals("Insurance", vm.uiState.stagedDocuments[0].title)
    }
}
