package com.yudemeng.app.domain

import com.yudemeng.app.data.ExpiryPolicy
import java.time.Instant
import java.time.ZoneId
import java.time.temporal.TemporalAdjusters

/**
 * 过期时刻计算：纯函数，时间源与地区通过参数注入，方便替换与测试。
 *
 * 约定（与需求确认一致）：
 *  - 今天  -> 当天 23:59:59.999
 *  - 3 天  -> 创建时间 + 72 小时
 *  - 一周  -> 创建时间 + 7 天
 *  - 本月  -> 当月最后一天 23:59:59.999
 *  - 永久  -> null（永不过期）
 *
 * 所有比较都基于 epoch millis 绝对时间戳，因此跨时区、夏令时都不会算错。
 */
object Expiry {

    const val DAY_MS: Long = 24L * 60 * 60 * 1000

    fun computeExpiresAt(policy: ExpiryPolicy, createdAt: Long, zone: ZoneId): Long? =
        when (policy) {
            ExpiryPolicy.FOREVER -> null
            ExpiryPolicy.TODAY -> endOfDay(createdAt, zone)
            ExpiryPolicy.THREE_DAYS -> createdAt + 3L * DAY_MS
            ExpiryPolicy.ONE_WEEK -> createdAt + 7L * DAY_MS
            ExpiryPolicy.THIS_MONTH -> endOfMonth(createdAt, zone)
        }

    /** 过期 = 时间戳比较的结果，与"定时任务"无关。 */
    fun isExpired(expiresAt: Long?, now: Long): Boolean = expiresAt != null && expiresAt <= now

    private fun endOfDay(timestamp: Long, zone: ZoneId): Long =
        Instant.ofEpochMilli(timestamp)
            .atZone(zone)
            .toLocalDate()
            .atTime(23, 59, 59, 999_000_000)
            .atZone(zone)
            .toInstant()
            .toEpochMilli()

    private fun endOfMonth(timestamp: Long, zone: ZoneId): Long {
        val lastDay = Instant.ofEpochMilli(timestamp)
            .atZone(zone)
            .toLocalDate()
            .with(TemporalAdjusters.lastDayOfMonth())
        return lastDay
            .atTime(23, 59, 59, 999_000_000)
            .atZone(zone)
            .toInstant()
            .toEpochMilli()
    }
}
