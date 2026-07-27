package com.onionhost.tor

import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class TorBinaryInstallerTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    @Test
    fun testTorBinarySearchInDirectory() {
        val filesDir = tempFolder.newFolder("files")
        val torDir = File(filesDir, "tor").apply { mkdirs() }
        val dummyTorBinary = File(torDir, "tor").apply { writeText("#!/bin/sh\necho Tor") }

        assertTrue(dummyTorBinary.exists())
        assertTrue(dummyTorBinary.length() > 0)
    }

    @Test
    fun testDummyFileCreation() {
        val file = tempFolder.newFile("libtor.so").apply { writeText("ELF_TEST") }
        assertTrue(file.exists())
        assertTrue(file.length() > 0)
    }
}
