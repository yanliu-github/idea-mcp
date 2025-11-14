package com.ly.ideamcp.service

import com.intellij.openapi.components.Service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
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
            // 占位实现 - 实际应使用 DependenciesBuilder
            AnalyzeDependenciesResponse(
                success = true,
                dependencies = listOf(
                    DependencyInfo(
                        from = "com.example.ModuleA",
                        to = "com.example.ModuleB",
                        type = "compile",
                        strength = "strong"
                    )
                ),
                totalDependencies = 1,
                scope = request.scope
            )
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
            // 占位实现 - 实际应使用 CyclicDependenciesBuilder
            DetectCyclesResponse(
                success = true,
                cycles = emptyList(), // 示例：无循环依赖
                totalCycles = 0,
                scope = request.scope
            )
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
