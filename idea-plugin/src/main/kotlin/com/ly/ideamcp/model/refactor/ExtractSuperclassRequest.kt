package com.ly.ideamcp.model.refactor

/**
 * 提取超类请求
 * @property filePath 文件路径
 * @property offset 类的偏移量
 * @property line 类所在行号(与offset二选一)
 * @property column 类所在列号
 * @property superclassName 新超类名称
 * @property targetPackage 目标包名(可选)
 * @property members 要提取的成员列表(字段和方法名)
 * @property preview 是否仅预览(默认false)
 */
data class ExtractSuperclassRequest(
    val filePath: String,
    val offset: Int? = null,
    val line: Int? = null,
    val column: Int? = null,
    val superclassName: String,
    val targetPackage: String? = null,
    val members: List<String>? = null,
    val preview: Boolean = false
) {
    init {
        require(offset != null || (line != null && column != null)) {
            "Must provide either offset or line/column"
        }

        require(filePath.isNotBlank()) {
            "File path cannot be blank"
        }

        require(superclassName.isNotBlank()) {
            "Superclass name cannot be blank"
        }
    }
}
