# Spring Boot 4 DDD 脚手架

基于 Spring Boot 4.1、JDK 25 和 MyBatis-Plus 的 Java 脚手架，采用接口、应用、领域、基础设施四层结构。user、order 是可替换的业务示例，用于展示聚合行为、数据库访问、事务、分页、缓存和领域事件。

开展自己的项目，请阅读[新项目起步指南](docs/START_NEW_PROJECT.md)：设置工程标识、选择数据库数量、实现业务用例，再裁剪不需要的示例和外围能力。

## 技术与能力

| 内容 | 技术 |
|---|---|
| 运行与构建 | JDK 25、Maven Wrapper |
| 应用框架 | Spring Boot 4.1.1、Spring MVC、Jakarta Validation |
| 持久化 | MyBatis-Plus 3.5.17 Boot 4 starter、HikariCP |
| 数据库示例 | MySQL 8.0.16+、PostgreSQL 14+ |
| 缓存 | Spring Data Redis |
| 领域事件传输 | RocketMQ starter 2.3.6 |
| 通知 | Spring Mail、Spring Scheduling |
| 测试 | JUnit、Mockito、Spring Test、H2 |

工程还提供统一响应、全局异常处理、SHA-256 接口签名和依赖健康检查。每项能力都可根据新项目需要裁剪。

## 结构与依赖方向

```text
interfaces → application → domain ← infrastructure
```

| 层 | 职责 | 示例 |
|---|---|---|
| interfaces | HTTP 请求、校验、协议和分页参数转换 | `OrderController`、`CreateOrderRequest` |
| application | 用例编排、事务、Command/DTO、能力端口 | `OrderService`、`CacheService` |
| domain | 聚合行为、业务规则、仓储和外部查询接口、事件 | `Order`、`User`、`OrderRepository` |
| infrastructure | 实现仓储和能力端口，装配数据库、缓存、消息与邮件 | `OrderRepositoryImpl`、`OrderDO` |

领域模型不依赖数据库框架。DO 承载 MyBatis-Plus 注解，Converter 在领域模型与 DO 之间显式转换；应用服务控制本地事务，Controller 将请求转换为应用命令。

```text
src/main/java/com/hanserwei/springboot4ddd/
├── Application.java
├── interfaces/       # Controller、请求、签名注解、分页转换
├── application/      # Command、DTO、Service、端口、任务编排
├── domain/           # 聚合、仓储接口、领域服务、事件、查询接口
└── infrastructure/   # Mapper、DO、仓储、配置、缓存、MQ、邮件
src/main/resources/
├── application.yaml
├── application-dev.yaml
├── application-prod.yaml
├── apiauth-config.yaml
├── db/mysql/init_users.sql
├── db/postgresql/init_orders.sql
└── templates/email/order-timeout.html
src/test/             # Controller、缓存和持久化的精简示例
```

## 运行示例

### 1. 准备环境

使用 JDK 25。测试只需要 JDK，Maven Wrapper 会处理构建依赖。

运行完整示例时，准备 MySQL、PostgreSQL、Redis 和 RocketMQ；根据实际地址调整配置。数据源与 MQ 提供启动降级开关，Redis 查询失败会回退到数据源。验证完整读写和消息链路时需要相应服务可用。

### 2. 创建并初始化数据库

在 MySQL 中执行：

```sql
CREATE DATABASE frog CHARACTER SET utf8mb4;
CREATE USER 'frog_admin'@'localhost' IDENTIFIED BY 'your-password';
GRANT ALL PRIVILEGES ON frog.* TO 'frog_admin'@'localhost';
```

在 PostgreSQL 中执行：

```sql
CREATE DATABASE seed ENCODING 'UTF8';
```

分别初始化示例表：

```bash
mysql -u frog_admin -p frog < src/main/resources/db/mysql/init_users.sql
psql -U postgres -d seed -f src/main/resources/db/postgresql/init_orders.sql
```

脚本包含少量演示数据。字段、约束和索引说明见[数据库文档](docs/DATABASE.md)。

### 3. 配置与启动

`application.yaml` 提供公共配置，默认使用 `dev`；连接地址在 `application-dev.yaml` 中设置。密码与调用方密钥通过环境变量提供：

```bash
export MYSQL_PASSWORD='your-mysql-password'
export POSTGRES_PASSWORD='your-postgresql-password'
export API_AUTH_IOS_SECRET='your-ios-secret'
export API_AUTH_H5_SECRET='your-h5-secret'

./mvnw test
./mvnw spring-boot:run
```

应用默认监听 `8080`。邮件功能使用 `spring.mail.*` 配置，收件人和任务周期使用 `notification.*` 配置。

```bash
curl http://localhost:8080/api/health/simple
curl http://localhost:8080/api/health
curl http://localhost:8080/api/users
```

打包及运行：

```bash
./mvnw package
java -jar target/springboot4ddd-0.0.1-SNAPSHOT.jar --spring.profiles.active=prod
```

## MyBatis-Plus 与多数据源

MyBatis-Plus 官方提供 `dynamic-datasource` 多数据源组件，支持 Boot 4。这个脚手架选择两套固定工厂，以展示数据库方言和本地事务的独立绑定：

| 示例 | Mapper 包 | 数据源 | 事务管理器 |
|---|---|---|---|
| user | `infrastructure.repository.user` | MySQL `userDataSource` | `userTransactionManager` |
| order | `infrastructure.repository.order` | PostgreSQL `orderDataSource` | `orderTransactionManager` |

`MybatisPlusConfig` 配置工厂、模板、乐观锁和分页插件，XML 资源按库隔离。官方支持依据、静态绑定与动态路由的适用场景见[数据库文档](docs/DATABASE.md)。单库项目的裁剪步骤见[新项目起步指南](docs/START_NEW_PROJECT.md)。

用户示例演示注册、唯一性校验和资料更新；订单示例演示创建、支付、取消、完成和乐观锁。订单列表通过用户查询接口批量获取简介。示例中的模型和表可以按自己的业务整体替换。

## 示例接口

用户接口：

| 方法 | 路径 | 用途 |
|---|---|---|
| POST | `/api/users` | 注册用户 |
| GET | `/api/users` | 用户列表 |
| GET | `/api/users/page` | 分页列表 |
| GET | `/api/users/{id}` | 用户详情 |
| GET | `/api/users/name/{name}` | 按名称查询 |
| PUT | `/api/users/{id}` | 修改资料 |
| DELETE | `/api/users/{id}` | 删除用户 |

订单接口：

| 方法 | 路径 | 用途 |
|---|---|---|
| POST | `/api/orders/create` | 创建订单 |
| GET | `/api/orders` | 订单列表 |
| GET | `/api/orders/page` | 分页列表 |
| GET | `/api/orders/{id}` | 订单详情 |
| GET | `/api/orders/no/{orderNo}` | 按订单号查询 |
| GET | `/api/orders/user/{userId}` | 用户订单列表 |
| GET | `/api/orders/user/{userId}/page` | 用户订单分页 |
| POST | `/api/orders/{id}/pay` | 支付 |
| POST | `/api/orders/{id}/cancel` | 取消 |
| POST | `/api/orders/{id}/complete` | 完成 |
| DELETE | `/api/orders/{id}` | 删除 |

创建示例：

```bash
curl -X POST http://localhost:8080/api/users \
  -H 'Content-Type: application/json' \
  -d '{"name":"alice","email":"alice@example.com","wechat":"alice_wx"}'

curl -X POST http://localhost:8080/api/orders/create \
  -H 'Content-Type: application/json' \
  -d '{"userId":1,"totalAmount":99.99}'
```

分页从 1 开始。排序使用领域属性名，经仓储白名单映射为列名；默认按创建时间和 ID 倒序。

```bash
curl 'http://localhost:8080/api/users/page?page=1&size=10&sort=name,asc'
curl 'http://localhost:8080/api/orders/page?page=1&size=10&sort=totalAmount,desc'
```

订单支付、取消、完成和删除使用 `@RequireSign`，权限与调用方密钥在 `apiauth-config.yaml` 中配置。签名使用服务端匹配的路径模板，参数规则与请求头说明见[签名指南](docs/SIGN_GUIDE.md)。

## 测试与扩展

测试包含 Controller、缓存和一份只有 4 个用例的持久化集成示例。持久化测试使用 H2 的两种兼容模式，不依赖外部服务。新增测试围绕自己业务的关键规则、事务和失败路径展开。

| 文档 | 用途 |
|---|---|
| [新项目起步指南](docs/START_NEW_PROJECT.md) | 改工程标识、选数据库、实现业务、裁剪能力 |
| [DDD 示例教程](docs/TUTORIAL.md) | 跟随一条用例理解四层结构 |
| [数据库说明](docs/DATABASE.md) | 官方多数据源支持、配置和示例 DDL |
| [仓储实现指南](docs/REPOSITORY_IMPLEMENTATION_GUIDE.md) | DO、Mapper、Converter、分页和事务 |
| [工程能力概览](docs/IMPLEMENTATION_SUMMARY.md) | 示例模型与通用能力入口 |
| [测试使用说明](docs/API_TEST_CASES.md) | 现有测试范围、运行与替换方式 |
| [Redis 缓存指南](docs/REDIS_CACHE_GUIDE.md) | 缓存端口、键和失效方式 |
| [定时任务与邮件](docs/SCHEDULED_TASKS_AND_EMAIL.md) | 扫描任务、模板和配置 |
| [API 签名指南](docs/SIGN_GUIDE.md) | 签名算法、配置和调用方式 |
