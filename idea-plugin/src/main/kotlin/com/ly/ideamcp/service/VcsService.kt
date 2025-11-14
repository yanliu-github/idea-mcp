package com.ly.ideamcp.service

import com.intellij.openapi.components.Service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.ly.ideamcp.model.vcs.*
import com.ly.ideamcp.util.ThreadHelper
import com.ly.ideamcp.util.PsiHelper

/**
 * 版本控制服务
 * 提供 Git 状态、历史、差异和 blame 功能
 */
@Service(Service.Level.PROJECT)
class VcsService(private val project: Project) {

    private val logger = Logger.getInstance(VcsService::class.java)

    /**
     * 获取 VCS 状态
     * @return VCS 状态响应
     */
    fun getStatus(): VcsStatusResponse {
        logger.info("Getting VCS status")

        return ThreadHelper.runReadAction {
            // 占位实现 - 实际应使用 ChangeListManager
            VcsStatusResponse(
                success = true,
                changes = listOf(
                    ChangeInfo(
                        filePath = "src/main/Example.java",
                        changeType = "MODIFIED",
                        oldRevision = "abc123",
                        newRevision = "def456"
                    )
                ),
                totalChanges = 1
            )
        }
    }

    /**
     * 获取文件历史
     * @param request 文件历史请求
     * @return 文件历史响应
     */
    fun getHistory(request: VcsHistoryRequest): VcsHistoryResponse {
        logger.info("Getting history for file: ${request.filePath}")

        return ThreadHelper.runReadAction {
            val psiFile = PsiHelper.findPsiFile(project, request.filePath)
                ?: throw IllegalArgumentException("File not found: ${request.filePath}")

            // 占位实现 - 实际应使用 AbstractVcs.getCommittedChangesProvider
            VcsHistoryResponse(
                success = true,
                filePath = request.filePath,
                commits = listOf(
                    CommitInfo(
                        hash = "abc123",
                        author = "John Doe",
                        date = "2025-11-15T10:00:00",
                        message = "Initial commit",
                        files = listOf(request.filePath)
                    )
                ),
                totalCommits = 1
            )
        }
    }

    /**
     * 获取文件差异
     * @param request 差异请求
     * @return 差异响应
     */
    fun getDiff(request: VcsDiffRequest): VcsDiffResponse {
        logger.info("Getting diff for file: ${request.filePath}")

        return ThreadHelper.runReadAction {
            val psiFile = PsiHelper.findPsiFile(project, request.filePath)
                ?: throw IllegalArgumentException("File not found: ${request.filePath}")

            // 占位实现 - 实际应使用 VcsUtil.getFileDiff
            VcsDiffResponse(
                success = true,
                filePath = request.filePath,
                diff = "--- a/${request.filePath}\n+++ b/${request.filePath}\n@@ -1,1 +1,1 @@\n-old line\n+new line",
                oldRevision = request.oldRevision,
                newRevision = request.newRevision
            )
        }
    }

    /**
     * 获取文件 blame (注解)
     * @param request blame 请求
     * @return blame 响应
     */
    fun blame(request: VcsBlameRequest): VcsBlameResponse {
        logger.info("Getting blame for file: ${request.filePath}")

        return ThreadHelper.runReadAction {
            val psiFile = PsiHelper.findPsiFile(project, request.filePath)
                ?: throw IllegalArgumentException("File not found: ${request.filePath}")

            // 占位实现 - 实际应使用 AnnotationProvider
            VcsBlameResponse(
                success = true,
                filePath = request.filePath,
                annotations = listOf(
                    LineAnnotation(
                        lineNumber = 1,
                        commitHash = "abc123",
                        author = "John Doe",
                        date = "2025-11-15T10:00:00",
                        commitMessage = "Initial commit"
                    )
                ),
                totalLines = 1
            )
        }
    }

    companion object {
        /**
         * 获取服务实例
         */
        fun getInstance(project: Project): VcsService {
            return project.getService(VcsService::class.java)
        }
    }
}
