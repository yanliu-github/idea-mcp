package com.ly.ideamcp.model.refactor

/**
 * 改变签名请求
 * @property filePath 文件路径
 * @property offset 方法/函数的偏移量
 * @property line 方法所在行号(与offset二选一)
 * @property column 方法所在列号
 * @property newName 新方法名(可选,如果不改名则为null)
 * @property newReturnType 新返回类型(可选)
 * @property parameters 新参数列表
 * @property newVisibility 新可见性(可选)
 * @property preview 是否仅预览(默认false)
 */
data class ChangeSignatureRequest(
    val filePath: String,
    val offset: Int? = null,
    val line: Int? = null,
    val column: Int? = null,
    val newName: String? = null,
    val newReturnType: String? = null,
    val parameters: List<ParameterInfo>? = null,
    val newVisibility: String? = null,
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
