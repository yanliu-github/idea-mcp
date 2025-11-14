package com.ly.ideamcp.service

import com.intellij.codeInspection.InspectionManager
import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.codeInspection.ex.InspectionManagerEx
import com.intellij.openapi.components.Service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFile
import com.ly.ideamcp.model.CodeLocation
import com.ly.ideamcp.model.analysis.*
import com.ly.ideamcp.util.OffsetHelper
import com.ly.ideamcp.util.PsiHelper
import com.ly.ideamcp.util.ThreadHelper

/**
 * 代码分析服务
 * 提供代码检查、错误检测等功能
 */
@Service(Service.Level.PROJECT)
class AnalysisService(private val project: Project) {

    private val logger = Logger.getInstance(AnalysisService::class.java)

    /**
     * 运行代码检查
     * @param request 检查请求
     * @return 检查响应
     */
    fun runInspections(request: InspectionRequest): InspectionResponse {
        logger.info("Running inspections, file: ${request.filePath ?: "entire project"}")

        return ThreadHelper.runReadAction {
            val problems = mutableListOf<ProblemInfo>()

            if (request.filePath != null) {
                // 检查单个文件
                val psiFile = PsiHelper.findPsiFile(project, request.filePath)
                    ?: throw IllegalArgumentException("File not found: ${request.filePath}")

                val fileProblems = inspectFile(psiFile, request)
                problems.addAll(fileProblems)
            } else {
                // 检查整个项目（简化实现）
                logger.warn("Project-wide inspection not fully implemented")
                // TODO: 实现项目级别检查
            }

            // 应用结果限制
            val truncated = request.maxResults > 0 && problems.size > request.maxResults
            val finalProblems = if (request.maxResults > 0) {
                problems.take(request.maxResults)
            } else {
                problems
            }

            InspectionResponse(
                totalCount = problems.size,
                problems = finalProblems,
                truncated = truncated
            )
        }
    }

    /**
     * 检查单个文件
     */
    private fun inspectFile(psiFile: PsiFile, request: InspectionRequest): List<ProblemInfo> {
        val problems = mutableListOf<ProblemInfo>()

        try {
            val inspectionManager = InspectionManager.getInstance(project) as? InspectionManagerEx
                ?: return emptyList()

            // 使用 IDEA 的高亮信息获取问题
            // 简化实现：实际应该运行所有配置的检查
            val document = PsiHelper.getDocument(psiFile)

            // 这里简化实现，实际需要运行完整的检查流程
            // TODO: 使用 DaemonCodeAnalyzerImpl 或 InspectionEngine 运行检查

            logger.info("File inspection completed with ${problems.size} problems")
        } catch (e: Exception) {
            logger.error("Failed to inspect file", e)
        }

        return problems
    }

    /**
     * 从 ProblemDescriptor 创建 ProblemInfo
     */
    private fun createProblemInfo(
        problem: ProblemDescriptor,
        psiFile: PsiFile,
        includeQuickFixes: Boolean
    ): ProblemInfo? {
        try {
            val element = problem.psiElement ?: return null
            val file = element.containingFile?.virtualFile ?: return null
            val document = PsiHelper.getDocument(psiFile) ?: return null

            val offset = element.textRange.startOffset
            val location = OffsetHelper.createLocation(
                PsiHelper.getRelativePath(project, file),
                document,
                offset
            ) ?: return null

            val quickFixes = if (includeQuickFixes) {
                problem.fixes?.mapNotNull { fix ->
                    QuickFixInfo(
                        name = fix.name ?: "Unknown",
                        familyName = fix.familyName ?: "Unknown"
                    )
                }
            } else {
                null
            }

            return ProblemInfo(
                location = location,
                severity = problem.highlightType.name,
                message = problem.descriptionTemplate,
                inspectionName = "Unknown", // 简化：实际应该从 problem 获取
                quickFixes = quickFixes
            )
        } catch (e: Exception) {
            logger.warn("Failed to create problem info", e)
            return null
        }
    }

    companion object {
        fun getInstance(project: Project): AnalysisService {
            return project.getService(AnalysisService::class.java)
        }
    }
}
