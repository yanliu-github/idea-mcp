package com.ly.ideamcp.model.refactor

import com.ly.ideamcp.model.CodeLocation

/**
 * 封装字段响应
 * @property success 是否成功
 * @property fieldName 字段名称
 * @property getterName getter方法名(如果生成)
 * @property setterName setter方法名(如果生成)
 * @property getterLocation getter位置
 * @property setterLocation setter位置
 * @property affectedFiles 受影响的文件列表
 * @property changes 代码变更列表(如果preview=true)
 * @property preview 是否为预览模式
 */
data class EncapsulateFieldResponse(
    val success: Boolean,
    val fieldName: String,
    val getterName: String? = null,
    val setterName: String? = null,
    val getterLocation: CodeLocation? = null,
    val setterLocation: CodeLocation? = null,
    val affectedFiles: List<FileChange>? = null,
    val changes: List<com.ly.ideamcp.model.CodeChange>? = null,
    val preview: Boolean = false
)
