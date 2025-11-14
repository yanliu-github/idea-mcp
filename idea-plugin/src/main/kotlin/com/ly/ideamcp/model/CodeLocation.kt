package com.ly.ideamcp.model

/**
 * 代码位置信息
 * @property filePath 文件路径（相对于项目根目录）
 * @property offset 文件偏移量（从0开始）
 * @property line 行号（从1开始，可选）
 * @property column 列号（从1开始，可选）
 */
data class CodeLocation(
    val filePath: String,
    val offset: Int,
    val line: Int? = null,
    val column: Int? = null
) {
    init {
        require(offset >= 0) { "Offset must be non-negative" }
        line?.let { require(it > 0) { "Line must be positive" } }
        column?.let { require(it > 0) { "Column must be positive" } }
    }
}

/**
 * 代码范围
 * @property filePath 文件路径
 * @property startOffset 起始偏移量
 * @property endOffset 结束偏移量
 * @property startLine 起始行号（可选）
 * @property startColumn 起始列号（可选）
 * @property endLine 结束行号（可选）
 * @property endColumn 结束列号（可选）
 */
data class CodeRange(
    val filePath: String,
    val startOffset: Int,
    val endOffset: Int,
    val startLine: Int? = null,
    val startColumn: Int? = null,
    val endLine: Int? = null,
    val endColumn: Int? = null
) {
    init {
        require(startOffset >= 0) { "Start offset must be non-negative" }
        require(endOffset >= startOffset) { "End offset must be >= start offset" }
        startLine?.let { require(it > 0) { "Start line must be positive" } }
        endLine?.let {
            require(it > 0) { "End line must be positive" }
            startLine?.let { start -> require(it >= start) { "End line must be >= start line" } }
        }
    }
}
