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
