package com.ly.ideamcp.model.codegen

/**
 * 生成构造函数响应
 */
data class GenerateConstructorResponse(
    /** 是否成功 */
    val success: Boolean,

    /** 生成的构造函数 */
    val constructor: GeneratedMethod,

    /** 受影响的文件 */
    val affectedFile: String
)
