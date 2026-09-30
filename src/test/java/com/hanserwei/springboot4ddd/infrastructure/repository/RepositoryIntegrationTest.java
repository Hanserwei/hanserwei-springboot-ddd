package com.hanserwei.springboot4ddd.infrastructure.repository;

import com.hanserwei.springboot4ddd.domain.client.user.UserInfoQueryClient;
import com.hanserwei.springboot4ddd.domain.exception.UniquenessViolationException;
import com.hanserwei.springboot4ddd.domain.model.order.Order;
import com.hanserwei.springboot4ddd.domain.model.user.User;
import com.hanserwei.springboot4ddd.domain.page.PageRequest;
import com.hanserwei.springboot4ddd.domain.page.SortOrder;
import com.hanserwei.springboot4ddd.domain.repository.order.OrderRepository;
import com.hanserwei.springboot4ddd.domain.repository.user.UserRepository;
import com.hanserwei.springboot4ddd.domain.service.user.UserUniquenessChecker;
import com.hanserwei.springboot4ddd.infrastructure.config.TestConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.ConfigDataApplicationContextInitializer;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.support.EncodedResource;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.jdbc.datasource.init.ScriptUtils;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** 精简的持久化示例：复用生产装配和 DDL，H2 模式测试不依赖外部服务。 */
@SpringJUnitConfig(classes = TestConfig.class,
        initializers = ConfigDataApplicationContextInitializer.class)
class RepositoryIntegrationTest {

    @Autowired
    private UserRepository users;
    @Autowired
    private OrderRepository orders;
    @Autowired
    private UserInfoQueryClient userBriefs;
    @Autowired
    @Qualifier("userDataSource")
    private DataSource userDataSource;
    @Autowired
    @Qualifier("orderDataSource")
    private DataSource orderDataSource;
    @Autowired
    @Qualifier("userTransactionManager")
    private PlatformTransactionManager userTransactionManager;
    @Autowired
    @Qualifier("orderTransactionManager")
    private PlatformTransactionManager orderTransactionManager;

    @BeforeEach
    void initializeTables() throws Exception {
        // H2 只去掉存储引擎/字符集选项并跳过演示数据，表字段和约束复用生产脚本。
        initialize(userDataSource, "users", "db/mysql/init_users.sql", "-- 重复执行",
                ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户表'",
                ")");
        initialize(orderDataSource, "orders", "db/postgresql/init_orders.sql", "-- 新库中", "", "");
    }

    @Test
    void userFieldsRoundTripAndUniqueConstraints() {
        User user = users.save(newUser("alice"));
        User loaded = users.findById(user.getId()).orElseThrow();
        assertThat(loaded.getWechat()).isEqualTo("wechat_alice");
        assertThat(loaded.getAddress()).isEqualTo("杭州");
        assertThat(loaded.getCreatedTime()).isEqualTo(user.getCreatedTime());
        assertThat(loaded.getUpdatedTime()).isEqualTo(user.getUpdatedTime());

        loaded.changePhone(null);
        loaded.changeWechat(null);
        loaded.changeAddress(null);
        users.update(loaded);
        User cleared = users.findById(user.getId()).orElseThrow();
        assertThat(cleared.getPhone()).isNull();
        assertThat(cleared.getWechat()).isNull();
        assertThat(cleared.getAddress()).isNull();
        assertThat(cleared.getUpdatedTime()).isEqualTo(loaded.getUpdatedTime());

        // 模拟唯一性预检查之后另一请求已经写入的情形。
        User duplicate = User.restore(null, "alice", "another@example.com", null, null, null,
                user.getCreatedTime(), user.getUpdatedTime());
        assertThatThrownBy(() -> users.save(duplicate)).isInstanceOf(UniquenessViolationException.class);
    }

    @Test
    void paginationHonorsSortAndMappersUseSeparateDatabases() {
        User user = users.save(newUser("alice"));
        LocalDateTime at = LocalDateTime.of(2026, 1, 1, 0, 0);
        Order first = orders.save(orderAt("ORD-first", user.getId(), "30.00", at));
        Order second = orders.save(orderAt("ORD-second", user.getId(), "10.00", at));
        Order third = orders.save(orderAt("ORD-third", user.getId() + 1, "20.00", at));

        assertThat(users.findAll()).hasSize(1);
        assertThat(orders.findAllOrders()).hasSize(3);
        // 订单事务中仍通过用户 Mapper 查询另一库，保持查询端口与数据源绑定。
        var briefs = new TransactionTemplate(orderTransactionManager).execute(status ->
                userBriefs.findBriefs(List.of(user.getId(), user.getId(), user.getId() + 1)));
        assertThat(briefs).containsOnlyKeys(user.getId());
        assertThat(briefs.get(user.getId()).getName()).isEqualTo("alice");
        assertThat(briefs.get(user.getId()).getPhone()).isEqualTo("13800138001");
        assertThat(userBriefs.findBriefs(List.of())).isEmpty();
        assertThat(orders.findByOrderNo(first.getOrderNo())).isPresent();
        assertThat(orders.findExpiredPendingOrders(at.plusSeconds(1))).hasSize(3);

        var sorted = orders.findAllOrders(PageRequest.of(1, 2,
                List.of(SortOrder.asc("totalAmount"))));
        assertThat(sorted.getTotalElements()).isEqualTo(3);
        assertThat(sorted.getContent()).extracting(Order::getId).containsExactly(second.getId(), third.getId());
        assertThat(orders.findByUserId(user.getId(), PageRequest.of(1, 10,
                List.of(SortOrder.desc("totalAmount")))).getContent())
                .extracting(Order::getId).containsExactly(first.getId(), second.getId());
        assertThat(orders.findAllOrders(PageRequest.of(1, 10,
                List.of(SortOrder.asc("id; DROP TABLE orders")))).getContent())
                .extracting(Order::getId).containsExactly(third.getId(), second.getId(), first.getId());
    }

    @Test
    void staleOrderCannotOverwritePayment() {
        Order created = orders.save(Order.create(1L, new BigDecimal("99.99")));
        Order payment = orders.findById(created.getId()).orElseThrow();
        Order cancellation = orders.findById(created.getId()).orElseThrow();
        payment.pay();
        orders.save(payment);
        cancellation.cancel();
        assertThatThrownBy(() -> orders.save(cancellation))
                .isInstanceOf(OptimisticLockingFailureException.class);
        assertThat(orders.findById(created.getId()).orElseThrow().getStatus()).isEqualTo(Order.OrderStatus.PAID);
        payment.complete();
        orders.save(payment);
        assertThat(orders.findById(created.getId()).orElseThrow().getVersion()).isEqualTo(2);
    }

    @Test
    void eachTransactionManagerRollsBackItsOwnDatabase() {
        new TransactionTemplate(userTransactionManager).executeWithoutResult(status -> {
            users.save(newUser("rolled_back"));
            status.setRollbackOnly();
        });
        assertThat(users.findByName("rolled_back")).isEmpty();
        new TransactionTemplate(orderTransactionManager).executeWithoutResult(status -> {
            orders.save(Order.create(1L, new BigDecimal("1.00")));
            status.setRollbackOnly();
        });
        assertThat(orders.findAllOrders()).isEmpty();
    }

    private User newUser(String name) {
        return User.register(new UserUniquenessChecker() {
            @Override
            public boolean existsByName(String value) { return users.existsByName(value); }
            @Override
            public boolean existsByEmail(String value) { return users.existsByEmail(value); }
        }, name, name + "@example.com", "13800138001", "wechat_" + name, "杭州");
    }

    private static Order orderAt(String orderNo, long userId, String amount, LocalDateTime at) {
        return Order.restore(null, orderNo, userId, new BigDecimal(amount), Order.OrderStatus.PENDING, 0, at, at);
    }

    private static void initialize(DataSource dataSource, String table, String resource,
                                   String seedMarker, String dialectClause, String replacement) throws Exception {
        String script = new ClassPathResource(resource).getContentAsString(StandardCharsets.UTF_8);
        script = script.substring(0, script.indexOf(seedMarker));
        if (!dialectClause.isEmpty()) {
            script = script.replace(dialectClause, replacement);
        }
        try (var connection = dataSource.getConnection(); var statement = connection.createStatement()) {
            statement.execute("DROP TABLE IF EXISTS " + table);
            ScriptUtils.executeSqlScript(connection, new EncodedResource(
                    new ByteArrayResource(script.getBytes(StandardCharsets.UTF_8)), StandardCharsets.UTF_8));
        }
    }
}
