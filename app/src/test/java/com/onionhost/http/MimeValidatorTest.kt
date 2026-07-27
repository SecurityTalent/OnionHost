package com.onionhost.http

import com.onionhost.app.security.MimeValidator
import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.File

class MimeValidatorTest {

    @Test
    fun testMimeTypeDetection() {
        assertEquals("text/html", MimeValidator.getMimeType(File("index.html")))
        assertEquals("text/css", MimeValidator.getMimeType(File("style.css")))
        assertEquals("application/javascript", MimeValidator.getMimeType(File("app.js")))
        assertEquals("application/pdf", MimeValidator.getMimeType(File("document.pdf")))
        assertEquals("application/zip", MimeValidator.getMimeType(File("archive.zip")))
        assertEquals("application/vnd.android.package-archive", MimeValidator.getMimeType(File("app.apk")))
    }
}
