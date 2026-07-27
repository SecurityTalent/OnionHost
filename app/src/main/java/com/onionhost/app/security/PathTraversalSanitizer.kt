package com.onionhost.app.security

import java.io.File
import java.net.URLDecoder

object PathTraversalSanitizer {

    /**
     * Resolves a URI path against the root directory while enforcing path traversal
     * and directory escape prevention.
     * Returns null if the requested path attempts to escape rootDir.
     */
    fun getSafeFile(rootDir: File, uriPath: String): File? {
        val decodedPath = try {
            URLDecoder.decode(uriPath, "UTF-8")
        } catch (e: Exception) {
            return null
        }

        // Prevent null byte injections and double-dot manipulation
        if (decodedPath.contains("\u0000")) return null

        val canonicalRootDir = rootDir.canonicalFile
        val targetFile = File(canonicalRootDir, decodedPath).canonicalFile

        // Verify target file stays strictly inside the canonical root directory tree
        if (!targetFile.path.startsWith(canonicalRootDir.path)) {
            return null
        }

        return targetFile
    }

    /**
     * Validates whether a file is safe for reading (exists, readable, not a device special file).
     */
    fun isSafeToServe(file: File): Boolean {
        return file.exists() && file.canRead() && file.isFile
    }
}
