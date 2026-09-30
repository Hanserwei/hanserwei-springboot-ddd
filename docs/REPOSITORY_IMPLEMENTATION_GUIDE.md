# MyBatis-Plus 仓储指南

user、order 的数据库访问使用 MyBatis-Plus。每个示例包含领域仓储接口、DO、Mapper、Converter 和仓储实现；订单列表需要的用户简介也通过 Mapper 批量查询。

## 类型与边界

```text
domain/repository/OrderRepository        # 领域接口，只使用领域类型
        ↑ 实现
infrastructure/repository/order/
├── OrderRepositoryImpl                 # 查询与持久化操作
├── OrderDO                             # 表字段及 MP 注解
├── OrderMybatisPlusMapper               # BaseMapper<OrderDO>
└── OrderConverter                      # Order ↔ OrderDO
```

DO 与数据库字段对应，MyBatis-Plus 注解集中在 DO。领域模型通过工厂创建、业务行为改变状态、restore 重建；应用层不使用 DO 或 Mapper。

## 查询与分页

简单查询使用 `LambdaQueryWrapper`，由字段引用生成列名和绑定参数。用户简介查询只选择列表实际需要的字段：

```java
LambdaQueryWrapper<UserDO> query = new LambdaQueryWrapper<UserDO>()
        .select(UserDO::getId, UserDO::getName, UserDO::getPhone)
        .in(UserDO::getId, userIds);
List<UserDO> rows = userMapper.selectList(query);
```

`UserInfoQueryClientImpl` 对输入 ID 去重并处理空集合，结果转换为 `UserBriefInfo`。订单应用服务只依赖 `UserInfoQueryClient`，没有用户表或 Mapper 的细节。

分页使用 `BaseMapper.selectPage`，MyBatis-Plus 插件执行 COUNT 并按数据库方言生成 LIMIT/OFFSET。领域 `PageRequest` 与 MP `Page` 都从第 1 页开始，结果转换为领域 `PageResult`。

HTTP 排序属性通过 `SORTABLE_COLUMNS` 白名单映射为真实列名。`RepositorySorts` 忽略未知属性，未提供有效排序时按创建时间倒序，并补充 ID 排序。动态排序使用 `QueryWrapper`，查询条件和固定字段优先使用 Lambda 引用。

## 写入约定

- **数据库主键**：`@TableId(type = IdType.AUTO)` 自动回填 ID，仓储同步到聚合。
- **时间字段**：由聚合行为维护，DO 只传递时间，保证返回对象与存储一致。
- **可选资料**：`FieldStrategy.ALWAYS` 允许将用户可选字段写为 NULL；创建时间使用 `NEVER` 更新策略。
- **业务唯一性**：用户名称和邮箱的数据库唯一约束保护并发写入，冲突转换为领域唯一性异常。
- **乐观锁**：订单 DO 的 `@Version` 配合插件阻止过期版本覆盖，成功后同步聚合版本；不存在的对象与并发冲突分别报告未找到和冲突。

`exist = false` 用于明确不存储的字段。需要保存的业务资料应拥有实际列，并在 Converter、DO、DDL 中保持一致。

## 多数据源与事务

`MybatisPlusConfig` 将 user/order Mapper 包绑定到各自的 SqlSessionTemplate：

| Mapper 包 | 数据库 | 事务管理器 |
|---|---|---|
| `infrastructure.repository.user` | MySQL | `userTransactionManager` |
| `infrastructure.repository.order` | PostgreSQL | `orderTransactionManager` |

每套工厂配置乐观锁插件和分页插件，分页位于插件链末尾。自定义 XML 按库放在 `mapper/user/**/*.xml` 或 `mapper/order/**/*.xml`。

事务定义在应用服务，事务管理器与 Mapper 工厂使用同一个 DataSource。固定包绑定的配置和官方动态数据源支持见[数据库说明](DATABASE.md)。

## 依赖

```xml
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

starter 与 SQL 解析支持使用相同版本。双工厂由配置类装配，插件与 MyBatis 属性也在配置类中维护。

添加自己的聚合和数据库映射时，可复用一份精简的 `RepositoryIntegrationTest` 验证绑定与关键读写行为。完整开发顺序见[新项目起步指南](START_NEW_PROJECT.md)。
