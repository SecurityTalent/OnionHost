package com.onionhost.security

import com.onionhost.app.security.PathTraversalSanitizer
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class PathTraversalSanitizerTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    @Test
    fun testValidRelativeFileAccess() {
        val rootDir = tempFolder.newFolder("webroot")
        val sampleFile = File(rootDir, "index.html").apply { writeText("<h1>Test</h1>") }

        val resolved = PathTraversalSanitizer.getSafeFile(rootDir, "index.html")

        assertNotNull(resolved)
        assertEquals(sampleFile.canonicalPath, resolved?.canonicalPath)
    }

    @Test
    fun testDirectoryEscapeAttemptReturnsNull() {
        val rootDir = tempFolder.newFolder("webroot")
        val secretsFile = tempFolder.newFile("passwords.txt").apply { writeText("secret") }

        val resolved = PathTraversalSanitizer.getSafeFile(rootDir, "../passwords.txt")

        assertNull("Path traversal attack must be blocked", resolved)
    }

    @Test
    fun testEncodedPathTraversalAttemptReturnsNull() {
        val rootDir = tempFolder.newFolder("webroot")

        val resolved = PathTraversalSanitizer.getSafeFile(rootDir, "%2e%2e%2fpasswords.txt")

        assertNull("Encoded path traversal attack must be blocked", resolved)
    }
}
