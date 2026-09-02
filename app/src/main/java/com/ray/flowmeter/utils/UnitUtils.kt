package com.ray.flowmeter.utils

/**
 * Utility functions for handling data limit units and conversions.
 */
object UnitUtils {
    const val MB_IN_BYTES = 1024L * 1024L
    const val GB_IN_BYTES = 1024L * 1024L * 1024L

    /**
     * Converts a byte value to a pair of (string value, unit) for UI inputs.
     * If the value is 0 or less, it returns ("100", "MB") as a default.
     */
    fun bytesToUiState(bytes: Long): Pair<String, String> {
        if (bytes <= 0L) return "100" to "MB"
        val isGB = (bytes >= GB_IN_BYTES) && (bytes % GB_IN_BYTES == 0L)
        val value = if (isGB) bytes / GB_IN_BYTES else bytes / MB_IN_BYTES
        return value.toString() to (if (isGB) "GB" else "MB")
    }

    /**
     * Converts UI input string and unit back to raw bytes.
     */
    fun uiStateToBytes(input: String, unit: String): Long {
        val value = input.toLongOrNull() ?: 0L
        val multiplier = if (unit == "GB") GB_IN_BYTES else MB_IN_BYTES
        return value * multiplier
    }
}
