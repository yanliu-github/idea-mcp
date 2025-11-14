package com.ly.ideamcp.model.analysis

/**
 * 代码检查请求
 * @property filePath 文件路径（可选，如果为空则检查整个项目）
 * @property severity 最小严重程度（ERROR, WARNING, WEAK_WARNING, INFO）
 * @property includeQuickFixes 是否包含快速修复建议
 * @property maxResults 最大结果数量（0表示不限制）
 */
data class InspectionRequest(
    val filePath: String? = null,
    val severity: String? = "WARNING",
    val includeQuickFixes: Boolean = true,
    val maxResults: Int = 0
) {
    init {
        require(maxResults >= 0) {
            "maxResults must be non-negative"
        }
    }
}
