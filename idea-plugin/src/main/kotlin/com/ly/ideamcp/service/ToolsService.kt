package com.ly.ideamcp.service

import com.intellij.openapi.components.Service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.ly.ideamcp.model.tools.*
import com.ly.ideamcp.util.PsiHelper
import com.ly.ideamcp.util.ThreadHelper

/**
 * 工具服务
 * 提供代码格式化、优化导入、快速修复等功能
 */
@Service(Service.Level.PROJECT)
class ToolsService(private val project: Project) {

    private val logger = Logger.getInstance(ToolsService::class.java)

    /**
     * 格式化代码
     * @param request 格式化请求
     * @return 格式化响应
     */
    fun formatCode(request: FormatCodeRequest): FormatCodeResponse {
        logger.info("Formatting code in file: ${request.filePath}")

        return ThreadHelper.runReadAction {
            val psiFile = PsiHelper.findPsiFile(project, request.filePath)
                ?: throw IllegalArgumentException("File not found: ${request.filePath}")

            // 占位实现 - 实际应使用 CodeStyleManager.reformat
            logger.info("Code formatting not fully implemented - placeholder response")

            FormatCodeResponse(
                success = true,
                filePath = request.filePath,
                formatted = true
            )
        }
    }

    /**
     * 优化导入
     * @param request 优化导入请求
     * @return 优化导入响应
     */
    fun optimizeImports(request: OptimizeImportsRequest): OptimizeImportsResponse {
        logger.info("Optimizing imports in file: ${request.filePath}")

        return ThreadHelper.runReadAction {
            val psiFile = PsiHelper.findPsiFile(project, request.filePath)
                ?: throw IllegalArgumentException("File not found: ${request.filePath}")

            // 占位实现 - 实际应使用 JavaCodeStyleManager.optimizeImports
            logger.info("Optimize imports not fully implemented - placeholder response")

            OptimizeImportsResponse(
                success = true,
                filePath = request.filePath,
                removedImports = emptyList(),
                addedImports = emptyList()
            )
        }
    }

    /**
     * 应用快速修复
     * @param request 快速修复请求
     * @return 快速修复响应
     */
    fun applyQuickFix(request: QuickFixRequest): QuickFixResponse {
        logger.info("Applying quick fix in file: ${request.filePath}")

        return ThreadHelper.runReadAction {
            val psiFile = PsiHelper.findPsiFile(project, request.filePath)
                ?: throw IllegalArgumentException("File not found: ${request.filePath}")

            // 占位实现 - 实际应使用 IntentionAction
            logger.info("Apply quick fix not fully implemented - placeholder response")

            QuickFixResponse(
                success = true,
                filePath = request.filePath,
                fixApplied = request.fixId
            )
        }
    }

    /**
     * 应用 Intention
     * @param request Intention 请求
     * @return Intention 响应
     */
    fun applyIntention(request: IntentionRequest): IntentionResponse {
        logger.info("Applying intention in file: ${request.filePath}")

        return ThreadHelper.runReadAction {
            val psiFile = PsiHelper.findPsiFile(project, request.filePath)
                ?: throw IllegalArgumentException("File not found: ${request.filePath}")

            // 占位实现 - 实际应使用 IntentionAction
            logger.info("Apply intention not fully implemented - placeholder response")

            IntentionResponse(
                success = true,
                filePath = request.filePath,
                intentionApplied = request.intentionId
            )
        }
    }

    companion object {
        /**
         * 获取服务实例
         */
        fun getInstance(project: Project): ToolsService {
            return project.getService(ToolsService::class.java)
        }
    }
}
