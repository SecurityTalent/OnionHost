package com.onionhost.tor

import com.onionhost.app.tor.TorManager
import com.onionhost.app.tor.TorV3KeyUtil
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class TorManagerTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    @Test
    fun testV3OnionAddressGenerationAndValidation() {
        val pubKey = ByteArray(32) { it.toByte() }
        val onionAddress = TorManager.generateValidV3OnionAddress(pubKey)
        
        assertNotNull(onionAddress)
        assertTrue("Onion address should end with .onion", onionAddress.endsWith(".onion"))
        assertEquals("Full onion address length should be 62 (.onion included)", 62, onionAddress.length)
        assertTrue("Onion address should be valid v3 address", TorManager.isValidTorV3Address(onionAddress))
    }

    @Test
    fun testInvalidOnionAddressValidation() {
        assertFalse(TorManager.isValidTorV3Address(""))
        assertFalse(TorManager.isValidTorV3Address("invalid.onion"))
        assertFalse(TorManager.isValidTorV3Address("abcdefghijklmnopqrstuvwxyz2345678901234567890123456789012.onion"))
    }

    @Test
    fun testTorV3KeyProvisionAndValidation() {
        val hsDir = tempFolder.newFolder("hs_website")
        
        // Before provisioning, directory validation should fail
        assertFalse(TorV3KeyUtil.validateHiddenServiceDirectory(hsDir))

        // Provision keys
        val hostname = TorV3KeyUtil.provisionTorV3Keys(hsDir)

        assertNotNull(hostname)
        assertTrue(hostname.endsWith(".onion"))
        assertEquals(62, hostname.length)
        assertTrue(TorManager.isValidTorV3Address(hostname))

        // Check required files exist
        assertTrue(File(hsDir, "hostname").exists())
        assertTrue(File(hsDir, "hs_ed25519_secret_key").exists())
        assertTrue(File(hsDir, "hs_ed25519_public_key").exists())

        // After provisioning, directory validation should pass
        assertTrue(TorV3KeyUtil.validateHiddenServiceDirectory(hsDir))
    }
}
