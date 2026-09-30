# Redis 缓存

缓存通过应用层 `CacheService` 端口使用，`SimpleCacheService` 提供 Redis 实现。Controller 和领域聚合不直接访问 Redis。

## 查询流程

应用服务调用 `getOrSet(key, ttl, supplier)`：命中时返回缓存，未命中时执行查询并缓存非空结果。Redis 异常会回退到数据源，删除异常写入日志。

```java
return cacheService.getOrSet(
        CachePolicy.USER_KEY_PREFIX + id,
        CachePolicy.USER_TTL,
        () -> toDTO(userRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("用户", "id", id)))
);
```

## 键与有效期

策略由 `application.port.CachePolicy` 定义。

| 查询 | 键 | TTL |
|---|---|---|
| 用户 ID | `user:{id}` | 30 分钟 |
| 用户名称 | `user:name:{name}` | 30 分钟 |
| 订单 ID | `order:id:{id}` | 15 分钟 |
| 订单号 | `order:no:{orderNo}` | 15 分钟 |

用户资料更新、删除同时清除 ID 与名称缓存；订单状态更新、删除同时清除 ID 与订单号缓存。

## 配置

```yaml
spring:
  data:
    redis:
      host: localhost
      port: 6379
      database: 0
      timeout: 3000ms
```

`RedisConfig` 装配 RedisTemplate，键使用 String 序列化，值使用 JSON 序列化。

## 用于自己的项目

替换业务时设计自己的缓存键、数据类型、TTL 与失效时机。缓存涉及事务提交和并发读写时，按业务一致性要求安排更新策略。

可以在应用服务继续依赖 `CacheService`，按需要提供 Redis、其他缓存或直接查询的实现。完全不使用缓存时，裁剪服务调用、实现和 Redis 依赖。

测试示例 `SimpleCacheServiceTest` Mock RedisTemplate，不依赖外部 Redis：

```bash
./mvnw test -Dtest=SimpleCacheServiceTest
```
