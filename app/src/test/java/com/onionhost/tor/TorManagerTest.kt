package com.onionhost.tor

import com.onionhost.app.tor.TorManager
import org.junit.Assert.*
import org.junit.Test

class TorManagerTest {

    @Test
    fun testV3OnionAddressGenerationAndValidation() {
        val pubKey = ByteArray(32) { it.toByte() }
        val onionAddress = TorManager.generateValidV3OnionAddress(pubKey)
        
        assertNotNull(onionAddress)
        assertTrue("Onion address should end with .onion", onionAddress.endsWith(".onion"))
        assertEquals("Full onion address length should be 63 (.onion included)", 63, onionAddress.length)
        assertTrue("Onion address should be valid v3 address", TorManager.isValidTorV3Address(onionAddress))
    }

    @Test
    fun testInvalidOnionAddressValidation() {
        assertFalse(TorManager.isValidTorV3Address(""))
        assertFalse(TorManager.isValidTorV3Address("invalid.onion"))
        assertFalse(TorManager.isValidTorV3Address("abcdefghijklmnopqrstuvwxyz2345678901234567890123456789012.onion"))
    }
}
