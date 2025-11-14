package com.ly.ideamcp.service

import com.intellij.openapi.components.Service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiReference
import com.intellij.psi.search.searches.ReferencesSearch
import com.ly.ideamcp.model.*
import com.ly.ideamcp.model.navigation.*
import com.ly.ideamcp.util.OffsetHelper
import com.ly.ideamcp.util.PsiHelper
import com.ly.ideamcp.util.ThreadHelper

/**
 * 导航服务
 * 提供代码导航功能（查找用途、跳转定义等）
 */
@Service(Service.Level.PROJECT)
class NavigationService(private val project: Project) {

    private val logger = Logger.getInstance(NavigationService::class.java)

    /**
     * 查找符号的所有用途
     * @param request 查找用途请求
     * @return 查找用途响应
     */
    fun findUsages(request: FindUsagesRequest): FindUsagesResponse {
        logger.info("Finding usages in file: ${request.filePath}")

        return ThreadHelper.runReadAction {
            // 1. 查找 PSI 文件
            val psiFile = PsiHelper.findPsiFile(project, request.filePath)
                ?: throw IllegalArgumentException("File not found: ${request.filePath}")

            // 2. 确定偏移量
            val offset = request.offset ?: run {
                val document = PsiHelper.getDocument(psiFile)
                    ?: throw IllegalStateException("Cannot get document")

                OffsetHelper.lineColumnToOffset(document, request.line!!, request.column!!)
                    ?: throw IllegalArgumentException("Invalid line/column")
            }

            // 3. 查找元素
            val element = PsiHelper.findElementAtOffset(psiFile, offset)
                ?: throw IllegalArgumentException("No element found at offset: $offset")

            // 4. 获取目标元素（如果是引用，解析到定义）
            val targetElement = resolveElement(element)
                ?: throw IllegalStateException("Cannot resolve element")

            val symbolName = getElementName(targetElement)

            // 5. 搜索用途
            val usages = searchUsages(targetElement, request)

            // 6. 返回结果
            FindUsagesResponse(
                symbolName = symbolName,
                totalCount = usages.size,
                usages = usages,
                truncated = request.maxResults > 0 && usages.size >= request.maxResults
            )
        }
    }

    /**
     * 跳转到符号定义
     * @param request 跳转定义请求
     * @return 跳转定义响应
     */
    fun gotoDefinition(request: GotoDefinitionRequest): GotoDefinitionResponse {
        logger.info("Goto definition in file: ${request.filePath}")

        return ThreadHelper.runReadAction {
            // 1. 查找 PSI 文件
            val psiFile = PsiHelper.findPsiFile(project, request.filePath)
                ?: throw IllegalArgumentException("File not found: ${request.filePath}")

            // 2. 确定偏移量
            val offset = request.offset ?: run {
                val document = PsiHelper.getDocument(psiFile)
                    ?: throw IllegalStateException("Cannot get document")

                OffsetHelper.lineColumnToOffset(document, request.line!!, request.column!!)
                    ?: throw IllegalArgumentException("Invalid line/column")
            }

            // 3. 查找元素
            val element = PsiHelper.findElementAtOffset(psiFile, offset)
                ?: return@runReadAction GotoDefinitionResponse(
                    symbolName = null,
                    definition = null,
                    found = false
                )

            // 4. 解析到定义
            val definition = resolveElement(element)

            if (definition == null) {
                return@runReadAction GotoDefinitionResponse(
                    symbolName = getElementName(element),
                    definition = null,
                    found = false
                )
            }

            // 5. 获取定义位置
            val defFile = definition.containingFile?.virtualFile
            if (defFile == null) {
                return@runReadAction GotoDefinitionResponse(
                    symbolName = getElementName(definition),
                    definition = null,
                    found = false
                )
            }

            val document = PsiHelper.getDocument(definition.containingFile)
            val defOffset = definition.textRange.startOffset

            val location = if (document != null) {
                OffsetHelper.createLocation(
                    PsiHelper.getRelativePath(project, defFile),
                    document,
                    defOffset
                )
            } else {
                CodeLocation(
                    filePath = PsiHelper.getRelativePath(project, defFile),
                    offset = defOffset
                )
            }

            GotoDefinitionResponse(
                symbolName = getElementName(definition),
                definition = location,
                found = true,
                kind = getElementKind(definition)
            )
        }
    }

    /**
     * 解析元素到定义
     */
    private fun resolveElement(element: PsiElement): PsiElement? {
        val reference = element.reference
        return reference?.resolve() ?: element
    }

    /**
     * 搜索元素的用途
     */
    private fun searchUsages(element: PsiElement, request: FindUsagesRequest): List<CodeUsage> {
        val usages = mutableListOf<CodeUsage>()

        // 使用 IDEA 的 ReferencesSearch 查找引用
        val references = ReferencesSearch.search(element).findAll()

        for (ref in references) {
            if (request.maxResults > 0 && usages.size >= request.maxResults) {
                break
            }

            try {
                val usage = createCodeUsage(ref)
                if (usage != null) {
                    usages.add(usage)
                }
            } catch (e: Exception) {
                logger.warn("Failed to create usage from reference", e)
            }
        }

        return usages
    }

    /**
     * 从 PsiReference 创建 CodeUsage
     */
    private fun createCodeUsage(ref: PsiReference): CodeUsage? {
        val refElement = ref.element
        val file = refElement.containingFile?.virtualFile ?: return null
        val document = PsiHelper.getDocument(refElement.containingFile) ?: return null

        val offset = refElement.textRange.startOffset
        val location = OffsetHelper.createLocation(
            PsiHelper.getRelativePath(project, file),
            document,
            offset
        ) ?: return null

        // 获取上下文（包含引用的那一行）
        val lineText = OffsetHelper.getLineText(document, location.line!!) ?: ""

        return CodeUsage(
            location = location,
            context = lineText.trim(),
            type = UsageType.OTHER // 简化：实际应该分析用途类型
        )
    }

    /**
     * 获取元素名称
     */
    private fun getElementName(element: PsiElement): String {
        return element.text?.take(50) ?: "Unknown"
    }

    /**
     * 获取元素类型
     */
    private fun getElementKind(element: PsiElement): String {
        return element.javaClass.simpleName
    }

    /**
     * 显示类型层次
     * @param request 类型层次请求
     * @return 类型层次响应
     */
    fun showTypeHierarchy(request: TypeHierarchyRequest): TypeHierarchyResponse {
        logger.info("Showing type hierarchy in file: ${request.filePath}")

        return ThreadHelper.runReadAction {
            val psiFile = PsiHelper.findPsiFile(project, request.filePath)
                ?: throw IllegalArgumentException("File not found: ${request.filePath}")

            val document = PsiHelper.getDocument(psiFile)
                ?: throw IllegalStateException("Cannot get document")

            val offset = request.offset ?: run {
                OffsetHelper.lineColumnToOffset(document, request.line!!, request.column!!)
                    ?: throw IllegalArgumentException("Invalid line/column")
            }

            val element = PsiHelper.findElementAtOffset(psiFile, offset)
                ?: throw IllegalArgumentException("No element found at offset: $offset")

            // 占位实现 - 实际应使用 TypeHierarchyBrowser
            logger.info("Type hierarchy not fully implemented - placeholder response")

            TypeHierarchyResponse(
                success = true,
                className = "ExampleClass",
                supertypes = emptyList(),
                subtypes = emptyList()
            )
        }
    }

    /**
     * 显示调用层次
     * @param request 调用层次请求
     * @return 调用层次响应
     */
    fun showCallHierarchy(request: CallHierarchyRequest): CallHierarchyResponse {
        logger.info("Showing call hierarchy in file: ${request.filePath}")

        return ThreadHelper.runReadAction {
            val psiFile = PsiHelper.findPsiFile(project, request.filePath)
                ?: throw IllegalArgumentException("File not found: ${request.filePath}")

            val document = PsiHelper.getDocument(psiFile)
                ?: throw IllegalStateException("Cannot get document")

            val offset = request.offset ?: run {
                OffsetHelper.lineColumnToOffset(document, request.line!!, request.column!!)
                    ?: throw IllegalArgumentException("Invalid line/column")
            }

            val element = PsiHelper.findElementAtOffset(psiFile, offset)
                ?: throw IllegalArgumentException("No element found at offset: $offset")

            // 占位实现 - 实际应使用 CallHierarchyBrowser
            logger.info("Call hierarchy not fully implemented - placeholder response")

            CallHierarchyResponse(
                success = true,
                methodName = "exampleMethod",
                callers = emptyList(),
                callees = emptyList()
            )
        }
    }

    /**
     * 查找实现
     * @param request 查找实现请求
     * @return 查找实现响应
     */
    fun findImplementations(request: FindImplementationsRequest): FindImplementationsResponse {
        logger.info("Finding implementations in file: ${request.filePath}")

        return ThreadHelper.runReadAction {
            val psiFile = PsiHelper.findPsiFile(project, request.filePath)
                ?: throw IllegalArgumentException("File not found: ${request.filePath}")

            val document = PsiHelper.getDocument(psiFile)
                ?: throw IllegalStateException("Cannot get document")

            val offset = request.offset ?: run {
                OffsetHelper.lineColumnToOffset(document, request.line!!, request.column!!)
                    ?: throw IllegalArgumentException("Invalid line/column")
            }

            val element = PsiHelper.findElementAtOffset(psiFile, offset)
                ?: throw IllegalArgumentException("No element found at offset: $offset")

            // 占位实现 - 实际应使用 DefinitionsScopedSearch
            logger.info("Find implementations not fully implemented - placeholder response")

            FindImplementationsResponse(
                success = true,
                elementName = getElementName(element),
                implementations = emptyList(),
                totalImplementations = 0
            )
        }
    }

    companion object {
        fun getInstance(project: Project): NavigationService {
            return project.getService(NavigationService::class.java)
        }
    }
}
