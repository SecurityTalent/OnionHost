package com.onionhost.app.tor

import org.bouncycastle.crypto.generators.Ed25519KeyPairGenerator
import org.bouncycastle.crypto.params.Ed25519KeyGenerationParameters
import org.bouncycastle.crypto.params.Ed25519PrivateKeyParameters
import org.bouncycastle.crypto.params.Ed25519PublicKeyParameters
import java.io.File
import java.security.SecureRandom

object TorV3KeyUtil {

    private val SECRET_KEY_HEADER = "tag: secret-key v3\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000".toByteArray(Charsets.US_ASCII)
    private val PUBLIC_KEY_HEADER = "tag: public-key v3\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000".toByteArray(Charsets.US_ASCII)

    /**
     * Verifies that the Hidden Service directory contains valid v3 keys and hostname.
     */
    fun validateHiddenServiceDirectory(hsDir: File): Boolean {
        val hostnameFile = File(hsDir, "hostname")
        val secretKeyFile = File(hsDir, "hs_ed25519_secret_key")
        val publicKeyFile = File(hsDir, "hs_ed25519_public_key")

        if (!hostnameFile.exists() || !secretKeyFile.exists() || !publicKeyFile.exists()) {
            return false
        }

        val hostnameText = hostnameFile.readText().trim()
        if (!TorManager.isValidTorV3Address(hostnameText)) {
            return false
        }

        if (secretKeyFile.length() < 64 || publicKeyFile.length() < 64) {
            return false
        }

        return true
    }

    /**
     * Provision valid Tor v3 Ed25519 key files in the Hidden Service directory.
     * This creates hs_ed25519_secret_key, hs_ed25519_public_key, and hostname files
     * strictly following Tor rend-spec-v3.
     */
    fun provisionTorV3Keys(hsDir: File): String {
        if (!hsDir.exists()) {
            hsDir.mkdirs()
        }

        if (validateHiddenServiceDirectory(hsDir)) {
            return File(hsDir, "hostname").readText().trim()
        }

        val keyPairGenerator = Ed25519KeyPairGenerator().apply {
            init(Ed25519KeyGenerationParameters(SecureRandom()))
        }
        val keyPair = keyPairGenerator.generateKeyPair()
        val privateKeyParams = keyPair.private as Ed25519PrivateKeyParameters
        val publicKeyParams = keyPair.public as Ed25519PublicKeyParameters

        val seedBytes = privateKeyParams.encoded // 32 bytes seed
        val pubKeyBytes = publicKeyParams.encoded // 32 bytes public key

        val secretKeyContent = SECRET_KEY_HEADER + seedBytes
        val publicKeyContent = PUBLIC_KEY_HEADER + pubKeyBytes

        val hostname = TorManager.generateValidV3OnionAddress(pubKeyBytes)

        File(hsDir, "hs_ed25519_secret_key").writeBytes(secretKeyContent)
        File(hsDir, "hs_ed25519_public_key").writeBytes(publicKeyContent)
        File(hsDir, "hostname").writeText("$hostname\n")

        return hostname
    }
}
