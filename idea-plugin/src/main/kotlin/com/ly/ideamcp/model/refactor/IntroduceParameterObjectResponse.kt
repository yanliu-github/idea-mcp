package com.ly.ideamcp.model.refactor

import com.ly.ideamcp.model.CodeLocation

/**
 * 引入参数对象响应
 * @property success 是否成功
 * @property className 参数对象类名
 * @property classLocation 类位置
 * @property methodName 方法名
 * @property oldSignature 旧签名
 * @property newSignature 新签名
 * @property affectedFiles 受影响的文件列表
 * @property changes 代码变更列表(如果preview=true)
 * @property preview 是否为预览模式
 */
data class IntroduceParameterObjectResponse(
    val success: Boolean,
    val className: String,
    val classLocation: CodeLocation? = null,
    val methodName: String? = null,
    val oldSignature: String? = null,
    val newSignature: String? = null,
    val affectedFiles: List<FileChange>? = null,
    val changes: List<com.ly.ideamcp.model.CodeChange>? = null,
    val preview: Boolean = false
)
