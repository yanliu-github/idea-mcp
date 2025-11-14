package com.ly.ideamcp.model.refactor

/**
 * 内联变量请求
 * @property filePath 文件路径
 * @property offset 变量声明的偏移量
 * @property line 变量所在行号(与offset二选一)
 * @property column 变量所在列号
 * @property inlineAll 是否内联所有引用(默认true)
 * @property preview 是否仅预览(默认false)
 */
data class InlineVariableRequest(
    val filePath: String,
    val offset: Int? = null,
    val line: Int? = null,
    val column: Int? = null,
    val inlineAll: Boolean = true,
    val preview: Boolean = false
) {
    init {
        require(offset != null || (line != null && column != null)) {
            "Must provide either offset or line/column"
        }

        require(filePath.isNotBlank()) {
            "File path cannot be blank"
        }
    }
}
