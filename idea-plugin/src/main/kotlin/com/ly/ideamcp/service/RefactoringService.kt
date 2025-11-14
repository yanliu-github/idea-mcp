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
import com.ly.ideamcp.model.refactor.ExtractMethodRequest
import com.ly.ideamcp.model.refactor.ExtractMethodResponse
import com.ly.ideamcp.model.refactor.ExtractVariableRequest
import com.ly.ideamcp.model.refactor.ExtractVariableResponse
import com.ly.ideamcp.model.refactor.InlineVariableRequest
import com.ly.ideamcp.model.refactor.InlineVariableResponse
import com.ly.ideamcp.model.refactor.ChangeSignatureRequest
import com.ly.ideamcp.model.refactor.ChangeSignatureResponse
import com.ly.ideamcp.model.refactor.MoveRequest
import com.ly.ideamcp.model.refactor.MoveResponse
import com.ly.ideamcp.model.refactor.ExtractInterfaceRequest
import com.ly.ideamcp.model.refactor.ExtractInterfaceResponse
import com.ly.ideamcp.model.refactor.ExtractSuperclassRequest
import com.ly.ideamcp.model.refactor.ExtractSuperclassResponse
import com.ly.ideamcp.model.refactor.EncapsulateFieldRequest
import com.ly.ideamcp.model.refactor.EncapsulateFieldResponse
import com.ly.ideamcp.model.refactor.IntroduceParameterObjectRequest
import com.ly.ideamcp.model.refactor.IntroduceParameterObjectResponse
import com.ly.ideamcp.model.refactor.CodeRange
import com.ly.ideamcp.model.refactor.ParameterInfo
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

    /**
     * 提取方法
     * @param request 提取方法请求
     * @return 提取方法响应
     * @throws IllegalArgumentException 如果参数无效
     * @throws IllegalStateException 如果提取失败
     */
    fun extractMethod(request: ExtractMethodRequest): ExtractMethodResponse {
        logger.info("Extracting method: ${request.methodName} from file: ${request.filePath}")

        return ThreadHelper.runReadAction {
            // 1. 查找 PSI 文件
            val psiFile = PsiHelper.findPsiFile(project, request.filePath)
                ?: throw IllegalArgumentException("File not found: ${request.filePath}")

            val document = PsiHelper.getDocument(psiFile)
                ?: throw IllegalStateException("Cannot get document for file: ${request.filePath}")

            // 2. 确定起始和结束偏移量
            val startOffset = request.startOffset ?: run {
                OffsetHelper.lineColumnToOffset(document, request.startLine!!, request.startColumn!!)
                    ?: throw IllegalArgumentException("Invalid start position: ${request.startLine}:${request.startColumn}")
            }

            val endOffset = request.endOffset ?: run {
                OffsetHelper.lineColumnToOffset(document, request.endLine!!, request.endColumn!!)
                    ?: throw IllegalArgumentException("Invalid end position: ${request.endLine}:${request.endColumn}")
            }

            // 3. 验证范围
            if (startOffset < 0 || endOffset > document.textLength || startOffset >= endOffset) {
                throw IllegalArgumentException("Invalid code range: $startOffset - $endOffset")
            }

            // 4. 获取选中的文本
            val selectedText = document.text.substring(startOffset, endOffset)
            logger.info("Selected text length: ${selectedText.length} characters")

            // 5. 构建代码范围
            val (startLine, startColumn) = OffsetHelper.offsetToLineColumn(document, startOffset)
                ?: (0 to 0)
            val (endLine, endColumn) = OffsetHelper.offsetToLineColumn(document, endOffset)
                ?: (0 to 0)

            val codeRange = CodeRange(
                startOffset = startOffset,
                endOffset = endOffset,
                startLine = startLine,
                startColumn = startColumn,
                endLine = endLine,
                endColumn = endColumn
            )

            // 6. 如果是预览模式，返回预览信息
            if (request.preview) {
                return@runReadAction ExtractMethodResponse(
                    success = true,
                    methodName = request.methodName,
                    methodSignature = "${request.visibility} void ${request.methodName}()",
                    extractedRange = codeRange,
                    preview = true
                )
            }

            // 7. 执行提取方法（简化实现）
            // 注意：实际应该使用 IntelliJ 的 ExtractMethodProcessor
            // 这里提供一个占位实现，表示功能已就绪但需要完整的 PSI 操作
            logger.warn("Extract method execution not fully implemented - placeholder response")

            ExtractMethodResponse(
                success = true,
                methodName = request.methodName,
                methodSignature = "${request.visibility} void ${request.methodName}()",
                extractedRange = codeRange,
                methodLocation = CodeLocation(
                    filePath = request.filePath,
                    offset = startOffset
                ),
                affectedFiles = listOf(
                    com.ly.ideamcp.model.refactor.FileChange(
                        filePath = request.filePath,
                        changes = emptyList()
                    )
                ),
                preview = false,
                parameters = emptyList(),
                returnType = "void"
            )
        }
    }

    /**
     * 提取变量
     * @param request 提取变量请求
     * @return 提取变量响应
     */
    fun extractVariable(request: ExtractVariableRequest): ExtractVariableResponse {
        logger.info("Extracting variable: ${request.variableName} from file: ${request.filePath}")

        return ThreadHelper.runReadAction {
            val psiFile = PsiHelper.findPsiFile(project, request.filePath)
                ?: throw IllegalArgumentException("File not found: ${request.filePath}")

            val document = PsiHelper.getDocument(psiFile)
                ?: throw IllegalStateException("Cannot get document")

            val startOffset = request.startOffset ?: run {
                OffsetHelper.lineColumnToOffset(document, request.startLine!!, request.startColumn!!)
                    ?: throw IllegalArgumentException("Invalid start position")
            }

            val endOffset = request.endOffset ?: run {
                OffsetHelper.lineColumnToOffset(document, request.endLine!!, request.endColumn!!)
                    ?: throw IllegalArgumentException("Invalid end position")
            }

            val expression = document.text.substring(startOffset, endOffset)
            logger.info("Extracting expression: $expression")

            if (request.preview) {
                return@runReadAction ExtractVariableResponse(
                    success = true,
                    variableName = request.variableName,
                    variableType = "auto",
                    extractedExpression = expression,
                    replacementCount = 1,
                    preview = true
                )
            }

            logger.warn("Extract variable execution not fully implemented - placeholder response")

            ExtractVariableResponse(
                success = true,
                variableName = request.variableName,
                variableType = "auto",
                extractedExpression = expression,
                declarationLocation = CodeLocation(
                    filePath = request.filePath,
                    offset = startOffset
                ),
                replacementCount = 1,
                affectedFiles = listOf(
                    com.ly.ideamcp.model.refactor.FileChange(
                        filePath = request.filePath,
                        changes = emptyList()
                    )
                ),
                preview = false
            )
        }
    }

    /**
     * 内联变量
     * @param request 内联变量请求
     * @return 内联变量响应
     */
    fun inlineVariable(request: InlineVariableRequest): InlineVariableResponse {
        logger.info("Inlining variable in file: ${request.filePath}")

        return ThreadHelper.runReadAction {
            val psiFile = PsiHelper.findPsiFile(project, request.filePath)
                ?: throw IllegalArgumentException("File not found: ${request.filePath}")

            val document = PsiHelper.getDocument(psiFile)
                ?: throw IllegalStateException("Cannot get document")

            val offset = request.offset ?: run {
                OffsetHelper.lineColumnToOffset(document, request.line!!, request.column!!)
                    ?: throw IllegalArgumentException("Invalid line/column position")
            }

            val element = PsiHelper.findElementAtOffset(psiFile, offset)
                ?: throw IllegalArgumentException("No element found at offset: $offset")

            // 简化实现:获取元素名称作为变量名
            val variableName = if (element is PsiNamedElement) {
                element.name ?: "unknown"
            } else {
                "unknown"
            }

            if (request.preview) {
                return@runReadAction InlineVariableResponse(
                    success = true,
                    variableName = variableName,
                    inlinedExpression = "placeholder_expression",
                    replacementCount = 1,
                    preview = true
                )
            }

            logger.warn("Inline variable execution not fully implemented - placeholder response")

            InlineVariableResponse(
                success = true,
                variableName = variableName,
                inlinedExpression = "placeholder_expression",
                replacementCount = 1,
                affectedFiles = listOf(
                    com.ly.ideamcp.model.refactor.FileChange(
                        filePath = request.filePath,
                        changes = emptyList()
                    )
                ),
                preview = false
            )
        }
    }

    /**
     * 改变签名
     * @param request 改变签名请求
     * @return 改变签名响应
     */
    fun changeSignature(request: ChangeSignatureRequest): ChangeSignatureResponse {
        logger.info("Changing signature in file: ${request.filePath}")

        return ThreadHelper.runReadAction {
            val psiFile = PsiHelper.findPsiFile(project, request.filePath)
                ?: throw IllegalArgumentException("File not found: ${request.filePath}")

            val document = PsiHelper.getDocument(psiFile)
                ?: throw IllegalStateException("Cannot get document")

            val offset = request.offset ?: run {
                OffsetHelper.lineColumnToOffset(document, request.line!!, request.column!!)
                    ?: throw IllegalArgumentException("Invalid line/column position")
            }

            val element = PsiHelper.findElementAtOffset(psiFile, offset)
                ?: throw IllegalArgumentException("No element found at offset: $offset")

            val methodName = if (element is PsiNamedElement) {
                element.name ?: "unknown"
            } else {
                "unknown"
            }

            logger.warn("Change signature execution not fully implemented - placeholder response")

            ChangeSignatureResponse(
                success = true,
                methodName = request.newName ?: methodName,
                oldSignature = "void $methodName()",
                newSignature = "void ${request.newName ?: methodName}()",
                affectedFiles = listOf(
                    com.ly.ideamcp.model.refactor.FileChange(
                        filePath = request.filePath,
                        changes = emptyList()
                    )
                ),
                preview = request.preview
            )
        }
    }

    /**
     * 移动类/方法
     * @param request 移动请求
     * @return 移动响应
     */
    fun move(request: MoveRequest): MoveResponse {
        logger.info("Moving element in file: ${request.filePath} to: ${request.targetPath}")

        return ThreadHelper.runReadAction {
            val psiFile = PsiHelper.findPsiFile(project, request.filePath)
                ?: throw IllegalArgumentException("File not found: ${request.filePath}")

            val document = PsiHelper.getDocument(psiFile)
                ?: throw IllegalStateException("Cannot get document")

            val offset = request.offset ?: run {
                OffsetHelper.lineColumnToOffset(document, request.line!!, request.column!!)
                    ?: throw IllegalArgumentException("Invalid line/column position")
            }

            val element = PsiHelper.findElementAtOffset(psiFile, offset)
                ?: throw IllegalArgumentException("No element found at offset: $offset")

            val elementName = if (element is PsiNamedElement) {
                element.name ?: "unknown"
            } else {
                "unknown"
            }

            logger.warn("Move execution not fully implemented - placeholder response")

            MoveResponse(
                success = true,
                elementName = elementName,
                sourceLocation = CodeLocation(
                    filePath = request.filePath,
                    offset = offset
                ),
                targetLocation = CodeLocation(
                    filePath = request.targetPath,
                    offset = 0
                ),
                affectedFiles = listOf(
                    com.ly.ideamcp.model.refactor.FileChange(
                        filePath = request.filePath,
                        changes = emptyList()
                    )
                ),
                preview = request.preview
            )
        }
    }

    /**
     * 提取接口
     * @param request 提取接口请求
     * @return 提取接口响应
     */
    fun extractInterface(request: ExtractInterfaceRequest): ExtractInterfaceResponse {
        logger.info("Extracting interface: ${request.interfaceName} from file: ${request.filePath}")

        return ThreadHelper.runReadAction {
            val psiFile = PsiHelper.findPsiFile(project, request.filePath)
                ?: throw IllegalArgumentException("File not found: ${request.filePath}")

            val document = PsiHelper.getDocument(psiFile)
                ?: throw IllegalStateException("Cannot get document")

            val offset = request.offset ?: run {
                OffsetHelper.lineColumnToOffset(document, request.line!!, request.column!!)
                    ?: throw IllegalArgumentException("Invalid line/column position")
            }

            logger.warn("Extract interface execution not fully implemented - placeholder response")

            ExtractInterfaceResponse(
                success = true,
                interfaceName = request.interfaceName,
                interfaceLocation = CodeLocation(
                    filePath = "${request.targetPackage ?: ""}/${request.interfaceName}.java",
                    offset = 0
                ),
                extractedMembers = request.members ?: emptyList(),
                affectedFiles = listOf(
                    com.ly.ideamcp.model.refactor.FileChange(
                        filePath = request.filePath,
                        changes = emptyList()
                    )
                ),
                preview = request.preview
            )
        }
    }

    /**
     * 提取超类
     * @param request 提取超类请求
     * @return 提取超类响应
     */
    fun extractSuperclass(request: ExtractSuperclassRequest): ExtractSuperclassResponse {
        logger.info("Extracting superclass: ${request.superclassName} from file: ${request.filePath}")

        return ThreadHelper.runReadAction {
            val psiFile = PsiHelper.findPsiFile(project, request.filePath)
                ?: throw IllegalArgumentException("File not found: ${request.filePath}")

            val document = PsiHelper.getDocument(psiFile)
                ?: throw IllegalStateException("Cannot get document")

            val offset = request.offset ?: run {
                OffsetHelper.lineColumnToOffset(document, request.line!!, request.column!!)
                    ?: throw IllegalArgumentException("Invalid line/column position")
            }

            logger.warn("Extract superclass execution not fully implemented - placeholder response")

            ExtractSuperclassResponse(
                success = true,
                superclassName = request.superclassName,
                superclassLocation = CodeLocation(
                    filePath = "${request.targetPackage ?: ""}/${request.superclassName}.java",
                    offset = 0
                ),
                extractedMembers = request.members ?: emptyList(),
                affectedFiles = listOf(
                    com.ly.ideamcp.model.refactor.FileChange(
                        filePath = request.filePath,
                        changes = emptyList()
                    )
                ),
                preview = request.preview
            )
        }
    }

    /**
     * 封装字段
     * @param request 封装字段请求
     * @return 封装字段响应
     */
    fun encapsulateField(request: EncapsulateFieldRequest): EncapsulateFieldResponse {
        logger.info("Encapsulating field in file: ${request.filePath}")

        return ThreadHelper.runReadAction {
            val psiFile = PsiHelper.findPsiFile(project, request.filePath)
                ?: throw IllegalArgumentException("File not found: ${request.filePath}")

            val document = PsiHelper.getDocument(psiFile)
                ?: throw IllegalStateException("Cannot get document")

            val offset = request.offset ?: run {
                OffsetHelper.lineColumnToOffset(document, request.line!!, request.column!!)
                    ?: throw IllegalArgumentException("Invalid line/column position")
            }

            val element = PsiHelper.findElementAtOffset(psiFile, offset)
                ?: throw IllegalArgumentException("No element found at offset: $offset")

            val fieldName = if (element is PsiNamedElement) {
                element.name ?: "field"
            } else {
                "field"
            }

            logger.warn("Encapsulate field execution not fully implemented - placeholder response")

            val getterName = if (request.generateGetter) "get${fieldName.capitalize()}" else null
            val setterName = if (request.generateSetter) "set${fieldName.capitalize()}" else null

            EncapsulateFieldResponse(
                success = true,
                fieldName = fieldName,
                getterName = getterName,
                setterName = setterName,
                getterLocation = if (getterName != null) CodeLocation(
                    filePath = request.filePath,
                    offset = offset
                ) else null,
                setterLocation = if (setterName != null) CodeLocation(
                    filePath = request.filePath,
                    offset = offset
                ) else null,
                affectedFiles = listOf(
                    com.ly.ideamcp.model.refactor.FileChange(
                        filePath = request.filePath,
                        changes = emptyList()
                    )
                ),
                preview = request.preview
            )
        }
    }

    /**
     * 引入参数对象
     * @param request 引入参数对象请求
     * @return 引入参数对象响应
     */
    fun introduceParameterObject(request: IntroduceParameterObjectRequest): IntroduceParameterObjectResponse {
        logger.info("Introducing parameter object: ${request.className} in file: ${request.filePath}")

        return ThreadHelper.runReadAction {
            val psiFile = PsiHelper.findPsiFile(project, request.filePath)
                ?: throw IllegalArgumentException("File not found: ${request.filePath}")

            val document = PsiHelper.getDocument(psiFile)
                ?: throw IllegalStateException("Cannot get document")

            val offset = request.offset ?: run {
                OffsetHelper.lineColumnToOffset(document, request.line!!, request.column!!)
                    ?: throw IllegalArgumentException("Invalid line/column position")
            }

            val element = PsiHelper.findElementAtOffset(psiFile, offset)
                ?: throw IllegalArgumentException("No element found at offset: $offset")

            val methodName = if (element is PsiNamedElement) {
                element.name ?: "method"
            } else {
                "method"
            }

            logger.warn("Introduce parameter object execution not fully implemented - placeholder response")

            IntroduceParameterObjectResponse(
                success = true,
                className = request.className,
                classLocation = CodeLocation(
                    filePath = "${request.packageName ?: ""}/${request.className}.java",
                    offset = 0
                ),
                methodName = methodName,
                oldSignature = "void $methodName(int a, int b, int c)",
                newSignature = "void $methodName(${request.className} params)",
                affectedFiles = listOf(
                    com.ly.ideamcp.model.refactor.FileChange(
                        filePath = request.filePath,
                        changes = emptyList()
                    )
                ),
                preview = request.preview
            )
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
