package com.ly.ideamcp.model.debug

import com.ly.ideamcp.model.CodeLocation

/**
 * 栈帧信息
 * @property frameIndex 栈帧索引(0为当前栈帧)
 * @property methodName 方法名
 * @property className 类名(全限定名)
 * @property location 源代码位置
 * @property variables 当前栈帧的局部变量(可选)
 */
data class StackFrame(
    val frameIndex: Int,
    val methodName: String,
    val className: String,
    val location: CodeLocation,
    val variables: List<VariableInfo>? = null
)
