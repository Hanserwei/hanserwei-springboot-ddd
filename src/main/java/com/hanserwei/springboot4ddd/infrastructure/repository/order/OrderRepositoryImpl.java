package com.hanserwei.springboot4ddd.infrastructure.repository.order;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.hanserwei.springboot4ddd.domain.model.order.Order;
import com.hanserwei.springboot4ddd.domain.page.PageRequest;
import com.hanserwei.springboot4ddd.domain.page.PageResult;
import com.hanserwei.springboot4ddd.domain.repository.order.OrderRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 订单仓储实现（MyBatis-Plus）
 *
 * <p>对外实现领域定义的 {@link OrderRepository}（操作 {@code Order} + 领域分页类型），
 * 对内通过 {@link OrderMybatisPlusMapper} 读写 {@link OrderDO}，
 * 两种模型的边界转换由 {@link OrderConverter} 显式完成。
 *
 * <p>注意：Repository 层不管理事务，事务由 Service 层控制。
 *
 * @author Hanserwei
 * @since 1.0.0
 */
@Repository
public class OrderRepositoryImpl implements OrderRepository {

    private final OrderMybatisPlusMapper orderMapper;

    public OrderRepositoryImpl(OrderMybatisPlusMapper orderMapper) {
        this.orderMapper = orderMapper;
    }

    @Override
    public Order save(Order order) {
        OrderDO orderDO = OrderConverter.toDO(order);
        if (orderDO.getId() == null) {
            // IdType.AUTO：insert 后主键自动回填到 orderDO.id
            orderMapper.insert(orderDO);
            order.markCreated(orderDO.getId());
        } else {
            orderMapper.updateById(orderDO);
        }
        return order;
    }

    @Override
    public Optional<Order> findById(Long id) {
        return Optional.ofNullable(orderMapper.selectById(id))
                .map(OrderConverter::toModel);
    }

    @Override
    public Optional<Order> findByOrderNo(String orderNo) {
        return orderMapper.findByOrderNo(orderNo)
                .map(OrderConverter::toModel);
    }

    @Override
    public List<Order> findByUserId(Long userId) {
        return OrderConverter.toModelList(orderMapper.findByUserId(userId));
    }

    @Override
    public PageResult<Order> findByUserId(Long userId, PageRequest pageRequest) {
        Page<OrderDO> page = orderMapper.findPageByUserId(userId, toMpPage(pageRequest));
        return toPageResult(page, pageRequest);
    }

    @Override
    public List<Order> findAllOrders() {
        return OrderConverter.toModelList(orderMapper.selectList(null));
    }

    @Override
    public PageResult<Order> findAllOrders(PageRequest pageRequest) {
        Page<OrderDO> page = orderMapper.selectPage(toMpPage(pageRequest), null);
        return toPageResult(page, pageRequest);
    }

    @Override
    public void deleteById(Long id) {
        orderMapper.deleteById(id);
    }

    @Override
    public List<Order> findExpiredPendingOrders(LocalDateTime createdBefore) {
        return OrderConverter.toModelList(
                orderMapper.findByStatusAndCreatedAtBefore(
                        Order.OrderStatus.PENDING.name(), createdBefore));
    }

    /**
     * 领域分页（pageNumber 从 1 开始）→ MyBatis-Plus 分页（current 从 1 开始），可直接透传
     */
    private static Page<OrderDO> toMpPage(PageRequest pageRequest) {
        return new Page<>(pageRequest.getPageNumber(), pageRequest.getPageSize());
    }

    /**
     * MyBatis-Plus 分页结果 → 领域分页结果
     */
    private static PageResult<Order> toPageResult(Page<OrderDO> page, PageRequest pageRequest) {
        return new PageResult<>(
                OrderConverter.toModelList(page.getRecords()),
                page.getTotal(),
                pageRequest.getPageNumber(),
                pageRequest.getPageSize()
        );
    }
}
