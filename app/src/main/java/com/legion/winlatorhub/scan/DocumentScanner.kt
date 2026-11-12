package com.legion.winlatorhub.scan

import android.content.ContentResolver
import android.net.Uri
import android.provider.DocumentsContract
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive

/**
 * Legge l'albero delle cartelle con una sola query per cartella.
 * DocumentFile.listFiles() + name/isDirectory facevano una query per ogni file: su un SSD
 * con migliaia di file la differenza è di ordini di grandezza.
 */
class DocumentScanner(
    private val resolver: ContentResolver,
    private val treeUri: Uri,
    private val maxDepth: Int = 6
) {
    private val projection = arrayOf(
        DocumentsContract.Document.COLUMN_DOCUMENT_ID,
        DocumentsContract.Document.COLUMN_DISPLAY_NAME,
        DocumentsContract.Document.COLUMN_MIME_TYPE,
        DocumentsContract.Document.COLUMN_SIZE
    )

    suspend fun scan(): ScanNode {
        val rootId = DocumentsContract.getTreeDocumentId(treeUri)
        return scanDir(rootId, name = "", depth = 0)
    }

    private suspend fun scanDir(documentId: String, name: String, depth: Int): ScanNode {
        currentCoroutineContext().ensureActive()

        val exes = mutableListOf<ExeFile>()
        val subDirs = mutableListOf<Pair<String, String>>()
        val childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(treeUri, documentId)

        resolver.query(childrenUri, projection, null, null, null)?.use { c ->
            val idCol = c.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_DOCUMENT_ID)
            val nameCol = c.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_DISPLAY_NAME)
            val mimeCol = c.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_MIME_TYPE)
            val sizeCol = c.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_SIZE)
            while (c.moveToNext()) {
                val childId = c.getString(idCol) ?: continue
                val childName = c.getString(nameCol) ?: continue
                if (c.getString(mimeCol) == DocumentsContract.Document.MIME_TYPE_DIR) {
                    if (depth < maxDepth && !GameDetector.isIgnoredDir(childName)) {
                        subDirs += childId to childName
                    }
                } else if (childName.endsWith(".exe", ignoreCase = true)) {
                    val size = if (c.isNull(sizeCol)) 0L else c.getLong(sizeCol)
                    exes += ExeFile(childName, childId, size)
                }
            }
        }
        // Ricorsione dopo aver chiuso il cursore, per non tenere aperti decine di cursori.
        val children = subDirs.map { (id, n) -> scanDir(id, n, depth + 1) }
        return ScanNode(name, documentId, exes, children)
    }
}
