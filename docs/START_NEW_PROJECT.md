# 用脚手架创建新项目

user、order 用来演示 DDD 分层、MyBatis-Plus、双数据库、缓存和领域事件。开始新项目时，先让一条自己的业务用例贯通，再按需要保留外围能力。

## 1. 设置工程标识

从模板创建自己的仓库，保留 Maven Wrapper。通过 IDE 重构将基础包 `com.hanserwei.springboot4ddd` 改为自己的包名，同时修改 `pom.xml` 的 groupId、artifactId、name，以及 `spring.application.name`。

检查配置中的包名字符串：`MybatisPlusConfig` 的 `@MapperScan`、日志包名和自己的 XML namespace。测试包名随主代码一起调整，接口签名路径与缓存键前缀使用新项目的命名。

## 2. 决定数据库数量

**使用一个数据库**时，可以采用 MyBatis-Plus Boot 4 starter 自动配置：

1. 裁剪 `MybatisPlusConfig` 的两套工厂，取消 `application.yaml` 对 `MybatisPlusAutoConfiguration` 的排除。
2. 使用 `spring.datasource.url/username/password` 配置连接，由 Boot 创建数据源和事务管理器，裁剪 `DataSourceConfig` 的双库装配。
3. 在配置类上添加自己的 `@MapperScan`，以 `@Bean` 提供 `MybatisPlusInterceptor`，配置所需的分页、乐观锁插件。
4. 应用服务使用普通 `@Transactional`；业务查询适配器使用绑定同一数据源的 Mapper。
5. 只保留所需 JDBC driver、建表脚本和测试数据源。

**使用两个固定数据库**时，沿用现有装配方式。新上下文的 Mapper 包绑定目标工厂，应用服务使用对应事务管理器，XML 放在对应数据库的资源目录。跨上下文查询可参考 `UserInfoQueryClient` 的接口与适配器。

**需要动态选库**时，参照[数据库说明](DATABASE.md)评估官方 `dynamic-datasource` Boot 4 starter。结合 `@DS`、事务入口、Mapper 和分页方言设计路由。跨数据库写入按业务需要选择一致性方案。

## 3. 实现自己的业务用例

按同一条用例贯通四层：

| 层 | 实现内容 | 参考示例 |
|---|---|---|
| domain | 聚合行为、业务不变量、仓储接口、必要的领域服务和事件 | `User`、`Order`、`OrderRepository` |
| application | Command、DTO、用例编排和事务入口 | `CreateOrderCommand`、`OrderService` |
| infrastructure | DO、Mapper、Converter、仓储及外部客户端 | `OrderDO`、`OrderConverter`、`OrderRepositoryImpl` |
| interfaces | 请求校验、Controller、响应与分页参数转换 | `CreateOrderRequest`、`OrderController` |

先明确业务规则，再设计聚合。订单示例只有金额和状态；自己的业务需要商品项、币种、价格计算等内容时，应从实际业务模型设计。

领域对象保持框架无关。MyBatis-Plus 注解放在 DO 上，数据库生成的 ID 和乐观锁版本由仓储同步回领域对象。领域仓储接口只使用领域类型，避免把 Mapper、DO 或 MyBatis-Plus 的 Page 传到上层。

新增字段时，同时调整聚合、Command/DTO、请求校验、Converter、DO 和 DDL。业务资料需要持久化时，确保数据库有对应列；`@TableField(exist = false)` 用于明确不存储的字段。

## 4. 裁剪示例与外围能力

完成自己的用例后，按依赖顺序替换示例的 Controller、应用服务、聚合、事件、消息处理器和定时任务。保留需要的通用组件，清理无用的注入关系、配置和依赖。

| 能力 | 参考入口 | 新项目中的调整 |
|---|---|---|
| 缓存 | `CacheService`、`CachePolicy` | 设计键、TTL 和更新后的失效时机 |
| 领域事件与 MQ | `DomainEventPublisher`、订单消息处理器 | 设计自己的事件、投递与消费规则 |
| 定时任务 | `OrderScanScheduledTask` | 替换任务或删除任务 Bean |
| 邮件 | `EmailSender` | 配置自己的 SMTP 与模板，或删除邮件能力 |
| 接口签名 | `apiauth-config.yaml`、签名注解 | 配置客户端约定、路径与密钥 |
| 数据库降级 | `fallback.enabled` | 决定数据库连接失败时能否继续启动 |

订单事件示例在服务事务中同步发布，可靠消息业务需要按自身要求设计数据库提交与消息投递的一致性。新项目也需实现自己的用户认证与授权。

## 5. 管理自己的数据库结构

`db/mysql/init_users.sql` 和 `db/postgresql/init_orders.sql` 用于初始化示例新库。新项目根据业务编写自己的建表脚本，并按团队习惯用 Flyway、Liquibase 或其他工具管理结构版本。

字段长度、金额精度、唯一约束、状态约束和索引应与业务规则及查询一致。演示数据按本地开发需要保留。涉及并发状态更新的聚合可参考订单的 `version` 乐观锁。

## 6. 使用精简测试验证关键行为

```bash
./mvnw test
./mvnw package
```

Controller 和 Cache 测试展示隔离测试；`RepositoryIntegrationTest` 只有 4 个用例，展示实际 Mapper 装配、字段读写、分页排序、乐观锁和事务回滚。测试使用 H2，不依赖外部数据库、Redis 或 MQ。

替换业务示例时同步替换对应测试。新增测试围绕业务关键规则与失败路径展开，例如状态转换、唯一性和事务行为；不需要为每个 getter 或 CRUD 方法分别堆叠测试。

先跑通自己业务的创建、查询和更新，再扩展其他能力。部署前在目标数据库上执行结构脚本并检查基本读写，确认驱动、字符集和方言与实际环境一致。
