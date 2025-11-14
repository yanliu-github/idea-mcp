package com.ly.ideamcp.model.debug

/**
 * 变量信息
 * @property name 变量名
 * @property value 变量值(字符串表示)
 * @property type 变量类型
 * @property scope 作用域(local, instance, static)
 * @property children 子变量(对于对象或数组)
 */
data class VariableInfo(
    val name: String,
    val value: String,
    val type: String,
    val scope: String,
    val children: List<VariableInfo>? = null
)
