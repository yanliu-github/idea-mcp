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
                    path = module.moduleFilePath ?: "",
                    type = module.moduleTypeName ?: "UNKNOWN"
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
                    path = module.moduleFilePath ?: "",
                    type = module.moduleTypeName ?: "UNKNOWN"
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
            try {
                val projectSdk = com.intellij.openapi.projectRoots.ProjectJdkTable.getInstance()
                    .allJdks.firstOrNull()

                val modules = ModuleManager.getInstance(project).modules
                val buildSystem = detectBuildSystem()

                // 尝试获取 JDK 信息
                val jdkVersion = projectSdk?.versionString ?: "Unknown"

                // 从模块配置中获取源代码兼容性
                val moduleRootManager = if (modules.isNotEmpty()) {
                    ModuleRootManager.getInstance(modules[0])
                } else {
                    null
                }

                val sdk = moduleRootManager?.sdk
                val languageLevel = sdk?.let {
                    com.intellij.pom.java.LanguageLevel.parse(it.versionString)
                }

                val sourceCompatibility = languageLevel?.toJavaVersion()?.toString() ?: "17"

                BuildConfigResponse(
                    success = true,
                    buildSystem = buildSystem,
                    jdkVersion = jdkVersion,
                    sourceCompatibility = sourceCompatibility,
                    targetCompatibility = sourceCompatibility
                )
            } catch (e: Exception) {
                logger.error("Failed to get build config", e)
                BuildConfigResponse(
                    success = false,
                    buildSystem = "Unknown",
                    jdkVersion = "Unknown",
                    sourceCompatibility = "Unknown",
                    targetCompatibility = "Unknown"
                )
            }
        }
    }

    /**
     * 检测构建系统
     */
    private fun detectBuildSystem(): String {
        val basePath = project.basePath ?: return "Unknown"

        // 检查 Gradle
        val gradleFiles = listOf(
            "$basePath/build.gradle",
            "$basePath/build.gradle.kts",
            "$basePath/settings.gradle",
            "$basePath/settings.gradle.kts"
        )
        if (gradleFiles.any { java.io.File(it).exists() }) {
            return "Gradle"
        }

        // 检查 Maven
        if (java.io.File("$basePath/pom.xml").exists()) {
            return "Maven"
        }

        // 检查 Ant
        if (java.io.File("$basePath/build.xml").exists()) {
            return "Ant"
        }

        return "Unknown"
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
