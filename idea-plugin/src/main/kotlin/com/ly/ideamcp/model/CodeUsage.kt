package com.ly.ideamcp.model

/**
 * 代码用途信息
 * @property location 用途位置
 * @property context 上下文代码（包含用途的代码片段）
 * @property type 用途类型
 */
data class CodeUsage(
    val location: CodeLocation,
    val context: String,
    val type: UsageType
)

/**
 * 用途类型
 */
enum class UsageType {
    READ,           // 读取
    WRITE,          // 写入
    METHOD_CALL,    // 方法调用
    CLASS_USAGE,    // 类使用
    IMPORT,         // 导入
    INHERITANCE,    // 继承
    IMPLEMENTATION, // 实现
    ANNOTATION,     // 注解
    OTHER           // 其他
}
