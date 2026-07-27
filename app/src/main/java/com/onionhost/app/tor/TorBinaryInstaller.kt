package com.onionhost.app.tor

import android.content.Context
import android.os.Build
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

object TorBinaryInstaller {

    private const val TAG = "TorBinaryInstaller"

    /**
     * Locate, extract, or provision an executable Tor daemon binary for the device context.
     */
    suspend fun getOrInstallTorBinary(context: Context): File = withContext(Dispatchers.IO) {
        val torDir = File(context.filesDir, "tor").apply { if (!exists()) mkdirs() }

        // 1. Check native library directory (libtor.so or tor)
        val nativeDir = File(context.applicationInfo.nativeLibraryDir)
        val nativeLibTor = File(nativeDir, "libtor.so")
        if (nativeLibTor.exists() && nativeLibTor.length() > 0) {
            Log.i(TAG, "Found native libtor.so daemon at ${nativeLibTor.absolutePath} (${nativeLibTor.length()} bytes)")
            return@withContext nativeLibTor
        }

        val nativeTor = File(nativeDir, "tor")
        if (nativeTor.exists() && nativeTor.length() > 0) {
            Log.i(TAG, "Found native tor binary at ${nativeTor.absolutePath}")
            return@withContext nativeTor
        }

        // 2. Check files directory (extracted tor binary)
        val extractedLibTor = File(torDir, "libtor.so")
        if (extractedLibTor.exists() && extractedLibTor.length() > 0) {
            extractedLibTor.setExecutable(true, false)
            Log.i(TAG, "Found extracted libtor.so at ${extractedLibTor.absolutePath}")
            return@withContext extractedLibTor
        }

        val extractedTor = File(torDir, "tor")
        if (extractedTor.exists() && extractedTor.length() > 0) {
            extractedTor.setExecutable(true, false)
            Log.i(TAG, "Found extracted tor binary at ${extractedTor.absolutePath}")
            return@withContext extractedTor
        }

        // 3. Try to extract from application assets
        val extractedFromAssets = extractFromAssets(context, torDir)
        if (extractedFromAssets != null && extractedFromAssets.exists() && extractedFromAssets.length() > 0) {
            extractedFromAssets.setExecutable(true, false)
            Log.i(TAG, "Extracted Tor binary from assets to ${extractedFromAssets.absolutePath}")
            return@withContext extractedFromAssets
        }

        throw IllegalStateException(
            "No compatible bundled Tor daemon was found for ${Build.SUPPORTED_ABIS.joinToString()}."
        )
    }

    private fun extractFromAssets(context: Context, targetDir: File): File? {
        val abis = Build.SUPPORTED_ABIS
        val assetPaths = mutableListOf<String>()

        for (abi in abis) {
            assetPaths.add("tor/$abi/tor")
            assetPaths.add("tor/$abi/libtor.so")
            assetPaths.add("bin/$abi/tor")
        }
        assetPaths.add("tor/tor")
        assetPaths.add("tor/libtor.so")

        for (path in assetPaths) {
            try {
                context.assets.open(path).use { input ->
                    val outFile = File(targetDir, if (path.endsWith("libtor.so")) "libtor.so" else "tor")
                    FileOutputStream(outFile).use { output ->
                        input.copyTo(output)
                    }
                    outFile.setExecutable(true, false)
                    Log.i(TAG, "Extracted asset: $path -> ${outFile.absolutePath}")
                    return outFile
                }
            } catch (e: Exception) {
                // Ignore and check next path
            }
        }
        return null
    }

}
