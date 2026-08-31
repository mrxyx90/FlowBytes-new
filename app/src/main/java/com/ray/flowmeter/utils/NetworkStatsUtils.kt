package com.ray.flowmeter.utils

import android.app.usage.NetworkStatsManager
import java.util.Calendar

object NetworkStatsUtils {

    /**
     * Calculates the start time for a given period (daily or monthly) based on reset settings.
     */
    fun getStartTimeForPeriod(
        period: String,
        currentTime: Long,
        resetHour: Int,
        resetMinute: Int,
        monthlyResetDay: Int
    ): Long {
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = currentTime
        
        if (period == "monthly") {
            val clampedDay = monthlyResetDay.coerceAtMost(calendar.getActualMaximum(Calendar.DAY_OF_MONTH))
            calendar[Calendar.DAY_OF_MONTH] = clampedDay
        }
        
        calendar[Calendar.HOUR_OF_DAY] = resetHour
        calendar[Calendar.MINUTE] = resetMinute
        calendar[Calendar.SECOND] = 0
        calendar[Calendar.MILLISECOND] = 0

        var startTime = calendar.timeInMillis

        if (currentTime < startTime) {
            if (period == "monthly") {
                calendar.add(Calendar.MONTH, -1)
                val prevMaxDay = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)
                calendar[Calendar.DAY_OF_MONTH] = monthlyResetDay.coerceAtMost(prevMaxDay)
            } else {
                calendar.add(Calendar.DAY_OF_YEAR, -1)
            }
            startTime = calendar.timeInMillis
        }
        return startTime
    }

    /**
     * Safely queries the device total usage and returns a Pair of (RX, TX).
     */
    fun getDeviceTotalUsagePair(
        manager: NetworkStatsManager,
        transportType: Int,
        startTime: Long,
        endTime: Long
    ): Pair<Long, Long> {
        return try {
            val bucket = manager.querySummaryForDevice(transportType, null, startTime, endTime)
            bucket.rxBytes to bucket.txBytes
        } catch (_: Exception) {
            0L to 0L
        }
    }

    /**
     * Safely queries the total usage for a device using querySummaryForDevice.
     */
    fun getDeviceTotalUsage(
        manager: NetworkStatsManager,
        transportType: Int,
        startTime: Long,
        endTime: Long
    ): Long {
        val (rx, tx) = getDeviceTotalUsagePair(manager, transportType, startTime, endTime)
        return rx + tx
    }
}
