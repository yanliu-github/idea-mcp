# IntelliJ IDEA MCP 服务器 - API 端点清单

## 文档信息

- **文档版本**: v1.0
- **创建日期**: 2025-11-13
- **文档状态**: API 定义
- **项目名称**: IDEA MCP Server

---

## 1. API 概述

### 1.1 基础信息

- **Base URL**: `http://localhost:58888/api/v1`
- **协议**: HTTP/1.1
- **数据格式**: JSON
- **认证**: Bearer Token (可选)
- **字符编码**: UTF-8

### 1.2 通用请求头

```http
Content-Type: application/json
Authorization: Bearer <token>  # 可选
X-Request-ID: <uuid>           # 推荐
Accept: application/json
```

### 1.3 通用响应格式

**成功响应** (2xx):
```json
{
  "success": true,
  "data": { ... },
  "requestId": "uuid",
  "timestamp": 1234567890
}
```

**错误响应** (4xx/5xx):
```json
{
  "success": false,
  "error": {
    "code": "ERROR_CODE",
    "message": "错误描述",
    "details": { ... }
  },
  "requestId": "uuid",
  "timestamp": 1234567890
}
```

---

## 2. 系统管理 API

### 2.1 健康检查

**端点**: `GET /health`

**描述**: 检查服务器健康状态

**请求参数**: 无

**响应示例**:
```json
{
  "success": true,
  "data": {
    "status": "ok",
    "version": "1.0.0",
    "ideaVersion": "2024.1.1",
    "indexReady": true,
    "projectOpen": true,
    "projectName": "MyProject",
    "uptime": 3600000
  }
}
```

**状态码**:
- `200 OK` - 服务正常
- `503 Service Unavailable` - 服务不可用

---

### 2.2 获取项目信息

**端点**: `GET /project/info`

**描述**: 获取当前打开项目的基本信息

**请求参数**: 无

**响应示例**:
```json
{
  "success": true,
  "data": {
    "name": "MyProject",
    "path": "/path/to/project",
    "modules": [
      {
        "name": "module1",
        "path": "/path/to/module1",
        "type": "JAVA_MODULE"
      }
    ],
    "sdk": {
      "name": "JDK 17",
      "version": "17.0.8"
    }
  }
}
```

---

## 3. 重构 API

### 3.1 重命名符号

**端点**: `POST /refactor/rename`

**描述**: 重命名代码中的符号(变量、方法、类等)

**请求体**:
```json
{
  "filePath": "/src/main/java/com/example/User.java",
  "offset": 1234,
  "newName": "Customer",
  "searchInComments": true,
  "searchInStrings": false,
  "preview": false
}
```

**请求参数说明**:
| 参数 | 类型 | 必需 | 说明 |
|------|------|------|------|
| filePath | string | 是 | 文件路径(项目相对路径或绝对路径) |
| offset | number | 是 | 符号在文件中的偏移量(从0开始) |
| newName | string | 是 | 新名称 |
| searchInComments | boolean | 否 | 是否在注释中搜索(默认false) |
| searchInStrings | boolean | 否 | 是否在字符串中搜索(默认false) |
| preview | boolean | 否 | 是否仅预览不执行(默认false) |

**响应示例**:
```json
{
  "success": true,
  "data": {
    "affectedFiles": [
      {
        "filePath": "/src/main/java/com/example/User.java",
        "changes": [
          {
            "startOffset": 1234,
            "endOffset": 1238,
            "oldText": "User",
            "newText": "Customer"
          }
        ]
      },
      {
        "filePath": "/src/main/java/com/example/Service.java",
        "changes": [...]
      }
    ],
    "usageCount": 15,
    "conflicts": []
  }
}
```

**状态码**:
- `200 OK` - 重命名成功
- `400 Bad Request` - 参数错误
- `404 Not Found` - 文件或元素未找到
- `409 Conflict` - 命名冲突
- `500 Internal Server Error` - 重构失败

---

### 3.2 提取方法

**端点**: `POST /refactor/extract-method`

**描述**: 将选中的代码片段提取为独立方法

**请求体**:
```json
{
  "filePath": "/src/main/java/com/example/Service.java",
  "startOffset": 1000,
  "endOffset": 1500,
  "methodName": "calculateTotal",
  "visibility": "private",
  "static": false,
  "preview": false
}
```

**请求参数说明**:
| 参数 | 类型 | 必需 | 说明 |
|------|------|------|------|
| filePath | string | 是 | 文件路径 |
| startOffset | number | 是 | 代码片段起始偏移量 |
| endOffset | number | 是 | 代码片段结束偏移量 |
| methodName | string | 是 | 新方法名称 |
| visibility | string | 否 | 可见性(private/public/protected,默认private) |
| static | boolean | 否 | 是否静态方法(默认false) |
| preview | boolean | 否 | 是否仅预览(默认false) |

**响应示例**:
```json
{
  "success": true,
  "data": {
    "extractedMethod": {
      "signature": "private int calculateTotal()",
      "body": "int total = 0;\n// ...\nreturn total;",
      "insertionPoint": 2000
    },
    "callSite": {
      "offset": 1000,
      "code": "int result = calculateTotal();"
    },
    "changes": [...]
  }
}
```

---

### 3.3 提取变量

**端点**: `POST /refactor/extract-variable`

**描述**: 将表达式提取为变量

**请求体**:
```json
{
  "filePath": "/src/main/java/com/example/Calculator.java",
  "startOffset": 500,
  "endOffset": 550,
  "variableName": "result",
  "constant": false,
  "replaceAll": true,
  "preview": false
}
```

**响应示例**: (类似重命名响应)

---

### 3.4 内联

**端点**: `POST /refactor/inline`

**描述**: 将变量或方法内联到使用位置

**请求体**:
```json
{
  "filePath": "/src/main/java/com/example/Service.java",
  "offset": 1234,
  "inlineAll": true,
  "preview": false
}
```

---

### 3.5 改变方法签名

**端点**: `POST /refactor/change-signature`

**描述**: 修改方法的参数、返回值、可见性

**请求体**:
```json
{
  "filePath": "/src/main/java/com/example/Service.java",
  "offset": 1234,
  "parameters": [
    {
      "name": "userId",
      "type": "String",
      "defaultValue": null
    },
    {
      "name": "active",
      "type": "boolean",
      "defaultValue": "true"
    }
  ],
  "returnType": "User",
  "visibility": "public",
  "preview": false
}
```

---

### 3.6 安全删除

**端点**: `POST /refactor/safe-delete`

**描述**: 检查并安全删除未使用的元素

**请求体**:
```json
{
  "filePath": "/src/main/java/com/example/OldService.java",
  "offset": 1234,
  "searchInComments": true,
  "checkUsages": true
}
```

**响应示例**:
```json
{
  "success": true,
  "data": {
    "canDelete": false,
    "usages": [
      {
        "filePath": "/src/main/java/com/example/Main.java",
        "line": 42,
        "column": 15,
        "context": "OldService service = new OldService();"
      }
    ],
    "usageCount": 5
  }
}
```

---

### 3.7 移动类/方法

**端点**: `POST /refactor/move`

**描述**: 移动类到不同包或方法到不同类

**请求体**:
```json
{
  "filePath": "/src/main/java/com/example/User.java",
  "offset": 1234,
  "targetPackage": "com.example.model",
  "preview": false
}
```

---

## 4. 导航 API

### 4.1 查找用途

**端点**: `POST /navigation/find-usages`

**描述**: 查找符号的所有使用位置

**请求体**:
```json
{
  "filePath": "/src/main/java/com/example/User.java",
  "offset": 1234,
  "scope": "project",
  "includeTests": true
}
```

**请求参数说明**:
| 参数 | 类型 | 必需 | 说明 |
|------|------|------|------|
| filePath | string | 是 | 文件路径 |
| offset | number | 是 | 符号偏移量 |
| scope | string | 否 | 搜索范围(project/module/directory,默认project) |
| includeTests | boolean | 否 | 是否包含测试代码(默认true) |

**响应示例**:
```json
{
  "success": true,
  "data": {
    "usages": [
      {
        "filePath": "/src/main/java/com/example/Service.java",
        "line": 42,
        "column": 15,
        "offset": 1234,
        "type": "METHOD_CALL",
        "context": "user.getName()",
        "snippet": "String name = user.getName();"
      }
    ],
    "totalCount": 25,
    "groupedByType": {
      "METHOD_CALL": 15,
      "FIELD_READ": 8,
      "FIELD_WRITE": 2
    }
  }
}
```

---

### 4.2 跳转到定义

**端点**: `POST /navigation/goto-definition`

**描述**: 跳转到符号的定义位置

**请求体**:
```json
{
  "filePath": "/src/main/java/com/example/Service.java",
  "offset": 1234
}
```

**响应示例**:
```json
{
  "success": true,
  "data": {
    "definitionFile": "/src/main/java/com/example/User.java",
    "line": 15,
    "column": 13,
    "offset": 567,
    "elementType": "CLASS",
    "signature": "public class User"
  }
}
```

---

### 4.3 查找实现

**端点**: `POST /navigation/find-implementations`

**描述**: 查找接口或抽象方法的所有实现

**请求体**:
```json
{
  "filePath": "/src/main/java/com/example/UserService.java",
  "offset": 1234
}
```

**响应示例**:
```json
{
  "success": true,
  "data": {
    "implementations": [
      {
        "filePath": "/src/main/java/com/example/UserServiceImpl.java",
        "line": 10,
        "column": 13,
        "className": "UserServiceImpl"
      }
    ],
    "totalCount": 3
  }
}
```

---

### 4.4 类型层次

**端点**: `POST /navigation/type-hierarchy`

**描述**: 显示类的继承关系树

**请求体**:
```json
{
  "filePath": "/src/main/java/com/example/User.java",
  "offset": 1234,
  "direction": "both"
}
```

**请求参数说明**:
| 参数 | 类型 | 必需 | 说明 |
|------|------|------|------|
| direction | string | 否 | 方向(supertypes/subtypes/both,默认both) |

**响应示例**:
```json
{
  "success": true,
  "data": {
    "hierarchy": {
      "className": "User",
      "supertypes": [
        {
          "className": "BaseEntity",
          "filePath": "/src/main/java/com/example/BaseEntity.java"
        }
      ],
      "subtypes": [
        {
          "className": "AdminUser",
          "filePath": "/src/main/java/com/example/AdminUser.java"
        }
      ]
    }
  }
}
```

---

### 4.5 调用层次

**端点**: `POST /navigation/call-hierarchy`

**描述**: 显示方法的调用关系

**请求体**:
```json
{
  "filePath": "/src/main/java/com/example/Service.java",
  "offset": 1234,
  "direction": "callers"
}
```

**请求参数说明**:
| 参数 | 类型 | 必需 | 说明 |
|------|------|------|------|
| direction | string | 否 | 方向(callers/callees/both,默认callers) |

---

## 5. 代码分析 API

### 5.1 运行代码检查

**端点**: `POST /analysis/inspections`

**描述**: 运行 IDEA 的代码检查规则

**请求体**:
```json
{
  "scope": "file",
  "filePath": "/src/main/java/com/example/User.java",
  "severity": ["ERROR", "WARNING"]
}
```

**请求参数说明**:
| 参数 | 类型 | 必需 | 说明 |
|------|------|------|------|
| scope | string | 是 | 检查范围(file/directory/project) |
| filePath | string | 条件 | 文件路径(scope=file时必需) |
| directoryPath | string | 条件 | 目录路径(scope=directory时必需) |
| severity | array | 否 | 严重程度过滤(默认全部) |

**响应示例**:
```json
{
  "success": true,
  "data": {
    "problems": [
      {
        "filePath": "/src/main/java/com/example/User.java",
        "line": 42,
        "column": 15,
        "severity": "WARNING",
        "message": "变量 'name' 从未使用",
        "inspectionName": "UnusedVariable",
        "quickFixes": [
          {
            "name": "删除变量",
            "description": "安全删除未使用的变量"
          },
          {
            "name": "重命名为 '_name'",
            "description": "标记为未使用"
          }
        ]
      }
    ],
    "totalCount": 15,
    "groupedBySeverity": {
      "ERROR": 2,
      "WARNING": 10,
      "WEAK_WARNING": 3
    }
  }
}
```

---

### 5.2 获取错误和警告

**端点**: `POST /analysis/errors`

**描述**: 获取文件的编译错误和警告

**请求体**:
```json
{
  "filePath": "/src/main/java/com/example/User.java"
}
```

**响应示例**:
```json
{
  "success": true,
  "data": {
    "errors": [
      {
        "line": 10,
        "column": 5,
        "message": "找不到符号: 类 NonExistentClass",
        "severity": "ERROR"
      }
    ],
    "warnings": [
      {
        "line": 15,
        "column": 10,
        "message": "方法返回值未使用",
        "severity": "WARNING"
      }
    ]
  }
}
```

---

### 5.3 依赖分析

**端点**: `POST /analysis/dependencies`

**描述**: 分析模块/包之间的依赖关系

**请求体**:
```json
{
  "scope": "module",
  "moduleName": "app",
  "includeTests": false
}
```

**响应示例**:
```json
{
  "success": true,
  "data": {
    "dependencies": [
      {
        "from": "com.example.service",
        "to": "com.example.model",
        "type": "DIRECT"
      }
    ],
    "circularDependencies": [],
    "unusedDependencies": []
  }
}
```

---

## 6. 调试 API

### 6.1 设置断点

**端点**: `POST /debug/breakpoint`

**描述**: 设置或更新断点

**请求体**:
```json
{
  "filePath": "/src/main/java/com/example/Service.java",
  "line": 42,
  "condition": "userId > 100",
  "logMessage": "User ID: {userId}",
  "suspend": true
}
```

**请求参数说明**:
| 参数 | 类型 | 必需 | 说明 |
|------|------|------|------|
| filePath | string | 是 | 文件路径 |
| line | number | 是 | 行号(从1开始) |
| condition | string | 否 | 条件表达式 |
| logMessage | string | 否 | 日志消息(日志断点) |
| suspend | boolean | 否 | 是否挂起线程(默认true) |

**响应示例**:
```json
{
  "success": true,
  "data": {
    "breakpointId": "bp-1234",
    "enabled": true,
    "verified": true
  }
}
```

---

### 6.2 列出所有断点

**端点**: `GET /debug/breakpoints`

**描述**: 获取所有断点列表

**响应示例**:
```json
{
  "success": true,
  "data": {
    "breakpoints": [
      {
        "id": "bp-1234",
        "filePath": "/src/main/java/com/example/Service.java",
        "line": 42,
        "enabled": true,
        "condition": "userId > 100"
      }
    ]
  }
}
```

---

### 6.3 删除断点

**端点**: `DELETE /debug/breakpoint/{breakpointId}`

**描述**: 删除指定断点

**响应示例**:
```json
{
  "success": true,
  "data": {
    "deleted": true
  }
}
```

---

### 6.4 启动调试

**端点**: `POST /debug/start`

**描述**: 启动调试会话

**请求体**:
```json
{
  "configurationName": "Main",
  "mainClass": "com.example.Main",
  "programArgs": "--port 8080",
  "vmArgs": "-Xmx512m"
}
```

**响应示例**:
```json
{
  "success": true,
  "data": {
    "sessionId": "debug-session-1234",
    "status": "RUNNING"
  }
}
```

---

### 6.5 调试控制

**端点**: `POST /debug/control`

**描述**: 控制调试执行流程

**请求体**:
```json
{
  "sessionId": "debug-session-1234",
  "action": "step_over"
}
```

**请求参数说明**:
| 参数 | 类型 | 必需 | 说明 |
|------|------|------|------|
| sessionId | string | 是 | 调试会话ID |
| action | string | 是 | 操作(continue/pause/step_over/step_into/step_out/stop) |

**响应示例**:
```json
{
  "success": true,
  "data": {
    "status": "PAUSED",
    "currentLocation": {
      "filePath": "/src/main/java/com/example/Service.java",
      "line": 43
    }
  }
}
```

---

### 6.6 计算表达式

**端点**: `POST /debug/evaluate`

**描述**: 在调试时计算表达式

**请求体**:
```json
{
  "sessionId": "debug-session-1234",
  "expression": "user.getName()",
  "frameIndex": 0
}
```

**响应示例**:
```json
{
  "success": true,
  "data": {
    "result": "John Doe",
    "type": "String"
  }
}
```

---

### 6.7 获取变量

**端点**: `POST /debug/variables`

**描述**: 获取当前作用域的变量

**请求体**:
```json
{
  "sessionId": "debug-session-1234",
  "frameIndex": 0,
  "scope": "local"
}
```

**请求参数说明**:
| 参数 | 类型 | 必需 | 说明 |
|------|------|------|------|
| scope | string | 否 | 作用域(local/instance/static,默认local) |

**响应示例**:
```json
{
  "success": true,
  "data": {
    "variables": [
      {
        "name": "userId",
        "value": "123",
        "type": "int"
      },
      {
        "name": "userName",
        "value": "John",
        "type": "String"
      }
    ]
  }
}
```

---

### 6.8 修改变量

**端点**: `POST /debug/set-variable`

**描述**: 修改变量值

**请求体**:
```json
{
  "sessionId": "debug-session-1234",
  "variableName": "userId",
  "newValue": "456",
  "frameIndex": 0
}
```

---

### 6.9 获取调用栈

**端点**: `POST /debug/stack-trace`

**描述**: 获取完整调用栈

**请求体**:
```json
{
  "sessionId": "debug-session-1234"
}
```

**响应示例**:
```json
{
  "success": true,
  "data": {
    "frames": [
      {
        "index": 0,
        "method": "calculateTotal",
        "className": "com.example.Service",
        "filePath": "/src/main/java/com/example/Service.java",
        "line": 42
      },
      {
        "index": 1,
        "method": "processOrder",
        "className": "com.example.OrderController",
        "filePath": "/src/main/java/com/example/OrderController.java",
        "line": 100
      }
    ]
  }
}
```

---

## 7. 代码生成 API

### 7.1 生成 Getter/Setter

**端点**: `POST /generate/getters-setters`

**描述**: 为类字段生成访问器方法

**请求体**:
```json
{
  "filePath": "/src/main/java/com/example/User.java",
  "classOffset": 100,
  "fields": ["name", "email", "age"],
  "generateGetter": true,
  "generateSetter": true
}
```

**响应示例**:
```json
{
  "success": true,
  "data": {
    "generatedMethods": [
      {
        "methodName": "getName",
        "code": "public String getName() {\n    return name;\n}"
      },
      {
        "methodName": "setName",
        "code": "public void setName(String name) {\n    this.name = name;\n}"
      }
    ]
  }
}
```

---

### 7.2 生成构造函数

**端点**: `POST /generate/constructor`

**描述**: 生成参数化构造函数

**请求体**:
```json
{
  "filePath": "/src/main/java/com/example/User.java",
  "classOffset": 100,
  "fields": ["name", "email"],
  "visibility": "public"
}
```

---

### 7.3 生成 equals/hashCode/toString

**端点**: `POST /generate/utility-methods`

**描述**: 生成常用方法

**请求体**:
```json
{
  "filePath": "/src/main/java/com/example/User.java",
  "classOffset": 100,
  "methods": ["equals", "hashCode", "toString"],
  "fields": ["id", "name", "email"]
}
```

---

### 7.4 创建测试

**端点**: `POST /generate/test`

**描述**: 为类创建测试文件

**请求体**:
```json
{
  "filePath": "/src/main/java/com/example/UserService.java",
  "classOffset": 100,
  "framework": "JUnit5",
  "methods": ["createUser", "updateUser"],
  "generateSetup": true,
  "generateTeardown": false
}
```

**请求参数说明**:
| 参数 | 类型 | 必需 | 说明 |
|------|------|------|------|
| framework | string | 否 | 测试框架(JUnit4/JUnit5/TestNG,默认JUnit5) |

**响应示例**:
```json
{
  "success": true,
  "data": {
    "testFilePath": "/src/test/java/com/example/UserServiceTest.java",
    "testContent": "package com.example;\n\nimport org.junit.jupiter.api.Test;\n..."
  }
}
```

---

## 8. 搜索 API

### 8.1 全局搜索

**端点**: `POST /search/everywhere`

**描述**: 全局搜索类、文件、符号

**请求体**:
```json
{
  "query": "UserService",
  "type": "all",
  "includeLibraries": false,
  "limit": 50
}
```

**请求参数说明**:
| 参数 | 类型 | 必需 | 说明 |
|------|------|------|------|
| type | string | 否 | 搜索类型(all/files/classes/symbols,默认all) |
| limit | number | 否 | 结果数量限制(默认50) |

**响应示例**:
```json
{
  "success": true,
  "data": {
    "results": [
      {
        "name": "UserService",
        "type": "CLASS",
        "filePath": "/src/main/java/com/example/UserService.java",
        "line": 10,
        "matchScore": 100
      }
    ],
    "totalCount": 5
  }
}
```

---

### 8.2 文本搜索

**端点**: `POST /search/text`

**描述**: 在文件中搜索文本

**请求体**:
```json
{
  "query": "TODO",
  "scope": "project",
  "caseSensitive": false,
  "regex": false,
  "fileType": "*.java"
}
```

**响应示例**:
```json
{
  "success": true,
  "data": {
    "matches": [
      {
        "filePath": "/src/main/java/com/example/Service.java",
        "line": 42,
        "column": 5,
        "matchText": "// TODO: implement this",
        "context": "public void doSomething() {\n    // TODO: implement this\n}"
      }
    ],
    "totalCount": 15
  }
}
```

---

### 8.3 结构化搜索

**端点**: `POST /search/structural`

**描述**: 基于代码结构搜索

**请求体**:
```json
{
  "pattern": "if ($condition$) { return $value$; }",
  "scope": "project",
  "caseSensitive": false
}
```

**响应示例**:
```json
{
  "success": true,
  "data": {
    "matches": [
      {
        "filePath": "/src/main/java/com/example/Service.java",
        "line": 42,
        "matchedCode": "if (user == null) { return null; }",
        "variables": {
          "condition": "user == null",
          "value": "null"
        }
      }
    ]
  }
}
```

---

## 9. 项目管理 API

### 9.1 获取项目结构

**端点**: `GET /project/structure`

**描述**: 获取项目的目录树结构

**请求参数**:
- `includeLibraries` (boolean, 可选) - 是否包含库
- `maxDepth` (number, 可选) - 最大深度

**响应示例**:
```json
{
  "success": true,
  "data": {
    "root": {
      "name": "MyProject",
      "type": "PROJECT",
      "children": [
        {
          "name": "src",
          "type": "DIRECTORY",
          "children": [...]
        }
      ]
    }
  }
}
```

---

### 9.2 构建项目

**端点**: `POST /project/build`

**描述**: 编译项目

**请求体**:
```json
{
  "clean": false,
  "module": "app"
}
```

**响应示例**:
```json
{
  "success": true,
  "data": {
    "buildStatus": "SUCCESS",
    "buildTime": 5000,
    "errors": [],
    "warnings": []
  }
}
```

---

### 9.3 运行测试

**端点**: `POST /project/test`

**描述**: 运行测试

**请求体**:
```json
{
  "scope": "class",
  "className": "com.example.UserServiceTest",
  "collectCoverage": false
}
```

**请求参数说明**:
| 参数 | 类型 | 必需 | 说明 |
|------|------|------|------|
| scope | string | 是 | 测试范围(all/package/class/method) |
| className | string | 条件 | 类名(scope=class时必需) |
| methodName | string | 条件 | 方法名(scope=method时必需) |
| collectCoverage | boolean | 否 | 是否收集覆盖率(默认false) |

**响应示例**:
```json
{
  "success": true,
  "data": {
    "totalTests": 10,
    "passed": 8,
    "failed": 2,
    "skipped": 0,
    "failures": [
      {
        "testName": "testCreateUser",
        "message": "Expected <John> but was <Jane>",
        "stackTrace": "..."
      }
    ],
    "coverage": {
      "lineCoverage": 85.5,
      "branchCoverage": 78.2
    }
  }
}
```

---

## 10. 代码格式化 API

### 10.1 格式化代码

**端点**: `POST /tools/format`

**描述**: 按照代码风格格式化

**请求体**:
```json
{
  "scope": "file",
  "filePath": "/src/main/java/com/example/User.java",
  "optimizeImports": true,
  "rearrangeCode": false
}
```

**响应示例**:
```json
{
  "success": true,
  "data": {
    "formatted": true,
    "changes": 5
  }
}
```

---

### 10.2 优化导入

**端点**: `POST /tools/optimize-imports`

**描述**: 移除未使用的导入并排序

**请求体**:
```json
{
  "scope": "file",
  "filePath": "/src/main/java/com/example/User.java"
}
```

---

### 10.3 应用快速修复

**端点**: `POST /tools/quick-fix`

**描述**: 应用 IDEA 的快速修复建议

**请求体**:
```json
{
  "filePath": "/src/main/java/com/example/User.java",
  "offset": 1234,
  "fixName": "Remove unused variable"
}
```

**响应示例**:
```json
{
  "success": true,
  "data": {
    "applied": true,
    "changes": [...]
  }
}
```

---

## 11. 错误码定义

### 11.1 客户端错误 (4xx)

| 错误码 | HTTP状态 | 说明 |
|-------|---------|------|
| INVALID_PARAMETERS | 400 | 请求参数无效 |
| MISSING_REQUIRED_FIELD | 400 | 缺少必需字段 |
| INVALID_FILE_PATH | 400 | 文件路径无效 |
| INVALID_OFFSET | 400 | 偏移量无效 |
| UNAUTHORIZED | 401 | 未授权访问 |
| INVALID_TOKEN | 401 | Token无效 |
| FORBIDDEN | 403 | 禁止访问 |
| FILE_NOT_FOUND | 404 | 文件未找到 |
| ELEMENT_NOT_FOUND | 404 | 元素未找到 |
| PROJECT_NOT_FOUND | 404 | 项目未找到 |
| NAMING_CONFLICT | 409 | 命名冲突 |
| ELEMENT_IN_USE | 409 | 元素正在使用中 |
| RATE_LIMIT_EXCEEDED | 429 | 请求频率超限 |

### 11.2 服务端错误 (5xx)

| 错误码 | HTTP状态 | 说明 |
|-------|---------|------|
| INTERNAL_ERROR | 500 | 内部错误 |
| REFACTORING_FAILED | 500 | 重构失败 |
| OPERATION_CANCELLED | 500 | 操作被取消 |
| PSI_OPERATION_FAILED | 500 | PSI操作失败 |
| INDEX_NOT_READY | 503 | 索引未就绪 |
| SERVICE_UNAVAILABLE | 503 | 服务不可用 |
| IDEA_NOT_READY | 503 | IDEA未就绪 |
| TIMEOUT | 504 | 操作超时 |

---

## 12. 附录

### 12.1 API 测试示例 (curl)

**健康检查**:
```bash
curl http://localhost:58888/api/v1/health
```

**重命名**:
```bash
curl -X POST http://localhost:58888/api/v1/refactor/rename \
  -H "Content-Type: application/json" \
  -d '{
    "filePath": "/src/User.java",
    "offset": 1234,
    "newName": "Customer"
  }'
```

**查找用途**:
```bash
curl -X POST http://localhost:58888/api/v1/navigation/find-usages \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer your-token" \
  -d '{
    "filePath": "/src/User.java",
    "offset": 1234,
    "scope": "project"
  }'
```

### 12.2 Postman 集合

可导入 Postman 使用预定义的请求集合 (后续提供)。

---

**文档维护**: API 变更时需及时更新此文档。
