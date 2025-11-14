package com.ly.ideamcp.service

import com.intellij.openapi.components.Service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.vcs.ProjectLevelVcsManager
import com.intellij.openapi.vcs.VcsException
import com.intellij.openapi.vcs.changes.Change
import com.intellij.openapi.vcs.changes.ChangeListManager
import com.intellij.openapi.vcs.history.VcsFileRevision
import com.intellij.openapi.vcs.history.VcsHistoryProvider
import com.intellij.openapi.vcs.history.VcsHistorySession
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.vcsUtil.VcsUtil
import com.ly.ideamcp.model.vcs.*
import com.ly.ideamcp.util.ThreadHelper
import com.ly.ideamcp.util.PsiHelper
import java.text.SimpleDateFormat
import java.util.*

/**
 * 版本控制服务
 * 提供 Git 状态、历史、差异和 blame 功能
 */
@Service(Service.Level.PROJECT)
class VcsService(private val project: Project) {

    private val logger = Logger.getInstance(VcsService::class.java)
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss")

    /**
     * 获取 VCS 状态
     * @return VCS 状态响应
     */
    fun getStatus(): VcsStatusResponse {
        logger.info("Getting VCS status")

        return ThreadHelper.runReadAction {
            try {
                val changeListManager = ChangeListManager.getInstance(project)
                val changes = mutableListOf<ChangeInfo>()

                // 获取所有变更
                changeListManager.allChanges.forEach { change ->
                    changes.add(createChangeInfo(change))
                }

                VcsStatusResponse(
                    success = true,
                    changes = changes,
                    totalChanges = changes.size
                )
            } catch (e: Exception) {
                logger.error("Failed to get VCS status", e)
                VcsStatusResponse(
                    success = false,
                    changes = emptyList(),
                    totalChanges = 0
                )
            }
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
            try {
                val virtualFile = VcsUtil.getVirtualFile(request.filePath)
                    ?: throw IllegalArgumentException("File not found: ${request.filePath}")

                val vcsManager = ProjectLevelVcsManager.getInstance(project)
                val vcs = vcsManager.getVcsFor(virtualFile)
                    ?: throw IllegalArgumentException("No VCS found for file: ${request.filePath}")

                val historyProvider = vcs.vcsHistoryProvider
                    ?: throw IllegalArgumentException("VCS history provider not available")

                val commits = mutableListOf<CommitInfo>()

                // 获取文件历史
                val session = historyProvider.createSessionFor(vcs.getVcsRootFor(virtualFile)!!, virtualFile)
                if (session != null) {
                    val revisions = session.revisionList
                    val maxRevisions = if (request.maxResults > 0) request.maxResults else revisions.size

                    revisions.take(maxRevisions).forEach { revision ->
                        commits.add(createCommitInfo(revision, request.filePath))
                    }
                }

                VcsHistoryResponse(
                    success = true,
                    filePath = request.filePath,
                    commits = commits,
                    totalCommits = commits.size
                )
            } catch (e: Exception) {
                logger.error("Failed to get file history", e)
                VcsHistoryResponse(
                    success = false,
                    filePath = request.filePath,
                    commits = emptyList(),
                    totalCommits = 0
                )
            }
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
            try {
                val virtualFile = VcsUtil.getVirtualFile(request.filePath)
                    ?: throw IllegalArgumentException("File not found: ${request.filePath}")

                val vcsManager = ProjectLevelVcsManager.getInstance(project)
                val vcs = vcsManager.getVcsFor(virtualFile)
                    ?: throw IllegalArgumentException("No VCS found for file: ${request.filePath}")

                val changeListManager = ChangeListManager.getInstance(project)
                val change = changeListManager.getChange(virtualFile)

                val diff = if (change != null) {
                    generateDiffText(change)
                } else {
                    "No changes detected"
                }

                VcsDiffResponse(
                    success = true,
                    filePath = request.filePath,
                    diff = diff,
                    oldRevision = request.oldRevision,
                    newRevision = request.newRevision
                )
            } catch (e: Exception) {
                logger.error("Failed to get diff", e)
                VcsDiffResponse(
                    success = false,
                    filePath = request.filePath,
                    diff = "",
                    oldRevision = request.oldRevision,
                    newRevision = request.newRevision
                )
            }
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
            try {
                val virtualFile = VcsUtil.getVirtualFile(request.filePath)
                    ?: throw IllegalArgumentException("File not found: ${request.filePath}")

                val vcsManager = ProjectLevelVcsManager.getInstance(project)
                val vcs = vcsManager.getVcsFor(virtualFile)
                    ?: throw IllegalArgumentException("No VCS found for file: ${request.filePath}")

                val annotationProvider = vcs.annotationProvider
                    ?: throw IllegalArgumentException("Annotation provider not available")

                val annotations = mutableListOf<LineAnnotation>()

                try {
                    val fileAnnotation = annotationProvider.annotate(virtualFile)
                    if (fileAnnotation != null) {
                        val lineCount = fileAnnotation.lineCount

                        for (lineNumber in 0 until lineCount) {
                            val revision = fileAnnotation.getLineRevisionNumber(lineNumber)
                            if (revision != null) {
                                val date = fileAnnotation.getLineDate(lineNumber)
                                val author = fileAnnotation.getLineAuthor(lineNumber)

                                annotations.add(
                                    LineAnnotation(
                                        lineNumber = lineNumber + 1,
                                        commitHash = revision.asString(),
                                        author = author ?: "Unknown",
                                        date = if (date != null) dateFormat.format(date) else "Unknown",
                                        commitMessage = "" // 需要额外查询获取提交消息
                                    )
                                )
                            }
                        }

                        fileAnnotation.dispose()
                    }
                } catch (e: VcsException) {
                    logger.warn("Failed to get annotation", e)
                }

                VcsBlameResponse(
                    success = true,
                    filePath = request.filePath,
                    annotations = annotations,
                    totalLines = annotations.size
                )
            } catch (e: Exception) {
                logger.error("Failed to get blame", e)
                VcsBlameResponse(
                    success = false,
                    filePath = request.filePath,
                    annotations = emptyList(),
                    totalLines = 0
                )
            }
        }
    }

    /**
     * 从 Change 创建 ChangeInfo
     */
    private fun createChangeInfo(change: Change): ChangeInfo {
        val filePath = when {
            change.afterRevision != null -> change.afterRevision!!.file.path
            change.beforeRevision != null -> change.beforeRevision!!.file.path
            else -> "Unknown"
        }

        val changeType = when (change.type) {
            Change.Type.NEW -> "NEW"
            Change.Type.DELETED -> "DELETED"
            Change.Type.MOVED -> "MOVED"
            Change.Type.MODIFICATION -> "MODIFIED"
            else -> "UNKNOWN"
        }

        val oldRevision = change.beforeRevision?.revisionNumber?.asString() ?: ""
        val newRevision = change.afterRevision?.revisionNumber?.asString() ?: ""

        return ChangeInfo(
            filePath = filePath,
            changeType = changeType,
            oldRevision = oldRevision,
            newRevision = newRevision
        )
    }

    /**
     * 从 VcsFileRevision 创建 CommitInfo
     */
    private fun createCommitInfo(revision: VcsFileRevision, filePath: String): CommitInfo {
        return CommitInfo(
            hash = revision.revisionNumber.asString(),
            author = revision.author ?: "Unknown",
            date = if (revision.revisionDate != null) {
                dateFormat.format(revision.revisionDate)
            } else {
                "Unknown"
            },
            message = revision.commitMessage ?: "",
            files = listOf(filePath)
        )
    }

    /**
     * 生成差异文本
     */
    private fun generateDiffText(change: Change): String {
        val beforeContent = change.beforeRevision?.content ?: ""
        val afterContent = change.afterRevision?.content ?: ""

        val filePath = change.afterRevision?.file?.path ?: change.beforeRevision?.file?.path ?: "Unknown"

        // 简单的差异格式
        val diff = StringBuilder()
        diff.append("--- a/$filePath\n")
        diff.append("+++ b/$filePath\n")

        val beforeLines = beforeContent.split("\n")
        val afterLines = afterContent.split("\n")

        // 基本的行对比 (简化版)
        diff.append("@@ -1,${beforeLines.size} +1,${afterLines.size} @@\n")

        val maxLines = maxOf(beforeLines.size, afterLines.size)
        for (i in 0 until maxLines) {
            if (i < beforeLines.size && i < afterLines.size) {
                if (beforeLines[i] != afterLines[i]) {
                    diff.append("-${beforeLines[i]}\n")
                    diff.append("+${afterLines[i]}\n")
                } else {
                    diff.append(" ${beforeLines[i]}\n")
                }
            } else if (i < beforeLines.size) {
                diff.append("-${beforeLines[i]}\n")
            } else if (i < afterLines.size) {
                diff.append("+${afterLines[i]}\n")
            }
        }

        return diff.toString()
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
