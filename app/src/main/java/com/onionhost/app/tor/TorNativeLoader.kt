package com.onionhost.app.tor

import android.content.Context
import android.os.Build
import android.util.Log
import java.io.File

object TorNativeLoader {

    private const val TAG = "TorNativeLoader"

    @Volatile
    private var isLoaded = false

    /**
     * Attempts to load native libtor.so using System.loadLibrary or System.load safely.
     */
    fun loadNativeTorLibrary(context: Context): Boolean {
        if (isLoaded) return true

        val abis = Build.SUPPORTED_ABIS.joinToString(", ")
        Log.i(TAG, "Attempting native Tor library loading for supported ABIs: $abis")

        // 1. Try standard System.loadLibrary("tor")
        try {
            System.loadLibrary("tor")
            isLoaded = true
            Log.i(TAG, "Successfully loaded libtor.so via System.loadLibrary(\"tor\")")
            return true
        } catch (e: UnsatisfiedLinkError) {
            Log.w(TAG, "System.loadLibrary(\"tor\") failed: ${e.message}")
        } catch (e: SecurityException) {
            Log.w(TAG, "System.loadLibrary(\"tor\") security error: ${e.message}")
        }

        // 2. Try loading explicitly from applicationInfo.nativeLibraryDir
        val nativeDir = File(context.applicationInfo.nativeLibraryDir)
        val nativeLibTor = File(nativeDir, "libtor.so")

        if (nativeLibTor.exists() && nativeLibTor.canRead()) {
            try {
                System.load(nativeLibTor.absolutePath)
                isLoaded = true
                Log.i(TAG, "Successfully loaded native library from ${nativeLibTor.absolutePath}")
                return true
            } catch (e: UnsatisfiedLinkError) {
                Log.w(TAG, "System.load(${nativeLibTor.absolutePath}) failed: ${e.message}")
            } catch (e: SecurityException) {
                Log.w(TAG, "System.load(${nativeLibTor.absolutePath}) security error: ${e.message}")
            }
        } else {
            Log.w(TAG, "Native library file not found or not readable at ${nativeLibTor.absolutePath}")
        }

        return false
    }

    fun isNativeLibraryLoaded(): Boolean = isLoaded
}
