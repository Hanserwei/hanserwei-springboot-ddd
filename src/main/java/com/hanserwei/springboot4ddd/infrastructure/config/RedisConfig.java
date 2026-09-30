package com.hanserwei.springboot4ddd.infrastructure.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJacksonJsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

/**
 * Redis 配置类
 *
 * <p>装配 RedisTemplate；缓存键前缀和 TTL 由 {@code application.port.CachePolicy} 定义。
 *
 * @author Hanserwei
 * @since 1.0.0
 */
@Slf4j
@Configuration
public class RedisConfig {

    /**
     * 配置 RedisTemplate
     * Key 使用 String 序列化，Value 使用 JSON 序列化
     */
    @Bean
    @ConditionalOnMissingBean(RedisTemplate.class)
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);

        StringRedisSerializer stringSerializer = new StringRedisSerializer();
        template.setKeySerializer(stringSerializer);
        template.setHashKeySerializer(stringSerializer);

        GenericJacksonJsonRedisSerializer jsonSerializer = GenericJacksonJsonRedisSerializer.create(config -> {
            // 如果需要可以配置具体参数，不需要直接保留空 Lambda 表达式即可
        });
        template.setValueSerializer(jsonSerializer);
        template.setHashValueSerializer(jsonSerializer);

        template.afterPropertiesSet();
        log.info("RedisTemplate 配置完成");
        return template;
    }
}
