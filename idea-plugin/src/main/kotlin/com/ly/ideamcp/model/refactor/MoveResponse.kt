package com.ly.ideamcp.model.refactor

import com.ly.ideamcp.model.CodeLocation

/**
 * 移动类/方法响应
 * @property success 是否成功
 * @property elementName 元素名称
 * @property sourceLocation 源位置
 * @property targetLocation 目标位置
 * @property affectedFiles 受影响的文件列表
 * @property changes 代码变更列表(如果preview=true)
 * @property preview 是否为预览模式
 */
data class MoveResponse(
    val success: Boolean,
    val elementName: String,
    val sourceLocation: CodeLocation? = null,
    val targetLocation: CodeLocation? = null,
    val affectedFiles: List<FileChange>? = null,
    val changes: List<com.ly.ideamcp.model.CodeChange>? = null,
    val preview: Boolean = false
)
