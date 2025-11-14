package com.ly.ideamcp.service

import com.intellij.openapi.components.Service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.module.Module
import com.intellij.openapi.module.ModuleManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ModuleRootManager
import com.intellij.openapi.roots.OrderEntry
import com.intellij.openapi.roots.ModuleOrderEntry
import com.intellij.openapi.roots.LibraryOrderEntry
import com.intellij.packageDependencies.DependenciesBuilder
import com.intellij.packageDependencies.ForwardDependenciesBuilder
import com.intellij.psi.PsiFile
import com.ly.ideamcp.model.dependency.*
import com.ly.ideamcp.util.ThreadHelper

/**
 * 依赖分析服务
 * 提供依赖分析和循环依赖检测功能
 */
@Service(Service.Level.PROJECT)
class DependencyService(private val project: Project) {

    private val logger = Logger.getInstance(DependencyService::class.java)

    /**
     * 分析依赖
     * @param request 依赖分析请求
     * @return 依赖分析响应
     */
    fun analyzeDependencies(request: AnalyzeDependenciesRequest): AnalyzeDependenciesResponse {
        logger.info("Analyzing dependencies for: ${request.scope}")

        return ThreadHelper.runReadAction {
            try {
                val dependencies = mutableListOf<DependencyInfo>()
                val moduleManager = ModuleManager.getInstance(project)

                when (request.scope.lowercase()) {
                    "project" -> {
                        // 分析整个项目的模块间依赖
                        moduleManager.modules.forEach { module ->
                            val moduleDeps = analyzeModuleDependencies(module)
                            dependencies.addAll(moduleDeps)
                        }
                    }
                    "module" -> {
                        // 分析特定模块
                        val module = request.moduleName?.let { name ->
                            moduleManager.modules.find { it.name == name }
                        }
                        if (module != null) {
                            dependencies.addAll(analyzeModuleDependencies(module))
                        } else {
                            logger.warn("Module not found: ${request.moduleName}")
                        }
                    }
                    else -> {
                        // 默认分析整个项目
                        moduleManager.modules.forEach { module ->
                            val moduleDeps = analyzeModuleDependencies(module)
                            dependencies.addAll(moduleDeps)
                        }
                    }
                }

                AnalyzeDependenciesResponse(
                    success = true,
                    dependencies = dependencies,
                    totalDependencies = dependencies.size,
                    scope = request.scope
                )
            } catch (e: Exception) {
                logger.error("Dependency analysis failed", e)
                AnalyzeDependenciesResponse(
                    success = false,
                    dependencies = emptyList(),
                    totalDependencies = 0,
                    scope = request.scope
                )
            }
        }
    }

    /**
     * 检测循环依赖
     * @param request 循环依赖检测请求
     * @return 循环依赖检测响应
     */
    fun detectCycles(request: DetectCyclesRequest): DetectCyclesResponse {
        logger.info("Detecting dependency cycles in scope: ${request.scope}")

        return ThreadHelper.runReadAction {
            try {
                val cycles = mutableListOf<DependencyCycle>()
                val moduleManager = ModuleManager.getInstance(project)
                val modules = moduleManager.modules.toList()

                // 构建模块依赖图
                val dependencyGraph = buildDependencyGraph(modules)

                // 检测循环依赖 (使用深度优先搜索)
                val visited = mutableSetOf<String>()
                val recursionStack = mutableSetOf<String>()
                val currentPath = mutableListOf<String>()

                modules.forEach { module ->
                    if (module.name !in visited) {
                        detectCyclesRecursive(
                            module.name,
                            dependencyGraph,
                            visited,
                            recursionStack,
                            currentPath,
                            cycles
                        )
                    }
                }

                DetectCyclesResponse(
                    success = true,
                    cycles = cycles,
                    totalCycles = cycles.size,
                    scope = request.scope
                )
            } catch (e: Exception) {
                logger.error("Cycle detection failed", e)
                DetectCyclesResponse(
                    success = false,
                    cycles = emptyList(),
                    totalCycles = 0,
                    scope = request.scope
                )
            }
        }
    }

    /**
     * 分析单个模块的依赖
     */
    private fun analyzeModuleDependencies(module: Module): List<DependencyInfo> {
        val dependencies = mutableListOf<DependencyInfo>()
        val rootManager = ModuleRootManager.getInstance(module)

        rootManager.orderEntries.forEach { orderEntry ->
            when (orderEntry) {
                is ModuleOrderEntry -> {
                    // 模块依赖
                    val dependencyModule = orderEntry.module
                    if (dependencyModule != null) {
                        dependencies.add(
                            DependencyInfo(
                                from = module.name,
                                to = dependencyModule.name,
                                type = "module",
                                strength = determineStrength(orderEntry)
                            )
                        )
                    }
                }
                is LibraryOrderEntry -> {
                    // 库依赖
                    val library = orderEntry.library
                    if (library != null) {
                        dependencies.add(
                            DependencyInfo(
                                from = module.name,
                                to = library.name ?: "Unknown Library",
                                type = "library",
                                strength = determineStrength(orderEntry)
                            )
                        )
                    }
                }
            }
        }

        return dependencies
    }

    /**
     * 构建依赖图
     */
    private fun buildDependencyGraph(modules: List<Module>): Map<String, List<String>> {
        val graph = mutableMapOf<String, MutableList<String>>()

        modules.forEach { module ->
            val dependencies = mutableListOf<String>()
            val rootManager = ModuleRootManager.getInstance(module)

            rootManager.orderEntries.forEach { orderEntry ->
                if (orderEntry is ModuleOrderEntry) {
                    val dependencyModule = orderEntry.module
                    if (dependencyModule != null) {
                        dependencies.add(dependencyModule.name)
                    }
                }
            }

            graph[module.name] = dependencies
        }

        return graph
    }

    /**
     * 递归检测循环依赖
     */
    private fun detectCyclesRecursive(
        moduleName: String,
        graph: Map<String, List<String>>,
        visited: MutableSet<String>,
        recursionStack: MutableSet<String>,
        currentPath: MutableList<String>,
        cycles: MutableList<DependencyCycle>
    ) {
        visited.add(moduleName)
        recursionStack.add(moduleName)
        currentPath.add(moduleName)

        val dependencies = graph[moduleName] ?: emptyList()
        dependencies.forEach { dependency ->
            if (dependency !in visited) {
                detectCyclesRecursive(
                    dependency,
                    graph,
                    visited,
                    recursionStack,
                    currentPath,
                    cycles
                )
            } else if (dependency in recursionStack) {
                // 发现循环
                val cycleStartIndex = currentPath.indexOf(dependency)
                if (cycleStartIndex >= 0) {
                    val cyclePath = currentPath.subList(cycleStartIndex, currentPath.size) + dependency
                    cycles.add(
                        DependencyCycle(
                            modules = cyclePath,
                            severity = determineCycleSeverity(cyclePath.size)
                        )
                    )
                }
            }
        }

        recursionStack.remove(moduleName)
        currentPath.removeAt(currentPath.size - 1)
    }

    /**
     * 确定依赖强度
     */
    private fun determineStrength(orderEntry: OrderEntry): String {
        return when {
            orderEntry.scope.name == "COMPILE" -> "strong"
            orderEntry.scope.name == "RUNTIME" -> "medium"
            orderEntry.scope.name == "PROVIDED" -> "weak"
            orderEntry.scope.name == "TEST" -> "test"
            else -> "unknown"
        }
    }

    /**
     * 确定循环依赖的严重程度
     */
    private fun determineCycleSeverity(cycleLength: Int): String {
        return when {
            cycleLength <= 2 -> "high"
            cycleLength <= 4 -> "medium"
            else -> "low"
        }
    }

    companion object {
        /**
         * 获取服务实例
         */
        fun getInstance(project: Project): DependencyService {
            return project.getService(DependencyService::class.java)
        }
    }
}
