package com.ly.ideamcp.model.analysis

import com.ly.ideamcp.model.CodeLocation

/**
 * 代码检查响应
 * @property totalCount 总问题数量
 * @property problems 问题列表
 * @property truncated 是否被截断
 */
data class InspectionResponse(
    val totalCount: Int,
    val problems: List<ProblemInfo>,
    val truncated: Boolean = false
)

/**
 * 问题信息
 * @property location 问题位置
 * @property severity 严重程度
 * @property message 问题描述
 * @property inspectionName 检查名称
 * @property quickFixes 快速修复列表（如果有）
 */
data class ProblemInfo(
    val location: CodeLocation,
    val severity: String,
    val message: String,
    val inspectionName: String,
    val quickFixes: List<QuickFixInfo>? = null
)

/**
 * 快速修复信息
 * @property name 修复名称
 * @property familyName 修复族名称
 */
data class QuickFixInfo(
    val name: String,
    val familyName: String
)
