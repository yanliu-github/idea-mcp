package com.ly.ideamcp.service

import com.intellij.codeInsight.generation.*
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.Service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.EditorFactory
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.Project
import com.intellij.psi.*
import com.intellij.psi.util.PsiTreeUtil
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

        val psiFile = ThreadHelper.runReadAction {
            PsiHelper.findPsiFile(project, request.filePath)
                ?: throw IllegalArgumentException("File not found: ${request.filePath}")
        }

        val document = ThreadHelper.runReadAction {
            PsiHelper.getDocument(psiFile)
                ?: throw IllegalStateException("Cannot get document")
        }

        val offset = request.offset ?: run {
            OffsetHelper.lineColumnToOffset(document, request.line!!, request.column!!)
                ?: throw IllegalArgumentException("Invalid line/column position")
        }

        logger.info("Generating getters/setters at offset: $offset")

        // 查找光标所在的类
        val psiClass = ThreadHelper.runReadAction {
            val element = psiFile.findElementAt(offset)
            PsiTreeUtil.getParentOfType(element, PsiClass::class.java)
                ?: throw IllegalArgumentException("No class found at offset: $offset")
        }

        // 获取类中的字段
        val fields = ThreadHelper.runReadAction {
            if (request.fieldNames?.isNotEmpty() == true) {
                // 如果指定了字段名，只处理这些字段
                psiClass.fields.filter { field ->
                    request.fieldNames?.contains(field.name) == true
                }
            } else {
                // 否则处理所有字段
                psiClass.fields.toList()
            }
        }

        if (fields.isEmpty()) {
            throw IllegalArgumentException("No fields found to generate getters/setters")
        }

        // 生成 getter/setter 方法
        val generatedMethods = mutableListOf<GeneratedMethod>()

        ThreadHelper.runWriteAction {
            val factory = JavaPsiFacade.getElementFactory(project)

            fields.forEach { field ->
                val fieldName = field.name ?: return@forEach
                val fieldType = field.type

                // 生成 Getter
                if (request.generateGetter) {
                    val getterName = "get${fieldName.replaceFirstChar { it.uppercaseChar() }}"

                    // 检查是否已存在
                    val existingGetter = psiClass.findMethodsByName(getterName, false).firstOrNull()
                    if (existingGetter == null) {
                        val getter = factory.createMethod(getterName, fieldType)
                        getter.modifierList.setModifierProperty(PsiModifier.PUBLIC, true)

                        val returnStatement = factory.createStatementFromText("return this.$fieldName;", getter)
                        getter.body?.add(returnStatement)

                        val addedGetter = psiClass.add(getter) as PsiMethod

                        generatedMethods.add(
                            GeneratedMethod(
                                name = getterName,
                                signature = buildMethodSignature(addedGetter),
                                offset = addedGetter.textRange.startOffset
                            )
                        )
                    }
                }

                // 生成 Setter
                if (request.generateSetter) {
                    val setterName = "set${fieldName.replaceFirstChar { it.uppercaseChar() }}"

                    // 检查是否已存在
                    val existingSetter = psiClass.findMethodsByName(setterName, false).firstOrNull()
                    if (existingSetter == null) {
                        val setter = factory.createMethod(setterName, PsiTypes.voidType())
                        setter.modifierList.setModifierProperty(PsiModifier.PUBLIC, true)

                        val parameter = factory.createParameter(fieldName, fieldType)
                        setter.parameterList.add(parameter)

                        val assignStatement = factory.createStatementFromText("this.$fieldName = $fieldName;", setter)
                        setter.body?.add(assignStatement)

                        val addedSetter = psiClass.add(setter) as PsiMethod

                        generatedMethods.add(
                            GeneratedMethod(
                                name = setterName,
                                signature = buildMethodSignature(addedSetter),
                                offset = addedSetter.textRange.startOffset
                            )
                        )
                    }
                }
            }

            // 格式化代码
            PsiHelper.reformatCode(project, psiClass)
        }

        return GenerateGettersSettersResponse(
            success = true,
            generatedMethods = generatedMethods,
            affectedFile = request.filePath
        )
    }

    /**
     * 生成构造函数
     * @param request 生成请求
     * @return 生成响应
     */
    fun generateConstructor(request: GenerateConstructorRequest): GenerateConstructorResponse {
        logger.info("Generating constructor in file: ${request.filePath}")

        val psiFile = ThreadHelper.runReadAction {
            PsiHelper.findPsiFile(project, request.filePath)
                ?: throw IllegalArgumentException("File not found: ${request.filePath}")
        }

        val document = ThreadHelper.runReadAction {
            PsiHelper.getDocument(psiFile)
                ?: throw IllegalStateException("Cannot get document")
        }

        val offset = request.offset ?: run {
            OffsetHelper.lineColumnToOffset(document, request.line!!, request.column!!)
                ?: throw IllegalArgumentException("Invalid line/column position")
        }

        logger.info("Generating constructor at offset: $offset")

        // 查找光标所在的类
        val psiClass = ThreadHelper.runReadAction {
            val element = psiFile.findElementAt(offset)
            PsiTreeUtil.getParentOfType(element, PsiClass::class.java)
                ?: throw IllegalArgumentException("No class found at offset: $offset")
        }

        val className = ThreadHelper.runReadAction { psiClass.name }
            ?: throw IllegalStateException("Class name is null")

        // 生成构造函数
        val constructor = ThreadHelper.runWriteAction {
            val factory = JavaPsiFacade.getElementFactory(project)

            // 创建构造函数
            val method = factory.createConstructor(className)
            method.modifierList.setModifierProperty(PsiModifier.PUBLIC, true)

            // 添加参数
            request.fields.forEach { field ->
                val paramType = factory.createTypeFromText(field.type, null)
                val parameter = factory.createParameter(field.name, paramType)
                method.parameterList.add(parameter)
            }

            // 添加赋值语句
            val body = method.body ?: throw IllegalStateException("Constructor body is null")
            request.fields.forEach { field ->
                val statement = factory.createStatementFromText(
                    "this.${field.name} = ${field.name};",
                    method
                )
                body.add(statement)
            }

            // 调用父类构造函数 (如果需要)
            if (request.callSuper) {
                val superCall = factory.createStatementFromText("super();", method)
                body.addAfter(superCall, body.lBrace)
            }

            // 添加到类中
            val addedConstructor = psiClass.add(method) as PsiMethod

            // 格式化代码
            PsiHelper.reformatCode(project, addedConstructor)

            addedConstructor
        }

        return GenerateConstructorResponse(
            success = true,
            constructor = GeneratedMethod(
                name = className,
                signature = buildMethodSignature(constructor),
                offset = constructor.textRange.startOffset
            ),
            affectedFile = request.filePath
        )
    }

    /**
     * 生成 toString 方法
     * @param request 生成请求
     * @return 生成响应
     */
    fun generateToString(request: GenerateMethodRequest): GenerateMethodResponse {
        logger.info("Generating toString method in file: ${request.filePath}")

        val psiFile = ThreadHelper.runReadAction {
            PsiHelper.findPsiFile(project, request.filePath)
                ?: throw IllegalArgumentException("File not found: ${request.filePath}")
        }

        val document = ThreadHelper.runReadAction {
            PsiHelper.getDocument(psiFile)
                ?: throw IllegalStateException("Cannot get document")
        }

        val offset = request.offset ?: run {
            OffsetHelper.lineColumnToOffset(document, request.line!!, request.column!!)
                ?: throw IllegalArgumentException("Invalid line/column position")
        }

        logger.info("Generating toString at offset: $offset")

        // 查找光标所在的类
        val psiClass = ThreadHelper.runReadAction {
            val element = psiFile.findElementAt(offset)
            PsiTreeUtil.getParentOfType(element, PsiClass::class.java)
                ?: throw IllegalArgumentException("No class found at offset: $offset")
        }

        val className = ThreadHelper.runReadAction { psiClass.name }
            ?: throw IllegalStateException("Class name is null")

        // 获取类的字段
        val fields = ThreadHelper.runReadAction {
            psiClass.fields.filter { !it.hasModifierProperty(PsiModifier.STATIC) }
        }

        // 生成 toString 方法
        val toStringMethod = ThreadHelper.runWriteAction {
            val factory = JavaPsiFacade.getElementFactory(project)

            // 检查是否已存在 toString 方法
            val existingMethod = psiClass.findMethodsByName("toString", false).firstOrNull()
            if (existingMethod != null) {
                // 如果存在，先删除
                existingMethod.delete()
            }

            // 创建 toString 方法
            val method = factory.createMethod("toString", PsiType.getJavaLangString(
                PsiManager.getInstance(project),
                psiClass.resolveScope
            ))
            method.modifierList.setModifierProperty(PsiModifier.PUBLIC, true)

            // 添加 @Override 注解
            val overrideAnnotation = factory.createAnnotationFromText("@Override", method)
            method.modifierList.addBefore(overrideAnnotation, method.modifierList.firstChild)

            // 构建 toString 内容
            val body = method.body ?: throw IllegalStateException("Method body is null")

            if (fields.isEmpty()) {
                // 没有字段，返回简单的类名
                val returnStatement = factory.createStatementFromText(
                    "return \"$className{}\";",
                    method
                )
                body.add(returnStatement)
            } else {
                // 构建包含所有字段的字符串
                val fieldParts = fields.joinToString(", ") { field ->
                    val fieldName = field.name
                    "$fieldName=\" + $fieldName + \""
                }
                val returnStatement = factory.createStatementFromText(
                    "return \"$className{$fieldParts}\";",
                    method
                )
                body.add(returnStatement)
            }

            // 添加到类中
            val addedMethod = psiClass.add(method) as PsiMethod

            // 格式化代码
            PsiHelper.reformatCode(project, addedMethod)

            addedMethod
        }

        return GenerateMethodResponse(
            success = true,
            method = GeneratedMethod(
                name = "toString",
                signature = buildMethodSignature(toStringMethod),
                offset = toStringMethod.textRange.startOffset
            ),
            affectedFile = request.filePath
        )
    }

    /**
     * 生成 equals 方法
     * @param request 生成请求
     * @return 生成响应
     */
    fun generateEquals(request: GenerateMethodRequest): GenerateMethodResponse {
        logger.info("Generating equals method in file: ${request.filePath}")

        val psiFile = ThreadHelper.runReadAction {
            PsiHelper.findPsiFile(project, request.filePath)
                ?: throw IllegalArgumentException("File not found: ${request.filePath}")
        }

        val document = ThreadHelper.runReadAction {
            PsiHelper.getDocument(psiFile)
                ?: throw IllegalStateException("Cannot get document")
        }

        val offset = request.offset ?: run {
            OffsetHelper.lineColumnToOffset(document, request.line!!, request.column!!)
                ?: throw IllegalArgumentException("Invalid line/column position")
        }

        logger.info("Generating equals at offset: $offset")

        // 查找光标所在的类
        val psiClass = ThreadHelper.runReadAction {
            val element = psiFile.findElementAt(offset)
            PsiTreeUtil.getParentOfType(element, PsiClass::class.java)
                ?: throw IllegalArgumentException("No class found at offset: $offset")
        }

        val className = ThreadHelper.runReadAction { psiClass.name }
            ?: throw IllegalStateException("Class name is null")

        // 获取类的字段
        val fields = ThreadHelper.runReadAction {
            psiClass.fields.filter { !it.hasModifierProperty(PsiModifier.STATIC) }
        }

        // 生成 equals 方法
        val equalsMethod = ThreadHelper.runWriteAction {
            val factory = JavaPsiFacade.getElementFactory(project)

            // 检查是否已存在 equals 方法
            val existingMethod = psiClass.findMethodsByName("equals", false).firstOrNull()
            if (existingMethod != null) {
                // 如果存在，先删除
                existingMethod.delete()
            }

            // 创建 equals 方法
            val method = factory.createMethod("equals", PsiTypes.booleanType())
            method.modifierList.setModifierProperty(PsiModifier.PUBLIC, true)

            // 添加参数
            val objType = factory.createTypeFromText("java.lang.Object", null)
            val parameter = factory.createParameter("obj", objType)
            method.parameterList.add(parameter)

            // 添加 @Override 注解
            val overrideAnnotation = factory.createAnnotationFromText("@Override", method)
            method.modifierList.addBefore(overrideAnnotation, method.modifierList.firstChild)

            // 构建方法体
            val body = method.body ?: throw IllegalStateException("Method body is null")

            // 添加标准检查
            body.add(factory.createStatementFromText("if (this == obj) return true;", method))
            body.add(factory.createStatementFromText("if (obj == null || getClass() != obj.getClass()) return false;", method))

            if (fields.isNotEmpty()) {
                // 类型转换
                body.add(factory.createStatementFromText("$className that = ($className) obj;", method))

                // 字段比较
                val comparisons = fields.joinToString(" && ") { field ->
                    val fieldName = field.name
                    val fieldType = field.type

                    when {
                        fieldType is PsiPrimitiveType -> {
                            // 基本类型直接比较
                            "$fieldName == that.$fieldName"
                        }
                        else -> {
                            // 引用类型使用 Objects.equals
                            "java.util.Objects.equals($fieldName, that.$fieldName)"
                        }
                    }
                }

                body.add(factory.createStatementFromText("return $comparisons;", method))
            } else {
                // 没有字段，直接返回 true
                body.add(factory.createStatementFromText("return true;", method))
            }

            // 添加到类中
            val addedMethod = psiClass.add(method) as PsiMethod

            // 格式化代码
            PsiHelper.reformatCode(project, addedMethod)

            addedMethod
        }

        return GenerateMethodResponse(
            success = true,
            method = GeneratedMethod(
                name = "equals",
                signature = buildMethodSignature(equalsMethod),
                offset = equalsMethod.textRange.startOffset
            ),
            affectedFile = request.filePath
        )
    }

    /**
     * 生成 hashCode 方法
     * @param request 生成请求
     * @return 生成响应
     */
    fun generateHashCode(request: GenerateMethodRequest): GenerateMethodResponse {
        logger.info("Generating hashCode method in file: ${request.filePath}")

        val psiFile = ThreadHelper.runReadAction {
            PsiHelper.findPsiFile(project, request.filePath)
                ?: throw IllegalArgumentException("File not found: ${request.filePath}")
        }

        val document = ThreadHelper.runReadAction {
            PsiHelper.getDocument(psiFile)
                ?: throw IllegalStateException("Cannot get document")
        }

        val offset = request.offset ?: run {
            OffsetHelper.lineColumnToOffset(document, request.line!!, request.column!!)
                ?: throw IllegalArgumentException("Invalid line/column position")
        }

        logger.info("Generating hashCode at offset: $offset")

        // 查找光标所在的类
        val psiClass = ThreadHelper.runReadAction {
            val element = psiFile.findElementAt(offset)
            PsiTreeUtil.getParentOfType(element, PsiClass::class.java)
                ?: throw IllegalArgumentException("No class found at offset: $offset")
        }

        // 获取类的字段
        val fields = ThreadHelper.runReadAction {
            psiClass.fields.filter { !it.hasModifierProperty(PsiModifier.STATIC) }
        }

        // 生成 hashCode 方法
        val hashCodeMethod = ThreadHelper.runWriteAction {
            val factory = JavaPsiFacade.getElementFactory(project)

            // 检查是否已存在 hashCode 方法
            val existingMethod = psiClass.findMethodsByName("hashCode", false).firstOrNull()
            if (existingMethod != null) {
                // 如果存在，先删除
                existingMethod.delete()
            }

            // 创建 hashCode 方法
            val method = factory.createMethod("hashCode", PsiTypes.intType())
            method.modifierList.setModifierProperty(PsiModifier.PUBLIC, true)

            // 添加 @Override 注解
            val overrideAnnotation = factory.createAnnotationFromText("@Override", method)
            method.modifierList.addBefore(overrideAnnotation, method.modifierList.firstChild)

            // 构建方法体
            val body = method.body ?: throw IllegalStateException("Method body is null")

            if (fields.isEmpty()) {
                // 没有字段，返回 0
                body.add(factory.createStatementFromText("return 0;", method))
            } else {
                // 使用 Objects.hash 方法
                val fieldNames = fields.joinToString(", ") { it.name ?: "" }
                body.add(factory.createStatementFromText("return java.util.Objects.hash($fieldNames);", method))
            }

            // 添加到类中
            val addedMethod = psiClass.add(method) as PsiMethod

            // 格式化代码
            PsiHelper.reformatCode(project, addedMethod)

            addedMethod
        }

        return GenerateMethodResponse(
            success = true,
            method = GeneratedMethod(
                name = "hashCode",
                signature = buildMethodSignature(hashCodeMethod),
                offset = hashCodeMethod.textRange.startOffset
            ),
            affectedFile = request.filePath
        )
    }

    /**
     * 重写方法
     * @param request 重写方法请求
     * @return 重写方法响应
     */
    fun overrideMethod(request: OverrideMethodRequest): OverrideMethodResponse {
        logger.info("Overriding method: ${request.methodName} in file: ${request.filePath}")

        val psiFile = ThreadHelper.runReadAction {
            PsiHelper.findPsiFile(project, request.filePath)
                ?: throw IllegalArgumentException("File not found: ${request.filePath}")
        }

        val document = ThreadHelper.runReadAction {
            PsiHelper.getDocument(psiFile)
                ?: throw IllegalStateException("Cannot get document")
        }

        val offset = request.offset ?: run {
            OffsetHelper.lineColumnToOffset(document, request.line!!, request.column!!)
                ?: throw IllegalArgumentException("Invalid line/column position")
        }

        logger.info("Overriding method at offset: $offset")

        // 查找光标所在的类
        val psiClass = ThreadHelper.runReadAction {
            val element = psiFile.findElementAt(offset)
            PsiTreeUtil.getParentOfType(element, PsiClass::class.java)
                ?: throw IllegalArgumentException("No class found at offset: $offset")
        }

        // 查找要重写的方法
        val methodToOverride = ThreadHelper.runReadAction {
            // 从父类和接口中查找方法
            val superMethods = mutableListOf<PsiMethod>()

            // 从父类查找
            psiClass.superClass?.let { superClass ->
                superClass.findMethodsByName(request.methodName, true)
                    .forEach { superMethods.add(it) }
            }

            // 从接口查找
            psiClass.interfaces.forEach { interfaceClass ->
                interfaceClass.findMethodsByName(request.methodName, true)
                    .forEach { superMethods.add(it) }
            }

            // 如果没有找到方法，抛出异常
            if (superMethods.isEmpty()) {
                throw IllegalArgumentException("Method '${request.methodName}' not found in parent classes or interfaces")
            }

            // 如果有多个重载，根据参数类型选择
            if (request.parameterTypes.isNotEmpty()) {
                superMethods.firstOrNull { method ->
                    val params = method.parameterList.parameters
                    params.size == request.parameterTypes.size &&
                            params.zip(request.parameterTypes).all { (param, type) ->
                                param.type.canonicalText == type
                            }
                } ?: superMethods.first()
            } else {
                // 如果没有指定参数类型，选择第一个
                superMethods.first()
            }
        }

        // 生成重写方法
        val overriddenMethod = ThreadHelper.runWriteAction {
            val factory = JavaPsiFacade.getElementFactory(project)

            // 检查是否已经重写
            val existingMethod = psiClass.findMethodsBySignature(methodToOverride, false).firstOrNull()
            if (existingMethod != null) {
                throw IllegalStateException("Method '${request.methodName}' is already overridden in this class")
            }

            // 创建重写方法
            val method = factory.createMethod(methodToOverride.name, methodToOverride.returnType ?: PsiTypes.voidType())
            method.modifierList.setModifierProperty(PsiModifier.PUBLIC, true)

            // 复制参数
            methodToOverride.parameterList.parameters.forEach { param ->
                val parameter = factory.createParameter(param.name ?: "param", param.type)
                method.parameterList.add(parameter)
            }

            // 添加 @Override 注解
            val overrideAnnotation = factory.createAnnotationFromText("@Override", method)
            method.modifierList.addBefore(overrideAnnotation, method.modifierList.firstChild)

            // 添加方法体
            val body = method.body ?: throw IllegalStateException("Method body is null")

            if (request.callSuper) {
                // 调用父类方法
                val paramNames = methodToOverride.parameterList.parameters.joinToString(", ") { it.name ?: "" }
                val superCall = if (methodToOverride.returnType == PsiTypes.voidType() || methodToOverride.returnType == null) {
                    factory.createStatementFromText("super.${request.methodName}($paramNames);", method)
                } else {
                    factory.createStatementFromText("return super.${request.methodName}($paramNames);", method)
                }
                body.add(superCall)
            } else {
                // 生成默认实现
                if (methodToOverride.returnType != PsiTypes.voidType() && methodToOverride.returnType != null) {
                    val returnType = methodToOverride.returnType!!
                    val defaultValue = when {
                        returnType is PsiPrimitiveType -> {
                            when (returnType) {
                                PsiTypes.booleanType() -> "false"
                                PsiTypes.intType(), PsiTypes.longType(), PsiTypes.shortType(), PsiTypes.byteType() -> "0"
                                PsiTypes.floatType() -> "0.0f"
                                PsiTypes.doubleType() -> "0.0"
                                PsiTypes.charType() -> "'\\u0000'"
                                else -> "null"
                            }
                        }
                        else -> "null"
                    }
                    val returnStatement = factory.createStatementFromText("return $defaultValue;", method)
                    body.add(returnStatement)
                }
            }

            // 添加到类中
            val addedMethod = psiClass.add(method) as PsiMethod

            // 格式化代码
            PsiHelper.reformatCode(project, addedMethod)

            addedMethod
        }

        return OverrideMethodResponse(
            success = true,
            method = GeneratedMethod(
                name = request.methodName,
                signature = buildMethodSignature(overriddenMethod),
                offset = overriddenMethod.textRange.startOffset
            ),
            affectedFile = request.filePath
        )
    }

    /**
     * 构建方法签名字符串
     */
    private fun buildMethodSignature(method: PsiMethod): String {
        val modifiers = method.modifierList.text
        val returnType = method.returnType?.presentableText ?: "void"
        val methodName = method.name
        val parameters = method.parameterList.parameters.joinToString(", ") {
            "${it.type.presentableText} ${it.name}"
        }
        return "$modifiers $returnType $methodName($parameters)"
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
