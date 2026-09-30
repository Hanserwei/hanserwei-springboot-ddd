package com.hanserwei.springboot4ddd.infrastructure.repository.order;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 订单数据对象（Data Object）
 *
 * <p>仅承担"与 orders 表字段一对一映射"的职责，MyBatis-Plus 的
 * ORM 注解（{@code @TableName} / {@code @TableId}）集中在这里，
 * 领域模型 {@code Order} 不背负持久化细节。
 *
 * <p>列名映射依赖 mapUnderscoreToCamelCase（orderNo ↔ order_no），
 * 转换由 {@link OrderConverter} 显式完成。
 *
 * @author Hanserwei
 * @since 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("orders")
public class OrderDO {

    /**
     * 主键由数据库自增生成，insert 后自动回填
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    private String orderNo;

    private Long userId;

    private BigDecimal totalAmount;

    private String status;

    @Version
    private Integer version;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
