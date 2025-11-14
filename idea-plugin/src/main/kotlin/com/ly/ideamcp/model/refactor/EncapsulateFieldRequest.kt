package com.ly.ideamcp.model.refactor

/**
 * 封装字段请求
 * @property filePath 文件路径
 * @property offset 字段的偏移量
 * @property line 字段所在行号(与offset二选一)
 * @property column 字段所在列号
 * @property generateGetter 是否生成getter(默认true)
 * @property generateSetter 是否生成setter(默认true)
 * @property getterVisibility getter可见性(默认public)
 * @property setterVisibility setter可见性(默认public)
 * @property usePropertyAnnotation 是否使用属性注解(默认false)
 * @property preview 是否仅预览(默认false)
 */
data class EncapsulateFieldRequest(
    val filePath: String,
    val offset: Int? = null,
    val line: Int? = null,
    val column: Int? = null,
    val generateGetter: Boolean = true,
    val generateSetter: Boolean = true,
    val getterVisibility: String = "public",
    val setterVisibility: String = "public",
    val usePropertyAnnotation: Boolean = false,
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
