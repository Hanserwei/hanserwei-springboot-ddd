package com.hanserwei.springboot4ddd.interfaces.vo.order;

import lombok.Data;

/**
 * 更新订单请求
 *
 * @author Hanserwei
 * @since 1.0.0
 */
@Data
public class UpdateOrderRequest {

    /**
     * 订单状态：PENDING, PAID, CANCELLED, COMPLETED
     */
    private String status;
}
