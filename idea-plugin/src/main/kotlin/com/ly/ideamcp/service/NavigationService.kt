package com.ly.ideamcp.service

import com.intellij.openapi.components.Service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.psi.*
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.search.searches.*
import com.intellij.psi.util.PsiTreeUtil
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

            // 查找类元素（向上遍历 PSI 树）
            val psiClass = PsiTreeUtil.getParentOfType(element, PsiClass::class.java)
                ?: throw IllegalArgumentException("Not a class element")

            val className = psiClass.name ?: "Unknown"

            // 收集父类型（超类和接口）
            val supertypes = mutableListOf<TypeInfo>()

            // 添加超类
            var currentClass: PsiClass? = psiClass
            var depth = 0
            while (depth < 10) { // 限制递归深度防止无限循环
                currentClass = currentClass?.superClass
                if (currentClass == null || currentClass.qualifiedName == "java.lang.Object") {
                    break
                }

                val typeInfo = createTypeInfo(currentClass)
                if (typeInfo != null) {
                    supertypes.add(typeInfo)
                }
                depth++
            }

            // 添加直接实现的接口
            psiClass.interfaces.forEach { iface ->
                val typeInfo = createTypeInfo(iface)
                if (typeInfo != null) {
                    supertypes.add(typeInfo)
                }
            }

            // 收集子类型（使用 ClassInheritorsSearch）
            val subtypes = mutableListOf<TypeInfo>()
            val searchScope = GlobalSearchScope.projectScope(project)
            val inheritorsQuery = ClassInheritorsSearch.search(psiClass, searchScope, true)

            inheritorsQuery.forEach { inheritor ->
                val typeInfo = createTypeInfo(inheritor)
                if (typeInfo != null) {
                    subtypes.add(typeInfo)
                }
            }

            logger.info("Type hierarchy for $className: ${supertypes.size} supertypes, ${subtypes.size} subtypes")

            TypeHierarchyResponse(
                success = true,
                className = className,
                supertypes = supertypes,
                subtypes = subtypes
            )
        }
    }

    /**
     * 从 PsiClass 创建 TypeInfo
     */
    private fun createTypeInfo(psiClass: PsiClass): TypeInfo? {
        val name = psiClass.name ?: return null
        val qualifiedName = psiClass.qualifiedName ?: name

        val containingFile = psiClass.containingFile?.virtualFile ?: return null
        val document = PsiHelper.getDocument(psiClass.containingFile) ?: return null

        val offset = psiClass.textRange.startOffset
        val location = OffsetHelper.createLocation(
            PsiHelper.getRelativePath(project, containingFile),
            document,
            offset
        ) ?: return null

        return TypeInfo(
            name = name,
            qualifiedName = qualifiedName,
            location = location
        )
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

            // 查找方法元素（向上遍历 PSI 树）
            val psiMethod = PsiTreeUtil.getParentOfType(element, PsiMethod::class.java)
                ?: throw IllegalArgumentException("Not a method element")

            val methodName = psiMethod.name
            val containingClass = psiMethod.containingClass

            // 查找调用者（Callers）- 谁调用了这个方法
            val callers = mutableListOf<CallInfo>()
            val references = MethodReferencesSearch.search(psiMethod).findAll()

            for (ref in references) {
                try {
                    val callInfo = createCallInfoFromReference(ref)
                    if (callInfo != null) {
                        callers.add(callInfo)
                    }
                } catch (e: Exception) {
                    logger.warn("Failed to create call info from reference", e)
                }
            }

            // 查找被调用者（Callees）- 这个方法调用了谁
            val callees = mutableListOf<CallInfo>()
            val methodBody = psiMethod.body

            if (methodBody != null) {
                // 收集方法体内所有的方法调用
                val methodCalls = PsiTreeUtil.findChildrenOfType(methodBody, PsiMethodCallExpression::class.java)

                for (call in methodCalls) {
                    try {
                        val resolvedMethod = call.resolveMethod()
                        if (resolvedMethod != null) {
                            val callInfo = createCallInfoFromMethod(resolvedMethod, call)
                            if (callInfo != null) {
                                callees.add(callInfo)
                            }
                        }
                    } catch (e: Exception) {
                        logger.warn("Failed to create call info from method call", e)
                    }
                }
            }

            logger.info("Call hierarchy for $methodName: ${callers.size} callers, ${callees.size} callees")

            CallHierarchyResponse(
                success = true,
                methodName = methodName,
                callers = callers,
                callees = callees
            )
        }
    }

    /**
     * 从引用创建 CallInfo（用于 callers）
     */
    private fun createCallInfoFromReference(ref: PsiReference): CallInfo? {
        val refElement = ref.element
        val containingMethod = PsiTreeUtil.getParentOfType(refElement, PsiMethod::class.java)
            ?: return null

        val methodName = containingMethod.name
        val className = containingMethod.containingClass?.name ?: "Unknown"

        val file = containingMethod.containingFile?.virtualFile ?: return null
        val document = PsiHelper.getDocument(containingMethod.containingFile) ?: return null

        val offset = containingMethod.textRange.startOffset
        val location = OffsetHelper.createLocation(
            PsiHelper.getRelativePath(project, file),
            document,
            offset
        ) ?: return null

        return CallInfo(
            methodName = methodName,
            className = className,
            location = location
        )
    }

    /**
     * 从方法创建 CallInfo（用于 callees）
     */
    private fun createCallInfoFromMethod(method: PsiMethod, callSite: PsiMethodCallExpression): CallInfo? {
        val methodName = method.name
        val className = method.containingClass?.name ?: "Unknown"

        val file = method.containingFile?.virtualFile ?: return null
        val document = PsiHelper.getDocument(method.containingFile) ?: return null

        val offset = method.textRange.startOffset
        val location = OffsetHelper.createLocation(
            PsiHelper.getRelativePath(project, file),
            document,
            offset
        ) ?: return null

        return CallInfo(
            methodName = methodName,
            className = className,
            location = location
        )
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

            val implementations = mutableListOf<ImplementationInfo>()
            var elementName = getElementName(element)

            // 尝试解析为类或方法
            val psiClass = PsiTreeUtil.getParentOfType(element, PsiClass::class.java)
            val psiMethod = PsiTreeUtil.getParentOfType(element, PsiMethod::class.java)

            when {
                // 1. 如果是方法 - 查找重写该方法的所有实现
                psiMethod != null -> {
                    elementName = psiMethod.name
                    val searchScope = GlobalSearchScope.projectScope(project)

                    // 使用 OverridingMethodsSearch 查找所有重写方法
                    val overridingMethods = OverridingMethodsSearch.search(psiMethod, searchScope, true)

                    overridingMethods.forEach { overridingMethod ->
                        val implInfo = createImplementationInfoFromMethod(overridingMethod)
                        if (implInfo != null) {
                            implementations.add(implInfo)
                        }
                    }
                }

                // 2. 如果是类/接口 - 查找所有实现类
                psiClass != null -> {
                    elementName = psiClass.name ?: "Unknown"
                    val searchScope = GlobalSearchScope.projectScope(project)

                    // 对于接口或抽象类，查找所有继承者
                    if (psiClass.isInterface || psiClass.hasModifierProperty(PsiModifier.ABSTRACT)) {
                        val inheritors = ClassInheritorsSearch.search(psiClass, searchScope, true)

                        inheritors.forEach { inheritor ->
                            // 仅包括具体类（非接口、非抽象类）
                            if (!inheritor.isInterface && !inheritor.hasModifierProperty(PsiModifier.ABSTRACT)) {
                                val implInfo = createImplementationInfoFromClass(inheritor)
                                if (implInfo != null) {
                                    implementations.add(implInfo)
                                }
                            }
                        }
                    }
                }

                // 3. 其他元素 - 使用 DefinitionsScopedSearch 查找定义
                else -> {
                    val searchScope = GlobalSearchScope.projectScope(project)
                    val definitions = DefinitionsScopedSearch.search(element, searchScope)

                    definitions.forEach { definition ->
                        val implInfo = createImplementationInfoFromElement(definition)
                        if (implInfo != null) {
                            implementations.add(implInfo)
                        }
                    }
                }
            }

            logger.info("Found ${implementations.size} implementations for $elementName")

            FindImplementationsResponse(
                success = true,
                elementName = elementName,
                implementations = implementations,
                totalImplementations = implementations.size
            )
        }
    }

    /**
     * 从方法创建 ImplementationInfo
     */
    private fun createImplementationInfoFromMethod(method: PsiMethod): ImplementationInfo? {
        val name = method.name
        val containingClass = method.containingClass
        val qualifiedName = if (containingClass != null) {
            "${containingClass.qualifiedName}.$name"
        } else {
            name
        }

        val file = method.containingFile?.virtualFile ?: return null
        val document = PsiHelper.getDocument(method.containingFile) ?: return null

        val offset = method.textRange.startOffset
        val location = OffsetHelper.createLocation(
            PsiHelper.getRelativePath(project, file),
            document,
            offset
        ) ?: return null

        return ImplementationInfo(
            name = name,
            qualifiedName = qualifiedName,
            location = location
        )
    }

    /**
     * 从类创建 ImplementationInfo
     */
    private fun createImplementationInfoFromClass(psiClass: PsiClass): ImplementationInfo? {
        val name = psiClass.name ?: return null
        val qualifiedName = psiClass.qualifiedName ?: name

        val file = psiClass.containingFile?.virtualFile ?: return null
        val document = PsiHelper.getDocument(psiClass.containingFile) ?: return null

        val offset = psiClass.textRange.startOffset
        val location = OffsetHelper.createLocation(
            PsiHelper.getRelativePath(project, file),
            document,
            offset
        ) ?: return null

        return ImplementationInfo(
            name = name,
            qualifiedName = qualifiedName,
            location = location
        )
    }

    /**
     * 从元素创建 ImplementationInfo
     */
    private fun createImplementationInfoFromElement(element: PsiElement): ImplementationInfo? {
        val name = getElementName(element)
        val qualifiedName = when (element) {
            is PsiClass -> element.qualifiedName ?: name
            is PsiMethod -> {
                val className = element.containingClass?.qualifiedName ?: ""
                "$className.${element.name}"
            }
            else -> name
        }

        val file = element.containingFile?.virtualFile ?: return null
        val document = PsiHelper.getDocument(element.containingFile) ?: return null

        val offset = element.textRange.startOffset
        val location = OffsetHelper.createLocation(
            PsiHelper.getRelativePath(project, file),
            document,
            offset
        ) ?: return null

        return ImplementationInfo(
            name = name,
            qualifiedName = qualifiedName,
            location = location
        )
    }

    companion object {
        fun getInstance(project: Project): NavigationService {
            return project.getService(NavigationService::class.java)
        }
    }
}
