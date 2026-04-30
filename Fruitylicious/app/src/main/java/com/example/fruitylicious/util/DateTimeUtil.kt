package com.example.fruitylicious.util

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

object DateTimeUtil {

    private const val DATE_TIME_PATTERN = "MMM dd, yyyy hh:mm a"
    private const val DATE_PATTERN = "MMM dd, yyyy"
    private const val TIME_PATTERN = "hh:mm a"
    private const val ISO_LIKE_PATTERN = "yyyy-MM-dd HH:mm:ss"

    fun now(): Long {
        return System.currentTimeMillis()
    }

    fun formatDateTime(timestamp: Long): String {
        return format(timestamp, DATE_TIME_PATTERN)
    }

    fun formatDate(timestamp: Long): String {
        return format(timestamp, DATE_PATTERN)
    }

    fun formatTime(timestamp: Long): String {
        return format(timestamp, TIME_PATTERN)
    }

    fun formatForApi(timestamp: Long): String {
        return format(timestamp, ISO_LIKE_PATTERN)
    }

    fun startOfToday(): Long {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        return calendar.timeInMillis
    }

    fun endOfToday(): Long {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.HOUR_OF_DAY, 23)
        calendar.set(Calendar.MINUTE, 59)
        calendar.set(Calendar.SECOND, 59)
        calendar.set(Calendar.MILLISECOND, 999)
        return calendar.timeInMillis
    }

    fun startOfMonth(): Long {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.DAY_OF_MONTH, 1)
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        return calendar.timeInMillis
    }

    fun endOfMonth(): Long {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.DAY_OF_MONTH, calendar.getActualMaximum(Calendar.DAY_OF_MONTH))
        calendar.set(Calendar.HOUR_OF_DAY, 23)
        calendar.set(Calendar.MINUTE, 59)
        calendar.set(Calendar.SECOND, 59)
        calendar.set(Calendar.MILLISECOND, 999)
        return calendar.timeInMillis
    }

    fun durationText(start: Long, end: Long?): String {
        if (end == null || end < start) {
            return "Active"
        }

        val totalMinutes = (end - start) / 60000L
        val hours = totalMinutes / 60L
        val minutes = totalMinutes % 60L

        return if (hours > 0) {
            "${hours}h ${minutes}m"
        } else {
            "${minutes}m"
        }
    }

    private fun format(timestamp: Long, pattern: String): String {
        val formatter = SimpleDateFormat(pattern, Locale.getDefault())
        return formatter.format(Date(timestamp))
    }

    fun formatIsoInstant(timestamp: Long): String {
        val formatter = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US)
        formatter.timeZone = TimeZone.getTimeZone("UTC")
        return formatter.format(Date(timestamp))
    }
}