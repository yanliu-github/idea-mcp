package com.ly.ideamcp.util

import com.intellij.openapi.editor.Document
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.EditorFactory
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFile

/**
 * 编辑器辅助工具
 * 安全地管理 Editor 资源，避免内存泄漏
 */
object EditorHelper {

    /**
     * 安全地使用编辑器执行操作
     * 自动管理编辑器的创建和释放
     *
     * @param T 返回类型
     * @param psiFile PSI 文件
     * @param block 使用编辑器执行的操作
     * @return 操作结果
     */
    inline fun <T> useEditor(psiFile: PsiFile, block: (Editor) -> T): T {
        val document = psiFile.viewProvider.document
            ?: throw IllegalStateException("Cannot get document for file: ${psiFile.name}")

        val editor = EditorFactory.getInstance().createEditor(document, psiFile.project)
        return try {
            block(editor)
        } finally {
            EditorFactory.getInstance().releaseEditor(editor)
        }
    }

    /**
     * 安全地使用编辑器执行操作（带选区设置）
     *
     * @param T 返回类型
     * @param psiFile PSI 文件
     * @param startOffset 选区开始位置
     * @param endOffset 选区结束位置
     * @param block 使用编辑器执行的操作
     * @return 操作结果
     */
    inline fun <T> useEditorWithSelection(
        psiFile: PsiFile,
        startOffset: Int,
        endOffset: Int,
        block: (Editor) -> T
    ): T {
        return useEditor(psiFile) { editor ->
            editor.selectionModel.setSelection(startOffset, endOffset)
            editor.caretModel.moveToOffset(startOffset)
            block(editor)
        }
    }

    /**
     * 安全地使用编辑器执行写操作
     *
     * @param T 返回类型
     * @param psiFile PSI 文件
     * @param block 使用编辑器执行的操作
     * @return 操作结果
     */
    inline fun <T> useEditorInWriteAction(psiFile: PsiFile, crossinline block: (Editor) -> T): T {
        return ThreadHelper.runWriteAction {
            useEditor(psiFile) { editor -> block(editor) }
        }
    }

    /**
     * 安全地使用编辑器执行带选区的写操作
     *
     * @param T 返回类型
     * @param psiFile PSI 文件
     * @param startOffset 选区开始位置
     * @param endOffset 选区结束位置
     * @param block 使用编辑器执行的操作
     * @return 操作结果
     */
    inline fun <T> useEditorWithSelectionInWriteAction(
        psiFile: PsiFile,
        startOffset: Int,
        endOffset: Int,
        crossinline block: (Editor) -> T
    ): T {
        return ThreadHelper.runWriteAction {
            useEditorWithSelection(psiFile, startOffset, endOffset) { editor -> block(editor) }
        }
    }

    /**
     * 从文件路径获取 Document
     *
     * @param project 项目
     * @param filePath 文件路径
     * @return Document 或 null
     */
    fun getDocument(project: Project, filePath: String): Document? {
        val psiFile = PsiHelper.findPsiFile(project, filePath) ?: return null
        return psiFile.viewProvider.document
    }

    /**
     * 保存文档
     *
     * @param document 要保存的文档
     */
    fun saveDocument(document: Document) {
        FileDocumentManager.getInstance().saveDocument(document)
    }

    /**
     * 保存所有文档
     */
    fun saveAllDocuments() {
        FileDocumentManager.getInstance().saveAllDocuments()
    }

    /**
     * 从行号和列号计算偏移量
     *
     * @param document 文档
     * @param line 行号（1-based）
     * @param column 列号（1-based）
     * @return 偏移量
     */
    fun calculateOffset(document: Document, line: Int, column: Int): Int {
        val lineIndex = (line - 1).coerceIn(0, document.lineCount - 1)
        val lineStartOffset = document.getLineStartOffset(lineIndex)
        val lineEndOffset = document.getLineEndOffset(lineIndex)
        val maxColumn = lineEndOffset - lineStartOffset
        val columnIndex = (column - 1).coerceIn(0, maxColumn)
        return lineStartOffset + columnIndex
    }

    /**
     * 从偏移量计算行号和列号
     *
     * @param document 文档
     * @param offset 偏移量
     * @return Pair<行号, 列号>（1-based）
     */
    fun calculateLineColumn(document: Document, offset: Int): Pair<Int, Int> {
        val safeOffset = offset.coerceIn(0, document.textLength)
        val line = document.getLineNumber(safeOffset)
        val lineStartOffset = document.getLineStartOffset(line)
        val column = safeOffset - lineStartOffset
        return Pair(line + 1, column + 1)
    }
}
