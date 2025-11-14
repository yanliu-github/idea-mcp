package com.ly.ideamcp.model.refactor

/**
 * 提取方法请求
 * @property filePath 文件路径
 * @property startOffset 起始偏移量
 * @property endOffset 结束偏移量
 * @property startLine 起始行号(与startOffset二选一)
 * @property startColumn 起始列号(与startLine配合使用)
 * @property endLine 结束行号(与endOffset二选一)
 * @property endColumn 结束列号(与endLine配合使用)
 * @property methodName 新方法名称
 * @property visibility 可见性(public/protected/private/package,默认private)
 * @property isStatic 是否静态方法(默认false)
 * @property preview 是否仅预览(默认false)
 */
data class ExtractMethodRequest(
    val filePath: String,
    val startOffset: Int? = null,
    val endOffset: Int? = null,
    val startLine: Int? = null,
    val startColumn: Int? = null,
    val endLine: Int? = null,
    val endColumn: Int? = null,
    val methodName: String,
    val visibility: String = "private",
    val isStatic: Boolean = false,
    val preview: Boolean = false
) {
    init {
        // 验证：必须提供 offset 或 line/column
        require(
            (startOffset != null && endOffset != null) ||
            (startLine != null && startColumn != null && endLine != null && endColumn != null)
        ) {
            "Must provide either startOffset/endOffset or startLine/startColumn/endLine/endColumn"
        }

        // 验证方法名不能为空
        require(methodName.isNotBlank()) {
            "Method name cannot be blank"
        }

        // 验证可见性
        require(visibility in listOf("public", "protected", "private", "package")) {
            "Visibility must be one of: public, protected, private, package"
        }

        // 验证范围
        if (startOffset != null && endOffset != null) {
            require(startOffset >= 0) { "Start offset must be non-negative" }
            require(endOffset > startOffset) { "End offset must be greater than start offset" }
        }
        if (startLine != null && endLine != null) {
            require(startLine >= 0) { "Start line must be non-negative" }
            require(endLine >= startLine) { "End line must be >= start line" }
        }
    }
}
