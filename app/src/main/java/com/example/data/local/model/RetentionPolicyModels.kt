package com.example.data.local.model

/**
 * Statistics regarding local Room database location history storage footprint and retention policy.
 */
data class LocalStorageStats(
    val totalLocationCount: Int = 0,
    val oldestRecordTimestamp: Long? = null,
    val newestRecordTimestamp: Long? = null,
    val estimatedSizeBytes: Long = 0L,
    val eligibleForPruningCount: Int = 0,
    val retentionDays: Int = 14,
    val isAutoDeleteEnabled: Boolean = true,
    val lastCleanupTime: Long = 0L,
    val lastDeletedCount: Int = 0
) {
    val estimatedSizeFormatted: String
        get() = when {
            estimatedSizeBytes >= 1024 * 1024 -> String.format(java.util.Locale.US, "%.1f MB", estimatedSizeBytes / (1024.0 * 1024.0))
            estimatedSizeBytes >= 1024 -> String.format(java.util.Locale.US, "%.1f KB", estimatedSizeBytes / 1024.0)
            else -> "$estimatedSizeBytes B"
        }
}

/**
 * Result returned after executing auto-deletion or manual cleanup on local Room database.
 */
data class RetentionCleanupResult(
    val purgedCount: Int,
    val timestamp: Long = System.currentTimeMillis(),
    val daysPolicy: Int,
    val isAutoDeleted: Boolean = true,
    val message: String
)

/**
 * Preset retention options for user configuration.
 */
enum class RetentionPeriodOption(val days: Int, val label: String, val description: String) {
    ONE_DAY(1, "24 Hours", "Keeps only today's movement trail for daily review"),
    SEVEN_DAYS(7, "7 Days", "Recommended for weekly activity audits and low storage use"),
    FOURTEEN_DAYS(14, "14 Days", "Balanced retention for 2-week travel and commute tracking"),
    THIRTY_DAYS(30, "30 Days", "Standard policy matching cloud privacy compliance standards"),
    SIXTY_DAYS(60, "60 Days", "Extended archive for monthly retrospectives"),
    NINETY_DAYS(90, "90 Days", "Maximum local storage window before auto-clearing");

    companion object {
        fun fromDays(days: Int): RetentionPeriodOption {
            return entries.find { it.days == days } ?: FOURTEEN_DAYS
        }
    }
}
