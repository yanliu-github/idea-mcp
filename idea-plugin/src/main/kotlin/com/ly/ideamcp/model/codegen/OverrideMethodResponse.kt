package com.ly.ideamcp.model.codegen

/**
 * 重写方法响应
 */
data class OverrideMethodResponse(
    /** 是否成功 */
    val success: Boolean,

    /** 重写的方法 */
    val method: GeneratedMethod,

    /** 受影响的文件 */
    val affectedFile: String
)
