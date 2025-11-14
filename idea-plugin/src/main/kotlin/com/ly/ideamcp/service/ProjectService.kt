package com.ly.ideamcp.service

import com.intellij.openapi.components.Service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.module.ModuleManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.LibraryOrderEntry
import com.intellij.openapi.roots.ModuleRootManager
import com.ly.ideamcp.model.project.*
import com.ly.ideamcp.util.ThreadHelper

/**
 * 项目管理服务
 * 提供项目结构、模块和库信息
 */
@Service(Service.Level.PROJECT)
class ProjectService(private val project: Project) {

    private val logger = Logger.getInstance(ProjectService::class.java)

    /**
     * 获取项目结构
     * @return 项目结构响应
     */
    fun getStructure(): ProjectStructureResponse {
        logger.info("Getting project structure")

        return ThreadHelper.runReadAction {
            val modules = ModuleManager.getInstance(project).modules.map { module ->
                ModuleInfo(
                    name = module.name,
                    path = module.moduleFilePath,
                    type = module.moduleTypeName
                )
            }

            ProjectStructureResponse(
                success = true,
                projectName = project.name,
                projectPath = project.basePath ?: "",
                modules = modules,
                totalModules = modules.size
            )
        }
    }

    /**
     * 获取模块列表
     * @return 模块列表响应
     */
    fun getModules(): ModulesResponse {
        logger.info("Getting modules")

        return ThreadHelper.runReadAction {
            val modules = ModuleManager.getInstance(project).modules.map { module ->
                ModuleInfo(
                    name = module.name,
                    path = module.moduleFilePath,
                    type = module.moduleTypeName
                )
            }

            ModulesResponse(
                success = true,
                modules = modules,
                totalModules = modules.size
            )
        }
    }

    /**
     * 获取库列表
     * @return 库列表响应
     */
    fun getLibraries(): LibrariesResponse {
        logger.info("Getting libraries")

        return ThreadHelper.runReadAction {
            val libraries = mutableListOf<LibraryInfo>()

            ModuleManager.getInstance(project).modules.forEach { module ->
                ModuleRootManager.getInstance(module).orderEntries.forEach { orderEntry ->
                    if (orderEntry is LibraryOrderEntry) {
                        val library = orderEntry.library
                        if (library != null) {
                            libraries.add(
                                LibraryInfo(
                                    name = library.name ?: "Unknown",
                                    version = "Unknown",
                                    scope = orderEntry.scope.name
                                )
                            )
                        }
                    }
                }
            }

            LibrariesResponse(
                success = true,
                libraries = libraries.distinctBy { it.name },
                totalLibraries = libraries.size
            )
        }
    }

    /**
     * 获取构建配置
     * @return 构建配置响应
     */
    fun getBuildConfig(): BuildConfigResponse {
        logger.info("Getting build config")

        return ThreadHelper.runReadAction {
            // 占位实现 - 实际应解析 build.gradle/pom.xml
            BuildConfigResponse(
                success = true,
                buildSystem = "Gradle",
                jdkVersion = "17",
                sourceCompatibility = "17",
                targetCompatibility = "17"
            )
        }
    }

    companion object {
        /**
         * 获取服务实例
         */
        fun getInstance(project: Project): ProjectService {
            return project.getService(ProjectService::class.java)
        }
    }
}
