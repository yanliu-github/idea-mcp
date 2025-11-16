package com.ly.ideamcp.util

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiManager
import com.intellij.openapi.editor.Document
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.psi.util.PsiUtilCore
import com.intellij.psi.codeStyle.CodeStyleManager
import com.ly.ideamcp.model.CodeLocation
import com.ly.ideamcp.model.CodeRange
import java.io.File

/**
 * PSI (Program Structure Interface) 辅助工具
 * 提供 PSI 相关的常用操作
 */
object PsiHelper {

    /**
     * 根据文件路径查找 PSI 文件
     * @param project 项目
     * @param filePath 文件路径（相对于项目根目录或绝对路径）
     * @return PSI 文件，如果找不到返回 null
     */
    fun findPsiFile(project: Project, filePath: String): PsiFile? {
        val virtualFile = findVirtualFile(project, filePath) ?: return null
        return PsiManager.getInstance(project).findFile(virtualFile)
    }

    /**
     * 根据文件路径查找虚拟文件
     * @param project 项目
     * @param filePath 文件路径
     * @return 虚拟文件，如果找不到返回 null
     */
    fun findVirtualFile(project: Project, filePath: String): VirtualFile? {
        // 尝试作为绝对路径
        val absoluteFile = File(filePath)
        if (absoluteFile.isAbsolute && absoluteFile.exists()) {
            return VirtualFileManager.getInstance().findFileByUrl("file://$filePath")
        }

        // 尝试作为相对于项目根目录的路径
        val projectBasePath = project.basePath ?: return null
        val relativeFile = File(projectBasePath, filePath)
        if (relativeFile.exists()) {
            return VirtualFileManager.getInstance().findFileByUrl("file://${relativeFile.absolutePath}")
        }

        return null
    }

    /**
     * 在指定偏移量处查找 PSI 元素
     * @param psiFile PSI 文件
     * @param offset 偏移量
     * @return PSI 元素，如果找不到返回 null
     */
    fun findElementAtOffset(psiFile: PsiFile, offset: Int): PsiElement? {
        if (offset < 0 || offset > psiFile.textLength) {
            return null
        }
        return psiFile.findElementAt(offset)
    }

    /**
     * 获取元素的文本范围
     * @param element PSI 元素
     * @return 文本范围 (startOffset, endOffset)
     */
    fun getTextRange(element: PsiElement): Pair<Int, Int> {
        val range = element.textRange
        return Pair(range.startOffset, range.endOffset)
    }

    /**
     * 获取元素的文本内容
     * @param element PSI 元素
     * @return 文本内容
     */
    fun getText(element: PsiElement): String {
        return element.text
    }

    /**
     * 获取文件的 Document
     * @param psiFile PSI 文件
     * @return Document，如果找不到返回 null
     */
    fun getDocument(psiFile: PsiFile): Document? {
        return PsiUtilCore.getVirtualFile(psiFile)?.let {
            FileDocumentManager.getInstance().getDocument(it)
        }
    }

    /**
     * 检查 PSI 文件是否有效
     * @param psiFile PSI 文件
     * @return 是否有效
     */
    fun isValid(psiFile: PsiFile): Boolean {
        return psiFile.isValid && psiFile.virtualFile != null
    }

    /**
     * 检查 PSI 元素是否有效
     * @param element PSI 元素
     * @return 是否有效
     */
    fun isValid(element: PsiElement): Boolean {
        return element.isValid
    }

    /**
     * 将 CodeLocation 转换为偏移量
     * @param project 项目
     * @param location 代码位置
     * @return 偏移量，如果转换失败返回 null
     */
    fun locationToOffset(project: Project, location: CodeLocation): Int? {
        // 如果已经有 offset，直接返回
        if (location.offset >= 0) {
            return location.offset
        }

        // 如果有行列号，转换为 offset
        val line = location.line ?: return null
        val column = location.column ?: return null

        val psiFile = findPsiFile(project, location.filePath) ?: return null
        val document = getDocument(psiFile) ?: return null

        return OffsetHelper.lineColumnToOffset(document, line, column)
    }

    /**
     * 将 CodeRange 转换为偏移量范围
     * @param project 项目
     * @param range 代码范围
     * @return 偏移量范围 (startOffset, endOffset)，如果转换失败返回 null
     */
    fun rangeToOffsets(project: Project, range: CodeRange): Pair<Int, Int>? {
        val psiFile = findPsiFile(project, range.filePath) ?: return null
        val document = getDocument(psiFile) ?: return null

        val startOffset = if (range.startOffset >= 0) {
            range.startOffset
        } else {
            val line = range.startLine ?: return null
            val column = range.startColumn ?: return null
            OffsetHelper.lineColumnToOffset(document, line, column) ?: return null
        }

        val endOffset = if (range.endOffset >= 0) {
            range.endOffset
        } else {
            val line = range.endLine ?: return null
            val column = range.endColumn ?: return null
            OffsetHelper.lineColumnToOffset(document, line, column) ?: return null
        }

        return Pair(startOffset, endOffset)
    }

    /**
     * 获取相对于项目根目录的路径
     * @param project 项目
     * @param virtualFile 虚拟文件
     * @return 相对路径
     */
    fun getRelativePath(project: Project, virtualFile: VirtualFile): String {
        val projectBasePath = project.basePath ?: return virtualFile.path
        val filePath = virtualFile.path

        return if (filePath.startsWith(projectBasePath)) {
            filePath.substring(projectBasePath.length).trimStart('/', '\\')
        } else {
            filePath
        }
    }

    /**
     * 格式化代码
     * @param project 项目
     * @param element PSI 元素
     */
    fun reformatCode(project: Project, element: PsiElement) {
        CodeStyleManager.getInstance(project).reformat(element)
    }

    /**
     * 格式化代码文件
     * @param project 项目
     * @param psiFile PSI 文件
     */
    fun reformatCode(project: Project, psiFile: PsiFile) {
        CodeStyleManager.getInstance(project).reformat(psiFile)
    }
}
