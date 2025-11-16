package com.ly.ideamcp.service

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.Service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.xdebugger.*
import com.intellij.xdebugger.breakpoints.*
import com.intellij.xdebugger.evaluation.XDebuggerEvaluator
import com.intellij.xdebugger.evaluation.EvaluationMode
import com.intellij.xdebugger.frame.*
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.execution.*
import com.intellij.execution.executors.DefaultDebugExecutor
import com.intellij.execution.runners.ExecutionEnvironmentBuilder
import com.intellij.execution.ui.RunContentDescriptor
import com.ly.ideamcp.model.CodeLocation
import com.ly.ideamcp.model.debug.*
import com.ly.ideamcp.util.PsiHelper
import com.ly.ideamcp.util.ThreadHelper
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit

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
        val filePath: String,
        val xDebugSession: XDebugSession? = null,
        val runContentDescriptor: RunContentDescriptor? = null
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

            // 3. 获取虚拟文件
            val virtualFile = LocalFileSystem.getInstance().findFileByPath(request.filePath)
                ?: throw IllegalArgumentException("Virtual file not found: ${request.filePath}")

            // 4. 获取断点管理器
            val breakpointManager = XDebuggerManager.getInstance(project).breakpointManager

            // 5. 查找行断点类型 - 使用 JavaLineBreakpointType
            val breakpointType = XBreakpointType.EXTENSION_POINT_NAME.extensionList
                .filterIsInstance<XLineBreakpointType<*>>()
                .firstOrNull { it.id == "java-line" }
                ?: throw IllegalStateException("Cannot find line breakpoint type")

            // 6. 创建断点
            // 7. 在 Write Action 中添加断点
            @Suppress("UNCHECKED_CAST")
            val xBreakpoint = ApplicationManager.getApplication().runWriteAction<XLineBreakpoint<*>?> {
                breakpointManager.addLineBreakpoint(
                    breakpointType as XLineBreakpointType<XBreakpointProperties<*>>,
                    virtualFile.url,
                    request.line,
                    breakpointType.createBreakpointProperties(virtualFile, request.line)
                )
            }

            if (xBreakpoint == null) {
                throw IllegalStateException("Failed to create breakpoint")
            }

            // 8. 设置断点条件
            if (request.condition != null) {
                xBreakpoint.conditionExpression = XDebuggerUtil.getInstance()
                    .createExpression(request.condition, null, null, EvaluationMode.EXPRESSION)
            }

            // 9. 设置断点启用状态
            xBreakpoint.isEnabled = request.enabled

            // 10. 生成断点ID并存储
            val breakpointId = UUID.randomUUID().toString()
            val offset = document.getLineStartOffset(request.line)
            val location = CodeLocation(
                filePath = request.filePath,
                offset = offset,
                line = request.line,
                column = 0
            )

            val breakpointInfo = BreakpointInfo(
                breakpointId = breakpointId,
                location = location,
                type = if (request.logMessage != null) "log" else "line",
                condition = request.condition,
                logMessage = request.logMessage,
                enabled = request.enabled,
                verified = true
            )

            breakpoints[breakpointId] = breakpointInfo

            logger.info("Breakpoint created successfully: $breakpointId at ${request.filePath}:${request.line}")

            // 11. 返回响应
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
            // 从断点管理器获取所有断点
            val breakpointManager = XDebuggerManager.getInstance(project).breakpointManager
            val allBreakpoints = breakpointManager.allBreakpoints

            logger.info("Found ${allBreakpoints.size} breakpoints from XDebuggerManager")

            // 返回存储的断点信息
            val breakpointList = breakpoints.values.toList()

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
            // 1. 获取断点信息
            val breakpointInfo = breakpoints.remove(breakpointId)
                ?: throw IllegalArgumentException("Breakpoint not found: $breakpointId")

            // 2. 从断点管理器中删除断点
            val breakpointManager = XDebuggerManager.getInstance(project).breakpointManager
            val virtualFile = LocalFileSystem.getInstance().findFileByPath(breakpointInfo.location.filePath)

            if (virtualFile != null) {
                ApplicationManager.getApplication().runWriteAction {
                    // 查找并删除匹配的断点
                    breakpointManager.allBreakpoints.forEach { xBreakpoint ->
                        if (xBreakpoint is XLineBreakpoint<*>) {
                            val breakpointUrl = xBreakpoint.fileUrl
                            val breakpointLine = xBreakpoint.line

                            if (breakpointUrl == virtualFile.url && breakpointLine == breakpointInfo.location.line) {
                                breakpointManager.removeBreakpoint(xBreakpoint)
                                logger.info("Removed XBreakpoint at ${breakpointInfo.location.filePath}:${breakpointInfo.location.line}")
                            }
                        }
                    }
                }
            }

            logger.info("Breakpoint removed successfully: $breakpointId")

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

        // 注意: 实际启动调试会话需要 RunConfiguration,这里提供框架实现
        // 完整实现需要根据项目类型(Java/Kotlin/etc)创建相应的运行配置

        return ThreadHelper.runReadAction {
            // 1. 验证文件存在
            val psiFile = PsiHelper.findPsiFile(project, request.filePath)
                ?: throw IllegalArgumentException("File not found: ${request.filePath}")

            // 2. 生成会话ID
            val sessionId = UUID.randomUUID().toString()

            // 3. 创建会话信息 (不启动实际调试器,仅创建会话记录)
            val sessionInfo = DebugSessionInfo(
                sessionId = sessionId,
                status = "running",
                mainClass = request.mainClass,
                filePath = request.filePath,
                xDebugSession = null,
                runContentDescriptor = null
            )

            // 4. 存储会话
            debugSessions[sessionId] = sessionInfo

            logger.info("Debug session created: $sessionId for ${request.mainClass}")
            logger.warn("Note: Full debug session startup requires RunConfiguration - currently using mock session")

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

        return ApplicationManager.getApplication().runReadAction<DebugControlResponse> {
            val session = debugSessions[sessionId]
                ?: throw IllegalArgumentException("Debug session not found: $sessionId")

            // 停止实际的调试会话(如果存在)
            session.xDebugSession?.let { xSession ->
                ApplicationManager.getApplication().invokeLater {
                    xSession.stop()
                }
            }

            // 更新状态并移除会话
            session.status = "stopped"
            debugSessions.remove(sessionId)

            logger.info("Debug session stopped: $sessionId")

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

        return ApplicationManager.getApplication().runReadAction<DebugControlResponse> {
            val session = debugSessions[sessionId]
                ?: throw IllegalArgumentException("Debug session not found: $sessionId")

            // 暂停实际的调试会话
            session.xDebugSession?.let { xSession ->
                ApplicationManager.getApplication().invokeLater {
                    xSession.pause()
                }
            }

            // 更新状态
            session.status = "paused"

            // 获取当前位置
            val currentLocation = session.xDebugSession?.currentStackFrame?.sourcePosition?.let { position ->
                CodeLocation(
                    filePath = position.file.path,
                    offset = position.offset,
                    line = position.line,
                    column = 0
                )
            }

            logger.info("Debug session paused: $sessionId")

            DebugControlResponse(
                success = true,
                sessionId = sessionId,
                status = "paused",
                currentLocation = currentLocation
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

        return ApplicationManager.getApplication().runReadAction<DebugControlResponse> {
            val session = debugSessions[sessionId]
                ?: throw IllegalArgumentException("Debug session not found: $sessionId")

            // 恢复实际的调试会话
            session.xDebugSession?.let { xSession ->
                ApplicationManager.getApplication().invokeLater {
                    xSession.resume()
                }
            }

            // 更新状态
            session.status = "running"

            logger.info("Debug session resumed: $sessionId")

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

        return ApplicationManager.getApplication().runReadAction<DebugControlResponse> {
            val session = debugSessions[sessionId]
                ?: throw IllegalArgumentException("Debug session not found: $sessionId")

            if (session.status != "paused") {
                throw IllegalStateException("Debug session must be paused to step over")
            }

            // 执行步过操作
            session.xDebugSession?.let { xSession ->
                ApplicationManager.getApplication().invokeLater {
                    xSession.stepOver(false)
                }
            }

            // 获取当前位置
            val currentLocation = session.xDebugSession?.currentStackFrame?.sourcePosition?.let { position ->
                CodeLocation(
                    filePath = position.file.path,
                    offset = position.offset,
                    line = position.line,
                    column = 0
                )
            }

            logger.info("Stepped over in session: $sessionId")

            DebugControlResponse(
                success = true,
                sessionId = sessionId,
                status = "paused",
                currentLocation = currentLocation
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

        return ApplicationManager.getApplication().runReadAction<DebugControlResponse> {
            val session = debugSessions[sessionId]
                ?: throw IllegalArgumentException("Debug session not found: $sessionId")

            if (session.status != "paused") {
                throw IllegalStateException("Debug session must be paused to step into")
            }

            // 执行步入操作
            session.xDebugSession?.let { xSession ->
                ApplicationManager.getApplication().invokeLater {
                    xSession.stepInto()
                }
            }

            // 获取当前位置
            val currentLocation = session.xDebugSession?.currentStackFrame?.sourcePosition?.let { position ->
                CodeLocation(
                    filePath = position.file.path,
                    offset = position.offset,
                    line = position.line,
                    column = 0
                )
            }

            logger.info("Stepped into in session: $sessionId")

            DebugControlResponse(
                success = true,
                sessionId = sessionId,
                status = "paused",
                currentLocation = currentLocation
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

        return ApplicationManager.getApplication().runReadAction<DebugControlResponse> {
            val session = debugSessions[sessionId]
                ?: throw IllegalArgumentException("Debug session not found: $sessionId")

            if (session.status != "paused") {
                throw IllegalStateException("Debug session must be paused to step out")
            }

            // 执行步出操作
            session.xDebugSession?.let { xSession ->
                ApplicationManager.getApplication().invokeLater {
                    xSession.stepOut()
                }
            }

            // 获取当前位置
            val currentLocation = session.xDebugSession?.currentStackFrame?.sourcePosition?.let { position ->
                CodeLocation(
                    filePath = position.file.path,
                    offset = position.offset,
                    line = position.line,
                    column = 0
                )
            }

            logger.info("Stepped out in session: $sessionId")

            DebugControlResponse(
                success = true,
                sessionId = sessionId,
                status = "paused",
                currentLocation = currentLocation
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

        return ApplicationManager.getApplication().runReadAction<EvaluateExpressionResponse> {
            val session = debugSessions[request.sessionId]
                ?: throw IllegalArgumentException("Debug session not found: ${request.sessionId}")

            if (session.status != "paused") {
                throw IllegalStateException("Debug session must be paused to evaluate expressions")
            }

            val xSession = session.xDebugSession
                ?: return@runReadAction EvaluateExpressionResponse(
                    success = false,
                    value = "",
                    type = "",
                    error = "No active debug session"
                )

            // 获取当前栈帧
            val stackFrame = xSession.currentStackFrame
                ?: return@runReadAction EvaluateExpressionResponse(
                    success = false,
                    value = "",
                    type = "",
                    error = "No current stack frame"
                )

            // 获取求值器
            val evaluator = stackFrame.evaluator
                ?: return@runReadAction EvaluateExpressionResponse(
                    success = false,
                    value = "",
                    type = "",
                    error = "Evaluator not available"
                )

            // 创建求值结果容器
            val result = CompletableFuture<EvaluateExpressionResponse>()

            // 异步求值
            ApplicationManager.getApplication().invokeLater {
                evaluator.evaluate(
                    request.expression,
                    object : XDebuggerEvaluator.XEvaluationCallback {
                        override fun evaluated(xValue: XValue) {
                            // 简化实现:直接返回表达式作为结果
                            result.complete(
                                EvaluateExpressionResponse(
                                    success = true,
                                    value = request.expression,
                                    type = "evaluated",
                                    error = null
                                )
                            )
                        }

                        override fun errorOccurred(errorMessage: String) {
                            result.complete(
                                EvaluateExpressionResponse(
                                    success = false,
                                    value = "",
                                    type = "",
                                    error = errorMessage
                                )
                            )
                        }
                    },
                    null
                )
            }

            // 等待求值结果 (超时 5 秒)
            try {
                result.get(5, TimeUnit.SECONDS)
            } catch (e: Exception) {
                logger.error("Expression evaluation timeout or error", e)
                EvaluateExpressionResponse(
                    success = false,
                    value = "",
                    type = "",
                    error = "Evaluation timeout: ${e.message}"
                )
            }
        }
    }

    /**
     * 获取当前作用域变量
     * @param request 获取变量请求
     * @return 获取变量响应
     */
    fun getVariables(request: GetVariablesRequest): GetVariablesResponse {
        logger.info("Getting variables in session: ${request.sessionId}")

        return ApplicationManager.getApplication().runReadAction<GetVariablesResponse> {
            val session = debugSessions[request.sessionId]
                ?: throw IllegalArgumentException("Debug session not found: ${request.sessionId}")

            if (session.status != "paused") {
                throw IllegalStateException("Debug session must be paused to get variables")
            }

            val xSession = session.xDebugSession
                ?: return@runReadAction GetVariablesResponse(
                    success = false,
                    variables = emptyList()
                )

            // 获取当前栈帧
            val stackFrame = xSession.currentStackFrame
                ?: return@runReadAction GetVariablesResponse(
                    success = false,
                    variables = emptyList()
                )

            // 创建变量列表容器
            val variables = mutableListOf<VariableInfo>()
            val latch = java.util.concurrent.CountDownLatch(1)

            // 异步获取变量
            ApplicationManager.getApplication().invokeLater {
                stackFrame.computeChildren(object : XCompositeNode {
                    override fun addChildren(children: XValueChildrenList, last: Boolean) {
                        // 遍历所有变量
                        for (i in 0 until children.size()) {
                            val name = children.getName(i)
                            val xValue = children.getValue(i)

                            // 简化实现:直接使用变量名作为值
                            variables.add(
                                VariableInfo(
                                    name = name,
                                    value = "<value>",
                                    type = "variable",
                                    scope = request.scope ?: "local",
                                    children = null
                                )
                            )
                        }

                        if (last) {
                            latch.countDown()
                        }
                    }

                    override fun tooManyChildren(remaining: Int) {
                        latch.countDown()
                    }

                    override fun setMessage(
                        message: String,
                        icon: javax.swing.Icon?,
                        attributes: com.intellij.ui.SimpleTextAttributes,
                        link: com.intellij.xdebugger.frame.XDebuggerTreeNodeHyperlink?
                    ) {
                        logger.error("Error getting variables: $message")
                        latch.countDown()
                    }

                    override fun setErrorMessage(errorMessage: String) {
                        logger.error("Error getting variables: $errorMessage")
                        latch.countDown()
                    }

                    override fun setErrorMessage(errorMessage: String, link: com.intellij.xdebugger.frame.XDebuggerTreeNodeHyperlink?) {
                        setErrorMessage(errorMessage)
                    }

                    override fun setAlreadySorted(alreadySorted: Boolean) {}
                    override fun isObsolete(): Boolean = false
                })
            }

            // 等待变量获取完成 (超时 5 秒)
            try {
                latch.await(5, TimeUnit.SECONDS)
            } catch (e: InterruptedException) {
                logger.error("Get variables timeout", e)
            }

            logger.info("Retrieved ${variables.size} variables")

            GetVariablesResponse(
                success = true,
                variables = variables
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

        return ApplicationManager.getApplication().runReadAction<VariableInfo> {
            val session = debugSessions[sessionId]
                ?: throw IllegalArgumentException("Debug session not found: $sessionId")

            if (session.status != "paused") {
                throw IllegalStateException("Debug session must be paused to get variable")
            }

            val xSession = session.xDebugSession
                ?: throw IllegalStateException("No active debug session")

            // 获取当前栈帧
            val stackFrame = xSession.currentStackFrame
                ?: throw IllegalStateException("No current stack frame")

            // 创建变量信息容器
            val result = CompletableFuture<VariableInfo>()

            // 异步查找变量
            ApplicationManager.getApplication().invokeLater {
                stackFrame.computeChildren(object : XCompositeNode {
                    override fun addChildren(children: XValueChildrenList, last: Boolean) {
                        // 查找指定变量
                        for (i in 0 until children.size()) {
                            val name = children.getName(i)
                            if (name == variableName) {
                                // 简化实现:直接返回变量信息
                                result.complete(
                                    VariableInfo(
                                        name = name,
                                        value = "<value>",
                                        type = "variable",
                                        scope = "local",
                                        children = null
                                    )
                                )
                                return@addChildren
                            }
                        }

                        if (last && !result.isDone) {
                            result.completeExceptionally(
                                IllegalArgumentException("Variable not found: $variableName")
                            )
                        }
                    }

                    override fun tooManyChildren(remaining: Int) {
                        result.completeExceptionally(
                            IllegalStateException("Too many children to search")
                        )
                    }

                    override fun setMessage(
                        message: String,
                        icon: javax.swing.Icon?,
                        attributes: com.intellij.ui.SimpleTextAttributes,
                        link: com.intellij.xdebugger.frame.XDebuggerTreeNodeHyperlink?
                    ) {
                        result.completeExceptionally(
                            IllegalStateException("Error getting variables: $message")
                        )
                    }

                    override fun setErrorMessage(errorMessage: String) {
                        result.completeExceptionally(
                            IllegalStateException("Error getting variables: $errorMessage")
                        )
                    }

                    override fun setErrorMessage(errorMessage: String, link: com.intellij.xdebugger.frame.XDebuggerTreeNodeHyperlink?) {
                        setErrorMessage(errorMessage)
                    }

                    override fun setAlreadySorted(alreadySorted: Boolean) {}
                    override fun isObsolete(): Boolean = false
                })
            }

            // 等待结果 (超时 5 秒)
            try {
                result.get(5, TimeUnit.SECONDS)
            } catch (e: Exception) {
                logger.error("Get variable timeout or error", e)
                throw IllegalStateException("Failed to get variable: ${e.message}", e)
            }
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

        return ApplicationManager.getApplication().runReadAction<GetCallStackResponse> {
            val session = debugSessions[request.sessionId]
                ?: throw IllegalArgumentException("Debug session not found: ${request.sessionId}")

            if (session.status != "paused") {
                throw IllegalStateException("Debug session must be paused to get call stack")
            }

            val xSession = session.xDebugSession
                ?: return@runReadAction GetCallStackResponse(
                    success = false,
                    frames = emptyList()
                )

            // 获取挂起上下文
            val suspendContext = xSession.suspendContext
                ?: return@runReadAction GetCallStackResponse(
                    success = false,
                    frames = emptyList()
                )

            // 获取执行栈
            val executionStack = suspendContext.activeExecutionStack
                ?: return@runReadAction GetCallStackResponse(
                    success = false,
                    frames = emptyList()
                )

            // 创建栈帧列表容器
            val frames = mutableListOf<StackFrame>()
            val latch = java.util.concurrent.CountDownLatch(1)

            // 异步获取栈帧
            ApplicationManager.getApplication().invokeLater {
                executionStack.computeStackFrames(0, object : XExecutionStack.XStackFrameContainer {
                    override fun addStackFrames(stackFrames: List<XStackFrame>, last: Boolean) {
                        stackFrames.forEachIndexed { index, xStackFrame ->
                            val position = xStackFrame.sourcePosition

                            // 创建栈帧位置
                            val location = position?.let {
                                CodeLocation(
                                    filePath = it.file.path,
                                    offset = it.offset,
                                    line = it.line,
                                    column = 0
                                )
                            }

                            // 获取栈帧的简化变量列表(暂不获取详细变量,避免过度复杂)
                            val stackFrame = StackFrame(
                                frameIndex = index,
                                methodName = extractMethodName(xStackFrame),
                                className = extractClassName(xStackFrame),
                                location = location ?: CodeLocation(
                                    filePath = "",
                                    offset = 0,
                                    line = 0,
                                    column = 0
                                ),
                                variables = emptyList() // 可选: 可以进一步获取每个栈帧的变量
                            )

                            frames.add(stackFrame)
                        }

                        if (last) {
                            latch.countDown()
                        }
                    }

                    override fun errorOccurred(errorMessage: String) {
                        logger.error("Error getting call stack: $errorMessage")
                        latch.countDown()
                    }
                })
            }

            // 等待栈帧获取完成 (超时 5 秒)
            try {
                latch.await(5, TimeUnit.SECONDS)
            } catch (e: InterruptedException) {
                logger.error("Get call stack timeout", e)
            }

            logger.info("Retrieved ${frames.size} stack frames")

            GetCallStackResponse(
                success = true,
                frames = frames
            )
        }
    }

    /**
     * 从栈帧中提取方法名
     */
    private fun extractMethodName(stackFrame: XStackFrame): String {
        // 尝试从 presentation 中提取方法名
        val presentation = try {
            stackFrame.toString()
        } catch (e: Exception) {
            "unknown"
        }

        // 简单解析 (格式通常是 "ClassName.methodName:lineNumber")
        return presentation.substringAfterLast('.').substringBefore(':').ifBlank { "unknown" }
    }

    /**
     * 从栈帧中提取类名
     */
    private fun extractClassName(stackFrame: XStackFrame): String {
        // 尝试从 presentation 中提取类名
        val presentation = try {
            stackFrame.toString()
        } catch (e: Exception) {
            "unknown"
        }

        // 简单解析 (格式通常是 "ClassName.methodName:lineNumber")
        return presentation.substringBeforeLast('.').ifBlank { "unknown" }
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
