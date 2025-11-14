// settings.gradle.kts
rootProject.name = "idea-mcp"

// 包含子模块
include("idea-plugin")

// 如果有多个 IDEA Plugin 模块,可以这样添加:
// include("idea-plugin-core")
// include("idea-plugin-ui")

pluginManagement {
    repositories {
        // 优先使用阿里云镜像 (中国大陆用户)
        maven("https://maven.aliyun.com/repository/gradle-plugin")
        maven("https://maven.aliyun.com/repository/public")

        // JetBrains 插件仓库
        maven("https://plugins.gradle.org/m2/")
        maven("https://cache-redirector.jetbrains.com/intellij-dependencies")

        // 默认仓库
        gradlePluginPortal()
        mavenCentral()
    }
}

// 依赖解析管理
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.PREFER_PROJECT)
    repositories {
        // 优先使用阿里云镜像
        maven("https://maven.aliyun.com/repository/public")
        maven("https://maven.aliyun.com/repository/spring")
        maven("https://maven.aliyun.com/repository/google")

        // JetBrains 仓库
        maven("https://cache-redirector.jetbrains.com/intellij-dependencies")
        maven("https://www.jetbrains.com/intellij-repository/releases")

        // 默认仓库
        mavenCentral()
        google()
    }
}
