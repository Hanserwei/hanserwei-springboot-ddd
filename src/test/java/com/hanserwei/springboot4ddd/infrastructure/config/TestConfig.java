package com.hanserwei.springboot4ddd.infrastructure.config;

import com.hanserwei.springboot4ddd.infrastructure.client.user.UserInfoQueryClientImpl;
import com.hanserwei.springboot4ddd.infrastructure.repository.order.OrderRepositoryImpl;
import com.hanserwei.springboot4ddd.infrastructure.repository.user.UserRepositoryImpl;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Import;

/** 加载生产中的双数据源和 Mapper 装配，测试不依赖 Redis、MQ 或 SMTP。 */
@TestConfiguration(proxyBeanMethods = false)
@EnableConfigurationProperties
@Import({DataSourceConfig.class, MybatisPlusConfig.class,
        UserRepositoryImpl.class, OrderRepositoryImpl.class, UserInfoQueryClientImpl.class})
public class TestConfig {
}
