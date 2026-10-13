package com.yudemeng.app.data

/**
 * 一条笔记。
 *
 * @param expiresAt 绝对过期时刻（epoch millis）；null = 永久保存
 * @param trashedAt 进入回收站的时刻；null = 仍在主列表
 */
data class Note(
    val id: Long,
    val content: String,
    val createdAt: Long,
    val updatedAt: Long,
    val policy: ExpiryPolicy,
    val expiresAt: Long?,
    val trashedAt: Long?,
    val tags: List<Tag> = emptyList(),
)

data class Tag(
    val id: Long,
    val name: String,
)

/** 标签 + 其下活动笔记数量，用于标签页与筛选条展示。 */
data class TagCount(
    val id: Long,
    val name: String,
    val count: Int,
)
