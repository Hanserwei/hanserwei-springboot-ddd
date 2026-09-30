package com.hanserwei.springboot4ddd.interfaces.vo.order;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 创建订单请求
 *
 * @author Hanserwei
 * @since 1.0.0
 */
@Data
public class CreateOrderRequest {

    /**
     * 用户ID
     */
    @NotNull(message = "用户 ID 不能为空")
    @Positive(message = "用户 ID 必须为正数")
    private Long userId;

    /**
     * 订单总金额
     */
    @NotNull(message = "订单金额不能为空")
    @DecimalMin(value = "0.01", message = "订单金额必须至少为 0.01")
    @Digits(integer = 17, fraction = 2, message = "订单金额最多支持 17 位整数、2 位小数")
    private BigDecimal totalAmount;
}
