package com.ly.ideamcp.model.navigation

import com.ly.ideamcp.model.CodeLocation

/**
 * 跳转到定义响应
 * @property symbolName 符号名称
 * @property definition 定义位置（如果找到）
 * @property found 是否找到定义
 * @property kind 符号类型（class, method, field, variable等）
 */
data class GotoDefinitionResponse(
    val symbolName: String?,
    val definition: CodeLocation?,
    val found: Boolean,
    val kind: String? = null
)
