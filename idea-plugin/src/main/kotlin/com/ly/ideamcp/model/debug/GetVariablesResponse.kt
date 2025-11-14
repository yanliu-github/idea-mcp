package com.ly.ideamcp.model.debug

/**
 * 获取变量响应
 * @property success 是否成功
 * @property variables 变量列表
 */
data class GetVariablesResponse(
    val success: Boolean,
    val variables: List<VariableInfo>
)
