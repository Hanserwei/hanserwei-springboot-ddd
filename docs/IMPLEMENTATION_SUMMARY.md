# 工程能力概览

这个脚手架以 user、order 展示 DDD 四层结构与常用基础设施。示例可整体替换，按[新项目起步指南](START_NEW_PROJECT.md)接入自己的业务。

## 业务示例

| 示例 | 展示内容 | 主要入口 |
|---|---|---|
| user | 注册、资料修改、名称和邮箱唯一性 | `User`、`UserService`、`UserRepositoryImpl` |
| order | 创建、支付、取消、完成、并发更新 | `Order`、`OrderService`、`OrderRepositoryImpl` |
| 用户订单列表 | 一次批量查询用户简介 | `UserInfoQueryClient`、`UserInfoQueryClientImpl` |

聚合通过行为改变状态。领域仓储接口与持久化实现分离，DO 和 Converter 位于基础设施层；用户简介只投影必要字段，全部业务数据库查询使用 MyBatis-Plus。

## 数据与事务

MySQL、PostgreSQL 各有数据源、Mapper 工厂和本地事务管理器。分页和乐观锁由 MyBatis-Plus 插件处理。数据库脚本与实际 DO 对齐，包含必要的唯一约束、状态约束和查询索引。

静态包绑定和官方动态数据源的使用场景见[数据库说明](DATABASE.md)，映射与查询约定见[仓储指南](REPOSITORY_IMPLEMENTATION_GUIDE.md)。

## 通用能力

| 能力 | 入口 | 配置 |
|---|---|---|
| HTTP 响应与异常 | `ApiResponse`、`GlobalExceptionHandler` | Web 配置 |
| 参数校验 | 请求对象上的 Jakarta Validation 注解 | Controller `@Valid` |
| 缓存 | `CacheService`、`SimpleCacheService` | `spring.data.redis`、`CachePolicy` |
| 事件传输 | `DomainEventPublisher`、订单事件 Producer | `rocketmq.*` |
| 接口签名 | `SignatureInterceptor`、`SignatureUtil` | `sign.*`、`apiauth-config.yaml` |
| 超时任务 | `OrderScanScheduledTask` | `notification.*` |
| 邮件 | `EmailSender`、`EmailService` | `spring.mail.*` |
| 依赖状态 | 健康接口与状态注册表 | 数据源和 MQ 降级开关 |

新项目按需裁剪能力，并为自身业务补充认证授权、可靠消息或通知去重等规则。

## 测试起点

Controller 和 Cache 测试演示隔离测试；一份 4 用例的持久化测试使用 H2 加载实际 Mapper 和工厂，验证关键字段、分页、双库绑定、乐观锁和回滚。测试可通过 `./mvnw test` 独立执行。
