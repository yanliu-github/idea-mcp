package com.ly.ideamcp.service

import com.intellij.openapi.components.Service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.ly.ideamcp.model.codegen.*
import com.ly.ideamcp.util.PsiHelper
import com.ly.ideamcp.util.ThreadHelper
import com.ly.ideamcp.util.OffsetHelper

/**
 * 代码生成服务
 * 提供各种代码生成功能
 */
@Service(Service.Level.PROJECT)
class CodeGenerationService(private val project: Project) {

    private val logger = Logger.getInstance(CodeGenerationService::class.java)

    /**
     * 生成 Getter 和 Setter 方法
     * @param request 生成请求
     * @return 生成响应
     */
    fun generateGettersSetters(request: GenerateGettersSettersRequest): GenerateGettersSettersResponse {
        logger.info("Generating getters/setters for fields in file: ${request.filePath}")

        return ThreadHelper.runReadAction {
            val psiFile = PsiHelper.findPsiFile(project, request.filePath)
                ?: throw IllegalArgumentException("File not found: ${request.filePath}")

            val document = PsiHelper.getDocument(psiFile)
                ?: throw IllegalStateException("Cannot get document")

            val offset = request.offset ?: run {
                OffsetHelper.lineColumnToOffset(document, request.line!!, request.column!!)
                    ?: throw IllegalArgumentException("Invalid line/column position")
            }

            logger.info("Generating getters/setters at offset: $offset")

            // 占位实现 - 实际应使用 IntelliJ GenerateMembersHandler
            GenerateGettersSettersResponse(
                success = true,
                generatedMethods = listOf(
                    GeneratedMethod(
                        name = "getFieldName",
                        signature = "public String getFieldName()",
                        offset = offset
                    ),
                    GeneratedMethod(
                        name = "setFieldName",
                        signature = "public void setFieldName(String fieldName)",
                        offset = offset + 100
                    )
                ),
                affectedFile = request.filePath
            )
        }
    }

    /**
     * 生成构造函数
     * @param request 生成请求
     * @return 生成响应
     */
    fun generateConstructor(request: GenerateConstructorRequest): GenerateConstructorResponse {
        logger.info("Generating constructor in file: ${request.filePath}")

        return ThreadHelper.runReadAction {
            val psiFile = PsiHelper.findPsiFile(project, request.filePath)
                ?: throw IllegalArgumentException("File not found: ${request.filePath}")

            val document = PsiHelper.getDocument(psiFile)
                ?: throw IllegalStateException("Cannot get document")

            val offset = request.offset ?: run {
                OffsetHelper.lineColumnToOffset(document, request.line!!, request.column!!)
                    ?: throw IllegalArgumentException("Invalid line/column position")
            }

            logger.info("Generating constructor at offset: $offset")

            // 占位实现
            GenerateConstructorResponse(
                success = true,
                constructor = GeneratedMethod(
                    name = "ClassName",
                    signature = "public ClassName(${request.fields.joinToString(", ") { "${it.type} ${it.name}" }})",
                    offset = offset
                ),
                affectedFile = request.filePath
            )
        }
    }

    /**
     * 生成 toString 方法
     * @param request 生成请求
     * @return 生成响应
     */
    fun generateToString(request: GenerateMethodRequest): GenerateMethodResponse {
        logger.info("Generating toString method in file: ${request.filePath}")

        return ThreadHelper.runReadAction {
            val psiFile = PsiHelper.findPsiFile(project, request.filePath)
                ?: throw IllegalArgumentException("File not found: ${request.filePath}")

            val document = PsiHelper.getDocument(psiFile)
                ?: throw IllegalStateException("Cannot get document")

            val offset = request.offset ?: run {
                OffsetHelper.lineColumnToOffset(document, request.line!!, request.column!!)
                    ?: throw IllegalArgumentException("Invalid line/column position")
            }

            logger.info("Generating toString at offset: $offset")

            // 占位实现
            GenerateMethodResponse(
                success = true,
                method = GeneratedMethod(
                    name = "toString",
                    signature = "@Override public String toString()",
                    offset = offset
                ),
                affectedFile = request.filePath
            )
        }
    }

    /**
     * 生成 equals 方法
     * @param request 生成请求
     * @return 生成响应
     */
    fun generateEquals(request: GenerateMethodRequest): GenerateMethodResponse {
        logger.info("Generating equals method in file: ${request.filePath}")

        return ThreadHelper.runReadAction {
            val psiFile = PsiHelper.findPsiFile(project, request.filePath)
                ?: throw IllegalArgumentException("File not found: ${request.filePath}")

            val document = PsiHelper.getDocument(psiFile)
                ?: throw IllegalStateException("Cannot get document")

            val offset = request.offset ?: run {
                OffsetHelper.lineColumnToOffset(document, request.line!!, request.column!!)
                    ?: throw IllegalArgumentException("Invalid line/column position")
            }

            logger.info("Generating equals at offset: $offset")

            // 占位实现
            GenerateMethodResponse(
                success = true,
                method = GeneratedMethod(
                    name = "equals",
                    signature = "@Override public boolean equals(Object obj)",
                    offset = offset
                ),
                affectedFile = request.filePath
            )
        }
    }

    /**
     * 生成 hashCode 方法
     * @param request 生成请求
     * @return 生成响应
     */
    fun generateHashCode(request: GenerateMethodRequest): GenerateMethodResponse {
        logger.info("Generating hashCode method in file: ${request.filePath}")

        return ThreadHelper.runReadAction {
            val psiFile = PsiHelper.findPsiFile(project, request.filePath)
                ?: throw IllegalArgumentException("File not found: ${request.filePath}")

            val document = PsiHelper.getDocument(psiFile)
                ?: throw IllegalStateException("Cannot get document")

            val offset = request.offset ?: run {
                OffsetHelper.lineColumnToOffset(document, request.line!!, request.column!!)
                    ?: throw IllegalArgumentException("Invalid line/column position")
            }

            logger.info("Generating hashCode at offset: $offset")

            // 占位实现
            GenerateMethodResponse(
                success = true,
                method = GeneratedMethod(
                    name = "hashCode",
                    signature = "@Override public int hashCode()",
                    offset = offset
                ),
                affectedFile = request.filePath
            )
        }
    }

    /**
     * 重写方法
     * @param request 重写方法请求
     * @return 重写方法响应
     */
    fun overrideMethod(request: OverrideMethodRequest): OverrideMethodResponse {
        logger.info("Overriding method: ${request.methodName} in file: ${request.filePath}")

        return ThreadHelper.runReadAction {
            val psiFile = PsiHelper.findPsiFile(project, request.filePath)
                ?: throw IllegalArgumentException("File not found: ${request.filePath}")

            val document = PsiHelper.getDocument(psiFile)
                ?: throw IllegalStateException("Cannot get document")

            val offset = request.offset ?: run {
                OffsetHelper.lineColumnToOffset(document, request.line!!, request.column!!)
                    ?: throw IllegalArgumentException("Invalid line/column position")
            }

            logger.info("Overriding method at offset: $offset")

            // 占位实现
            OverrideMethodResponse(
                success = true,
                method = GeneratedMethod(
                    name = request.methodName,
                    signature = "@Override public void ${request.methodName}()",
                    offset = offset
                ),
                affectedFile = request.filePath
            )
        }
    }

    companion object {
        /**
         * 获取服务实例
         */
        fun getInstance(project: Project): CodeGenerationService {
            return project.getService(CodeGenerationService::class.java)
        }
    }
}
