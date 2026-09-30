# 数据库与多数据源

user 和 order 是两个业务示例，分别演示 MySQL、PostgreSQL 持久化与跨上下文查询。新项目可以保留双库，也可以按业务需要裁剪为单库。

| 数据源 | 数据库 | 示例库名 | 示例表 | 本地事务管理器 |
|---|---|---|---|---|
| `userDataSource` | MySQL 8.0.16+ | `frog` | `users` | `userTransactionManager` |
| `orderDataSource` | PostgreSQL 14+ | `seed` | `orders` | `orderTransactionManager` |

仓储和用户简介查询均使用 MyBatis-Plus。`UserInfoQueryClientImpl` 通过用户 Mapper 的 Lambda 字段投影与批量 IN 查询获取 `id/name/phone`；订单上下文只依赖查询接口。

## 官方多数据源支持

MyBatis-Plus [官方多数据源文档](https://baomidou.com/guides/dynamic-datasource/)介绍了开源组件 `dynamic-datasource` 和企业组件 `mybatis-mate`。多数据源路由由独立组件提供，核心 starter 的自动配置默认创建一套会话工厂。

`dynamic-datasource` [官方仓库](https://github.com/baomidou/dynamic-datasource)支持 Spring Boot 4，专用依赖如下；4.5.0 已发布至 [Maven Central](https://repo.maven.apache.org/maven2/com/baomidou/dynamic-datasource-spring-boot4-starter/maven-metadata.xml)：

```xml
<dependency>
    <groupId>com.baomidou</groupId>
    <artifactId>dynamic-datasource-spring-boot4-starter</artifactId>
    <version>4.5.0</version>
</dependency>
```

该组件通过 `spring.datasource.dynamic.datasource` 配置连接，以 `@DS("user")` / `@DS("order")` 切换数据源，支持分组和读写分离。配合 MyBatis-Plus Boot 4 starter 时可以使用一套会话工厂，异构数据库的分页方言需随实际连接识别。

本脚手架采用固定 Mapper 包绑定，以展示两个上下文各自的方言和本地事务。[MyBatis-Spring 官方](https://mybatis.org/spring/mappers.html)支持通过 `sqlSessionFactoryRef` / `sqlSessionTemplateRef` 将 Mapper 扫描绑定到指定工厂或模板。

| 方案 | 适合场景 | 配置内容 |
|---|---|---|
| 两套工厂、固定 Mapper 包 | 少量固定库，需要明确隔离方言和事务 | 各库的工厂、模板、事务管理器 |
| `dynamic-datasource` | 动态选库、读写分离、多个租户库 | 数据源配置、路由注解、切库与事务边界 |

使用动态组件时，在事务开始前确定数据源。Spring 本地事务会绑定连接，普通 `@Transactional` 加 `@DS` 不会自动提供跨库原子提交。跨库写入按业务的一致性要求另行设计。

## 配置位置

- `DataSourceConfig`：Hikari 数据源和各库的事务管理器。
- `MybatisPlusConfig`：每个库一套 `MybatisSqlSessionFactoryBean`、模板及插件。XML 分别放在 `mapper/user/**/*.xml`、`mapper/order/**/*.xml`。
- `application.yaml`：公共连接池参数、降级开关，关闭默认 SQL 初始化，排除 `MybatisPlusAutoConfiguration`。
- `application-dev.yaml` / `application-prod.yaml`：各环境的连接地址与账号。密码通过环境变量提供。

Hikari 属性前缀为 `spring.user.datasource` 和 `spring.order.datasource`。连接验证失败时，配置类关闭已创建的连接池，再按降级开关返回不可用数据源；该方式允许应用启动，数据库恢复后需重启应用以建立真实数据源。

[MyBatis-Spring 事务文档](https://mybatis.org/spring/transactions.html)要求事务管理器与会话工厂使用同一个 DataSource。用户服务指定 `userTransactionManager`，订单服务指定 `orderTransactionManager`。Mapper 的数据库归属由扫描包绑定决定。

手动装配时，MyBatis 配置和插件在 `MybatisPlusConfig` 中维护。核心自动配置被排除后，直接增加 `mybatis-plus.*` 属性不会自动应用到两套工厂。

## 示例表设计

### users：用户资料

字段与 `User` / `UserDO` 对齐：`id`、`name`、`email`、`phone`、`wechat`、`address`、`created_time`、`updated_time`。

用户名和邮箱有数据库唯一约束，兜住并发注册中的唯一性竞争；领域服务在创建用户或修改邮箱时提供业务校验。邮箱长度为 254，微信号为 64，地址为 255，接口校验与列长度对应。

手机号、微信号和地址允许为空。Mapper 的更新策略支持将已有资料清空为 NULL；HTTP 更新接口将未传或 NULL 视为不修改。设计自己的更新接口时应明确“省略字段”与“清空字段”的区别。

时间列采用 `DATETIME(6)`，对应领域 `LocalDateTime` 的微秒精度。更新时间由聚合行为维护。默认分页按 `created_time DESC, id DESC`，有对应组合索引。

### orders：金额与状态迁移

字段与 `Order` / `OrderDO` 对齐：`id`、`order_no`、`user_id`、`total_amount`、`status`、`version`、`created_at`、`updated_at`。

- 主键采用 PostgreSQL identity；业务订单号由 UUID 生成，具有唯一约束。
- 金额采用 `NUMERIC(19,2)`，要求正数；领域与接口均限制最多两位小数。
- 状态限定为 `PENDING / PAID / CANCELLED / COMPLETED`，合法转换由聚合行为控制。
- `version` 配合 MyBatis-Plus 乐观锁，同一版本的并发更新只有一个成功，冲突请求返回 HTTP 409。
- `user_id` 保存用户上下文标识；两个上下文的数据库独立，不建立跨库外键。
- 时间列采用 `TIMESTAMP(6)`。`(user_id, created_at, id)` 支持用户订单列表，`(status, created_at, id)` 支持超时扫描，`(created_at, id)` 支持默认分页。

唯一约束已为 `name`、`email`、`order_no` 建立索引。DO 驼峰字段由 `mapUnderscoreToCamelCase` 映射为数据库列名。

这两张表只服务于示例：实际项目根据业务设计自己的聚合、字段、金额精度、状态和索引，参考[新项目起步指南](START_NEW_PROJECT.md)。

## 初始化示例数据库

创建数据库与应用账号后，分别执行各库脚本：

```sql
-- MySQL
CREATE DATABASE frog CHARACTER SET utf8mb4;
CREATE USER 'frog_admin'@'localhost' IDENTIFIED BY 'your-password';
GRANT ALL PRIVILEGES ON frog.* TO 'frog_admin'@'localhost';
```

```sql
-- PostgreSQL
CREATE DATABASE seed ENCODING 'UTF8';
```

```bash
mysql -u frog_admin -p frog < src/main/resources/db/mysql/init_users.sql
psql -U postgres -d seed -f src/main/resources/db/postgresql/init_orders.sql
```

脚本各自创建一张示例表和少量演示数据；按业务唯一键保证演示数据可重复初始化。新库中 user1、user2 的 ID 分别为 1、2，对应订单中的用户标识。

新项目按团队习惯使用 Flyway、Liquibase 或其他工具管理自己的数据库结构，演示数据仅在本地开发需要时保留。

## 持久化测试

`RepositoryIntegrationTest` 提供 4 个用例，加载生产双数据源配置，通过 H2 的 MySQL / PostgreSQL 模式执行建表结构与约束。测试跳过演示数据和 MySQL 存储引擎、字符集选项，验证字段读写、分页排序、数据库归属、乐观锁及本地事务回滚。

H2 用于快速验证示例。正式数据库的驱动、排序规则和完整方言行为，在新项目部署前通过实际数据库的基本读写检查验证。
