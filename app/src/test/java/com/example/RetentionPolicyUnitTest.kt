package com.example

import com.example.data.local.model.LocalStorageStats
import com.example.data.local.model.RetentionPeriodOption
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RetentionPolicyUnitTest {

    @Test
    fun testRetentionPeriodOptionMapping() {
        assertEquals(RetentionPeriodOption.ONE_DAY, RetentionPeriodOption.fromDays(1))
        assertEquals(RetentionPeriodOption.SEVEN_DAYS, RetentionPeriodOption.fromDays(7))
        assertEquals(RetentionPeriodOption.FOURTEEN_DAYS, RetentionPeriodOption.fromDays(14))
        assertEquals(RetentionPeriodOption.THIRTY_DAYS, RetentionPeriodOption.fromDays(30))
        assertEquals(RetentionPeriodOption.SIXTY_DAYS, RetentionPeriodOption.fromDays(60))
        assertEquals(RetentionPeriodOption.NINETY_DAYS, RetentionPeriodOption.fromDays(90))

        // Default fallback for unrecognized days
        assertEquals(RetentionPeriodOption.FOURTEEN_DAYS, RetentionPeriodOption.fromDays(999))
    }

    @Test
    fun testLocalStorageStatsFormattedSize() {
        val bytesSmall = LocalStorageStats(estimatedSizeBytes = 512).estimatedSizeFormatted
        assertEquals("512 B", bytesSmall)

        val bytesKb = LocalStorageStats(estimatedSizeBytes = 2048).estimatedSizeFormatted
        assertEquals("2.0 KB", bytesKb)

        val bytesMb = LocalStorageStats(estimatedSizeBytes = 2 * 1024 * 1024).estimatedSizeFormatted
        assertEquals("2.0 MB", bytesMb)
    }

    @Test
    fun testCutoffTimestampCalculation() {
        val now = 1726480000000L
        val sevenDaysMs = 7L * 24 * 60 * 60 * 1000L
        val cutoff = now - sevenDaysMs

        val sampleTimestampOld = now - (8L * 24 * 60 * 60 * 1000L)
        val sampleTimestampRecent = now - (3L * 24 * 60 * 60 * 1000L)

        assertTrue(sampleTimestampOld < cutoff)
        assertTrue(sampleTimestampRecent >= cutoff)
    }
}
