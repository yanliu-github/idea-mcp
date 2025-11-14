package com.ly.ideamcp.model.codegen

/**
 * 生成 Getter/Setter 响应
 */
data class GenerateGettersSettersResponse(
    /** 是否成功 */
    val success: Boolean,

    /** 生成的方法列表 */
    val generatedMethods: List<GeneratedMethod>,

    /** 受影响的文件 */
    val affectedFile: String
)
