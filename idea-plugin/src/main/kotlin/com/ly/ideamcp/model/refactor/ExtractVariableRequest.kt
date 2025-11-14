package com.ly.ideamcp.model.refactor

/**
 * 提取变量请求
 * @property filePath 文件路径
 * @property startOffset 起始偏移量
 * @property endOffset 结束偏移量
 * @property startLine 起始行号(与startOffset二选一)
 * @property startColumn 起始列号
 * @property endLine 结束行号
 * @property endColumn 结束列号
 * @property variableName 新变量名称
 * @property replaceAll 是否替换所有相同表达式(默认true)
 * @property declareFinal 是否声明为final(默认true)
 * @property preview 是否仅预览(默认false)
 */
data class ExtractVariableRequest(
    val filePath: String,
    val startOffset: Int? = null,
    val endOffset: Int? = null,
    val startLine: Int? = null,
    val startColumn: Int? = null,
    val endLine: Int? = null,
    val endColumn: Int? = null,
    val variableName: String,
    val replaceAll: Boolean = true,
    val declareFinal: Boolean = true,
    val preview: Boolean = false
) {
    init {
        require(
            (startOffset != null && endOffset != null) ||
            (startLine != null && startColumn != null && endLine != null && endColumn != null)
        ) {
            "Must provide either startOffset/endOffset or startLine/startColumn/endLine/endColumn"
        }

        require(variableName.isNotBlank()) {
            "Variable name cannot be blank"
        }
    }
}
