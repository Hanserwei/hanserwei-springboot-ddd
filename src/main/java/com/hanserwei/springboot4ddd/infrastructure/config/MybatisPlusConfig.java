package com.hanserwei.springboot4ddd.infrastructure.config;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.OptimisticLockerInnerInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import com.baomidou.mybatisplus.spring.MybatisSqlSessionFactoryBean;
import org.apache.ibatis.logging.slf4j.Slf4jImpl;
import org.apache.ibatis.session.SqlSessionFactory;
import org.mybatis.spring.SqlSessionTemplate;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

import javax.sql.DataSource;

/**
 * MyBatis-Plus 多数据源配置
 *
 * <p>采用固定 Mapper 包绑定，保持两个异构库与本地事务相互独立。
 * 官方另有 dynamic-datasource（含 Boot 4 starter），选型说明见 docs/DATABASE.md。
 * 由于 {@code MybatisPlusAutoConfiguration} 已在 application.yaml 中排除，
 * 此处手动装配两套 SqlSessionFactory / SqlSessionTemplate，
 * 并通过两个 {@code @MapperScan} 把不同包下的 Mapper 绑定到各自数据源：
 *
 * <ul>
 *   <li>订单（PostgreSQL，@Primary）：{@code infrastructure.repository.order}</li>
 *   <li>用户（MySQL）：{@code infrastructure.repository.user}</li>
 * </ul>
 *
 * <p>分页由 {@link PaginationInnerInterceptor} 按数据库方言处理：
 * {@code selectPage} 与带 {@code Page} 参数的自定义查询会自动追加 LIMIT/OFFSET 并执行 COUNT。
 *
 * @author Hanserwei
 * @since 1.0.0
 */
@Configuration
@MapperScan(
        basePackages = "com.hanserwei.springboot4ddd.infrastructure.repository.order",
        sqlSessionTemplateRef = "orderSqlSessionTemplate")
@MapperScan(
        basePackages = "com.hanserwei.springboot4ddd.infrastructure.repository.user",
        sqlSessionTemplateRef = "userSqlSessionTemplate")
public class MybatisPlusConfig {

    /**
     * 订单库（PostgreSQL）SqlSessionFactory
     */
    @Bean
    @Primary
    public SqlSessionFactory orderSqlSessionFactory(
            @Qualifier("orderDataSource") DataSource dataSource) throws Exception {
        return buildSqlSessionFactory(dataSource, DbType.POSTGRE_SQL, "classpath*:/mapper/order/**/*.xml");
    }

    @Bean
    @Primary
    public SqlSessionTemplate orderSqlSessionTemplate(
            @Qualifier("orderSqlSessionFactory") SqlSessionFactory sqlSessionFactory) {
        return new SqlSessionTemplate(sqlSessionFactory);
    }

    /**
     * 用户库（MySQL）SqlSessionFactory
     */
    @Bean
    public SqlSessionFactory userSqlSessionFactory(
            @Qualifier("userDataSource") DataSource dataSource) throws Exception {
        return buildSqlSessionFactory(dataSource, DbType.MYSQL, "classpath*:/mapper/user/**/*.xml");
    }

    @Bean
    public SqlSessionTemplate userSqlSessionTemplate(
            @Qualifier("userSqlSessionFactory") SqlSessionFactory sqlSessionFactory) {
        return new SqlSessionTemplate(sqlSessionFactory);
    }

    /**
     * 构建绑定指定数据源的 MyBatis-Plus SqlSessionFactory
     */
    private SqlSessionFactory buildSqlSessionFactory(DataSource dataSource, DbType dbType,
                                                    String mapperLocations) throws Exception {
        MybatisSqlSessionFactoryBean factoryBean = new MybatisSqlSessionFactoryBean();
        factoryBean.setDataSource(dataSource);

        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        interceptor.addInnerInterceptor(new OptimisticLockerInnerInterceptor());
        // 分页放在拦截器链末尾，按数据库方言生成 LIMIT/OFFSET 并自动 COUNT。
        interceptor.addInnerInterceptor(new PaginationInnerInterceptor(dbType));
        factoryBean.setPlugins(interceptor);

        MybatisConfiguration configuration = new MybatisConfiguration();
        configuration.setMapUnderscoreToCamelCase(true);
        configuration.setLogImpl(Slf4jImpl.class);
        factoryBean.setConfiguration(configuration);

        // XML 和注解 Mapper 一样按数据源隔离，避免两套工厂都加载所有 XML。
        factoryBean.setMapperLocations(new PathMatchingResourcePatternResolver()
                .getResources(mapperLocations));

        return factoryBean.getObject();
    }
}
