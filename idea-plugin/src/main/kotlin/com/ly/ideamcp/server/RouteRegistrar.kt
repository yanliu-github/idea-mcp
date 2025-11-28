package com.ly.ideamcp.server

import com.intellij.openapi.application.ApplicationInfo
import com.intellij.openapi.module.ModuleManager
import com.intellij.openapi.project.DumbService
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectManager
import com.ly.ideamcp.model.analysis.InspectionRequest
import com.ly.ideamcp.model.codegen.*
import com.ly.ideamcp.model.debug.*
import com.ly.ideamcp.model.dependency.AnalyzeDependenciesRequest
import com.ly.ideamcp.model.dependency.DetectCyclesRequest
import com.ly.ideamcp.model.navigation.*
import com.ly.ideamcp.model.refactor.*
import com.ly.ideamcp.model.search.*
import com.ly.ideamcp.model.tools.*
import com.ly.ideamcp.model.vcs.*
import com.ly.ideamcp.service.*

/**
 * 路由注册器
 * 集中管理所有 API 端点的注册
 */
object RouteRegistrar {

    /**
     * 注册所有路由
     */
    fun registerAll(router: RouterConfig) {
        router.routes {
            registerSystemRoutes()
            registerRefactoringRoutes()
            registerNavigationRoutes()
            registerAnalysisRoutes()
            registerDebugRoutes()
            registerCodeGenRoutes()
            registerSearchRoutes()
            registerDependencyRoutes()
            registerVcsRoutes()
            registerProjectRoutes()
            registerToolsRoutes()
        }
    }

    // ==================== 系统管理 API ====================

    private fun RouteDsl.registerSystemRoutes() {
        // 健康检查
        get("/api/v1/health") {
            mapOf(
                "status" to "ok",
                "ideaVersion" to getIdeaVersion(),
                "indexReady" to isIndexReady()
            )
        }

        // 项目信息
        getWithProject("/api/v1/project/info") { project ->
            mapOf(
                "name" to project.name,
                "path" to project.basePath,
                "modules" to getProjectModules(project)
            )
        }
    }

    // ==================== 重构 API ====================

    private fun RouteDsl.registerRefactoringRoutes() {
        postWithBody<RenameRequest>("/api/v1/refactor/rename") { project, request ->
            RefactoringService.getInstance(project).renameSymbol(request)
        }

        postWithBody<ExtractMethodRequest>("/api/v1/refactor/extract-method") { project, request ->
            RefactoringService.getInstance(project).extractMethod(request)
        }

        postWithBody<ExtractVariableRequest>("/api/v1/refactor/extract-variable") { project, request ->
            RefactoringService.getInstance(project).extractVariable(request)
        }

        postWithBody<InlineVariableRequest>("/api/v1/refactor/inline-variable") { project, request ->
            RefactoringService.getInstance(project).inlineVariable(request)
        }

        postWithBody<ChangeSignatureRequest>("/api/v1/refactor/change-signature") { project, request ->
            RefactoringService.getInstance(project).changeSignature(request)
        }

        postWithBody<MoveRequest>("/api/v1/refactor/move") { project, request ->
            RefactoringService.getInstance(project).move(request)
        }

        postWithBody<ExtractInterfaceRequest>("/api/v1/refactor/extract-interface") { project, request ->
            RefactoringService.getInstance(project).extractInterface(request)
        }

        postWithBody<ExtractSuperclassRequest>("/api/v1/refactor/extract-superclass") { project, request ->
            RefactoringService.getInstance(project).extractSuperclass(request)
        }

        postWithBody<EncapsulateFieldRequest>("/api/v1/refactor/encapsulate-field") { project, request ->
            RefactoringService.getInstance(project).encapsulateField(request)
        }

        postWithBody<IntroduceParameterObjectRequest>("/api/v1/refactor/introduce-parameter-object") { project, request ->
            RefactoringService.getInstance(project).introduceParameterObject(request)
        }
    }

    // ==================== 导航 API ====================

    private fun RouteDsl.registerNavigationRoutes() {
        postWithBody<FindUsagesRequest>("/api/v1/navigation/find-usages") { project, request ->
            NavigationService.getInstance(project).findUsages(request)
        }

        postWithBody<GotoDefinitionRequest>("/api/v1/navigation/goto-definition") { project, request ->
            NavigationService.getInstance(project).gotoDefinition(request)
        }

        postWithBody<TypeHierarchyRequest>("/api/v1/navigation/type-hierarchy") { project, request ->
            NavigationService.getInstance(project).showTypeHierarchy(request)
        }

        postWithBody<CallHierarchyRequest>("/api/v1/navigation/call-hierarchy") { project, request ->
            NavigationService.getInstance(project).showCallHierarchy(request)
        }

        postWithBody<FindImplementationsRequest>("/api/v1/navigation/find-implementations") { project, request ->
            NavigationService.getInstance(project).findImplementations(request)
        }
    }

    // ==================== 分析 API ====================

    private fun RouteDsl.registerAnalysisRoutes() {
        postWithBody<InspectionRequest>("/api/v1/analysis/inspections") { project, request ->
            AnalysisService.getInstance(project).runInspections(request)
        }
    }

    // ==================== 调试 API ====================

    private fun RouteDsl.registerDebugRoutes() {
        // 断点管理
        postWithBody<BreakpointRequest>("/api/v1/debug/breakpoint") { project, request ->
            DebugService.getInstance(project).setBreakpoint(request)
        }

        getWithProject("/api/v1/debug/breakpoints") { project ->
            DebugService.getInstance(project).listBreakpoints()
        }

        deleteWithPathParam("/api/v1/debug/breakpoint/{id}", "id") { project, breakpointId ->
            DebugService.getInstance(project).removeBreakpoint(breakpointId)
        }

        // 调试会话管理
        postWithBody<DebugSessionRequest>("/api/v1/debug/session/start") { project, request ->
            DebugService.getInstance(project).startDebugSession(request)
        }

        postWithMap("/api/v1/debug/session/stop") { project, body ->
            val sessionId = body["sessionId"] as? String
                ?: throw IllegalArgumentException("Session ID is required")
            DebugService.getInstance(project).stopDebugSession(sessionId)
        }

        postWithMap("/api/v1/debug/session/pause") { project, body ->
            val sessionId = body["sessionId"] as? String
                ?: throw IllegalArgumentException("Session ID is required")
            DebugService.getInstance(project).pauseDebugSession(sessionId)
        }

        postWithMap("/api/v1/debug/session/resume") { project, body ->
            val sessionId = body["sessionId"] as? String
                ?: throw IllegalArgumentException("Session ID is required")
            DebugService.getInstance(project).resumeDebugSession(sessionId)
        }

        postWithMap("/api/v1/debug/session/step-over") { project, body ->
            val sessionId = body["sessionId"] as? String
                ?: throw IllegalArgumentException("Session ID is required")
            DebugService.getInstance(project).stepOver(sessionId)
        }

        postWithMap("/api/v1/debug/session/step-into") { project, body ->
            val sessionId = body["sessionId"] as? String
                ?: throw IllegalArgumentException("Session ID is required")
            DebugService.getInstance(project).stepInto(sessionId)
        }

        postWithMap("/api/v1/debug/session/step-out") { project, body ->
            val sessionId = body["sessionId"] as? String
                ?: throw IllegalArgumentException("Session ID is required")
            DebugService.getInstance(project).stepOut(sessionId)
        }

        // 表达式和变量
        postWithBody<EvaluateExpressionRequest>("/api/v1/debug/evaluate") { project, request ->
            DebugService.getInstance(project).evaluateExpression(request)
        }

        getWithParams("/api/v1/debug/variables") { project, params ->
            val sessionId = params["sessionId"]
                ?: throw IllegalArgumentException("Session ID is required")
            val frameIndex = params["frameIndex"]?.toIntOrNull() ?: 0
            val scope = params["scope"] ?: "local"

            val request = GetVariablesRequest(
                sessionId = sessionId,
                frameIndex = frameIndex,
                scope = scope
            )
            DebugService.getInstance(project).getVariables(request)
        }

        getWithParams("/api/v1/debug/variable/{name}") { project, params ->
            val sessionId = params["sessionId"]
                ?: throw IllegalArgumentException("Session ID is required")
            // 从路径提取变量名需要特殊处理
            DebugService.getInstance(project).getVariable(sessionId, "")
        }

        // 调用栈
        getWithParams("/api/v1/debug/call-stack") { project, params ->
            val sessionId = params["sessionId"]
                ?: throw IllegalArgumentException("Session ID is required")
            val request = GetCallStackRequest(sessionId = sessionId)
            DebugService.getInstance(project).getCallStack(request)
        }
    }

    // ==================== 代码生成 API ====================

    private fun RouteDsl.registerCodeGenRoutes() {
        postWithBody<GenerateGettersSettersRequest>("/api/v1/codegen/getters-setters") { project, request ->
            CodeGenerationService.getInstance(project).generateGettersSetters(request)
        }

        postWithBody<GenerateConstructorRequest>("/api/v1/codegen/constructor") { project, request ->
            CodeGenerationService.getInstance(project).generateConstructor(request)
        }

        postWithBody<GenerateMethodRequest>("/api/v1/codegen/tostring") { project, request ->
            CodeGenerationService.getInstance(project).generateToString(request)
        }

        postWithBody<GenerateMethodRequest>("/api/v1/codegen/equals") { project, request ->
            CodeGenerationService.getInstance(project).generateEquals(request)
        }

        postWithBody<GenerateMethodRequest>("/api/v1/codegen/hashcode") { project, request ->
            CodeGenerationService.getInstance(project).generateHashCode(request)
        }

        postWithBody<OverrideMethodRequest>("/api/v1/codegen/override-method") { project, request ->
            CodeGenerationService.getInstance(project).overrideMethod(request)
        }
    }

    // ==================== 搜索 API ====================

    private fun RouteDsl.registerSearchRoutes() {
        postWithBody<GlobalSearchRequest>("/api/v1/search/global") { project, request ->
            SearchService.getInstance(project).globalSearch(request)
        }

        postWithBody<TextSearchRequest>("/api/v1/search/text") { project, request ->
            SearchService.getInstance(project).textSearch(request)
        }

        postWithBody<StructuralSearchRequest>("/api/v1/search/structural") { project, request ->
            SearchService.getInstance(project).structuralSearch(request)
        }
    }

    // ==================== 依赖分析 API ====================

    private fun RouteDsl.registerDependencyRoutes() {
        postWithBody<AnalyzeDependenciesRequest>("/api/v1/analysis/dependencies") { project, request ->
            DependencyService.getInstance(project).analyzeDependencies(request)
        }

        postWithBody<DetectCyclesRequest>("/api/v1/analysis/dependency-cycles") { project, request ->
            DependencyService.getInstance(project).detectCycles(request)
        }
    }

    // ==================== VCS API ====================

    private fun RouteDsl.registerVcsRoutes() {
        getWithProject("/api/v1/vcs/status") { project ->
            VcsService.getInstance(project).getStatus()
        }

        postWithBody<VcsHistoryRequest>("/api/v1/vcs/history") { project, request ->
            VcsService.getInstance(project).getHistory(request)
        }

        postWithBody<VcsDiffRequest>("/api/v1/vcs/diff") { project, request ->
            VcsService.getInstance(project).getDiff(request)
        }

        postWithBody<VcsBlameRequest>("/api/v1/vcs/blame") { project, request ->
            VcsService.getInstance(project).blame(request)
        }
    }

    // ==================== 项目管理 API ====================

    private fun RouteDsl.registerProjectRoutes() {
        getWithProject("/api/v1/project/structure") { project ->
            ProjectService.getInstance(project).getStructure()
        }

        getWithProject("/api/v1/project/modules") { project ->
            ProjectService.getInstance(project).getModules()
        }

        getWithProject("/api/v1/project/libraries") { project ->
            ProjectService.getInstance(project).getLibraries()
        }

        getWithProject("/api/v1/project/build-config") { project ->
            ProjectService.getInstance(project).getBuildConfig()
        }
    }

    // ==================== 工具 API ====================

    private fun RouteDsl.registerToolsRoutes() {
        postWithBody<FormatCodeRequest>("/api/v1/tools/format") { project, request ->
            ToolsService.getInstance(project).formatCode(request)
        }

        postWithBody<OptimizeImportsRequest>("/api/v1/tools/optimize-imports") { project, request ->
            ToolsService.getInstance(project).optimizeImports(request)
        }

        postWithBody<QuickFixRequest>("/api/v1/tools/quick-fix") { project, request ->
            ToolsService.getInstance(project).applyQuickFix(request)
        }

        postWithBody<IntentionRequest>("/api/v1/tools/intention") { project, request ->
            ToolsService.getInstance(project).applyIntention(request)
        }
    }

    // ==================== 辅助方法 ====================

    private fun getIdeaVersion(): String {
        return try {
            ApplicationInfo.getInstance().fullVersion
        } catch (e: Exception) {
            "Unknown"
        }
    }

    private fun isIndexReady(): Boolean {
        return try {
            val project = ProjectManager.getInstance().openProjects.firstOrNull() ?: return false
            !DumbService.isDumb(project)
        } catch (e: Exception) {
            false
        }
    }

    private fun getProjectModules(project: Project): List<String> {
        return try {
            ModuleManager.getInstance(project).modules.map { it.name }
        } catch (e: Exception) {
            emptyList()
        }
    }
}
