package com.ly.ideamcp.model.refactor

import com.ly.ideamcp.model.CodeChange
import com.ly.ideamcp.model.CodeLocation

/**
 * 提取方法响应
 * @property success 是否成功
 * @property methodName 新方法名称
 * @property methodSignature 方法签名(如: private int calculateSum(int a, int b))
 * @property extractedRange 提取的代码范围
 * @property methodLocation 新方法插入位置
 * @property affectedFiles 受影响的文件列表
 * @property changes 代码变更列表(如果preview=true)
 * @property preview 是否为预览模式
 * @property parameters 方法参数列表
 * @property returnType 返回类型(如果有)
 * @property conflicts 冲突信息(如果有)
 */
data class ExtractMethodResponse(
    val success: Boolean,
    val methodName: String,
    val methodSignature: String? = null,
    val extractedRange: CodeRange? = null,
    val methodLocation: CodeLocation? = null,
    val affectedFiles: List<FileChange>? = null,
    val changes: List<CodeChange>? = null,
    val preview: Boolean = false,
    val parameters: List<ParameterInfo>? = null,
    val returnType: String? = null,
    val conflicts: List<ConflictInfo>? = null
)

/**
 * 代码范围
 * @property startOffset 起始偏移量
 * @property endOffset 结束偏移量
 * @property startLine 起始行号
 * @property startColumn 起始列号
 * @property endLine 结束行号
 * @property endColumn 结束列号
 */
data class CodeRange(
    val startOffset: Int,
    val endOffset: Int,
    val startLine: Int? = null,
    val startColumn: Int? = null,
    val endLine: Int? = null,
    val endColumn: Int? = null
)

/**
 * 方法参数信息
 * @property name 参数名称
 * @property type 参数类型
 * @property index 参数索引
 */
data class ParameterInfo(
    val name: String,
    val type: String,
    val index: Int
)

/**
 * 冲突信息
 * @property type 冲突类型
 * @property message 冲突描述
 * @property location 冲突位置
 */
data class ConflictInfo(
    val type: String,
    val message: String,
    val location: CodeLocation? = null
)

/**
 * 文件变更
 * @property filePath 文件路径
 * @property changes 变更列表
 */
data class FileChange(
    val filePath: String,
    val changes: List<CodeChange>
)
