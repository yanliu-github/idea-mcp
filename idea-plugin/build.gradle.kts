// idea-plugin/build.gradle.kts
import org.jetbrains.intellij.tasks.RunIdeTask

plugins {
    id("java")
    id("org.jetbrains.kotlin.jvm") version "1.9.21"
    id("org.jetbrains.intellij") version "1.16.1"
}

// 项目信息
group = "com.ly.ideamcp"
version = "1.0.0-SNAPSHOT"

// Java 配置
java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

// Kotlin 配置
kotlin {
    jvmToolchain(17)
}

// 仓库配置
repositories {
    maven("https://maven.aliyun.com/repository/public")
    maven("https://cache-redirector.jetbrains.com/intellij-dependencies")
    mavenCentral()
}

// IntelliJ Platform 配置
intellij {
    // 平台版本
    version.set("2024.1")

    // 平台类型: IC (Community) 或 IU (Ultimate)
    type.set("IC")

    // 需要的插件依赖
    plugins.set(listOf(
        "com.intellij.java",         // Java 支持
        "org.jetbrains.kotlin",      // Kotlin 支持
        // "com.intellij.gradle",    // Gradle 支持 (可选)
    ))

    // 下载源码 (用于调试)
    downloadSources.set(true)

    // 更新插件后的行为
    updateSinceUntilBuild.set(true)

    // 沙箱目录
    sandboxDir.set("${project.buildDir}/idea-sandbox")
}

// 依赖配置
dependencies {
    // Kotlin 标准库
    // Kotlin 标准库由 IntelliJ Platform 提供，不需要显式依赖
    // implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")

    // HTTP Server - Undertow
    implementation("io.undertow:undertow-core:2.3.10.Final")

    // JSON 处理 - Gson
    implementation("com.google.code.gson:gson:2.10.1")

    // 日志 (IDEA 已提供 slf4j,这里可选)
    // implementation("org.slf4j:slf4j-api:2.0.9")

    // 工具库
    implementation("com.google.guava:guava:32.1.3-jre")

    // Apache Commons (可选)
    implementation("org.apache.commons:commons-lang3:3.14.0")

    // 测试依赖
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.10.1")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.10.1")
    testImplementation("org.mockito:mockito-core:5.8.0")
    testImplementation("org.assertj:assertj-core:3.24.2")
}

// 任务配置
tasks {
    // Java 编译配置
    withType<JavaCompile> {
        options.encoding = "UTF-8"
        options.compilerArgs.add("-parameters")
        options.compilerArgs.add("-Xlint:unchecked")
        options.compilerArgs.add("-Xlint:deprecation")
    }

    // Kotlin 编译配置
    withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
        kotlinOptions {
            jvmTarget = "17"
            freeCompilerArgs = listOf("-Xjsr305=strict")
        }
    }

    // 补丁插件描述文件
    patchPluginXml {
        sinceBuild.set("241")  // 2024.1
        untilBuild.set("243.*") // 2024.3.*

        // 从 plugin.xml 读取,也可以在这里覆盖
        // version.set("${project.version}")
        // pluginDescription.set("""
        //     Expose IntelliJ IDEA capabilities through MCP protocol.
        // """.trimIndent())

        // 变更日志 (可选)
        changeNotes.set("""
            <h2>Version 1.0.0</h2>
            <ul>
                <li>初始版本发布</li>
                <li>支持基础重构功能</li>
                <li>支持代码导航</li>
                <li>支持代码分析</li>
            </ul>
        """.trimIndent())
    }

    // 运行 IDE 任务配置
    runIde {
        // JVM 参数
        jvmArgs = listOf(
            "-Xmx2048m",
            "-XX:+UseG1GC",
            "-Didea.plugin.in.sandbox.mode=true",
            "-Didea.auto.reload.plugins=true"
        )

        // 系统属性
        systemProperty("idea.is.internal", "true")
        systemProperty("idea.debug.mode", "true")

        // 自动重载
        autoReloadPlugins.set(true)
    }

    // 验证插件配置
    verifyPlugin {
        // 忽略的问题
        // ignoredProblems.set(setOf())
    }

    // 签名插件 (发布到插件市场时需要)
    signPlugin {
        // certificateChain.set(System.getenv("CERTIFICATE_CHAIN"))
        // privateKey.set(System.getenv("PRIVATE_KEY"))
        // password.set(System.getenv("PRIVATE_KEY_PASSWORD"))
    }

    // 发布插件
    publishPlugin {
        // token.set(System.getenv("PUBLISH_TOKEN"))
        // channels.set(listOf("default"))
    }

    // 构建插件 ZIP
    buildPlugin {
        archiveBaseName.set("idea-mcp-plugin")
        archiveVersion.set("${project.version}")
    }

    // 测试配置
    test {
        useJUnitPlatform()

        testLogging {
            events("passed", "skipped", "failed")
            showStandardStreams = true
            exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
        }

        // 测试 JVM 参数
        jvmArgs = listOf(
            "-Xmx1024m",
            "-Didea.system.path=${buildDir}/test-system",
            "-Didea.config.path=${buildDir}/test-config"
        )
    }
}

// 自定义任务
tasks.register("printVersion") {
    group = "help"
    description = "Print project version"
    doLast {
        println("Project version: ${project.version}")
        println("Platform version: ${intellij.version.get()}")
    }
}

tasks.register("runWithDebug", RunIdeTask::class) {
    group = "intellij"
    description = "Run IDE with remote debug enabled"

    jvmArgs = listOf(
        "-Xmx2048m",
        "-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:5005"
    )
}

// 输出配置
tasks.register("showConfig") {
    group = "help"
    description = "Show build configuration"
    doLast {
        println("=".repeat(60))
        println("Build Configuration")
        println("=".repeat(60))
        println("Group: ${project.group}")
        println("Version: ${project.version}")
        println("Java: ${java.sourceCompatibility}")
        println("Platform: ${intellij.type.get()} ${intellij.version.get()}")
        println("Build dir: ${project.buildDir}")
        println("=".repeat(60))
    }
}
