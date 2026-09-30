package com.hanserwei.springboot4ddd.infrastructure.repository.order;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 订单 MyBatis-Plus Mapper 接口
 *
 * <p>持久化层只与 {@link OrderDO} 打交道，不引用领域模型 {@code Order}。
 * 通用 CRUD（insert / updateById / selectById / deleteById / selectPage 等）
 * 直接继承 {@link BaseMapper}，由 MyBatis-Plus 按注解元数据生成 SQL；
 * 仅非典型查询手写 {@code @Select}。
 *
 * <p>由 {@code MybatisPlusConfig} 的 {@code @MapperScan} 注册，
 * 绑定订单数据源（PostgreSQL）的 SqlSessionTemplate。
 *
 * @author Hanserwei
 * @since 1.0.0
 */
public interface OrderMybatisPlusMapper extends BaseMapper<OrderDO> {

    /**
     * 按订单号查询
     */
    @Select("SELECT * FROM orders WHERE order_no = #{orderNo}")
    Optional<OrderDO> findByOrderNo(@Param("orderNo") String orderNo);

    /**
     * 按用户ID查询全部订单（按创建时间倒序）
     */
    @Select("SELECT * FROM orders WHERE user_id = #{userId} ORDER BY created_at DESC")
    List<OrderDO> findByUserId(@Param("userId") Long userId);

    /**
     * 按用户ID分页查询订单。
     *
     * <p>传入 {@link Page} 参数后，{@code PaginationInnerInterceptor} 会按方言
     * 自动追加 LIMIT/OFFSET 并执行 COUNT，无需手写分页 SQL。
     */
    @Select("SELECT * FROM orders WHERE user_id = #{userId} ORDER BY created_at DESC")
    Page<OrderDO> findPageByUserId(@Param("userId") Long userId, Page<OrderDO> page);

    /**
     * 查询指定状态且创建时间早于阈值的订单（超时扫描用，按创建时间正序）
     */
    @Select("SELECT * FROM orders WHERE status = #{status} AND created_at < #{createdAtBefore} " +
            "ORDER BY created_at ASC")
    List<OrderDO> findByStatusAndCreatedAtBefore(@Param("status") String status,
                                                 @Param("createdAtBefore") LocalDateTime createdAtBefore);
}
