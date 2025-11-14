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

    companion object {
        /**
         * 获取服务实例
         */
        fun getInstance(project: Project): DebugService {
            return project.getService(DebugService::class.java)
        }
    }
}
