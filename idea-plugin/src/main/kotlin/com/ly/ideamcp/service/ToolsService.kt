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

        try {
            val psiFile = ThreadHelper.runReadAction {
                PsiHelper.findPsiFile(project, request.filePath)
                    ?: throw IllegalArgumentException("File not found: ${request.filePath}")
            }

            // 使用 WriteAction 执行格式化操作
            ThreadHelper.runWriteAction {
                val codeStyleManager = com.intellij.psi.codeStyle.CodeStyleManager.getInstance(project)
                codeStyleManager.reformat(psiFile)
            }

            logger.info("Successfully formatted code in file: ${request.filePath}")

            return FormatCodeResponse(
                success = true,
                filePath = request.filePath,
                formatted = true
            )
        } catch (e: Exception) {
            logger.error("Failed to format code in file: ${request.filePath}", e)
            return FormatCodeResponse(
                success = false,
                filePath = request.filePath,
                formatted = false
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

        try {
            val psiFile = ThreadHelper.runReadAction {
                PsiHelper.findPsiFile(project, request.filePath)
                    ?: throw IllegalArgumentException("File not found: ${request.filePath}")
            }

            // 获取优化前的导入语句
            val importsBefore = ThreadHelper.runReadAction {
                extractImportStatements(psiFile)
            }

            // 使用 WriteAction 执行优化导入操作
            ThreadHelper.runWriteAction {
                when {
                    psiFile is com.intellij.psi.PsiJavaFile -> {
                        // Java 文件使用 JavaCodeStyleManager
                        val javaCodeStyleManager = com.intellij.psi.codeStyle.JavaCodeStyleManager.getInstance(project)
                        javaCodeStyleManager.optimizeImports(psiFile)
                    }
                    psiFile.language.id == "kotlin" -> {
                        // Kotlin 文件使用 KotlinCodeStyleManager (如果可用)
                        try {
                            val kotlinStyleManager = Class.forName("org.jetbrains.kotlin.idea.core.formatter.KotlinCodeStyleManager")
                                .getMethod("getInstance", com.intellij.openapi.project.Project::class.java)
                                .invoke(null, project)

                            kotlinStyleManager.javaClass
                                .getMethod("optimizeImports", com.intellij.psi.PsiFile::class.java)
                                .invoke(kotlinStyleManager, psiFile)
                        } catch (e: Exception) {
                            logger.warn("Kotlin code style manager not available, using generic approach", e)
                            // 降级处理:仅使用基本的代码格式化
                            val codeStyleManager = com.intellij.psi.codeStyle.CodeStyleManager.getInstance(project)
                            codeStyleManager.reformat(psiFile)
                        }
                    }
                    else -> {
                        logger.warn("Optimize imports not fully supported for language: ${psiFile.language.id}")
                        // 对于其他语言,仅执行基本格式化
                        val codeStyleManager = com.intellij.psi.codeStyle.CodeStyleManager.getInstance(project)
                        codeStyleManager.reformat(psiFile)
                    }
                }
            }

            // 获取优化后的导入语句
            val importsAfter = ThreadHelper.runReadAction {
                extractImportStatements(psiFile)
            }

            // 计算变化
            val removedImports = importsBefore - importsAfter.toSet()
            val addedImports = importsAfter - importsBefore.toSet()

            logger.info("Successfully optimized imports in file: ${request.filePath}")

            return OptimizeImportsResponse(
                success = true,
                filePath = request.filePath,
                removedImports = removedImports.toList(),
                addedImports = addedImports.toList()
            )
        } catch (e: Exception) {
            logger.error("Failed to optimize imports in file: ${request.filePath}", e)
            return OptimizeImportsResponse(
                success = false,
                filePath = request.filePath,
                removedImports = emptyList(),
                addedImports = emptyList()
            )
        }
    }

    /**
     * 提取文件中的导入语句
     */
    private fun extractImportStatements(psiFile: com.intellij.psi.PsiFile): Set<String> {
        return when (psiFile) {
            is com.intellij.psi.PsiJavaFile -> {
                psiFile.importList?.importStatements?.map { it.text }?.toSet() ?: emptySet()
            }
            else -> {
                // 对于其他语言,尝试查找 import 关键字
                val imports = mutableSetOf<String>()
                psiFile.accept(object : com.intellij.psi.PsiRecursiveElementVisitor() {
                    override fun visitElement(element: com.intellij.psi.PsiElement) {
                        if (element.text.trim().startsWith("import ")) {
                            imports.add(element.text.trim())
                        }
                        super.visitElement(element)
                    }
                })
                imports
            }
        }
    }

    /**
     * 应用快速修复
     * @param request 快速修复请求
     * @return 快速修复响应
     */
    fun applyQuickFix(request: QuickFixRequest): QuickFixResponse {
        logger.info("Applying quick fix '${request.fixId}' in file: ${request.filePath}")

        try {
            // Phase 1: ReadAction - 收集数据和查找匹配的 Intention
            data class PreparedData(
                val offset: Int,
                val matchingIntentionText: String?
            )

            val preparedData = ThreadHelper.runReadAction {
                val psiFile = PsiHelper.findPsiFile(project, request.filePath)
                    ?: throw IllegalArgumentException("File not found: ${request.filePath}")

                // 确定要检查的位置
                val offset = when {
                    request.offset != null -> request.offset
                    request.line != null -> {
                        val document = com.intellij.psi.PsiDocumentManager.getInstance(project).getDocument(psiFile)
                            ?: throw IllegalArgumentException("Cannot get document for file: ${request.filePath}")

                        val lineNumber = (request.line - 1).coerceAtLeast(0)
                        if (lineNumber >= document.lineCount) {
                            throw IllegalArgumentException("Line number out of bounds: ${request.line}")
                        }

                        val lineStart = document.getLineStartOffset(lineNumber)
                        val column = request.column?.let { (it - 1).coerceAtLeast(0) } ?: 0
                        lineStart + column
                    }
                    else -> 0
                }

                val editor = com.intellij.openapi.fileEditor.FileEditorManager.getInstance(project)
                    .selectedTextEditor
                    ?: createTemporaryEditor(psiFile)

                // 移动光标到指定位置
                editor.caretModel.moveToOffset(offset)

                // 获取指定位置的所有可用快速修复
                val intentionManager = com.intellij.codeInsight.intention.IntentionManager.getInstance()
                val availableIntentions = intentionManager.availableIntentions

                // 查找匹配的快速修复
                val matchingIntention = availableIntentions.firstOrNull { intention ->
                    val isAvailable = try {
                        intention.isAvailable(project, editor, psiFile)
                    } catch (e: Exception) {
                        false
                    }

                    isAvailable && (
                        intention.text.contains(request.fixId, ignoreCase = true) ||
                        intention.familyName.contains(request.fixId, ignoreCase = true) ||
                        intention.javaClass.simpleName.contains(request.fixId, ignoreCase = true)
                    )
                }

                PreparedData(offset, matchingIntention?.text)
            }

            if (preparedData.matchingIntentionText == null) {
                logger.warn("Quick fix '${request.fixId}' not found at offset ${preparedData.offset} in file: ${request.filePath}")
                return QuickFixResponse(
                    success = false,
                    filePath = request.filePath,
                    fixApplied = ""
                )
            }

            // Phase 2: WriteAction - 应用快速修复（在 WriteAction 中重新查找并应用）
            val applied = ThreadHelper.runWriteAction {
                val psiFile = PsiHelper.findPsiFile(project, request.filePath)!!

                val editor = com.intellij.openapi.fileEditor.FileEditorManager.getInstance(project)
                    .selectedTextEditor
                    ?: createTemporaryEditor(psiFile)

                // 移动光标到指定位置
                editor.caretModel.moveToOffset(preparedData.offset)

                // 获取指定位置的所有可用快速修复
                val intentionManager = com.intellij.codeInsight.intention.IntentionManager.getInstance()
                val availableIntentions = intentionManager.availableIntentions

                // 重新查找匹配的快速修复（在 WriteAction 上下文中）
                val matchingIntention = availableIntentions.firstOrNull { intention ->
                    val isAvailable = try {
                        intention.isAvailable(project, editor, psiFile)
                    } catch (e: Exception) {
                        false
                    }

                    isAvailable && intention.text == preparedData.matchingIntentionText
                }

                if (matchingIntention != null) {
                    matchingIntention.invoke(project, editor, psiFile)
                    true
                } else {
                    false
                }
            }

            return if (applied) {
                logger.info("Successfully applied quick fix '${request.fixId}' in file: ${request.filePath}")
                QuickFixResponse(
                    success = true,
                    filePath = request.filePath,
                    fixApplied = request.fixId
                )
            } else {
                QuickFixResponse(
                    success = false,
                    filePath = request.filePath,
                    fixApplied = ""
                )
            }
        } catch (e: Exception) {
            logger.error("Failed to apply quick fix '${request.fixId}' in file: ${request.filePath}", e)
            return QuickFixResponse(
                success = false,
                filePath = request.filePath,
                fixApplied = ""
            )
        }
    }

    /**
     * 创建临时编辑器用于执行操作
     */
    private fun createTemporaryEditor(psiFile: com.intellij.psi.PsiFile): com.intellij.openapi.editor.Editor {
        val document = com.intellij.psi.PsiDocumentManager.getInstance(project).getDocument(psiFile)
            ?: throw IllegalStateException("Cannot create document for file: ${psiFile.virtualFile.path}")

        return com.intellij.openapi.editor.EditorFactory.getInstance()
            .createEditor(document, project)
    }

    /**
     * 应用 Intention
     * @param request Intention 请求
     * @return Intention 响应
     */
    fun applyIntention(request: IntentionRequest): IntentionResponse {
        logger.info("Applying intention '${request.intentionId}' in file: ${request.filePath}")

        try {
            // Phase 1: ReadAction - 收集数据和查找匹配的 Intention
            data class PreparedData(
                val offset: Int,
                val matchingIntentionText: String?
            )

            val preparedData = ThreadHelper.runReadAction {
                val psiFile = PsiHelper.findPsiFile(project, request.filePath)
                    ?: throw IllegalArgumentException("File not found: ${request.filePath}")

                // 确定要检查的位置
                val offset = when {
                    request.offset != null -> request.offset
                    request.line != null -> {
                        val document = com.intellij.psi.PsiDocumentManager.getInstance(project).getDocument(psiFile)
                            ?: throw IllegalArgumentException("Cannot get document for file: ${request.filePath}")

                        val lineNumber = (request.line - 1).coerceAtLeast(0)
                        if (lineNumber >= document.lineCount) {
                            throw IllegalArgumentException("Line number out of bounds: ${request.line}")
                        }

                        val lineStart = document.getLineStartOffset(lineNumber)
                        val column = request.column?.let { (it - 1).coerceAtLeast(0) } ?: 0
                        lineStart + column
                    }
                    else -> 0
                }

                val editor = com.intellij.openapi.fileEditor.FileEditorManager.getInstance(project)
                    .selectedTextEditor
                    ?: createTemporaryEditor(psiFile)

                // 移动光标到指定位置
                editor.caretModel.moveToOffset(offset)

                // 获取所有可用的 Intention Actions
                val intentionManager = com.intellij.codeInsight.intention.IntentionManager.getInstance()
                val availableIntentions = intentionManager.availableIntentions

                // 查找匹配的 Intention
                val matchingIntention = availableIntentions.firstOrNull { intention ->
                    // 检查 Intention 是否在当前位置可用
                    val isAvailable = try {
                        intention.isAvailable(project, editor, psiFile)
                    } catch (e: Exception) {
                        logger.warn("Error checking intention availability: ${intention.text}", e)
                        false
                    }

                    // 匹配 Intention ID
                    isAvailable && (
                        intention.text.contains(request.intentionId, ignoreCase = true) ||
                        intention.familyName.contains(request.intentionId, ignoreCase = true) ||
                        intention.javaClass.simpleName.contains(request.intentionId, ignoreCase = true)
                    )
                }

                PreparedData(offset, matchingIntention?.text)
            }

            if (preparedData.matchingIntentionText == null) {
                logger.warn("Intention '${request.intentionId}' not found or not available at offset ${preparedData.offset} in file: ${request.filePath}")
                return IntentionResponse(
                    success = false,
                    filePath = request.filePath,
                    intentionApplied = ""
                )
            }

            // Phase 2: WriteAction - 应用 Intention（在 WriteAction 中重新查找并应用）
            val applied = ThreadHelper.runWriteAction {
                val psiFile = PsiHelper.findPsiFile(project, request.filePath)!!

                val editor = com.intellij.openapi.fileEditor.FileEditorManager.getInstance(project)
                    .selectedTextEditor
                    ?: createTemporaryEditor(psiFile)

                // 移动光标到指定位置
                editor.caretModel.moveToOffset(preparedData.offset)

                // 获取所有可用的 Intention Actions
                val intentionManager = com.intellij.codeInsight.intention.IntentionManager.getInstance()
                val availableIntentions = intentionManager.availableIntentions

                // 重新查找匹配的 Intention（在 WriteAction 上下文中）
                val matchingIntention = availableIntentions.firstOrNull { intention ->
                    val isAvailable = try {
                        intention.isAvailable(project, editor, psiFile)
                    } catch (e: Exception) {
                        false
                    }

                    isAvailable && intention.text == preparedData.matchingIntentionText
                }

                if (matchingIntention != null) {
                    try {
                        matchingIntention.invoke(project, editor, psiFile)
                        true
                    } catch (e: Exception) {
                        logger.error("Error invoking intention: ${matchingIntention.text}", e)
                        false
                    }
                } else {
                    false
                }
            }

            return if (applied) {
                logger.info("Successfully applied intention '${request.intentionId}' in file: ${request.filePath}")
                IntentionResponse(
                    success = true,
                    filePath = request.filePath,
                    intentionApplied = request.intentionId
                )
            } else {
                IntentionResponse(
                    success = false,
                    filePath = request.filePath,
                    intentionApplied = ""
                )
            }
        } catch (e: Exception) {
            logger.error("Failed to apply intention '${request.intentionId}' in file: ${request.filePath}", e)
            return IntentionResponse(
                success = false,
                filePath = request.filePath,
                intentionApplied = ""
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
