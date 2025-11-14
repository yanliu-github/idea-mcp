package com.ly.ideamcp.model.refactor

import com.ly.ideamcp.model.CodeChange

/**
 * 重命名响应
 * @property success 是否成功
 * @property oldName 原名称
 * @property newName 新名称
 * @property affectedFiles 受影响的文件数量
 * @property changes 代码变更列表
 * @property preview 是否为预览结果
 * @property conflicts 命名冲突列表（如果有）
 */
data class RenameResponse(
    val success: Boolean,
    val oldName: String,
    val newName: String,
    val affectedFiles: Int,
    val changes: List<CodeChange>,
    val preview: Boolean = false,
    val conflicts: List<String>? = null
)
