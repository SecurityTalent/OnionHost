package com.onionhost.tor

import com.onionhost.app.tor.Sha3
import com.onionhost.app.tor.TorManager
import org.junit.Assert.*
import org.junit.Test
import java.security.MessageDigest

class Sha3Test {

    @Test
    fun testSha3BouncyCastleMatchesStandardDigest() {
        val testInputs = listOf(
            "".toByteArray(),
            "abc".toByteArray(),
            "The quick brown fox jumps over the lazy dog".toByteArray(),
            ByteArray(200) { it.toByte() }
        )

        val jvmDigest = MessageDigest.getInstance("SHA3-256")

        for (input in testInputs) {
            val expected = jvmDigest.digest(input)
            val actual = Sha3.digest256BouncyCastle(input)
            assertArrayEquals("Sha3 BouncyCastle hash should match JVM MessageDigest SHA3-256", expected, actual)
        }
    }

    @Test
    fun testKnownNistSha3_256Vectors() {
        // Test vector 1: Empty string "" -> a7ffc6f8bf1ed76651c14756a061d662f580ff4de43b49fa82d80a4b80f8434a
        val emptyHash = Sha3.digest256("".toByteArray())
        val emptyHex = emptyHash.joinToString("") { "%02x".format(it) }
        assertEquals("a7ffc6f8bf1ed76651c14756a061d662f580ff4de43b49fa82d80a4b80f8434a", emptyHex)

        // Test vector 2: "abc" -> 3a985da74fe225b2045c172d6bd390bd855f086e3e9d525b46bfe24511431532
        val abcHash = Sha3.digest256("abc".toByteArray())
        val abcHex = abcHash.joinToString("") { "%02x".format(it) }
        assertEquals("3a985da74fe225b2045c172d6bd390bd855f086e3e9d525b46bfe24511431532", abcHex)
    }

    @Test
    fun testV3OnionAddressGenerationWithSha3() {
        val pubKey = ByteArray(32) { (it * 7).toByte() }
        val onionAddress = TorManager.generateValidV3OnionAddress(pubKey)

        assertNotNull(onionAddress)
        assertTrue(onionAddress.endsWith(".onion"))
        assertEquals(62, onionAddress.length)
        assertTrue(TorManager.isValidTorV3Address(onionAddress))
    }
}
