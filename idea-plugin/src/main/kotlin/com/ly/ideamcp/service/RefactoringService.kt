package com.ly.ideamcp.service

import com.intellij.openapi.components.Service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiNamedElement
import com.intellij.refactoring.rename.RenameProcessor
import com.ly.ideamcp.model.*
import com.ly.ideamcp.model.refactor.RenameRequest
import com.ly.ideamcp.model.refactor.RenameResponse
import com.ly.ideamcp.util.OffsetHelper
import com.ly.ideamcp.util.PsiHelper
import com.ly.ideamcp.util.ThreadHelper

/**
 * 重构服务
 * 提供各种代码重构功能
 */
@Service(Service.Level.PROJECT)
class RefactoringService(private val project: Project) {

    private val logger = Logger.getInstance(RefactoringService::class.java)

    /**
     * 重命名符号
     * @param request 重命名请求
     * @return 重命名响应
     * @throws IllegalArgumentException 如果参数无效
     * @throws IllegalStateException 如果找不到元素或重命名失败
     */
    fun renameSymbol(request: RenameRequest): RenameResponse {
        logger.info("Renaming symbol in file: ${request.filePath}, new name: ${request.newName}")

        return ThreadHelper.runReadAction {
            // 1. 查找 PSI 文件
            val psiFile = PsiHelper.findPsiFile(project, request.filePath)
                ?: throw IllegalArgumentException("File not found: ${request.filePath}")

            // 2. 确定偏移量
            val offset = request.offset ?: run {
                val document = PsiHelper.getDocument(psiFile)
                    ?: throw IllegalStateException("Cannot get document for file: ${request.filePath}")

                val line = request.line!!
                val column = request.column!!

                OffsetHelper.lineColumnToOffset(document, line, column)
                    ?: throw IllegalArgumentException("Invalid line/column: $line:$column")
            }

            // 3. 查找元素
            val element = PsiHelper.findElementAtOffset(psiFile, offset)
                ?: throw IllegalArgumentException("No element found at offset: $offset")

            // 4. 获取可重命名的元素（可能是引用指向的元素）
            val targetElement = findRenameableElement(element)
                ?: throw IllegalStateException("Element at offset $offset is not renameable")

            // 5. 检查是否是命名元素
            if (targetElement !is PsiNamedElement) {
                throw IllegalStateException("Element is not a named element")
            }

            val oldName = targetElement.name
                ?: throw IllegalStateException("Element has no name")

            // 6. 检查名称是否相同
            if (oldName == request.newName) {
                return@runReadAction RenameResponse(
                    success = true,
                    oldName = oldName,
                    newName = request.newName,
                    affectedFiles = 0,
                    changes = emptyList(),
                    preview = request.preview
                )
            }

            // 7. 如果是预览模式，返回预计的变更
            if (request.preview) {
                return@runReadAction previewRename(targetElement, oldName, request.newName)
            }

            // 8. 执行重命名
            executeRename(targetElement, request)

            // 9. 返回成功响应（简化版）
            RenameResponse(
                success = true,
                oldName = oldName,
                newName = request.newName,
                affectedFiles = 1, // 简化：实际应该统计所有受影响的文件
                changes = emptyList(), // 简化：实际应该收集所有变更
                preview = false
            )
        }
    }

    /**
     * 查找可重命名的元素
     * 如果元素是引用，返回引用指向的元素
     */
    private fun findRenameableElement(element: PsiElement): PsiElement? {
        // 如果是引用，获取引用的目标
        val reference = element.reference
        if (reference != null) {
            val resolved = reference.resolve()
            if (resolved != null) {
                return resolved
            }
        }

        // 向上查找命名元素
        var current: PsiElement? = element
        while (current != null) {
            if (current is PsiNamedElement && current.name != null) {
                return current
            }
            current = current.parent
        }

        return null
    }

    /**
     * 预览重命名
     * 返回预计的变更，但不实际执行
     */
    private fun previewRename(
        element: PsiNamedElement,
        oldName: String,
        newName: String
    ): RenameResponse {
        // 简化实现：实际应该使用 IDEA 的 Refactoring Preview 功能
        return RenameResponse(
            success = true,
            oldName = oldName,
            newName = newName,
            affectedFiles = 1,
            changes = listOf(
                CodeChange(
                    filePath = element.containingFile.virtualFile.path,
                    modifications = emptyList() // 简化：实际应该包含详细变更
                )
            ),
            preview = true
        )
    }

    /**
     * 执行重命名
     * 使用 IDEA 的 RenameProcessor
     */
    private fun executeRename(element: PsiNamedElement, request: RenameRequest) {
        ThreadHelper.runWriteAction {
            try {
                // 创建重命名处理器
                val processor = RenameProcessor(
                    project,
                    element,
                    request.newName,
                    request.searchInComments,
                    request.searchInStrings
                )

                // 执行重命名
                processor.run()

                logger.info("Rename completed successfully")
            } catch (e: Exception) {
                logger.error("Rename failed", e)
                throw IllegalStateException("Rename failed: ${e.message}", e)
            }
        }
    }

    companion object {
        /**
         * 获取服务实例
         */
        fun getInstance(project: Project): RefactoringService {
            return project.getService(RefactoringService::class.java)
        }
    }
}
