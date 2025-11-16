package com.ly.ideamcp.service

import com.intellij.find.FindManager
import com.intellij.find.FindModel
import com.intellij.find.impl.FindInProjectUtil
import com.intellij.openapi.components.Service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiMethod
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.search.PsiShortNamesCache
import com.intellij.structuralsearch.Matcher
import com.intellij.structuralsearch.MatchOptions
import com.intellij.structuralsearch.plugin.ui.Configuration
import com.ly.ideamcp.model.search.*
import com.ly.ideamcp.util.ThreadHelper
import com.ly.ideamcp.util.PsiHelper

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
            val results = mutableListOf<SearchResult>()
            val cache = PsiShortNamesCache.getInstance(project)
            val scope = GlobalSearchScope.projectScope(project)

            try {
                // 搜索类
                val classes = cache.getClassesByName(request.query, scope)
                classes.forEach { psiClass ->
                    results.add(createSearchResultFromClass(psiClass))
                }

                // 搜索方法
                val methods = cache.getMethodsByName(request.query, scope)
                methods.forEach { method ->
                    results.add(createSearchResultFromMethod(method))
                }

                // 如果指定了最大结果数，限制返回数量
                val limitedResults = request.maxResults?.let { max ->
                    if (max > 0) results.take(max) else results
                } ?: results

                GlobalSearchResponse(
                    success = true,
                    query = request.query,
                    results = limitedResults,
                    totalResults = results.size
                )
            } catch (e: Exception) {
                logger.error("Global search failed", e)
                GlobalSearchResponse(
                    success = false,
                    query = request.query,
                    results = emptyList(),
                    totalResults = 0
                )
            }
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
            val results = mutableListOf<SearchResult>()

            try {
                val findManager = FindManager.getInstance(project)
                val findModel = FindModel().apply {
                    stringToFind = request.pattern
                    isCaseSensitive = request.caseSensitive
                    isRegularExpressions = request.regex
                    isWholeWordsOnly = request.wholeWords
                    searchContext = FindModel.SearchContext.ANY
                }

                // 执行查找 - 使用Processor收集UsageInfo
                val usageInfoProcessor = com.intellij.util.Processor<com.intellij.usageView.UsageInfo> { usageInfo ->
                    val psiFile = usageInfo.element?.containingFile
                    if (psiFile != null) {
                        val document = com.intellij.psi.PsiDocumentManager.getInstance(project)
                            .getDocument(psiFile)
                        if (document != null) {
                            val textRange = usageInfo.element?.textRange
                            if (textRange != null) {
                                val line = document.getLineNumber(textRange.startOffset)
                                val column = textRange.startOffset - document.getLineStartOffset(line)
                                val preview = getPreviewText(document, textRange)

                                results.add(
                                    SearchResult(
                                        filePath = psiFile.virtualFile?.path ?: psiFile.name,
                                        line = line + 1,
                                        column = column,
                                        preview = preview,
                                        matchType = "text"
                                    )
                                )
                            }
                        }
                    }

                    // 如果指定了最大结果数且已达到，停止处理
                    val maxResults = request.maxResults ?: 0
                    maxResults <= 0 || results.size < maxResults
                }

                val presentation = com.intellij.usages.FindUsagesProcessPresentation(
                    com.intellij.usages.UsageViewPresentation()
                )

                FindInProjectUtil.findUsages(
                    findModel,
                    project,
                    usageInfoProcessor,
                    presentation
                )

                TextSearchResponse(
                    success = true,
                    pattern = request.pattern,
                    results = results,
                    totalResults = results.size,
                    caseSensitive = request.caseSensitive,
                    regex = request.regex
                )
            } catch (e: Exception) {
                logger.error("Text search failed", e)
                TextSearchResponse(
                    success = false,
                    pattern = request.pattern,
                    results = emptyList(),
                    totalResults = 0,
                    caseSensitive = request.caseSensitive,
                    regex = request.regex
                )
            }
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
            val results = mutableListOf<SearchResult>()

            try {
                // 创建匹配选项
                val matchOptions = MatchOptions()
                matchOptions.searchPattern = request.pattern
                matchOptions.scope = GlobalSearchScope.projectScope(project)
                matchOptions.isCaseSensitiveMatch = request.caseSensitive

                // 创建匹配器
                val matcher = Matcher(project, matchOptions)

                // 创建结果收集器
                val matchResultSink = object : com.intellij.structuralsearch.MatchResultSink {
                    private var matchingProcess: com.intellij.structuralsearch.MatchingProcess? = null

                    override fun setMatchingProcess(matchingProcess: com.intellij.structuralsearch.MatchingProcess) {
                        this.matchingProcess = matchingProcess
                    }

                    override fun getProgressIndicator(): com.intellij.openapi.progress.ProgressIndicator? {
                        return null
                    }

                    override fun newMatch(result: com.intellij.structuralsearch.MatchResult) {
                        val psiFile = result.match?.containingFile
                        if (psiFile != null) {
                            val document = com.intellij.psi.PsiDocumentManager.getInstance(project)
                                .getDocument(psiFile)
                            if (document != null) {
                                val textRange = result.match?.textRange
                                if (textRange != null) {
                                    val line = document.getLineNumber(textRange.startOffset)
                                    val column = textRange.startOffset - document.getLineStartOffset(line)
                                    val preview = getPreviewText(document, textRange)

                                    results.add(
                                        SearchResult(
                                            filePath = psiFile.virtualFile?.path ?: psiFile.name,
                                            line = line + 1,
                                            column = column,
                                            preview = preview,
                                            matchType = "structure"
                                        )
                                    )

                                    // 如果指定了最大结果数且已达到，停止匹配
                                    val maxResults = request.maxResults ?: 0
                                    if (maxResults > 0 && results.size >= maxResults) {
                                        matchingProcess?.stop()
                                    }
                                }
                            }
                        }
                    }

                    override fun processFile(file: com.intellij.psi.PsiFile) {
                        // 必须实现的回调，用于处理文件
                    }

                    override fun matchingFinished() {
                        // 匹配完成的回调
                    }
                }

                // 执行结构化搜索
                matcher.testFindMatches(matchResultSink)

                val maxResults = request.maxResults ?: 0
                StructuralSearchResponse(
                    success = true,
                    pattern = request.pattern,
                    results = results.take(maxResults.takeIf { it > 0 } ?: results.size),
                    totalResults = results.size
                )
            } catch (e: Exception) {
                logger.error("Structural search failed", e)
                StructuralSearchResponse(
                    success = false,
                    pattern = request.pattern,
                    results = emptyList(),
                    totalResults = 0
                )
            }
        }
    }

    /**
     * 从类创建搜索结果
     */
    private fun createSearchResultFromClass(psiClass: PsiClass): SearchResult {
        val file = psiClass.containingFile
        val document = com.intellij.psi.PsiDocumentManager.getInstance(project).getDocument(file)
        val textRange = psiClass.textRange

        val line = document?.getLineNumber(textRange.startOffset) ?: 0
        val column = if (document != null) {
            textRange.startOffset - document.getLineStartOffset(line)
        } else {
            0
        }

        return SearchResult(
            filePath = file.virtualFile?.path ?: file.name,
            line = line + 1,
            column = column,
            preview = psiClass.text.take(100),
            matchType = "class"
        )
    }

    /**
     * 从方法创建搜索结果
     */
    private fun createSearchResultFromMethod(method: PsiMethod): SearchResult {
        val file = method.containingFile
        val document = com.intellij.psi.PsiDocumentManager.getInstance(project).getDocument(file)
        val textRange = method.textRange

        val line = document?.getLineNumber(textRange.startOffset) ?: 0
        val column = if (document != null) {
            textRange.startOffset - document.getLineStartOffset(line)
        } else {
            0
        }

        return SearchResult(
            filePath = file.virtualFile?.path ?: file.name,
            line = line + 1,
            column = column,
            preview = method.text.take(100),
            matchType = "method"
        )
    }

    /**
     * 获取预览文本
     */
    private fun getPreviewText(
        document: com.intellij.openapi.editor.Document,
        textRange: TextRange
    ): String {
        val lineStart = document.getLineStartOffset(document.getLineNumber(textRange.startOffset))
        val lineEnd = document.getLineEndOffset(document.getLineNumber(textRange.endOffset))
        return document.getText(TextRange(lineStart, lineEnd)).trim()
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
