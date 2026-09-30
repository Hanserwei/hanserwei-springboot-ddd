# 数据库说明

## 数据源概览

| 数据源 | 数据库 | 库名 | 表 | 用途 | 访问方式 |
|---|---|---|---|---|---|
| `userDataSource` | MySQL 8.0+ | `frog` | `users` | 用户数据 | MyBatis-Plus（`UserMybatisPlusMapper`） |
| `orderDataSource` | PostgreSQL 14+ | `seed` | `orders` | 订单数据 | MyBatis-Plus（`OrderMybatisPlusMapper`） |

> 仓储层统一使用 MyBatis-Plus；`userJdbcClient`（Spring `JdbcClient`）仅供
> 防腐层 `UserInfoQueryClientImpl` 做跨上下文只读查询，不属于仓储实现。

## 多数据源配置

### DataSourceConfig.java

配置两个独立的数据源、事务管理器，并支持优雅降级（连接失败时记录错误、
以虚拟数据源继续启动，不阻塞应用）：

```java
// MySQL 数据源 - users
@Bean(name = "userDataSource")
public DataSource userDataSource(@Qualifier("userHikariConfig") HikariConfig hikariConfig) { ... }

@Bean(name = "userTransactionManager")
public PlatformTransactionManager userTransactionManager(...) { ... }

// PostgreSQL 数据源 - orders（@Primary）
@Bean(name = "orderDataSource")
@Primary
public DataSource orderDataSource(@Qualifier("orderHikariConfig") HikariConfig hikariConfig) { ... }

@Bean(name = "orderTransactionManager")
@Primary
public PlatformTransactionManager orderTransactionManager(...) { ... }

// 防腐层只读查询用的 JdbcClient
@Bean(name = "userJdbcClient")
public JdbcClient userJdbcClient(@Qualifier("userDataSource") DataSource dataSource) { ... }
```

### MybatisPlusConfig.java

手动装配两套 MyBatis-Plus 会话工厂，把不同包下的 Mapper 绑定到各自数据源
（`MybatisPlusAutoConfiguration` 已在 `application.yaml` 中排除）：

```java
@Configuration
@MapperScan(basePackages = "...infrastructure.repository.order",
        sqlSessionTemplateRef = "orderSqlSessionTemplate")   // PostgreSQL
@MapperScan(basePackages = "...infrastructure.repository.user",
        sqlSessionTemplateRef = "userSqlSessionTemplate")    // MySQL
public class MybatisPlusConfig { ... }
```

每套会话工厂各挂一个 `PaginationInnerInterceptor`，按方言
（`POSTGRE_SQL` / `MYSQL`）生成 LIMIT/OFFSET 并自动 COUNT。

## 字段命名规范

Java 驼峰 ↔ 数据库下划线，依赖 MyBatis-Plus 的 `mapUnderscoreToCamelCase`
（配置类中已显式开启）：

| Java（DO） | MySQL (users) | PostgreSQL (orders) |
|---|---|---|
| `createdTime` / `updatedTime` | `created_time` / `updated_time` | — |
| `createdAt` / `updatedAt` | — | `created_at` / `updated_at` |

**注意**: 两张表的时间列命名不同（`*_time` vs `*_at`），DO 字段名也随之区分，
转换时留意。

## Order 的映射配置

`OrderDO` 携带 MyBatis-Plus 注解，领域模型 `Order` 完全不感知持久化：

```java
@Data
@Builder
@TableName("orders")
public class OrderDO {

    @TableId(type = IdType.AUTO)   // 自增主键，insert 后回填
    private Long id;

    private String orderNo;        // order_no（驼峰自动映射）
    private Long userId;
    private BigDecimal totalAmount;
    private String status;
    private LocalDateTime createdAt;   // created_at
    private LocalDateTime updatedAt;   // updated_at
}
```

## User 的映射配置

`UserDO` 同样以注解映射；`wechat` 列暂不存在于表中，显式声明不参与 SQL：

```java
@Data
@TableName("users")
public class UserDO {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String name;
    private String email;
    private String phone;

    @TableField(exist = false)   // 表中暂无此列，不参与生成的 SQL
    private String wechat;

    private String address;
    private LocalDateTime createdTime;
    private LocalDateTime updatedTime;
}
```

## 数据库初始化

### MySQL 初始化
```bash
mysql -u root -p < src/main/resources/db/mysql/init_users.sql
```

### PostgreSQL 初始化
```bash
psql -U postgres -c "CREATE DATABASE seed;"
psql -U postgres -d seed -f src/main/resources/db/postgresql/init_orders.sql
```

## 配置文件

`application.yaml` 中两个数据源的 Hikari 连接池配置前缀分别为
`spring.user.datasource` 与 `spring.order.datasource`，具体连接地址、
账号密码见 `application-dev.yaml` / `application-prod.yaml`。
降级开关：`spring.user.fallback.enabled` / `spring.order.fallback.enabled`。

## 注意事项

1. **数据源路由**: 由 `@MapperScan` 的 `sqlSessionTemplateRef` 显式绑定，
   order 侧的 `SqlSessionFactory` / `SqlSessionTemplate` 标记 `@Primary`。
2. **事务管理**: Service 层显式指定事务管理器——
   - User: `@Transactional(transactionManager = "userTransactionManager")`
   - Order: `@Transactional(transactionManager = "orderTransactionManager")`
   - 跨库操作不做本地强一致，通过应用服务编排 + 领域事件最终一致。
3. **动态排序**: 用户列表 `?sort=` 入参经 `UserRepositoryImpl.SORTABLE_COLUMNS`
   白名单映射后才进入 SQL，防注入；动态列名用 `QueryWrapper`（String 列名）。
4. **数据库驱动**: MySQL `com.mysql.cj.jdbc.Driver`；PostgreSQL `org.postgresql.Driver`。

## 测试数据

### users 表
- user1 / user2 / user3（见 `init_users.sql`）

### orders 表
- ORD1000000001 (PENDING)、ORD1000000002 (PAID)、ORD1000000003 (COMPLETED)
  （见 `init_orders.sql`）
