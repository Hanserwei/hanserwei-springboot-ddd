package com.hanserwei.springboot4ddd.domain.model.order;

import com.hanserwei.springboot4ddd.domain.event.DomainEvent;
import com.hanserwei.springboot4ddd.domain.event.order.OrderCancelledEvent;
import com.hanserwei.springboot4ddd.domain.event.order.OrderCompletedEvent;
import com.hanserwei.springboot4ddd.domain.event.order.OrderCreatedEvent;
import com.hanserwei.springboot4ddd.domain.event.order.OrderPaidEvent;
import lombok.Getter;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

/**
 * 订单领域模型（聚合根）
 *
 * <p>纯领域模型，零持久化注解、零框架依赖。状态字段直接持有 {@link OrderStatus}，
 * 只能通过 {@link #pay()} / {@link #cancel()} / {@link #complete()} 等行为方法
 * 迁移，不暴露任何 setter。
 *
 * <p>持久化重建走 {@link #restore} 静态工厂；数据库自增主键回填与创建事件补登
 * 合并为 {@link #markCreated(Long)}，仅供 infrastructure 层在 save 完成后调用一次。
 *
 * @author Hanserwei
 * @since 1.0.0
 */
@Getter
public class Order {

    private Long id;
    private final String orderNo;
    private final Long userId;
    private final BigDecimal totalAmount;
    private OrderStatus status;
    private int version;
    private final LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    private final List<DomainEvent> domainEvents = new ArrayList<>();

    public enum OrderStatus {
        PENDING("待支付"),
        PAID("已支付"),
        CANCELLED("已取消"),
        COMPLETED("已完成");

        private final String description;

        OrderStatus(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }
    }

    Order(Long id,
          String orderNo,
          Long userId,
          BigDecimal totalAmount,
          OrderStatus status,
          int version,
          LocalDateTime createdAt,
          LocalDateTime updatedAt) {
        this.id = id;
        this.orderNo = orderNo;
        this.userId = userId;
        this.totalAmount = totalAmount;
        this.status = status;
        this.version = version;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static Order create(Long userId, BigDecimal totalAmount) {
        if (userId == null || userId <= 0) {
            throw new IllegalArgumentException("用户 ID 必须为正数");
        }
        if (totalAmount == null || totalAmount.signum() <= 0) {
            throw new IllegalArgumentException("订单金额必须大于 0");
        }
        BigDecimal amount;
        try {
            amount = totalAmount.setScale(2, RoundingMode.UNNECESSARY);
        } catch (ArithmeticException ex) {
            throw new IllegalArgumentException("订单金额最多支持两位小数", ex);
        }
        if (amount.precision() > 19) {
            throw new IllegalArgumentException("订单金额整数部分最多支持 17 位");
        }
        LocalDateTime now = LocalDateTime.now().truncatedTo(ChronoUnit.MICROS);
        return new Order(null, generateOrderNo(), userId, amount,
                OrderStatus.PENDING, 0, now, now);
    }

    /**
     * 从持久化数据重建订单。仅供 infrastructure 层的 Converter 使用。
     */
    public static Order restore(Long id,
                                String orderNo,
                                Long userId,
                                BigDecimal totalAmount,
                                OrderStatus status,
                                int version,
                                LocalDateTime createdAt,
                                LocalDateTime updatedAt) {
        return new Order(id, orderNo, userId, totalAmount, status, version, createdAt, updatedAt);
    }

    /**
     * 持久化生成主键后由仓储调用一次：回填 id 并补登创建事件。
     */
    public void markCreated(Long generatedId) {
        if (this.id != null) {
            throw new IllegalStateException("订单 ID 已存在，不可重复初始化");
        }
        this.id = generatedId;
        recordEvent(new OrderCreatedEvent(
                this.id,
                this.orderNo,
                this.userId,
                this.totalAmount,
                this.status.name()
        ));
    }

    /** 持久化成功后同步乐观锁版本，支持同一聚合继续执行后续行为。 */
    public void markUpdated(int nextVersion) {
        if (nextVersion != this.version + 1) {
            throw new IllegalStateException("订单版本必须递增一次");
        }
        this.version = nextVersion;
    }

    private static String generateOrderNo() {
        // 毫秒时间戳在同一毫秒内会重复；UUID 支持多线程、多实例创建。
        return "ORD" + UUID.randomUUID().toString().replace("-", "");
    }

    public void cancel() {
        if (this.status != OrderStatus.PENDING) {
            throw new IllegalStateException("只有待支付订单可以取消");
        }
        this.status = OrderStatus.CANCELLED;
        this.updatedAt = LocalDateTime.now().truncatedTo(ChronoUnit.MICROS);

        recordEvent(new OrderCancelledEvent(
                this.id,
                this.orderNo,
                this.userId,
                this.totalAmount,
                this.status.name()
        ));
    }

    public void pay() {
        if (this.status != OrderStatus.PENDING) {
            throw new IllegalStateException("只有待支付订单可以支付");
        }
        this.status = OrderStatus.PAID;
        this.updatedAt = LocalDateTime.now().truncatedTo(ChronoUnit.MICROS);

        recordEvent(new OrderPaidEvent(
                this.id,
                this.orderNo,
                this.userId,
                this.totalAmount,
                this.status.name()
        ));
    }

    public void complete() {
        if (this.status != OrderStatus.PAID) {
            throw new IllegalStateException("只有已支付订单可以完成");
        }
        this.status = OrderStatus.COMPLETED;
        this.updatedAt = LocalDateTime.now().truncatedTo(ChronoUnit.MICROS);

        recordEvent(new OrderCompletedEvent(
                this.id,
                this.orderNo,
                this.userId,
                this.totalAmount,
                this.status.name()
        ));
    }

    private void recordEvent(DomainEvent event) {
        this.domainEvents.add(event);
    }

    public List<DomainEvent> getDomainEvents() {
        return Collections.unmodifiableList(new ArrayList<>(this.domainEvents));
    }

    public void clearDomainEvents() {
        this.domainEvents.clear();
    }
}
