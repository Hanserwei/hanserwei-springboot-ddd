package com.hanserwei.springboot4ddd.infrastructure.repository.user;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;

/**
 * 用户 MyBatis-Plus Mapper 接口
 *
 * <p>持久化层只与 {@link UserDO} 打交道，不引用领域模型 {@code User}。
 * 全部查询均可通过 {@link BaseMapper} 内置方法 + QueryWrapper 条件构造完成，
 * 无需手写 SQL。
 *
 * <p>由 {@code MybatisPlusConfig} 的 {@code @MapperScan} 注册，
 * 绑定用户数据源（MySQL）的 SqlSessionTemplate。
 *
 * @author Hanserwei
 * @since 1.0.0
 */
public interface UserMybatisPlusMapper extends BaseMapper<UserDO> {
}
