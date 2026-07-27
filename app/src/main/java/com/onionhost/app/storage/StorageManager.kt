package com.onionhost.app.storage

import android.content.Context
import android.net.Uri
import com.onionhost.app.database.entity.WebsiteType
import com.onionhost.app.security.PathTraversalSanitizer
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.util.UUID
import java.util.zip.ZipInputStream

class StorageManager(private val context: Context) {

    private val websitesDir: File
        get() = File(context.filesDir, "hosted_websites").apply { if (!exists()) mkdirs() }

    /**
     * Imports site contents from a folder, ZIP, or single file Uri into isolated app storage.
     */
    fun importWebsiteContent(
        sourceUri: Uri,
        websiteType: WebsiteType,
        websiteId: String = UUID.randomUUID().toString()
    ): File {
        val targetDir = File(websitesDir, websiteId).apply { mkdirs() }

        when (websiteType) {
            WebsiteType.ZIP -> extractZipFromUri(sourceUri, targetDir)
            WebsiteType.SINGLE_FILE -> copySingleFileFromUri(sourceUri, targetDir)
            WebsiteType.FOLDER -> copyFolderFromUri(sourceUri, targetDir)
        }

        return targetDir
    }

    private fun extractZipFromUri(zipUri: Uri, targetDir: File) {
        context.contentResolver.openInputStream(zipUri)?.use { inputStream ->
            ZipInputStream(inputStream).use { zipInput ->
                var entry = zipInput.nextEntry
                while (entry != null) {
                    val safeFile = PathTraversalSanitizer.getSafeFile(targetDir, entry.name)
                    if (safeFile != null) {
                        if (entry.isDirectory) {
                            safeFile.mkdirs()
                        } else {
                            safeFile.parentFile?.mkdirs()
                            FileOutputStream(safeFile).use { zipInput.copyTo(it) }
                        }
                    }
                    zipInput.closeEntry()
                    entry = zipInput.nextEntry
                }
            }
        }
    }

    private fun copySingleFileFromUri(fileUri: Uri, targetDir: File) {
        val fileName = getFileNameFromUri(fileUri) ?: "index.html"
        val destFile = File(targetDir, fileName)
        context.contentResolver.openInputStream(fileUri)?.use { inputStream ->
            FileOutputStream(destFile).use { outputStream ->
                inputStream.copyTo(outputStream)
            }
        }
        // If single file is not named index.html, create an index.html pointing or renaming
        if (fileName.lowercase().endsWith(".html") && fileName != "index.html") {
            val indexFile = File(targetDir, "index.html")
            destFile.copyTo(indexFile, overwrite = true)
        }
    }

    private fun copyFolderFromUri(folderUri: Uri, targetDir: File) {
        val docTree = androidx.documentfile.provider.DocumentFile.fromTreeUri(context, folderUri)
        if (docTree != null && docTree.exists()) {
            copyDocumentDirectory(docTree, targetDir)
        }
        val indexFile = File(targetDir, "index.html")
        if (!indexFile.exists() && (targetDir.listFiles() == null || targetDir.listFiles()!!.isEmpty())) {
            indexFile.writeText(
                """
                <!DOCTYPE html>
                <html>
                <head>
                    <title>OnionHost Site</title>
                    <style>body { font-family: sans-serif; background: #121212; color: #fff; text-align: center; padding: 50px; }</style>
                </head>
                <body>
                    <h1>Welcome to your Tor Onion Website!</h1>
                    <p>Hosted securely from Android via OnionHost.</p>
                </body>
                </html>
                """.trimIndent()
            )
        }
    }

    private fun copyDocumentDirectory(sourceDir: androidx.documentfile.provider.DocumentFile, targetDir: File) {
        targetDir.mkdirs()
        for (file in sourceDir.listFiles()) {
            val fileName = file.name ?: continue
            if (file.isDirectory) {
                val subDir = File(targetDir, fileName)
                copyDocumentDirectory(file, subDir)
            } else if (file.isFile) {
                val destFile = File(targetDir, fileName)
                try {
                    context.contentResolver.openInputStream(file.uri)?.use { inputStream ->
                        FileOutputStream(destFile).use { outputStream ->
                            inputStream.copyTo(outputStream)
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    fun getWebsiteDirectory(websiteId: String): File {
        return File(websitesDir, websiteId)
    }

    fun deleteWebsiteDirectory(websiteId: String): Boolean {
        val dir = File(websitesDir, websiteId)
        return if (dir.exists()) dir.deleteRecursively() else true
    }

    private fun getFileNameFromUri(uri: Uri): String? {
        var name: String? = null
        val cursor = context.contentResolver.query(uri, null, null, null, null)
        cursor?.use {
            if (it.moveToFirst()) {
                val displayNameIndex = it.getColumnIndex("_display_name")
                if (displayNameIndex != -1) {
                    name = it.getString(displayNameIndex)
                }
            }
        }
        return name ?: uri.lastPathSegment
    }
}
