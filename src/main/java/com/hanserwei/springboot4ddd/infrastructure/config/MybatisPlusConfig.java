package com.hanserwei.springboot4ddd.infrastructure.config;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
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
 * <p>由于 {@code MybatisPlusAutoConfiguration} 已在 application.yaml 中排除，
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
        return buildSqlSessionFactory(dataSource, DbType.POSTGRE_SQL);
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
        return buildSqlSessionFactory(dataSource, DbType.MYSQL);
    }

    @Bean
    public SqlSessionTemplate userSqlSessionTemplate(
            @Qualifier("userSqlSessionFactory") SqlSessionFactory sqlSessionFactory) {
        return new SqlSessionTemplate(sqlSessionFactory);
    }

    /**
     * 构建绑定指定数据源的 MyBatis-Plus SqlSessionFactory
     */
    private SqlSessionFactory buildSqlSessionFactory(DataSource dataSource, DbType dbType) throws Exception {
        MybatisSqlSessionFactoryBean factoryBean = new MybatisSqlSessionFactoryBean();
        factoryBean.setDataSource(dataSource);

        // 分页拦截器：按方言生成 LIMIT/OFFSET，并自动执行 COUNT
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        interceptor.addInnerInterceptor(new PaginationInnerInterceptor(dbType));
        factoryBean.setPlugins(interceptor);

        MybatisConfiguration configuration = new MybatisConfiguration();
        configuration.setMapUnderscoreToCamelCase(true);
        configuration.setLogImpl(Slf4jImpl.class);
        factoryBean.setConfiguration(configuration);

        // 预留 XML mapper 位置（当前全部使用注解 SQL）
        factoryBean.setMapperLocations(new PathMatchingResourcePatternResolver()
                .getResources("classpath*:/mapper/**/*.xml"));

        return factoryBean.getObject();
    }
}
