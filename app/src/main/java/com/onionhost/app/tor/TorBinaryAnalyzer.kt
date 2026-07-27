package com.onionhost.app.tor

import android.content.Context
import android.os.Build
import android.util.Log
import java.io.File
import java.io.RandomAccessFile

enum class BinaryType {
    EXECUTABLE,
    SHARED_LIBRARY,
    UNKNOWN_ELF,
    INVALID_FORMAT,
    NOT_FOUND
}

data class BinaryAnalysisResult(
    val file: File?,
    val type: BinaryType,
    val isNativeDir: Boolean,
    val canExecute: Boolean,
    val supportedAbis: List<String>,
    val detailMessage: String
)

object TorBinaryAnalyzer {

    private const val TAG = "TorBinaryAnalyzer"

    /**
     * Inspects a binary file to verify ELF header, execution permissions, and directory boundary.
     */
    fun analyzeBinary(context: Context, binaryFile: File?): BinaryAnalysisResult {
        val abis = Build.SUPPORTED_ABIS.toList()
        if (binaryFile == null || !binaryFile.exists()) {
            return BinaryAnalysisResult(
                file = null,
                type = BinaryType.NOT_FOUND,
                isNativeDir = false,
                canExecute = false,
                supportedAbis = abis,
                detailMessage = "Binary file is null or does not exist."
            )
        }

        val nativeDir = File(context.applicationInfo.nativeLibraryDir)
        val isNativeDir = binaryFile.absolutePath.startsWith(nativeDir.absolutePath)
        val canExecute = binaryFile.canExecute() && isNativeDir

        val elfType = inspectElfType(binaryFile)

        val detail = buildString {
            append("Path: ${binaryFile.absolutePath} | ")
            append("NativeDir: $isNativeDir | ")
            append("CanExecute: $canExecute | ")
            append("ELF Type: $elfType | ")
            append("Supported ABIs: ${abis.joinToString(", ")}")
        }

        Log.i(TAG, detail)

        return BinaryAnalysisResult(
            file = binaryFile,
            type = elfType,
            isNativeDir = isNativeDir,
            canExecute = canExecute,
            supportedAbis = abis,
            detailMessage = detail
        )
    }

    private fun inspectElfType(file: File): BinaryType {
        if (file.length() < 18) return BinaryType.INVALID_FORMAT
        return try {
            RandomAccessFile(file, "r").use { raf ->
                val magic = ByteArray(4)
                raf.readFully(magic)
                // 0x7F 'E' 'L' 'F'
                if (magic[0] != 0x7F.toByte() || magic[1] != 'E'.toByte() ||
                    magic[2] != 'L'.toByte() || magic[3] != 'F'.toByte()
                ) {
                    return BinaryType.INVALID_FORMAT
                }

                raf.seek(5) // Endianness byte (1 = little, 2 = big)
                val isLittleEndian = raf.readByte().toInt() == 1

                raf.seek(16) // e_type offset
                val typeLow = raf.readUnsignedByte()
                val typeHigh = raf.readUnsignedByte()
                val eType = if (isLittleEndian) {
                    (typeHigh shl 8) or typeLow
                } else {
                    (typeLow shl 8) or typeHigh
                }

                when (eType) {
                    2 -> BinaryType.EXECUTABLE // ET_EXEC
                    3 -> BinaryType.SHARED_LIBRARY // ET_DYN (Shared object or PIE)
                    else -> BinaryType.UNKNOWN_ELF
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to parse ELF header for ${file.absolutePath}: ${e.message}")
            BinaryType.INVALID_FORMAT
        }
    }
}
