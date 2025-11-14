package com.ly.ideamcp.service

import com.intellij.openapi.components.Service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.ly.ideamcp.model.CodeLocation
import com.ly.ideamcp.model.debug.*
import com.ly.ideamcp.util.PsiHelper
import com.ly.ideamcp.util.ThreadHelper
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * 调试服务
 * 提供断点管理和调试控制功能
 */
@Service(Service.Level.PROJECT)
class DebugService(private val project: Project) {

    private val logger = Logger.getInstance(DebugService::class.java)

    // 存储断点信息 (breakpointId -> BreakpointInfo)
    private val breakpoints = ConcurrentHashMap<String, BreakpointInfo>()

    // 存储调试会话信息 (sessionId -> SessionInfo)
    private val debugSessions = ConcurrentHashMap<String, DebugSessionInfo>()

    /**
     * 调试会话信息
     */
    private data class DebugSessionInfo(
        val sessionId: String,
        var status: String, // running, paused, stopped
        val mainClass: String,
        val filePath: String
    )

    /**
     * 设置断点
     * @param request 断点设置请求
     * @return 断点设置响应
     * @throws IllegalArgumentException 如果参数无效
     */
    fun setBreakpoint(request: BreakpointRequest): BreakpointResponse {
        logger.info("Setting breakpoint in file: ${request.filePath} at line: ${request.line}")

        return ThreadHelper.runReadAction {
            // 1. 验证文件存在
            val psiFile = PsiHelper.findPsiFile(project, request.filePath)
                ?: throw IllegalArgumentException("File not found: ${request.filePath}")

            val document = PsiHelper.getDocument(psiFile)
                ?: throw IllegalStateException("Cannot get document for file: ${request.filePath}")

            // 2. 验证行号有效
            if (request.line < 0 || request.line >= document.lineCount) {
                throw IllegalArgumentException("Invalid line number: ${request.line}")
            }

            // 3. 生成断点ID
            val breakpointId = UUID.randomUUID().toString()

            // 4. 创建断点位置
            val offset = document.getLineStartOffset(request.line)
            val location = CodeLocation(
                filePath = request.filePath,
                offset = offset,
                line = request.line,
                column = 0
            )

            // 5. 创建断点信息
            val breakpointInfo = BreakpointInfo(
                breakpointId = breakpointId,
                location = location,
                type = if (request.logMessage != null) "log" else "line",
                condition = request.condition,
                logMessage = request.logMessage,
                enabled = request.enabled,
                verified = false // 实际实现中需要在调试会话中验证
            )

            // 6. 存储断点
            breakpoints[breakpointId] = breakpointInfo

            logger.warn("Breakpoint set - Note: Full IDEA debugging API integration not yet implemented")
            logger.info("Breakpoint ID: $breakpointId, Type: ${breakpointInfo.type}")

            // 7. 返回响应
            BreakpointResponse(
                success = true,
                breakpointId = breakpointId,
                location = location,
                type = breakpointInfo.type,
                condition = request.condition,
                enabled = request.enabled
            )
        }
    }

    /**
     * 列出所有断点
     * @return 断点列表响应
     */
    fun listBreakpoints(): ListBreakpointsResponse {
        logger.info("Listing all breakpoints")

        return ThreadHelper.runReadAction {
            val breakpointList = breakpoints.values.toList()
            logger.info("Found ${breakpointList.size} breakpoints")

            ListBreakpointsResponse(
                success = true,
                breakpoints = breakpointList
            )
        }
    }

    /**
     * 删除断点
     * @param breakpointId 断点ID
     * @return 删除断点响应
     * @throws IllegalArgumentException 如果断点不存在
     */
    fun removeBreakpoint(breakpointId: String): RemoveBreakpointResponse {
        logger.info("Removing breakpoint: $breakpointId")

        return ThreadHelper.runReadAction {
            val breakpoint = breakpoints.remove(breakpointId)
                ?: throw IllegalArgumentException("Breakpoint not found: $breakpointId")

            logger.warn("Breakpoint removed - Note: Full IDEA debugging API integration not yet implemented")
            logger.info("Removed breakpoint at: ${breakpoint.location.filePath}:${breakpoint.location.line}")

            RemoveBreakpointResponse(
                success = true,
                breakpointId = breakpointId,
                message = "Breakpoint removed successfully"
            )
        }
    }

    // ========== Phase 3.2: 调试会话管理 ==========

    /**
     * 启动调试会话
     * @param request 调试会话启动请求
     * @return 调试会话响应
     */
    fun startDebugSession(request: DebugSessionRequest): DebugSessionResponse {
        logger.info("Starting debug session for: ${request.mainClass}")

        return ThreadHelper.runReadAction {
            // 1. 验证文件存在
            val psiFile = PsiHelper.findPsiFile(project, request.filePath)
                ?: throw IllegalArgumentException("File not found: ${request.filePath}")

            // 2. 生成会话ID
            val sessionId = UUID.randomUUID().toString()

            // 3. 创建会话信息
            val sessionInfo = DebugSessionInfo(
                sessionId = sessionId,
                status = "running",
                mainClass = request.mainClass,
                filePath = request.filePath
            )

            // 4. 存储会话
            debugSessions[sessionId] = sessionInfo

            logger.warn("Debug session start - placeholder implementation")
            logger.info("Session ID: $sessionId, Main class: ${request.mainClass}")

            // 5. 返回响应
            DebugSessionResponse(
                success = true,
                sessionId = sessionId,
                status = "running",
                breakpoints = breakpoints.values.toList()
            )
        }
    }

    /**
     * 停止调试会话
     * @param sessionId 会话ID
     * @return 调试控制响应
     */
    fun stopDebugSession(sessionId: String): DebugControlResponse {
        logger.info("Stopping debug session: $sessionId")

        return ThreadHelper.runReadAction {
            val session = debugSessions[sessionId]
                ?: throw IllegalArgumentException("Debug session not found: $sessionId")

            session.status = "stopped"
            debugSessions.remove(sessionId)

            logger.warn("Debug session stop - placeholder implementation")
            logger.info("Session stopped: $sessionId")

            DebugControlResponse(
                success = true,
                sessionId = sessionId,
                status = "stopped",
                currentLocation = null
            )
        }
    }

    /**
     * 暂停调试会话
     * @param sessionId 会话ID
     * @return 调试控制响应
     */
    fun pauseDebugSession(sessionId: String): DebugControlResponse {
        logger.info("Pausing debug session: $sessionId")

        return ThreadHelper.runReadAction {
            val session = debugSessions[sessionId]
                ?: throw IllegalArgumentException("Debug session not found: $sessionId")

            session.status = "paused"

            logger.warn("Debug session pause - placeholder implementation")
            logger.info("Session paused: $sessionId")

            // 模拟当前位置
            val mockLocation = CodeLocation(
                filePath = session.filePath,
                offset = 0,
                line = 10,
                column = 0
            )

            DebugControlResponse(
                success = true,
                sessionId = sessionId,
                status = "paused",
                currentLocation = mockLocation
            )
        }
    }

    /**
     * 继续调试会话
     * @param sessionId 会话ID
     * @return 调试控制响应
     */
    fun resumeDebugSession(sessionId: String): DebugControlResponse {
        logger.info("Resuming debug session: $sessionId")

        return ThreadHelper.runReadAction {
            val session = debugSessions[sessionId]
                ?: throw IllegalArgumentException("Debug session not found: $sessionId")

            session.status = "running"

            logger.warn("Debug session resume - placeholder implementation")
            logger.info("Session resumed: $sessionId")

            DebugControlResponse(
                success = true,
                sessionId = sessionId,
                status = "running",
                currentLocation = null
            )
        }
    }

    /**
     * 步过（Step Over）
     * @param sessionId 会话ID
     * @return 调试控制响应
     */
    fun stepOver(sessionId: String): DebugControlResponse {
        logger.info("Step over in session: $sessionId")

        return ThreadHelper.runReadAction {
            val session = debugSessions[sessionId]
                ?: throw IllegalArgumentException("Debug session not found: $sessionId")

            session.status = "paused"

            logger.warn("Debug step over - placeholder implementation")
            logger.info("Stepped over in session: $sessionId")

            // 模拟步进后的位置
            val mockLocation = CodeLocation(
                filePath = session.filePath,
                offset = 0,
                line = 11,
                column = 0
            )

            DebugControlResponse(
                success = true,
                sessionId = sessionId,
                status = "paused",
                currentLocation = mockLocation
            )
        }
    }

    /**
     * 步入（Step Into）
     * @param sessionId 会话ID
     * @return 调试控制响应
     */
    fun stepInto(sessionId: String): DebugControlResponse {
        logger.info("Step into in session: $sessionId")

        return ThreadHelper.runReadAction {
            val session = debugSessions[sessionId]
                ?: throw IllegalArgumentException("Debug session not found: $sessionId")

            session.status = "paused"

            logger.warn("Debug step into - placeholder implementation")
            logger.info("Stepped into in session: $sessionId")

            // 模拟步入后的位置
            val mockLocation = CodeLocation(
                filePath = session.filePath,
                offset = 0,
                line = 12,
                column = 0
            )

            DebugControlResponse(
                success = true,
                sessionId = sessionId,
                status = "paused",
                currentLocation = mockLocation
            )
        }
    }

    /**
     * 步出（Step Out）
     * @param sessionId 会话ID
     * @return 调试控制响应
     */
    fun stepOut(sessionId: String): DebugControlResponse {
        logger.info("Step out in session: $sessionId")

        return ThreadHelper.runReadAction {
            val session = debugSessions[sessionId]
                ?: throw IllegalArgumentException("Debug session not found: $sessionId")

            session.status = "paused"

            logger.warn("Debug step out - placeholder implementation")
            logger.info("Stepped out in session: $sessionId")

            // 模拟步出后的位置
            val mockLocation = CodeLocation(
                filePath = session.filePath,
                offset = 0,
                line = 13,
                column = 0
            )

            DebugControlResponse(
                success = true,
                sessionId = sessionId,
                status = "paused",
                currentLocation = mockLocation
            )
        }
    }

    // ========== Phase 3.3: 表达式和变量 ==========

    /**
     * 计算表达式
     * @param request 表达式求值请求
     * @return 表达式求值响应
     */
    fun evaluateExpression(request: EvaluateExpressionRequest): EvaluateExpressionResponse {
        logger.info("Evaluating expression in session: ${request.sessionId}")

        return ThreadHelper.runReadAction {
            val session = debugSessions[request.sessionId]
                ?: throw IllegalArgumentException("Debug session not found: ${request.sessionId}")

            if (session.status != "paused") {
                throw IllegalStateException("Debug session must be paused to evaluate expressions")
            }

            logger.warn("Expression evaluation - placeholder implementation")
            logger.info("Expression: ${request.expression}, Frame: ${request.frameIndex ?: 0}")

            // 模拟求值结果
            EvaluateExpressionResponse(
                success = true,
                value = "42",
                type = "int",
                error = null
            )
        }
    }

    /**
     * 获取当前作用域变量
     * @param request 获取变量请求
     * @return 获取变量响应
     */
    fun getVariables(request: GetVariablesRequest): GetVariablesResponse {
        logger.info("Getting variables in session: ${request.sessionId}")

        return ThreadHelper.runReadAction {
            val session = debugSessions[request.sessionId]
                ?: throw IllegalArgumentException("Debug session not found: ${request.sessionId}")

            if (session.status != "paused") {
                throw IllegalStateException("Debug session must be paused to get variables")
            }

            logger.warn("Get variables - placeholder implementation")
            logger.info("Scope: ${request.scope ?: "local"}, Frame: ${request.frameIndex ?: 0}")

            // 模拟变量列表
            val mockVariables = listOf(
                VariableInfo(
                    name = "count",
                    value = "10",
                    type = "int",
                    scope = "local",
                    children = null
                ),
                VariableInfo(
                    name = "message",
                    value = "\"Hello World\"",
                    type = "String",
                    scope = "local",
                    children = null
                ),
                VariableInfo(
                    name = "this",
                    value = "MainClass@12345",
                    type = "MainClass",
                    scope = "instance",
                    children = listOf(
                        VariableInfo(
                            name = "field1",
                            value = "100",
                            type = "int",
                            scope = "instance",
                            children = null
                        )
                    )
                )
            )

            GetVariablesResponse(
                success = true,
                variables = mockVariables
            )
        }
    }

    /**
     * 获取指定变量详情
     * @param sessionId 会话ID
     * @param variableName 变量名
     * @return 变量信息
     */
    fun getVariable(sessionId: String, variableName: String): VariableInfo {
        logger.info("Getting variable '$variableName' in session: $sessionId")

        return ThreadHelper.runReadAction {
            val session = debugSessions[sessionId]
                ?: throw IllegalArgumentException("Debug session not found: $sessionId")

            if (session.status != "paused") {
                throw IllegalStateException("Debug session must be paused to get variable")
            }

            logger.warn("Get variable - placeholder implementation")
            logger.info("Variable name: $variableName")

            // 模拟变量详情
            VariableInfo(
                name = variableName,
                value = "42",
                type = "int",
                scope = "local",
                children = null
            )
        }
    }

    // ========== Phase 3.4: 调用栈 ==========

    /**
     * 获取调用栈
     * @param request 调用栈请求
     * @return 调用栈响应
     */
    fun getCallStack(request: GetCallStackRequest): GetCallStackResponse {
        logger.info("Getting call stack for session: ${request.sessionId}")

        return ThreadHelper.runReadAction {
            val session = debugSessions[request.sessionId]
                ?: throw IllegalArgumentException("Debug session not found: ${request.sessionId}")

            if (session.status != "paused") {
                throw IllegalStateException("Debug session must be paused to get call stack")
            }

            logger.warn("Get call stack - placeholder implementation")
            logger.info("Session ID: ${request.sessionId}")

            // 模拟调用栈
            val mockFrames = listOf(
                StackFrame(
                    frameIndex = 0,
                    methodName = "main",
                    className = session.mainClass,
                    location = CodeLocation(
                        filePath = session.filePath,
                        offset = 0,
                        line = 10,
                        column = 0
                    ),
                    variables = listOf(
                        VariableInfo(
                            name = "args",
                            value = "String[0]",
                            type = "String[]",
                            scope = "local",
                            children = null
                        )
                    )
                ),
                StackFrame(
                    frameIndex = 1,
                    methodName = "processData",
                    className = session.mainClass,
                    location = CodeLocation(
                        filePath = session.filePath,
                        offset = 0,
                        line = 25,
                        column = 0
                    ),
                    variables = listOf(
                        VariableInfo(
                            name = "data",
                            value = "42",
                            type = "int",
                            scope = "local",
                            children = null
                        )
                    )
                )
            )

            GetCallStackResponse(
                success = true,
                frames = mockFrames
            )
        }
    }

    companion object {
        /**
         * 获取服务实例
         */
        fun getInstance(project: Project): DebugService {
            return project.getService(DebugService::class.java)
        }
    }
}
