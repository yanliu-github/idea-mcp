package com.ly.ideamcp.model.codegen

/**
 * 生成方法响应
 */
data class GenerateMethodResponse(
    /** 是否成功 */
    val success: Boolean,

    /** 生成的方法 */
    val method: GeneratedMethod,

    /** 受影响的文件 */
    val affectedFile: String
)
