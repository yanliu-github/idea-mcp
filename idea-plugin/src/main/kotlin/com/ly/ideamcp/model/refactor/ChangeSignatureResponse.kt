package com.ly.ideamcp.model.refactor

/**
 * 改变签名响应
 * @property success 是否成功
 * @property methodName 方法名
 * @property oldSignature 旧签名
 * @property newSignature 新签名
 * @property affectedFiles 受影响的文件列表
 * @property changes 代码变更列表(如果preview=true)
 * @property preview 是否为预览模式
 */
data class ChangeSignatureResponse(
    val success: Boolean,
    val methodName: String,
    val oldSignature: String? = null,
    val newSignature: String? = null,
    val affectedFiles: List<FileChange>? = null,
    val changes: List<com.ly.ideamcp.model.CodeChange>? = null,
    val preview: Boolean = false
)
