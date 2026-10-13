package com.yudemeng.app.ui

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val DATE_TIME_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")

/** 剩余时间文案。expiresAt == null 表示永久保存。 */
fun formatRemaining(expiresAt: Long?, now: Long): String {
    if (expiresAt == null) return "永久保存"
    val diff = expiresAt - now
    if (diff <= 0) return "已到期"

    val totalSeconds = diff / 1000
    val days = totalSeconds / 86400
    val hours = (totalSeconds % 86400) / 3600
    val minutes = (totalSeconds % 3600) / 60

    return when {
        days > 0 -> "剩 $days 天 $hours 小时"
        hours > 0 -> "剩 $hours 小时 $minutes 分"
        minutes > 0 -> "剩 $minutes 分"
        else -> "剩不到 1 分钟"
    }
}

fun formatDateTime(timestamp: Long): String =
    Instant.ofEpochMilli(timestamp).atZone(ZoneId.systemDefault()).format(DATE_TIME_FORMATTER)
