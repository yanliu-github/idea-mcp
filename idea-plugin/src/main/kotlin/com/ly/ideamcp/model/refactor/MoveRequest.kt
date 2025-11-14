package com.ly.ideamcp.model.refactor

/**
 * 移动类/方法请求
 * @property filePath 源文件路径
 * @property offset 要移动的元素偏移量
 * @property line 元素所在行号(与offset二选一)
 * @property column 元素所在列号
 * @property targetPath 目标路径(包名或文件路径)
 * @property searchInComments 是否搜索注释(默认false)
 * @property searchInStrings 是否搜索字符串(默认false)
 * @property preview 是否仅预览(默认false)
 */
data class MoveRequest(
    val filePath: String,
    val offset: Int? = null,
    val line: Int? = null,
    val column: Int? = null,
    val targetPath: String,
    val searchInComments: Boolean = false,
    val searchInStrings: Boolean = false,
    val preview: Boolean = false
) {
    init {
        require(offset != null || (line != null && column != null)) {
            "Must provide either offset or line/column"
        }

        require(filePath.isNotBlank()) {
            "File path cannot be blank"
        }

        require(targetPath.isNotBlank()) {
            "Target path cannot be blank"
        }
    }
}
