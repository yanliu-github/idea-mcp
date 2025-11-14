package com.ly.ideamcp.model.refactor

import com.ly.ideamcp.model.CodeLocation

/**
 * 内联变量响应
 * @property success 是否成功
 * @property variableName 变量名称
 * @property inlinedExpression 内联的表达式
 * @property replacementCount 替换次数
 * @property affectedFiles 受影响的文件列表
 * @property changes 代码变更列表(如果preview=true)
 * @property preview 是否为预览模式
 */
data class InlineVariableResponse(
    val success: Boolean,
    val variableName: String,
    val inlinedExpression: String? = null,
    val replacementCount: Int = 0,
    val affectedFiles: List<FileChange>? = null,
    val changes: List<com.ly.ideamcp.model.CodeChange>? = null,
    val preview: Boolean = false
)
