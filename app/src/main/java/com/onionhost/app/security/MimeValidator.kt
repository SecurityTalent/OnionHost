package com.onionhost.app.security

import java.io.File
import java.util.Locale

object MimeValidator {

    private val mimeTypes = mapOf(
        "html" to "text/html",
        "htm" to "text/html",
        "css" to "text/css",
        "js" to "application/javascript",
        "json" to "application/json",
        "png" to "image/png",
        "jpg" to "image/jpeg",
        "jpeg" to "image/jpeg",
        "gif" to "image/gif",
        "svg" to "image/svg+xml",
        "webp" to "image/webp",
        "ico" to "image/x-icon",
        "pdf" to "application/pdf",
        "zip" to "application/zip",
        "md" to "text/markdown",
        "markdown" to "text/markdown",
        "mp3" to "audio/mpeg",
        "ogg" to "audio/ogg",
        "wav" to "audio/wav",
        "mp4" to "video/mp4",
        "webm" to "video/webm",
        "apk" to "application/vnd.android.package-archive",
        "iso" to "application/x-iso9660-image",
        "woff" to "font/woff",
        "woff2" to "font/woff2",
        "ttf" to "font/ttf",
        "otf" to "font/otf"
    )

    fun getMimeType(file: File): String {
        val extension = file.extension.lowercase(Locale.ROOT)
        return mimeTypes[extension] ?: "application/octet-stream"
    }

    fun isDownloadableType(file: File): Boolean {
        val ext = file.extension.lowercase(Locale.ROOT)
        return ext in setOf("pdf", "zip", "iso", "apk", "mp4", "mp3", "tar", "gz")
    }
}
