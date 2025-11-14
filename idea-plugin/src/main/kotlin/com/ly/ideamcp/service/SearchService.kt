package com.ly.ideamcp.service

import com.intellij.openapi.components.Service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.ly.ideamcp.model.search.*
import com.ly.ideamcp.util.ThreadHelper

/**
 * 搜索服务
 * 提供全局搜索、文本搜索和结构化搜索功能
 */
@Service(Service.Level.PROJECT)
class SearchService(private val project: Project) {

    private val logger = Logger.getInstance(SearchService::class.java)

    /**
     * 全局搜索
     * @param request 全局搜索请求
     * @return 全局搜索响应
     */
    fun globalSearch(request: GlobalSearchRequest): GlobalSearchResponse {
        logger.info("Global search for: ${request.query}")

        return ThreadHelper.runReadAction {
            // 占位实现 - 实际应使用 FindInProjectUtil
            GlobalSearchResponse(
                success = true,
                query = request.query,
                results = listOf(
                    SearchResult(
                        filePath = "src/main/Example.java",
                        line = 10,
                        column = 5,
                        preview = "public class Example { }",
                        matchType = "class"
                    )
                ),
                totalResults = 1
            )
        }
    }

    /**
     * 文本搜索
     * @param request 文本搜索请求
     * @return 文本搜索响应
     */
    fun textSearch(request: TextSearchRequest): TextSearchResponse {
        logger.info("Text search for pattern: ${request.pattern}")

        return ThreadHelper.runReadAction {
            // 占位实现 - 实际应使用 FindManager
            TextSearchResponse(
                success = true,
                pattern = request.pattern,
                results = listOf(
                    SearchResult(
                        filePath = "src/main/Example.java",
                        line = 15,
                        column = 10,
                        preview = "String text = \"example\";",
                        matchType = "text"
                    )
                ),
                totalResults = 1,
                caseSensitive = request.caseSensitive,
                regex = request.regex
            )
        }
    }

    /**
     * 结构化搜索
     * @param request 结构化搜索请求
     * @return 结构化搜索响应
     */
    fun structuralSearch(request: StructuralSearchRequest): StructuralSearchResponse {
        logger.info("Structural search for pattern: ${request.pattern}")

        return ThreadHelper.runReadAction {
            // 占位实现 - 实际应使用 StructuralSearchUtil
            StructuralSearchResponse(
                success = true,
                pattern = request.pattern,
                results = listOf(
                    SearchResult(
                        filePath = "src/main/Example.java",
                        line = 20,
                        column = 0,
                        preview = "if (x == null) { }",
                        matchType = "structure"
                    )
                ),
                totalResults = 1
            )
        }
    }

    companion object {
        /**
         * 获取服务实例
         */
        fun getInstance(project: Project): SearchService {
            return project.getService(SearchService::class.java)
        }
    }
}
