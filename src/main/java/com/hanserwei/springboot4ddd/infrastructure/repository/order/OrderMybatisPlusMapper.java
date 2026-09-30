package com.hanserwei.springboot4ddd.infrastructure.repository.order;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;

/** 订单持久化 Mapper，由 MybatisPlusConfig 绑定 PostgreSQL 会话模板。 */
public interface OrderMybatisPlusMapper extends BaseMapper<OrderDO> {
}
