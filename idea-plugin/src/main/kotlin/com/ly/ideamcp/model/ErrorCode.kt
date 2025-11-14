package com.ly.ideamcp.model

/**
 * 标准错误码枚举
 * 根据需求文档定义的26个错误码
 */
enum class ErrorCode(val code: String, val defaultMessage: String) {
    // 4xx 客户端错误 (18个)
    FILE_NOT_FOUND("FILE_NOT_FOUND", "指定文件不存在"),
    INVALID_OFFSET("INVALID_OFFSET", "文件偏移量无效"),
    INVALID_RANGE("INVALID_RANGE", "文本范围无效"),
    ELEMENT_NOT_FOUND("ELEMENT_NOT_FOUND", "指定元素未找到"),
    NAMING_CONFLICT("NAMING_CONFLICT", "命名冲突"),
    INVALID_NAME("INVALID_NAME", "名称无效"),
    INVALID_PARAMETER("INVALID_PARAMETER", "参数无效"),
    MISSING_PARAMETER("MISSING_PARAMETER", "缺少必需参数"),
    UNSUPPORTED_LANGUAGE("UNSUPPORTED_LANGUAGE", "不支持的语言类型"),
    UNSUPPORTED_OPERATION("UNSUPPORTED_OPERATION", "不支持的操作"),
    NO_ACTIVE_PROJECT("NO_ACTIVE_PROJECT", "没有活动项目"),
    BREAKPOINT_NOT_FOUND("BREAKPOINT_NOT_FOUND", "断点未找到"),
    NO_DEBUG_SESSION("NO_DEBUG_SESSION", "没有调试会话"),
    INVALID_EXPRESSION("INVALID_EXPRESSION", "表达式无效"),
    VARIABLE_NOT_FOUND("VARIABLE_NOT_FOUND", "变量未找到"),
    INSPECTION_NOT_FOUND("INSPECTION_NOT_FOUND", "检查项未找到"),
    DEPENDENCY_CONFLICT("DEPENDENCY_CONFLICT", "依赖冲突"),
    VALIDATION_ERROR("VALIDATION_ERROR", "验证失败"),

    // 5xx 服务端错误 (10个)
    INDEX_NOT_READY("INDEX_NOT_READY", "索引未就绪"),
    REFACTORING_FAILED("REFACTORING_FAILED", "重构操作失败"),
    SEARCH_FAILED("SEARCH_FAILED", "搜索操作失败"),
    GENERATION_FAILED("GENERATION_FAILED", "代码生成失败"),
    DEBUG_ERROR("DEBUG_ERROR", "调试错误"),
    BUILD_FAILED("BUILD_FAILED", "构建失败"),
    TEST_FAILED("TEST_FAILED", "测试失败"),
    VCS_ERROR("VCS_ERROR", "版本控制错误"),
    TIMEOUT("TIMEOUT", "操作超时"),
    INTERNAL_ERROR("INTERNAL_ERROR", "内部服务器错误"),

    // 其他
    UNKNOWN_ERROR("UNKNOWN_ERROR", "未知错误");

    companion object {
        /**
         * 根据错误码字符串获取枚举值
         */
        fun fromCode(code: String): ErrorCode {
            return values().find { it.code == code } ?: UNKNOWN_ERROR
        }
    }
}
