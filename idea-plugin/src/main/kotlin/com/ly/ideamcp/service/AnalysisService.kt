package com.ly.ideamcp.service

import com.intellij.codeInspection.InspectionManager
import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.codeInspection.ex.InspectionManagerEx
import com.intellij.codeInsight.daemon.impl.DaemonCodeAnalyzerImpl
import com.intellij.codeInsight.daemon.impl.HighlightInfo
import com.intellij.lang.annotation.HighlightSeverity
import com.intellij.openapi.components.Service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.editor.Document
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
                // 检查整个项目（暂不支持）
                logger.warn("Project-wide inspection is not supported yet. Please specify a file path.")
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
            val document = PsiHelper.getDocument(psiFile) ?: return emptyList()

            // 使用 DaemonCodeAnalyzer 获取文件的所有高亮信息（包括错误、警告等）
            val daemonCodeAnalyzer = DaemonCodeAnalyzerImpl.getInstance(project) as DaemonCodeAnalyzerImpl
            val highlights = daemonCodeAnalyzer.getFileLevelHighlights(project, psiFile)

            // 将高亮信息转换为 ProblemInfo
            for (highlight in highlights) {
                val problemInfo = createProblemInfoFromHighlight(highlight, psiFile, document, request)
                if (problemInfo != null) {
                    problems.add(problemInfo)
                }
            }

            logger.info("File inspection completed with ${problems.size} problems")
        } catch (e: Exception) {
            logger.error("Failed to inspect file", e)
        }

        return problems
    }

    /**
     * 从 HighlightInfo 创建 ProblemInfo
     */
    private fun createProblemInfoFromHighlight(
        highlight: HighlightInfo,
        psiFile: PsiFile,
        document: Document,
        request: InspectionRequest
    ): ProblemInfo? {
        try {
            val severity = highlight.severity

            // 根据请求的最小严重程度过滤
            val severityName = mapSeverityToString(severity)
            if (request.severity != null && !shouldIncludeSeverity(severityName, request.severity)) {
                return null
            }

            // 获取问题位置
            val startOffset = highlight.startOffset
            val file = psiFile.virtualFile ?: return null
            val location = OffsetHelper.createLocation(
                PsiHelper.getRelativePath(project, file),
                document,
                startOffset
            ) ?: return null

            // 获取快速修复（如果需要）
            val quickFixes = if (request.includeQuickFixes) {
                highlight.quickFixActionRanges?.mapNotNull { range ->
                    val action = range.first.action
                    QuickFixInfo(
                        name = action.text,
                        familyName = action.familyName ?: action.text
                    )
                } ?: emptyList()
            } else {
                null
            }

            return ProblemInfo(
                location = location,
                severity = severityName,
                message = highlight.description ?: "Unknown problem",
                inspectionName = highlight.inspectionToolId ?: "Unknown",
                quickFixes = quickFixes
            )
        } catch (e: Exception) {
            logger.warn("Failed to create problem info from highlight", e)
            return null
        }
    }

    /**
     * 将 HighlightSeverity 映射为字符串
     */
    private fun mapSeverityToString(severity: HighlightSeverity): String {
        return when {
            severity == HighlightSeverity.ERROR -> "ERROR"
            severity == HighlightSeverity.WARNING -> "WARNING"
            severity == HighlightSeverity.WEAK_WARNING -> "WEAK_WARNING"
            severity == HighlightSeverity.INFORMATION -> "INFORMATION"
            severity.name == "TYPO" -> "TYPO"
            else -> severity.name
        }
    }

    /**
     * 判断是否应该包含指定严重程度的问题
     * @param actualSeverity 实际严重程度
     * @param minSeverity 最小严重程度
     * @return 是否应该包含
     */
    private fun shouldIncludeSeverity(actualSeverity: String, minSeverity: String): Boolean {
        val severityLevels = listOf("INFORMATION", "WEAK_WARNING", "WARNING", "ERROR")
        val actualLevel = severityLevels.indexOf(actualSeverity).takeIf { it >= 0 } ?: 0
        val minLevel = severityLevels.indexOf(minSeverity).takeIf { it >= 0 } ?: 0
        return actualLevel >= minLevel
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
