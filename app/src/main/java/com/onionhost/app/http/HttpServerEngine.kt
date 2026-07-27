package com.onionhost.app.http

import fi.iki.elonen.NanoHTTPD
import com.onionhost.app.security.MimeValidator
import com.onionhost.app.security.PathTraversalSanitizer
import com.onionhost.app.security.RateLimiter
import java.io.File
import java.io.FileInputStream
import java.io.InputStream
import java.util.Locale

class HttpServerEngine(
    private val port: Int,
    private val webRootDir: File,
    private val rateLimiter: RateLimiter = RateLimiter(),
    private val requiresAuth: Boolean = false,
    private val authUsername: String = "",
    private val authPasswordHash: String = "",
    private val onRequestServed: ((path: String, statusCode: Int, bytesSent: Long, isDownload: Boolean) -> Unit)? = null
) : NanoHTTPD("127.0.0.1", port) {

    override fun serve(session: IHTTPSession): Response {
        // Rate Limiting Check
        if (!rateLimiter.isAllowed(session.remoteIpAddress ?: "127.0.0.1")) {
            return newFixedLengthResponse(Response.Status.TOO_MANY_REQUESTS, MIME_PLAINTEXT, "429 Too Many Requests")
        }

        // Basic Authentication Check
        if (requiresAuth && authUsername.isNotBlank()) {
            val authHeader = session.headers["authorization"]
            if (authHeader == null || !checkBasicAuth(authHeader)) {
                val response = newFixedLengthResponse(
                    Response.Status.UNAUTHORIZED,
                    MIME_PLAINTEXT,
                    "401 Unauthorized"
                )
                response.addHeader("WWW-Authenticate", "Basic realm=\"OnionHost Private Area\"")
                return response
            }
        }

        var uri = session.uri
        if (uri.endsWith("/") || uri.isEmpty()) {
            uri += "index.html"
        }

        // Path Traversal Security check
        val safeFile = PathTraversalSanitizer.getSafeFile(webRootDir, uri)
            ?: return newFixedLengthResponse(
                Response.Status.FORBIDDEN,
                MIME_PLAINTEXT,
                "403 Forbidden: Directory Escape Detected"
            )

        if (!safeFile.exists()) {
            // Check if user requested a directory without trailing slash
            val dirFile = PathTraversalSanitizer.getSafeFile(webRootDir, session.uri)
            if (dirFile != null && dirFile.isDirectory) {
                val indexInDir = File(dirFile, "index.html")
                if (indexInDir.exists()) {
                    return serveFile(indexInDir, session.headers)
                }
                return generateDirectoryListing(dirFile, session.uri)
            }
            return newFixedLengthResponse(
                Response.Status.NOT_FOUND,
                MIME_PLAINTEXT,
                "404 Not Found"
            )
        }

        if (safeFile.isDirectory) {
            val indexFile = File(safeFile, "index.html")
            if (indexFile.exists()) {
                return serveFile(indexFile, session.headers)
            }
            return generateDirectoryListing(safeFile, session.uri)
        }

        return serveFile(safeFile, session.headers)
    }

    private fun serveFile(file: File, headers: Map<String, String>): Response {
        val mimeType = MimeValidator.getMimeType(file)
        val fileLength = file.length()
        val isDownload = MimeValidator.isDownloadableType(file)

        // Support for Range requests (HTTP 206 Partial Content)
        var rangeHeader = headers["range"]
        if (rangeHeader != null && rangeHeader.startsWith("bytes=")) {
            rangeHeader = rangeHeader.substring("bytes=".length)
            val minusIndex = rangeHeader.indexOf('-')
            var start: Long = 0
            var end: Long = fileLength - 1

            if (minusIndex >= 0) {
                try {
                    val startStr = rangeHeader.substring(0, minusIndex)
                    if (startStr.isNotEmpty()) start = startStr.toLong()

                    val endStr = rangeHeader.substring(minusIndex + 1)
                    if (endStr.isNotEmpty()) end = endStr.toLong()
                } catch (ignored: NumberFormatException) {
                }
            }

            if (start > end || start >= fileLength) {
                val res = newFixedLengthResponse(Response.Status.RANGE_NOT_SATISFIABLE, MIME_PLAINTEXT, "")
                res.addHeader("Content-Range", "bytes */$fileLength")
                return res
            }

            if (end >= fileLength) end = fileLength - 1
            val contentLength = end - start + 1

            val fileStream = FileInputStream(file)
            fileStream.skip(start)

            val response = newFixedLengthResponse(
                Response.Status.PARTIAL_CONTENT,
                mimeType,
                fileStream,
                contentLength
            )
            response.addHeader("Content-Range", "bytes $start-$end/$fileLength")
            response.addHeader("Accept-Ranges", "bytes")
            response.addHeader("Cache-Control", "public, max-age=3600")

            onRequestServed?.invoke(file.name, 206, contentLength, isDownload)
            return response
        }

        val response = newFixedLengthResponse(
            Response.Status.OK,
            mimeType,
            FileInputStream(file),
            fileLength
        )

        response.addHeader("Accept-Ranges", "bytes")
        response.addHeader("Cache-Control", "public, max-age=3600")
        response.addHeader("X-Content-Type-Options", "nosniff")
        response.addHeader("X-Frame-Options", "DENY")
        response.addHeader("X-XSS-Protection", "1; mode=block")

        if (isDownload) {
            response.addHeader("Content-Disposition", "attachment; filename=\"${file.name}\"")
        }

        onRequestServed?.invoke(file.name, 200, fileLength, isDownload)
        return response
    }

    private fun generateDirectoryListing(dir: File, requestUri: String): Response {
        val files = dir.listFiles() ?: arrayOf()
        val sb = StringBuilder()
        sb.append("<!DOCTYPE html><html><head><title>Index of ").append(requestUri).append("</title>")
        sb.append("<style>body{font-family:monospace;background:#18181b;color:#e4e4e7;padding:2rem;}a{color:#38bdf8;text-decoration:none;}a:hover{text-decoration:underline;}</style></head><body>")
        sb.append("<h2>Directory Index: ").append(requestUri).append("</h2><hr/><ul>")
        if (requestUri != "/") {
            sb.append("<li><a href=\"..\">.. (Parent Directory)</a></li>")
        }
        for (f in files) {
            val name = f.name + if (f.isDirectory) "/" else ""
            sb.append("<li><a href=\"").append(name).append("\">").append(name).append("</a></li>")
        }
        sb.append("</ul><hr/><p><em>Powered by OnionHost Embedded Engine</em></p></body></html>")

        val res = newFixedLengthResponse(Response.Status.OK, MIME_HTML, sb.toString())
        onRequestServed?.invoke(requestUri, 200, sb.length.toLong(), false)
        return res
    }

    private fun checkBasicAuth(authHeader: String): Boolean {
        return try {
            val base64Credentials = authHeader.substring("Basic ".length).trim()
            val credentials = String(android.util.Base64.decode(base64Credentials, android.util.Base64.DEFAULT), Charsets.UTF_8)
            val parts = credentials.split(":", limit = 2)
            if (parts.size == 2) {
                parts[0] == authUsername && parts[1] == authPasswordHash
            } else false
        } catch (e: Exception) {
            false
        }
    }
}
