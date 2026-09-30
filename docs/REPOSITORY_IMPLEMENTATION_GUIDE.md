# 仓储实现指南（MyBatis-Plus 统一实现）

## 概述

本脚手架的仓储层统一使用 **MyBatis-Plus** 作为持久化实现（自 2026-09 修订起，
不再提供 Spring Data JDBC / 手写 JDBC 的多实现切换机制）。

订单（PostgreSQL）与用户（MySQL）两个限界上下文各自拥有：

- 一个 `XxxDO`（数据对象，携带 MyBatis-Plus 注解）
- 一个 `XxxConverter`（DO ↔ 领域模型显式转换）
- 一个 `XxxMybatisPlusMapper`（继承 `BaseMapper<XxxDO>`）
- 一个 `XxxRepositoryImpl`（实现领域层定义的 `XxxRepository` 接口）

## 架构设计

### 依赖方向

```text
domain/repository/order/OrderRepository        （接口，零框架依赖）
        ↑ 实现
infrastructure/repository/order/
    OrderRepositoryImpl        ← 注入 →  OrderMybatisPlusMapper（BaseMapper<OrderDO>）
    OrderDO / OrderConverter                OrderDO（@TableName/@TableId）
```

领域层只认识 `Order` 与领域分页类型；持久化细节（DO、MP 注解、SQL）
全部收敛在 infrastructure 层，边界处由 Converter 显式转换。

### 多数据源绑定

`MybatisPlusConfig` 手动装配两套会话工厂（`MybatisPlusAutoConfiguration`
已在 `application.yaml` 中排除）：

| Mapper 包 | 数据源 | 方言 |
|---|---|---|
| `infrastructure.repository.order` | `orderDataSource`（PostgreSQL，@Primary） | POSTGRE_SQL |
| `infrastructure.repository.user` | `userDataSource`（MySQL） | MYSQL |

分页由 `PaginationInnerInterceptor` 按方言自动生成 LIMIT/OFFSET 并执行 COUNT，
`selectPage` 与带 `Page` 参数的自定义 `@Select` 均可分页。

## 关键实现约定

1. **主键回填**：DO 上使用 `@TableId(type = IdType.AUTO)`，`insert` 后主键自动回填，
   仓储随后调用 `order.markCreated(id)` / `user.markPersisted(id)` 同步领域模型。
2. **通用 CRUD 优先**：单表 CRUD 直接使用 `BaseMapper` 内置方法 +
   `LambdaQueryWrapper` / `QueryWrapper`，仅非典型查询手写 `@Select`。
3. **动态排序列必须走白名单**：排序字段来自 HTTP 入参（`?sort=`），
   需经 `SORTABLE_COLUMNS` 这类白名单映射为真实列名，防止 SQL 注入；
   动态列名只能用 `QueryWrapper`（String 列名），Lambda 版仅支持编译期字段引用。
4. **表中暂不存在的字段**：用 `@TableField(exist = false)` 标注
   （示例：`UserDO.wechat`），MyBatis-Plus 不会将其纳入生成的 SQL。
5. **事务边界**：Repository 层不管理事务，事务由 Service 层通过
   `@Transactional(transactionManager = "orderTransactionManager")` 显式指定。

## 依赖配置

```xml
<!-- Spring Boot 4 专用 starter（3.5.9 起分页拦截器拆分到 jsqlparser 包，两者版本保持一致） -->
<dependency>
    <groupId>com.baomidou</groupId>
    <artifactId>mybatis-plus-spring-boot4-starter</artifactId>
    <version>3.5.17</version>
</dependency>
<dependency>
    <groupId>com.baomidou</groupId>
    <artifactId>mybatis-plus-jsqlparser</artifactId>
    <version>3.5.17</version>
</dependency>
```

> 迁移提示：若从旧版本（`mybatis-plus-spring-boot3-starter`）升级，
> `MybatisSqlSessionFactoryBean` 的包名由 `com.baomidou.mybatisplus.extension.spring`
> 变更为 `com.baomidou.mybatisplus.spring`；`DbType.POSTGRESQL` 改为
> `DbType.POSTGRE_SQL`。

## 注意事项

- `MybatisPlusAutoConfiguration` 被排除的原因：双数据源需要手动装配两套
  `SqlSessionFactory`，自动配置只会装配单数据源。
- `PageRequest`（领域分页）与 MP 的 `Page` 均为 1 起始页码，可直接透传。
- Mapper 接口不支持方法重载（statement id 为方法名），分页版请命名为
  `findPageByXxx` 等独立方法名。
