package com.ly.ideamcp.model

/**
 * 代码变更信息
 * @property filePath 文件路径
 * @property modifications 修改列表
 * @property originalContent 原始内容（可选）
 * @property newContent 新内容（可选）
 */
data class CodeChange(
    val filePath: String,
    val modifications: List<Modification>,
    val originalContent: String? = null,
    val newContent: String? = null
)

/**
 * 单个修改
 * @property type 修改类型
 * @property range 修改范围
 * @property oldText 旧文本
 * @property newText 新文本
 * @property description 修改描述
 */
data class Modification(
    val type: ModificationType,
    val range: CodeRange,
    val oldText: String,
    val newText: String,
    val description: String? = null
)

/**
 * 修改类型
 */
enum class ModificationType {
    RENAME,         // 重命名
    REPLACE,        // 替换
    INSERT,         // 插入
    DELETE,         // 删除
    MOVE,           // 移动
    EXTRACT,        // 提取
    INLINE,         // 内联
    OTHER           // 其他
}
