package com.ly.ideamcp.util

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.Project
import com.ly.ideamcp.config.PluginSettings
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ExecutionException

/**
 * 线程辅助工具
 * 处理 IDEA 的线程模型（ReadAction/WriteAction）
 */
object ThreadHelper {

    /**
     * 获取配置的默认超时时间（秒）
     */
    private fun getDefaultTimeout(): Long {
        return try {
            PluginSettings.getInstance().requestTimeout.toLong()
        } catch (e: Exception) {
            30L // 回退默认值
        }
    }

    /**
     * 在读锁中执行操作
     * @param T 返回类型
     * @param action 要执行的操作
     * @return 操作结果
     */
    fun <T> runReadAction(action: () -> T): T {
        return ApplicationManager.getApplication().runReadAction<T> {
            action()
        }
    }

    /**
     * 在写锁中执行操作
     * @param T 返回类型
     * @param action 要执行的操作
     * @return 操作结果
     */
    fun <T> runWriteAction(action: () -> T): T {
        return ApplicationManager.getApplication().runWriteAction<T> {
            action()
        }
    }

    /**
     * 异步执行后台任务
     * @param T 返回类型
     * @param project 项目
     * @param title 任务标题
     * @param canBeCancelled 是否可取消
     * @param action 要执行的操作
     * @return CompletableFuture
     */
    fun <T> executeAsync(
        project: Project?,
        title: String,
        canBeCancelled: Boolean = true,
        action: (ProgressIndicator) -> T
    ): CompletableFuture<T> {
        val future = CompletableFuture<T>()

        ProgressManager.getInstance().run(
            object : Task.Backgroundable(project, title, canBeCancelled) {
                override fun run(indicator: ProgressIndicator) {
                    try {
                        val result = action(indicator)
                        future.complete(result)
                    } catch (e: Exception) {
                        future.completeExceptionally(e)
                    }
                }

                override fun onCancel() {
                    future.cancel(true)
                }

                override fun onThrowable(error: Throwable) {
                    future.completeExceptionally(error)
                }
            }
        )

        return future
    }

    /**
     * 同步执行后台任务（带超时）
     * @param T 返回类型
     * @param project 项目
     * @param title 任务标题
     * @param timeoutSeconds 超时时间（秒）
     * @param action 要执行的操作
     * @return 操作结果
     * @throws TimeoutException 如果超时
     * @throws ExecutionException 如果执行出错
     */
    fun <T> executeWithTimeout(
        project: Project?,
        title: String,
        timeoutSeconds: Long = getDefaultTimeout(),
        action: (ProgressIndicator) -> T
    ): T {
        val future = executeAsync(project, title, true, action)
        return try {
            future.get(timeoutSeconds, TimeUnit.SECONDS)
        } catch (e: java.util.concurrent.TimeoutException) {
            future.cancel(true)
            throw TimeoutException("Operation timed out after $timeoutSeconds seconds")
        } catch (e: ExecutionException) {
            throw e.cause ?: e
        }
    }

    /**
     * 在 EDT (Event Dispatch Thread) 中执行操作
     * @param action 要执行的操作
     */
    fun invokeLater(action: () -> Unit) {
        ApplicationManager.getApplication().invokeLater(action)
    }

    /**
     * 在 EDT 中同步执行操作
     * @param action 要执行的操作
     */
    fun invokeAndWait(action: () -> Unit) {
        ApplicationManager.getApplication().invokeAndWait(action)
    }

    /**
     * 检查当前是否在 EDT 线程
     * @return 是否在 EDT
     */
    fun isEventDispatchThread(): Boolean {
        return ApplicationManager.getApplication().isDispatchThread
    }

    /**
     * 检查当前是否在读锁中
     * @return 是否在读锁中
     */
    fun isReadAccessAllowed(): Boolean {
        return ApplicationManager.getApplication().isReadAccessAllowed
    }

    /**
     * 检查当前是否在写锁中
     * @return 是否在写锁中
     */
    fun isWriteAccessAllowed(): Boolean {
        return ApplicationManager.getApplication().isWriteAccessAllowed
    }

    /**
     * 确保在读锁中执行
     * 如果已经在读锁中，直接执行；否则获取读锁
     * @param T 返回类型
     * @param action 要执行的操作
     * @return 操作结果
     */
    fun <T> ensureReadAction(action: () -> T): T {
        return if (isReadAccessAllowed()) {
            action()
        } else {
            runReadAction(action)
        }
    }

    /**
     * 确保在写锁中执行
     * 如果已经在写锁中，直接执行；否则获取写锁
     * @param T 返回类型
     * @param action 要执行的操作
     * @return 操作结果
     */
    fun <T> ensureWriteAction(action: () -> T): T {
        return if (isWriteAccessAllowed()) {
            action()
        } else {
            runWriteAction(action)
        }
    }

    /**
     * 带超时的读操作
     * @param T 返回类型
     * @param timeoutSeconds 超时时间（秒）
     * @param action 要执行的操作
     * @return 操作结果
     * @throws TimeoutException 如果超时
     */
    fun <T> runReadActionWithTimeout(
        timeoutSeconds: Long = getDefaultTimeout(),
        action: () -> T
    ): T {
        val future = CompletableFuture.supplyAsync {
            runReadAction(action)
        }
        return try {
            future.get(timeoutSeconds, TimeUnit.SECONDS)
        } catch (e: java.util.concurrent.TimeoutException) {
            future.cancel(true)
            throw TimeoutException("Read operation timed out after $timeoutSeconds seconds")
        } catch (e: ExecutionException) {
            throw e.cause ?: e
        }
    }

    /**
     * 带超时的写操作
     * @param T 返回类型
     * @param timeoutSeconds 超时时间（秒）
     * @param action 要执行的操作
     * @return 操作结果
     * @throws TimeoutException 如果超时
     */
    fun <T> runWriteActionWithTimeout(
        timeoutSeconds: Long = getDefaultTimeout(),
        action: () -> T
    ): T {
        val future = CompletableFuture<T>()

        invokeLater {
            try {
                val result = runWriteAction(action)
                future.complete(result)
            } catch (e: Exception) {
                future.completeExceptionally(e)
            }
        }

        return try {
            future.get(timeoutSeconds, TimeUnit.SECONDS)
        } catch (e: java.util.concurrent.TimeoutException) {
            future.cancel(true)
            throw TimeoutException("Write operation timed out after $timeoutSeconds seconds")
        } catch (e: ExecutionException) {
            throw e.cause ?: e
        }
    }
}
