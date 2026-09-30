package com.hanserwei.springboot4ddd.application.port;

import java.time.Duration;

/**
 * 缓存策略常量（application 层）
 *
 * <p>定义应用用例使用的缓存键前缀和 TTL，与具体中间件实现无关。
 *
 * @author Hanserwei
 * @since 1.0.0
 */
public final class CachePolicy {

    private CachePolicy() {
    }

    public static final String USER_KEY_PREFIX = "user:";
    public static final String ORDER_KEY_PREFIX = "order:";

    public static final Duration USER_TTL = Duration.ofMinutes(30);
    public static final Duration ORDER_TTL = Duration.ofMinutes(15);
}
