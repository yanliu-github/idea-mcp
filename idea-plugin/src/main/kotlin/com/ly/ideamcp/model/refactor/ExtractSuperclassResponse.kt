package com.ly.ideamcp.model.refactor

import com.ly.ideamcp.model.CodeLocation

/**
 * 提取超类响应
 * @property success 是否成功
 * @property superclassName 超类名称
 * @property superclassLocation 超类位置
 * @property extractedMembers 提取的成员列表
 * @property affectedFiles 受影响的文件列表
 * @property changes 代码变更列表(如果preview=true)
 * @property preview 是否为预览模式
 */
data class ExtractSuperclassResponse(
    val success: Boolean,
    val superclassName: String,
    val superclassLocation: CodeLocation? = null,
    val extractedMembers: List<String>? = null,
    val affectedFiles: List<FileChange>? = null,
    val changes: List<com.ly.ideamcp.model.CodeChange>? = null,
    val preview: Boolean = false
)
