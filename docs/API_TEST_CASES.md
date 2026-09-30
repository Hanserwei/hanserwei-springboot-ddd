# 测试使用说明

测试作为脚手架示例，展示隔离测试和关键持久化行为。测试可独立执行，不要求启动 MySQL、PostgreSQL、Redis 或 RocketMQ。

```bash
./mvnw test
```

## 现有范围

| 测试类 | 用例数 | 示例内容 |
|---|---|---|
| `UserControllerTest` | 6 | 用户创建、查询、更新、删除的请求与响应 |
| `OrderControllerTest` | 8 | 订单创建、列表、详情与状态操作 |
| `SimpleCacheServiceTest` | 6 | 缓存命中、回填、空结果和缓存异常 |
| `RepositoryIntegrationTest` | 4 | 字段读写、排序、双库绑定、简介批量查询、乐观锁和回滚 |

Controller 测试使用 `MockMvcBuilders.standaloneSetup`，Mock 应用服务；缓存测试 Mock RedisTemplate。它们展示协议与适配器的隔离验证，不会启动完整应用。

持久化测试加载 `TestConfig` 中的生产数据源和 MyBatis-Plus 配置，使用两个 H2 内存数据库。建表字段与约束复用示例 DDL；MySQL 存储引擎和字符集选项不在 H2 中执行。

## 单独运行

```bash
./mvnw test -Dtest=UserControllerTest,OrderControllerTest
./mvnw test -Dtest=SimpleCacheServiceTest
./mvnw test -Dtest=RepositoryIntegrationTest
```

## 新项目如何调整

user/order 是可替换示例。替换业务代码时，同步替换请求样例、测试聚合、数据库表和关键断言；不需要保留与业务无关的测试。

先验证重要业务规则及失败路径，例如金额精度、状态转换、唯一约束、Mapper 归属或事务回滚。简单 getter 和通用 CRUD 不需要逐个添加测试。

HTTP 示例和参数见 [README](../README.md#示例接口)。完整链路中的接口签名、目标数据库方言、消息投递与邮件发送，根据新项目实际使用的服务单独验证。

H2 用于快速开发检查，目标数据库的驱动、字符集和结构脚本仍需在部署环境进行基本读写验证。新项目开发顺序见[起步指南](START_NEW_PROJECT.md)。
