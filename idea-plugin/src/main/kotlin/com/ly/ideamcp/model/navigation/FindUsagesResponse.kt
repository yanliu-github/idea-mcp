package com.ly.ideamcp.model.navigation

import com.ly.ideamcp.model.CodeUsage

/**
 * 查找用途响应
 * @property symbolName 符号名称
 * @property totalCount 总用途数量
 * @property usages 用途列表
 * @property truncated 是否被截断（达到 maxResults 限制）
 */
data class FindUsagesResponse(
    val symbolName: String,
    val totalCount: Int,
    val usages: List<CodeUsage>,
    val truncated: Boolean = false
)
