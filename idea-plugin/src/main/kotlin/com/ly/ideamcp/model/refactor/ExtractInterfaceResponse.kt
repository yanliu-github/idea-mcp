package com.ly.ideamcp.model.refactor

import com.ly.ideamcp.model.CodeLocation

/**
 * 提取接口响应
 * @property success 是否成功
 * @property interfaceName 接口名称
 * @property interfaceLocation 接口位置
 * @property extractedMembers 提取的成员列表
 * @property affectedFiles 受影响的文件列表
 * @property changes 代码变更列表(如果preview=true)
 * @property preview 是否为预览模式
 */
data class ExtractInterfaceResponse(
    val success: Boolean,
    val interfaceName: String,
    val interfaceLocation: CodeLocation? = null,
    val extractedMembers: List<String>? = null,
    val affectedFiles: List<FileChange>? = null,
    val changes: List<com.ly.ideamcp.model.CodeChange>? = null,
    val preview: Boolean = false
)
