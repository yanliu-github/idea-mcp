package com.ly.ideamcp.util

import com.intellij.openapi.editor.Document
import com.ly.ideamcp.model.CodeLocation

/**
 * 偏移量辅助工具
 * 处理行列号与偏移量之间的转换
 */
object OffsetHelper {

    /**
     * 将行列号转换为偏移量
     * @param document 文档
     * @param line 行号（从1开始）
     * @param column 列号（从1开始）
     * @return 偏移量，如果转换失败返回 null
     */
    fun lineColumnToOffset(document: Document, line: Int, column: Int): Int? {
        try {
            // 检查行号有效性
            if (line < 1 || line > document.lineCount) {
                return null
            }

            // 行号从1开始，但 Document API 从0开始
            val lineIndex = line - 1
            val lineStartOffset = document.getLineStartOffset(lineIndex)
            val lineEndOffset = document.getLineEndOffset(lineIndex)

            // 列号从1开始
            val offset = lineStartOffset + (column - 1)

            // 检查偏移量是否在行范围内
            if (offset < lineStartOffset || offset > lineEndOffset) {
                return null
            }

            return offset
        } catch (e: Exception) {
            return null
        }
    }

    /**
     * 将偏移量转换为行列号
     * @param document 文档
     * @param offset 偏移量（从0开始）
     * @return 行列号 (line, column)，如果转换失败返回 null
     */
    fun offsetToLineColumn(document: Document, offset: Int): Pair<Int, Int>? {
        try {
            // 检查偏移量有效性
            if (offset < 0 || offset > document.textLength) {
                return null
            }

            val lineNumber = document.getLineNumber(offset)
            val lineStartOffset = document.getLineStartOffset(lineNumber)
            val column = offset - lineStartOffset

            // 返回基于1的行列号
            return Pair(lineNumber + 1, column + 1)
        } catch (e: Exception) {
            return null
        }
    }

    /**
     * 创建带行列号的 CodeLocation
     * @param filePath 文件路径
     * @param document 文档
     * @param offset 偏移量
     * @return CodeLocation
     */
    fun createLocation(filePath: String, document: Document, offset: Int): CodeLocation? {
        val (line, column) = offsetToLineColumn(document, offset) ?: return null
        return CodeLocation(filePath, offset, line, column)
    }

    /**
     * 检查偏移量是否有效
     * @param document 文档
     * @param offset 偏移量
     * @return 是否有效
     */
    fun isValidOffset(document: Document, offset: Int): Boolean {
        return offset >= 0 && offset <= document.textLength
    }

    /**
     * 检查行号是否有效
     * @param document 文档
     * @param line 行号（从1开始）
     * @return 是否有效
     */
    fun isValidLine(document: Document, line: Int): Boolean {
        return line >= 1 && line <= document.lineCount
    }

    /**
     * 获取行的文本内容
     * @param document 文档
     * @param line 行号（从1开始）
     * @return 行内容，如果行号无效返回 null
     */
    fun getLineText(document: Document, line: Int): String? {
        if (!isValidLine(document, line)) {
            return null
        }

        val lineIndex = line - 1
        val startOffset = document.getLineStartOffset(lineIndex)
        val endOffset = document.getLineEndOffset(lineIndex)

        return document.getText(com.intellij.openapi.util.TextRange(startOffset, endOffset))
    }

    /**
     * 获取指定范围的文本
     * @param document 文档
     * @param startOffset 起始偏移量
     * @param endOffset 结束偏移量
     * @return 文本内容，如果范围无效返回 null
     */
    fun getRangeText(document: Document, startOffset: Int, endOffset: Int): String? {
        if (!isValidOffset(document, startOffset) || !isValidOffset(document, endOffset)) {
            return null
        }

        if (startOffset > endOffset) {
            return null
        }

        return document.getText(com.intellij.openapi.util.TextRange(startOffset, endOffset))
    }

    /**
     * 规范化偏移量（确保在有效范围内）
     * @param document 文档
     * @param offset 偏移量
     * @return 规范化后的偏移量
     */
    fun normalizeOffset(document: Document, offset: Int): Int {
        return when {
            offset < 0 -> 0
            offset > document.textLength -> document.textLength
            else -> offset
        }
    }
}
