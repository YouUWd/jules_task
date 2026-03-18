# 企业信息化平台开发规范文档

## 1. 概述
本文档旨在统一企业信息化平台的开发标准与规范。为保证代码质量、提高团队协作效率、降低后期维护成本，基于 **JDK 17**、**Spring Boot 3.4** 及相关中间件技术栈，特制定以下开发规范。

## 2. 核心技术栈与版本要求
* **后端语言**: Java 17 (LTS版本，强制使用)
* **核心框架**: Spring Boot 3.4.x
* **数据访问**: MyBatis-Plus 3.5+ 或 Spring Data JPA
* **API 文档**: Springdoc OpenAPI (Swagger 3)
* **代码生成与简化**: Lombok
* **构建工具**: Maven 3.8+ / Gradle 8.x
* **IDE**: IntelliJ IDEA 2023+ (建议统一格式化插件和检查规则)

## 3. 工程结构规范

采用分层清晰的单体/微服务多模块架构 (Multi-module Architecture)。以下为典型结构参考：

```text
enterprise-platform
├── platform-common     # 公共核心模块 (工具类, 全局异常, 基础常量, 统一响应等)
├── platform-api        # 接口定义模块 (Feign Client, DTO, 共享枚举)
├── platform-core       # 核心业务逻辑模块 (Domain模型, Service接口及实现)
├── platform-dao        # 数据访问模块 (Entity, Mapper, XML)
├── platform-web        # Web展示/入口模块 (Controller, 统一拦截器, 权限配置, 启动类)
└── pom.xml             # 父级POM文件 (统一依赖管理和版本控制)
```

## 4. 命名规范

遵循阿里巴巴Java开发手册及相关业界通用标准。

### 4.1 包名与类名
* **包名 (Package)**: 统一全小写，单数形式（例如 `com.company.project.module.service`）。
* **类名 (Class/Interface)**: 采用 UpperCamelCase（大驼峰命名），例如 `UserService`、`OrderInfo`。
* **常量类/枚举名 (Constant/Enum)**: 全大写，单词间用下划线 `_` 分隔，例如 `OrderStatusEnum`。
* **抽象类 (Abstract Class)**: 以 `Abstract` 或 `Base` 开头。
* **异常类 (Exception)**: 必须以 `Exception` 结尾。

### 4.2 变量与方法名
* **变量/方法名 (Variable/Method)**: 采用 lowerCamelCase（小驼峰命名），例如 `getUserById()`、`totalAmount`。
* **布尔变量 (Boolean)**: 避免使用 `is` 前缀作为变量名（防止 RPC 框架或 Lombok 序列化问题），如 `deleted` 而非 `isDeleted`。

## 5. 编码规范与新特性利用

### 5.1 JDK 17 新特性建议
强烈建议并鼓励使用 JDK 17 的新特性来提升代码的可读性和性能：
1. **Records (记录类)**:
   代替 Lombok 的 `@Data` 生成简单的不可变 DTO/VO。
   ```java
   public record UserDto(Long id, String username, String email) {}
   ```
2. **Text Blocks (文本块)**:
   处理多行字符串（如 SQL 拼接、JSON 字符串等），避免繁琐的转义。
   ```java
   String query = """
       SELECT id, name
       FROM users
       WHERE status = 'ACTIVE'
       """;
   ```
3. **Pattern Matching for `instanceof` (模式匹配)**:
   简化类型判断与强制转换。
   ```java
   if (obj instanceof String s) {
       System.out.println(s.length()); // 直接使用 s
   }
   ```
4. **Switch Expressions (增强的 Switch)**:
   使代码更紧凑，避免 `break` 遗漏，可直接返回值。
   ```java
   String result = switch (day) {
       case MONDAY, FRIDAY, SUNDAY -> "Weekend or Edge";
       case TUESDAY                -> "Tuesday";
       default                     -> "Midweek";
   };
   ```

### 5.2 异常处理规范
* 统一定义并抛出自定义业务异常（如 `BusinessException`）。
* 禁止在循环内部进行 `try-catch`。
* 使用全局异常处理器（`@RestControllerAdvice` + `@ExceptionHandler`）统一包装异常响应。
* 明确区分受检异常 (Checked Exception) 和非受检异常 (Unchecked Exception/RuntimeException)。业务异常应继承 `RuntimeException`。

### 5.3 接口设计与 RESTful 规范
* 遵循 RESTful 架构风格，使用名词复数表示资源（如 `/api/v1/users`）。
* 正确使用 HTTP 动词：
  * `GET`: 查询资源
  * `POST`: 创建资源
  * `PUT`: 全量更新资源
  * `PATCH`: 局部更新资源
  * `DELETE`: 删除资源
* 版本控制：在 URL 或 Header 中携带 API 版本号（如 `/api/v1/...`）。
* 统一响应格式：前端与后端的交互必须使用统一的 JSON 结构（如包含 `code`, `message`, `data`）。

## 6. 数据库开发规范 (MySQL 8.0+)

* **表与字段命名**: 统一使用小写字母和下划线 `_`（例如 `user_info`）。
* **主键设计**: 推荐使用雪花算法生成的分布式 ID (Long) 或无意义的自增 ID。
* **审计字段**: 每张表必须包含 `create_time`、`update_time`（自动更新时间戳）、`create_by`、`update_by`。
* **软删除**: 建议使用 `is_deleted`（0-正常，1-删除）实现逻辑删除，避免物理删除。
* **索引规范**:
  * 索引名以 `idx_` 开头（普通索引），唯一索引以 `uk_` 开头。
  * 避免在频繁更新的列上建立过多索引。
  * 遵循最左前缀原则。
* **禁止使用外键**: 业务逻辑层保证数据一致性，避免数据库层面建立外键约束，提升扩展性。

## 7. 日志与监控规范

* **日志框架**: 统一使用 SLF4J + Logback。
* **日志级别**:
  * `ERROR`: 系统发生错误，需要立即关注并修复。
  * `WARN`: 潜在问题或异常流程，但不影响核心功能。
  * `INFO`: 关键业务流程节点、启动信息、请求参数与响应（视情况而定）。
  * `DEBUG`: 开发调试信息（生产环境关闭）。
* **日志追踪**: 在日志格式中加入 Trace ID（通过 MDC 机制结合分布式追踪工具如 SkyWalking），以便全链路追踪问题。
* **禁止使用 `System.out.println()` 或 `e.printStackTrace()` 输出日志**。

## 8. 代码审查 (Code Review) 与提交规范

* **Git 提交信息**: 必须清晰描述变更意图。推荐遵循 Conventional Commits 规范（如 `feat: add user login`, `fix: resolve NPE in order creation`）。
* **提交前检查**: 开发人员提交代码前，必须确保本地能够编译通过，且所有单元测试运行通过。
* **MR/PR 流程**: 所有合并至主分支（如 `main` 或 `develop`）的代码，必须经过至少一名其他开发人员的 Code Review (代码审查)。
