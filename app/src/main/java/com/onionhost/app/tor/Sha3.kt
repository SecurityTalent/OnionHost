package com.onionhost.app.tor

import org.bouncycastle.crypto.digests.SHA3Digest
import org.bouncycastle.jce.provider.BouncyCastleProvider
import java.security.MessageDigest
import java.security.Security

object Sha3 {

    init {
        try {
            if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null) {
                Security.addProvider(BouncyCastleProvider())
            }
        } catch (e: Throwable) {
            // Ignore security manager restriction if any
        }
    }

    /**
     * Computes SHA3-256 hash of the input bytes.
     * Guaranteed to work across all Android API levels and JVM environments.
     */
    fun digest256(input: ByteArray): ByteArray {
        return try {
            MessageDigest.getInstance("SHA3-256").digest(input)
        } catch (e: Exception) {
            try {
                MessageDigest.getInstance("SHA3-256", BouncyCastleProvider.PROVIDER_NAME).digest(input)
            } catch (e2: Exception) {
                digest256BouncyCastle(input)
            }
        }
    }

    fun digest256BouncyCastle(input: ByteArray): ByteArray {
        val digest = SHA3Digest(256)
        digest.update(input, 0, input.size)
        val out = ByteArray(32)
        digest.doFinal(out, 0)
        return out
    }
}
