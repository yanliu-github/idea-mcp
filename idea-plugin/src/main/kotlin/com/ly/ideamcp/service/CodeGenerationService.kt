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

        // Phase 1: ReadAction - 收集所有需要的数据
        data class FieldData(
            val name: String,
            val typeName: String,
            val typeCanonicalText: String
        )

        data class PreparedData(
            val offset: Int,
            val fields: List<FieldData>,
            val existingGetters: Set<String>,
            val existingSetters: Set<String>
        )

        val preparedData = ThreadHelper.runReadAction {
            val psiFile = PsiHelper.findPsiFile(project, request.filePath)
                ?: throw IllegalArgumentException("File not found: ${request.filePath}")

            val document = PsiHelper.getDocument(psiFile)
                ?: throw IllegalStateException("Cannot get document")

            val offset = request.offset ?: run {
                OffsetHelper.lineColumnToOffset(document, request.line!!, request.column!!)
                    ?: throw IllegalArgumentException("Invalid line/column position")
            }

            // 查找光标所在的类
            val element = psiFile.findElementAt(offset)
            val psiClass = PsiTreeUtil.getParentOfType(element, PsiClass::class.java)
                ?: throw IllegalArgumentException("No class found at offset: $offset")

            // 获取类中的字段
            val allFields = if (request.fieldNames?.isNotEmpty() == true) {
                psiClass.fields.filter { field ->
                    request.fieldNames?.contains(field.name) == true
                }
            } else {
                psiClass.fields.toList()
            }

            if (allFields.isEmpty()) {
                throw IllegalArgumentException("No fields found to generate getters/setters")
            }

            // 收集字段信息
            val fields = allFields.mapNotNull { field ->
                val name = field.name ?: return@mapNotNull null
                FieldData(
                    name = name,
                    typeName = field.type.presentableText,
                    typeCanonicalText = field.type.canonicalText
                )
            }

            // 收集已存在的方法名
            val existingGetters = psiClass.methods
                .filter { it.name.startsWith("get") }
                .map { it.name }
                .toSet()

            val existingSetters = psiClass.methods
                .filter { it.name.startsWith("set") }
                .map { it.name }
                .toSet()

            PreparedData(offset, fields, existingGetters, existingSetters)
        }

        logger.info("Generating getters/setters at offset: ${preparedData.offset}")

        // Phase 2: WriteAction - 生成代码（所有 PSI 操作在单个 WriteAction 中完成）
        val generatedMethods = ThreadHelper.runWriteAction {
            val psiFile = PsiHelper.findPsiFile(project, request.filePath)!!
            val element = psiFile.findElementAt(preparedData.offset)
            val psiClass = PsiTreeUtil.getParentOfType(element, PsiClass::class.java)!!

            val factory = JavaPsiFacade.getElementFactory(project)
            val methods = mutableListOf<GeneratedMethod>()

            preparedData.fields.forEach { fieldData ->
                val fieldName = fieldData.name
                val fieldType = factory.createTypeFromText(fieldData.typeCanonicalText, psiClass)

                // 生成 Getter
                if (request.generateGetter) {
                    val getterName = "get${fieldName.replaceFirstChar { it.uppercaseChar() }}"

                    if (getterName !in preparedData.existingGetters) {
                        val getter = factory.createMethod(getterName, fieldType)
                        getter.modifierList.setModifierProperty(PsiModifier.PUBLIC, true)

                        val returnStatement = factory.createStatementFromText("return this.$fieldName;", getter)
                        getter.body?.add(returnStatement)

                        val addedGetter = psiClass.add(getter) as PsiMethod

                        methods.add(
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

                    if (setterName !in preparedData.existingSetters) {
                        val setter = factory.createMethod(setterName, PsiTypes.voidType())
                        setter.modifierList.setModifierProperty(PsiModifier.PUBLIC, true)

                        val parameter = factory.createParameter(fieldName, fieldType)
                        setter.parameterList.add(parameter)

                        val assignStatement = factory.createStatementFromText("this.$fieldName = $fieldName;", setter)
                        setter.body?.add(assignStatement)

                        val addedSetter = psiClass.add(setter) as PsiMethod

                        methods.add(
                            GeneratedMethod(
                                name = setterName,
                                signature = buildMethodSignature(addedSetter),
                                offset = addedSetter.textRange.startOffset
                            )
                        )
                    }
                }
            }

            // 格式化代码（已在 WriteAction 中）
            com.intellij.psi.codeStyle.CodeStyleManager.getInstance(project).reformat(psiClass)

            methods
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

        // Phase 1: ReadAction - 收集所有需要的数据
        data class PreparedData(
            val offset: Int,
            val className: String
        )

        val preparedData = ThreadHelper.runReadAction {
            val psiFile = PsiHelper.findPsiFile(project, request.filePath)
                ?: throw IllegalArgumentException("File not found: ${request.filePath}")

            val document = PsiHelper.getDocument(psiFile)
                ?: throw IllegalStateException("Cannot get document")

            val offset = request.offset ?: run {
                OffsetHelper.lineColumnToOffset(document, request.line!!, request.column!!)
                    ?: throw IllegalArgumentException("Invalid line/column position")
            }

            // 查找光标所在的类
            val element = psiFile.findElementAt(offset)
            val psiClass = PsiTreeUtil.getParentOfType(element, PsiClass::class.java)
                ?: throw IllegalArgumentException("No class found at offset: $offset")

            val className = psiClass.name
                ?: throw IllegalStateException("Class name is null")

            PreparedData(offset, className)
        }

        logger.info("Generating constructor at offset: ${preparedData.offset}")

        // Phase 2: WriteAction - 生成构造函数（所有 PSI 操作在单个 WriteAction 中完成）
        val constructor = ThreadHelper.runWriteAction {
            val psiFile = PsiHelper.findPsiFile(project, request.filePath)!!
            val element = psiFile.findElementAt(preparedData.offset)
            val psiClass = PsiTreeUtil.getParentOfType(element, PsiClass::class.java)!!

            val factory = JavaPsiFacade.getElementFactory(project)

            // 创建构造函数
            val method = factory.createConstructor(preparedData.className)
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

            // 格式化代码（已在 WriteAction 中）
            com.intellij.psi.codeStyle.CodeStyleManager.getInstance(project).reformat(addedConstructor)

            addedConstructor
        }

        return GenerateConstructorResponse(
            success = true,
            constructor = GeneratedMethod(
                name = preparedData.className,
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

        // Phase 1: ReadAction - 收集所有需要的数据
        data class PreparedData(
            val offset: Int,
            val className: String,
            val fieldNames: List<String>,
            val hasExistingToString: Boolean
        )

        val preparedData = ThreadHelper.runReadAction {
            val psiFile = PsiHelper.findPsiFile(project, request.filePath)
                ?: throw IllegalArgumentException("File not found: ${request.filePath}")

            val document = PsiHelper.getDocument(psiFile)
                ?: throw IllegalStateException("Cannot get document")

            val offset = request.offset ?: run {
                OffsetHelper.lineColumnToOffset(document, request.line!!, request.column!!)
                    ?: throw IllegalArgumentException("Invalid line/column position")
            }

            // 查找光标所在的类
            val element = psiFile.findElementAt(offset)
            val psiClass = PsiTreeUtil.getParentOfType(element, PsiClass::class.java)
                ?: throw IllegalArgumentException("No class found at offset: $offset")

            val className = psiClass.name
                ?: throw IllegalStateException("Class name is null")

            // 获取类的非静态字段名
            val fieldNames = psiClass.fields
                .filter { !it.hasModifierProperty(PsiModifier.STATIC) }
                .mapNotNull { it.name }

            val hasExistingToString = psiClass.findMethodsByName("toString", false).isNotEmpty()

            PreparedData(offset, className, fieldNames, hasExistingToString)
        }

        logger.info("Generating toString at offset: ${preparedData.offset}")

        // Phase 2: WriteAction - 生成方法（所有 PSI 操作在单个 WriteAction 中完成）
        val toStringMethod = ThreadHelper.runWriteAction {
            val psiFile = PsiHelper.findPsiFile(project, request.filePath)!!
            val element = psiFile.findElementAt(preparedData.offset)
            val psiClass = PsiTreeUtil.getParentOfType(element, PsiClass::class.java)!!

            val factory = JavaPsiFacade.getElementFactory(project)

            // 检查是否已存在 toString 方法，如果存在则删除
            if (preparedData.hasExistingToString) {
                psiClass.findMethodsByName("toString", false).firstOrNull()?.delete()
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

            if (preparedData.fieldNames.isEmpty()) {
                // 没有字段，返回简单的类名
                val returnStatement = factory.createStatementFromText(
                    "return \"${preparedData.className}{}\";",
                    method
                )
                body.add(returnStatement)
            } else {
                // 构建包含所有字段的字符串
                val fieldParts = preparedData.fieldNames.joinToString(", ") { fieldName ->
                    "$fieldName=\" + $fieldName + \""
                }
                val returnStatement = factory.createStatementFromText(
                    "return \"${preparedData.className}{$fieldParts}\";",
                    method
                )
                body.add(returnStatement)
            }

            // 添加到类中
            val addedMethod = psiClass.add(method) as PsiMethod

            // 格式化代码（已在 WriteAction 中）
            com.intellij.psi.codeStyle.CodeStyleManager.getInstance(project).reformat(addedMethod)

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

        // Phase 1: ReadAction - 收集所有需要的数据
        data class FieldInfo(
            val name: String,
            val isPrimitive: Boolean
        )

        data class PreparedData(
            val offset: Int,
            val className: String,
            val fields: List<FieldInfo>,
            val hasExistingEquals: Boolean
        )

        val preparedData = ThreadHelper.runReadAction {
            val psiFile = PsiHelper.findPsiFile(project, request.filePath)
                ?: throw IllegalArgumentException("File not found: ${request.filePath}")

            val document = PsiHelper.getDocument(psiFile)
                ?: throw IllegalStateException("Cannot get document")

            val offset = request.offset ?: run {
                OffsetHelper.lineColumnToOffset(document, request.line!!, request.column!!)
                    ?: throw IllegalArgumentException("Invalid line/column position")
            }

            // 查找光标所在的类
            val element = psiFile.findElementAt(offset)
            val psiClass = PsiTreeUtil.getParentOfType(element, PsiClass::class.java)
                ?: throw IllegalArgumentException("No class found at offset: $offset")

            val className = psiClass.name
                ?: throw IllegalStateException("Class name is null")

            // 获取类的非静态字段信息
            val fields = psiClass.fields
                .filter { !it.hasModifierProperty(PsiModifier.STATIC) }
                .mapNotNull { field ->
                    val name = field.name ?: return@mapNotNull null
                    FieldInfo(name, field.type is PsiPrimitiveType)
                }

            val hasExistingEquals = psiClass.findMethodsByName("equals", false).isNotEmpty()

            PreparedData(offset, className, fields, hasExistingEquals)
        }

        logger.info("Generating equals at offset: ${preparedData.offset}")

        // Phase 2: WriteAction - 生成方法（所有 PSI 操作在单个 WriteAction 中完成）
        val equalsMethod = ThreadHelper.runWriteAction {
            val psiFile = PsiHelper.findPsiFile(project, request.filePath)!!
            val element = psiFile.findElementAt(preparedData.offset)
            val psiClass = PsiTreeUtil.getParentOfType(element, PsiClass::class.java)!!

            val factory = JavaPsiFacade.getElementFactory(project)

            // 检查是否已存在 equals 方法，如果存在则删除
            if (preparedData.hasExistingEquals) {
                psiClass.findMethodsByName("equals", false).firstOrNull()?.delete()
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

            if (preparedData.fields.isNotEmpty()) {
                // 类型转换
                body.add(factory.createStatementFromText("${preparedData.className} that = (${preparedData.className}) obj;", method))

                // 字段比较
                val comparisons = preparedData.fields.joinToString(" && ") { field ->
                    if (field.isPrimitive) {
                        // 基本类型直接比较
                        "${field.name} == that.${field.name}"
                    } else {
                        // 引用类型使用 Objects.equals
                        "java.util.Objects.equals(${field.name}, that.${field.name})"
                    }
                }

                body.add(factory.createStatementFromText("return $comparisons;", method))
            } else {
                // 没有字段，直接返回 true
                body.add(factory.createStatementFromText("return true;", method))
            }

            // 添加到类中
            val addedMethod = psiClass.add(method) as PsiMethod

            // 格式化代码（已在 WriteAction 中）
            com.intellij.psi.codeStyle.CodeStyleManager.getInstance(project).reformat(addedMethod)

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

        // Phase 1: ReadAction - 收集所有需要的数据
        data class PreparedData(
            val offset: Int,
            val fieldNames: List<String>,
            val hasExistingHashCode: Boolean
        )

        val preparedData = ThreadHelper.runReadAction {
            val psiFile = PsiHelper.findPsiFile(project, request.filePath)
                ?: throw IllegalArgumentException("File not found: ${request.filePath}")

            val document = PsiHelper.getDocument(psiFile)
                ?: throw IllegalStateException("Cannot get document")

            val offset = request.offset ?: run {
                OffsetHelper.lineColumnToOffset(document, request.line!!, request.column!!)
                    ?: throw IllegalArgumentException("Invalid line/column position")
            }

            // 查找光标所在的类
            val element = psiFile.findElementAt(offset)
            val psiClass = PsiTreeUtil.getParentOfType(element, PsiClass::class.java)
                ?: throw IllegalArgumentException("No class found at offset: $offset")

            // 获取类的非静态字段名
            val fieldNames = psiClass.fields
                .filter { !it.hasModifierProperty(PsiModifier.STATIC) }
                .mapNotNull { it.name }

            val hasExistingHashCode = psiClass.findMethodsByName("hashCode", false).isNotEmpty()

            PreparedData(offset, fieldNames, hasExistingHashCode)
        }

        logger.info("Generating hashCode at offset: ${preparedData.offset}")

        // Phase 2: WriteAction - 生成方法（所有 PSI 操作在单个 WriteAction 中完成）
        val hashCodeMethod = ThreadHelper.runWriteAction {
            val psiFile = PsiHelper.findPsiFile(project, request.filePath)!!
            val element = psiFile.findElementAt(preparedData.offset)
            val psiClass = PsiTreeUtil.getParentOfType(element, PsiClass::class.java)!!

            val factory = JavaPsiFacade.getElementFactory(project)

            // 检查是否已存在 hashCode 方法，如果存在则删除
            if (preparedData.hasExistingHashCode) {
                psiClass.findMethodsByName("hashCode", false).firstOrNull()?.delete()
            }

            // 创建 hashCode 方法
            val method = factory.createMethod("hashCode", PsiTypes.intType())
            method.modifierList.setModifierProperty(PsiModifier.PUBLIC, true)

            // 添加 @Override 注解
            val overrideAnnotation = factory.createAnnotationFromText("@Override", method)
            method.modifierList.addBefore(overrideAnnotation, method.modifierList.firstChild)

            // 构建方法体
            val body = method.body ?: throw IllegalStateException("Method body is null")

            if (preparedData.fieldNames.isEmpty()) {
                // 没有字段，返回 0
                body.add(factory.createStatementFromText("return 0;", method))
            } else {
                // 使用 Objects.hash 方法
                val fieldNames = preparedData.fieldNames.joinToString(", ")
                body.add(factory.createStatementFromText("return java.util.Objects.hash($fieldNames);", method))
            }

            // 添加到类中
            val addedMethod = psiClass.add(method) as PsiMethod

            // 格式化代码（已在 WriteAction 中）
            com.intellij.psi.codeStyle.CodeStyleManager.getInstance(project).reformat(addedMethod)

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

        // Phase 1: ReadAction - 收集所有需要的数据
        data class ParameterInfo(
            val name: String,
            val typeCanonicalText: String
        )

        data class MethodInfo(
            val name: String,
            val returnTypeCanonicalText: String?,
            val isReturnVoid: Boolean,
            val isReturnPrimitive: Boolean,
            val returnPrimitiveDefault: String,
            val parameters: List<ParameterInfo>
        )

        data class PreparedData(
            val offset: Int,
            val methodToOverride: MethodInfo,
            val isAlreadyOverridden: Boolean
        )

        val preparedData = ThreadHelper.runReadAction {
            val psiFile = PsiHelper.findPsiFile(project, request.filePath)
                ?: throw IllegalArgumentException("File not found: ${request.filePath}")

            val document = PsiHelper.getDocument(psiFile)
                ?: throw IllegalStateException("Cannot get document")

            val offset = request.offset ?: run {
                OffsetHelper.lineColumnToOffset(document, request.line!!, request.column!!)
                    ?: throw IllegalArgumentException("Invalid line/column position")
            }

            // 查找光标所在的类
            val element = psiFile.findElementAt(offset)
            val psiClass = PsiTreeUtil.getParentOfType(element, PsiClass::class.java)
                ?: throw IllegalArgumentException("No class found at offset: $offset")

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
            val methodToOverride = if (request.parameterTypes.isNotEmpty()) {
                superMethods.firstOrNull { method ->
                    val params = method.parameterList.parameters
                    params.size == request.parameterTypes.size &&
                            params.zip(request.parameterTypes).all { (param, type) ->
                                param.type.canonicalText == type
                            }
                } ?: superMethods.first()
            } else {
                superMethods.first()
            }

            // 检查是否已经重写
            val isAlreadyOverridden = psiClass.findMethodsBySignature(methodToOverride, false).isNotEmpty()

            // 收集方法信息
            val returnType = methodToOverride.returnType
            val isReturnVoid = returnType == PsiTypes.voidType() || returnType == null
            val isReturnPrimitive = returnType is PsiPrimitiveType
            val returnPrimitiveDefault = when (returnType) {
                PsiTypes.booleanType() -> "false"
                PsiTypes.intType(), PsiTypes.longType(), PsiTypes.shortType(), PsiTypes.byteType() -> "0"
                PsiTypes.floatType() -> "0.0f"
                PsiTypes.doubleType() -> "0.0"
                PsiTypes.charType() -> "'\\u0000'"
                else -> "null"
            }

            val parameters = methodToOverride.parameterList.parameters.map { param ->
                ParameterInfo(param.name ?: "param", param.type.canonicalText)
            }

            val methodInfo = MethodInfo(
                name = methodToOverride.name,
                returnTypeCanonicalText = returnType?.canonicalText,
                isReturnVoid = isReturnVoid,
                isReturnPrimitive = isReturnPrimitive,
                returnPrimitiveDefault = returnPrimitiveDefault,
                parameters = parameters
            )

            PreparedData(offset, methodInfo, isAlreadyOverridden)
        }

        if (preparedData.isAlreadyOverridden) {
            throw IllegalStateException("Method '${request.methodName}' is already overridden in this class")
        }

        logger.info("Overriding method at offset: ${preparedData.offset}")

        // Phase 2: WriteAction - 生成方法（所有 PSI 操作在单个 WriteAction 中完成）
        val overriddenMethod = ThreadHelper.runWriteAction {
            val psiFile = PsiHelper.findPsiFile(project, request.filePath)!!
            val element = psiFile.findElementAt(preparedData.offset)
            val psiClass = PsiTreeUtil.getParentOfType(element, PsiClass::class.java)!!

            val factory = JavaPsiFacade.getElementFactory(project)
            val methodInfo = preparedData.methodToOverride

            // 创建返回类型
            val returnType = if (methodInfo.returnTypeCanonicalText != null) {
                factory.createTypeFromText(methodInfo.returnTypeCanonicalText, psiClass)
            } else {
                PsiTypes.voidType()
            }

            // 创建重写方法
            val method = factory.createMethod(methodInfo.name, returnType)
            method.modifierList.setModifierProperty(PsiModifier.PUBLIC, true)

            // 添加参数
            methodInfo.parameters.forEach { param ->
                val paramType = factory.createTypeFromText(param.typeCanonicalText, psiClass)
                val parameter = factory.createParameter(param.name, paramType)
                method.parameterList.add(parameter)
            }

            // 添加 @Override 注解
            val overrideAnnotation = factory.createAnnotationFromText("@Override", method)
            method.modifierList.addBefore(overrideAnnotation, method.modifierList.firstChild)

            // 添加方法体
            val body = method.body ?: throw IllegalStateException("Method body is null")

            if (request.callSuper) {
                // 调用父类方法
                val paramNames = methodInfo.parameters.joinToString(", ") { it.name }
                val superCall = if (methodInfo.isReturnVoid) {
                    factory.createStatementFromText("super.${request.methodName}($paramNames);", method)
                } else {
                    factory.createStatementFromText("return super.${request.methodName}($paramNames);", method)
                }
                body.add(superCall)
            } else {
                // 生成默认实现
                if (!methodInfo.isReturnVoid) {
                    val defaultValue = if (methodInfo.isReturnPrimitive) {
                        methodInfo.returnPrimitiveDefault
                    } else {
                        "null"
                    }
                    val returnStatement = factory.createStatementFromText("return $defaultValue;", method)
                    body.add(returnStatement)
                }
            }

            // 添加到类中
            val addedMethod = psiClass.add(method) as PsiMethod

            // 格式化代码（已在 WriteAction 中）
            com.intellij.psi.codeStyle.CodeStyleManager.getInstance(project).reformat(addedMethod)

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
