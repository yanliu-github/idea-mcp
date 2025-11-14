# IntelliJ IDEA MCP 服务器 - Gradle 配置示例

## 文档信息

- **文档版本**: v1.0
- **创建日期**: 2025-11-13
- **文档状态**: 配置示例
- **项目名称**: IDEA MCP Server

---

## 1. 项目结构

```
idea-mcp/
├── settings.gradle.kts          # 根设置文件
├── build.gradle.kts             # 根构建文件
├── gradle.properties            # Gradle 属性
├── gradlew                      # Gradle Wrapper (Unix)
├── gradlew.bat                  # Gradle Wrapper (Windows)
├── gradle/
│   └── wrapper/
│       ├── gradle-wrapper.jar
│       └── gradle-wrapper.properties
├── idea-plugin/                 # IDEA Plugin 模块
│   ├── build.gradle.kts
│   ├── src/
│   │   ├── main/
│   │   │   ├── kotlin/
│   │   │   │   └── com/ly/ideamcp/
│   │   │   └── resources/
│   │   │       └── META-INF/
│   │   │           └── plugin.xml
│   │   └── test/
│   │       └── kotlin/
│   └── README.md
└── mcp-server/                  # MCP Server 模块 (Node.js)
    ├── package.json
    ├── tsconfig.json
    └── src/
```

---

## 2. 根项目配置

### 2.1 settings.gradle.kts

```kotlin
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
```

---

### 2.2 build.gradle.kts (根)

```kotlin
// build.gradle.kts (根项目)
plugins {
    // 应用到所有子项目
    id("java") apply false
    id("org.jetbrains.kotlin.jvm") version "1.9.21" apply false
    id("org.jetbrains.intellij") version "1.16.1" apply false
}

// 全局配置
allprojects {
    group = "com.ly.ideamcp"
    version = "1.0.0-SNAPSHOT"

    repositories {
        maven("https://maven.aliyun.com/repository/public")
        maven("https://cache-redirector.jetbrains.com/intellij-dependencies")
        mavenCentral()
    }
}

// 子项目通用配置
subprojects {
    apply(plugin = "java")

    java {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    tasks.withType<JavaCompile> {
        options.encoding = "UTF-8"
        options.compilerArgs.add("-parameters")
    }

    tasks.withType<Test> {
        useJUnitPlatform()
        testLogging {
            events("passed", "skipped", "failed")
            showStandardStreams = false
        }
    }
}

// 清理任务
tasks.register("cleanAll") {
    dependsOn(subprojects.map { it.tasks.named("clean") })
    description = "Clean all subprojects"
    group = "build"
}
```

---

### 2.3 gradle.properties

```properties
# Gradle Properties
org.gradle.jvmargs=-Xmx2048m -XX:MaxMetaspaceSize=512m -XX:+HeapDumpOnOutOfMemoryError
org.gradle.parallel=true
org.gradle.caching=true
org.gradle.daemon=true

# Kotlin
kotlin.code.style=official

# Project Properties
projectGroup=com.ly.ideamcp
projectVersion=1.0.0-SNAPSHOT

# IDEA Platform
platformVersion=2024.1
platformType=IC
# IC = IntelliJ IDEA Community Edition
# IU = IntelliJ IDEA Ultimate Edition

# Java
javaVersion=17

# Plugin Info
pluginName=IDEA MCP Server
pluginDescription=Expose IntelliJ IDEA capabilities through MCP protocol
pluginVendor=ly
pluginSinceBuild=241
pluginUntilBuild=243.*

# HTTP Server
httpServerPort=58888
```

---

## 3. IDEA Plugin 模块配置

### 3.1 idea-plugin/build.gradle.kts

```kotlin
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
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")

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
```

---

### 3.2 plugin.xml

```xml
<!-- idea-plugin/src/main/resources/META-INF/plugin.xml -->
<idea-plugin>
    <!-- 插件唯一 ID -->
    <id>com.ly.ideamcp</id>

    <!-- 插件名称 -->
    <name>IDEA MCP Server</name>

    <!-- 插件供应商 -->
    <vendor email="support@example.com" url="https://github.com/your-org/idea-mcp">
        ly
    </vendor>

    <!-- 插件描述 -->
    <description><![CDATA[
        <h2>IntelliJ IDEA MCP Server</h2>
        <p>
            Expose IntelliJ IDEA's powerful capabilities through MCP (Model Context Protocol),
            enabling AI tools like Claude Code to perform precise refactoring, navigation,
            and code analysis.
        </p>

        <h3>Features:</h3>
        <ul>
            <li><b>Refactoring</b>: Rename, Extract Method, Inline, etc.</li>
            <li><b>Navigation</b>: Find Usages, Go to Definition, Call Hierarchy</li>
            <li><b>Code Analysis</b>: Inspections, Error Detection, Dependency Analysis</li>
            <li><b>Debugging</b>: Breakpoint Management, Expression Evaluation</li>
            <li><b>Code Generation</b>: Getters/Setters, Constructors, Tests</li>
        </ul>

        <h3>Usage:</h3>
        <p>
            The plugin starts an HTTP server (default port: 58888) on IDEA startup.
            Connect your MCP server to this endpoint to enable AI-powered development.
        </p>
    ]]></description>

    <!-- 变更日志 -->
    <change-notes><![CDATA[
        <h3>Version 1.0.0</h3>
        <ul>
            <li>Initial release</li>
            <li>Support for basic refactoring operations</li>
            <li>Code navigation features</li>
            <li>Code analysis and inspections</li>
        </ul>
    ]]></change-notes>

    <!-- 依赖的平台和插件 -->
    <depends>com.intellij.modules.platform</depends>
    <depends>com.intellij.modules.java</depends>

    <!-- 可选依赖 (如果安装了才启用相关功能) -->
    <!-- <depends optional="true" config-file="kotlin-support.xml">org.jetbrains.kotlin</depends> -->

    <!-- 兼容的 IDEA 版本 -->
    <idea-version since-build="241" until-build="243.*"/>

    <!-- 扩展点和监听器 -->
    <extensions defaultExtensionNs="com.intellij">
        <!-- 应用级别的服务 (单例) -->
        <applicationService
            serviceImplementation="com.ly.ideamcp.config.PluginSettings"/>

        <!-- 项目级别的服务 -->
        <projectService
            serviceImplementation="com.ly.ideamcp.service.RefactoringService"/>
        <projectService
            serviceImplementation="com.ly.ideamcp.service.NavigationService"/>
        <projectService
            serviceImplementation="com.ly.ideamcp.service.AnalysisService"/>

        <!-- 项目打开监听器 -->
        <postStartupActivity
            implementation="com.ly.ideamcp.startup.ServerStartupActivity"/>

        <!-- 配置界面 -->
        <applicationConfigurable
            parentId="tools"
            instance="com.ly.ideamcp.ui.PluginConfigurable"
            id="com.ly.ideamcp.settings"
            displayName="IDEA MCP Server"/>
    </extensions>

    <!-- Actions -->
    <actions>
        <!-- Tools 菜单 -->
        <group id="IdeaMcp.ToolsMenu" text="IDEA MCP" popup="true">
            <add-to-group group-id="ToolsMenu" anchor="last"/>

            <action id="IdeaMcp.StartServer"
                    class="com.ly.ideamcp.action.StartServerAction"
                    text="Start MCP Server"
                    description="Start the MCP HTTP server">
                <keyboard-shortcut first-keystroke="control alt M" keymap="$default"/>
            </action>

            <action id="IdeaMcp.StopServer"
                    class="com.ly.ideamcp.action.StopServerAction"
                    text="Stop MCP Server"
                    description="Stop the MCP HTTP server"/>

            <action id="IdeaMcp.RestartServer"
                    class="com.ly.ideamcp.action.RestartServerAction"
                    text="Restart MCP Server"
                    description="Restart the MCP HTTP server"/>

            <separator/>

            <action id="IdeaMcp.ViewStatus"
                    class="com.ly.ideamcp.action.ViewStatusAction"
                    text="View Server Status"
                    description="View MCP server status"/>
        </group>

        <!-- 右键菜单 (Editor Popup) -->
        <!-- <group id="IdeaMcp.EditorPopup" text="IDEA MCP" popup="true">
            <add-to-group group-id="EditorPopupMenu" anchor="last"/>
        </group> -->
    </actions>

    <!-- 应用组件 (deprecated, 使用 postStartupActivity 代替) -->
    <!-- <application-components>
        <component>
            <implementation-class>com.ly.ideamcp.IdeaMcpPlugin</implementation-class>
        </component>
    </application-components> -->
</idea-plugin>
```

---

## 4. Gradle Wrapper 配置

### 4.1 gradle/wrapper/gradle-wrapper.properties

```properties
distributionBase=GRADLE_USER_HOME
distributionPath=wrapper/dists
distributionUrl=https\://services.gradle.org/distributions/gradle-8.5-bin.zip
# 使用国内镜像 (可选)
# distributionUrl=https\://mirrors.cloud.tencent.com/gradle/gradle-8.5-bin.zip
networkTimeout=60000
validateDistributionUrl=true
zipStoreBase=GRADLE_USER_HOME
zipStorePath=wrapper/dists
```

---

## 5. 常用 Gradle 命令

### 5.1 构建命令

```bash
# 清理构建
./gradlew clean

# 编译项目
./gradlew build

# 仅编译不运行测试
./gradlew build -x test

# 运行 IDEA 沙箱 (开发模式)
./gradlew runIde

# 运行带远程调试的 IDEA
./gradlew runWithDebug

# 构建插件 ZIP (用于分发)
./gradlew buildPlugin

# 验证插件
./gradlew verifyPlugin
```

### 5.2 测试命令

```bash
# 运行所有测试
./gradlew test

# 运行特定测试类
./gradlew test --tests "com.ly.ideamcp.service.RefactoringServiceTest"

# 运行测试并生成报告
./gradlew test jacocoTestReport
```

### 5.3 依赖命令

```bash
# 查看依赖树
./gradlew dependencies

# 查看子项目依赖
./gradlew :idea-plugin:dependencies

# 刷新依赖
./gradlew build --refresh-dependencies
```

### 5.4 信息命令

```bash
# 查看所有任务
./gradlew tasks

# 查看项目信息
./gradlew projects

# 查看配置
./gradlew showConfig

# 查看版本
./gradlew printVersion
```

---

## 6. IDEA 集成配置

### 6.1 导入项目到 IDEA

1. 打开 IDEA
2. `File > Open`
3. 选择项目根目录的 `build.gradle.kts`
4. 选择 `Open as Project`
5. 等待 Gradle 同步完成

### 6.2 配置运行配置

**方法一: 使用 Gradle 任务**

1. 打开 `Run > Edit Configurations`
2. 点击 `+` > `Gradle`
3. 配置:
   - Name: `Run Plugin`
   - Gradle project: `idea-mcp:idea-plugin`
   - Tasks: `runIde`
   - VM options: `-Xmx2048m`

**方法二: 使用 IntelliJ Platform Plugin**

1. 打开 `Run > Edit Configurations`
2. 点击 `+` > `Gradle` (选择 runIde 任务)
3. IDEA 会自动识别插件项目

### 6.3 配置调试

1. 添加 Remote JVM Debug 配置
2. Port: `5005`
3. 先运行 `./gradlew runWithDebug`
4. 然后 Attach 调试器

---

## 7. 发布配置

### 7.1 发布到 JetBrains Marketplace

```kotlin
// 在 build.gradle.kts 中配置
tasks {
    publishPlugin {
        // 从环境变量读取 Token
        token.set(System.getenv("PUBLISH_TOKEN"))

        // 发布渠道
        channels.set(listOf("default"))
        // 或 beta 渠道: channels.set(listOf("beta"))
    }
}
```

### 7.2 发布命令

```bash
# 设置环境变量
export PUBLISH_TOKEN="your-token-here"

# 发布插件
./gradlew publishPlugin

# 发布到 beta 渠道
./gradlew publishPlugin -Pchannels=beta
```

---

## 8. 常见问题

### 8.1 依赖下载失败

**问题**: Gradle 下载依赖超时

**解决方案**:
1. 配置国内镜像 (见 2.1 节)
2. 增加超时时间:
   ```properties
   # gradle.properties
   systemProp.http.connectionTimeout=60000
   systemProp.http.socketTimeout=60000
   ```

### 8.2 IDEA 版本不兼容

**问题**: Plugin 无法在目标 IDEA 版本运行

**解决方案**:
1. 检查 `gradle.properties` 中的 `platformVersion`
2. 检查 `plugin.xml` 中的 `since-build` 和 `until-build`
3. 运行 `./gradlew verifyPlugin` 验证

### 8.3 内存不足

**问题**: Gradle 构建时 OutOfMemoryError

**解决方案**:
```properties
# gradle.properties
org.gradle.jvmargs=-Xmx4096m -XX:MaxMetaspaceSize=1024m
```

---

## 9. 参考资源

- [Gradle 官方文档](https://docs.gradle.org/)
- [IntelliJ Platform Plugin 开发指南](https://plugins.jetbrains.com/docs/intellij/)
- [Gradle IntelliJ Plugin](https://plugins.jetbrains.com/docs/intellij/tools-gradle-intellij-plugin.html)

---

**文档维护**: Gradle 配置变更时需更新此文档。
