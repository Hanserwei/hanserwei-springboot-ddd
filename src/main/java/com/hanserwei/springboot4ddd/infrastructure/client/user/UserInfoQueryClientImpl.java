package com.hanserwei.springboot4ddd.infrastructure.client.user;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.hanserwei.springboot4ddd.domain.client.user.UserBriefInfo;
import com.hanserwei.springboot4ddd.domain.client.user.UserInfoQueryClient;
import com.hanserwei.springboot4ddd.infrastructure.repository.user.UserDO;
import com.hanserwei.springboot4ddd.infrastructure.repository.user.UserMybatisPlusMapper;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 用户简介防腐层适配器：通过用户 Mapper 批量读取 id/name/phone。
 * 订单上下文只依赖 UserInfoQueryClient，不接触用户 DO 和 Mapper。
 */
@Component
public class UserInfoQueryClientImpl implements UserInfoQueryClient {

    private final UserMybatisPlusMapper userMapper;

    public UserInfoQueryClientImpl(UserMybatisPlusMapper userMapper) {
        this.userMapper = userMapper;
    }

    @Override
    public Map<Long, UserBriefInfo> findBriefs(Collection<Long> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return Collections.emptyMap();
        }
        Set<Long> distinct = userIds.stream().filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        if (distinct.isEmpty()) {
            return Collections.emptyMap();
        }
        LambdaQueryWrapper<UserDO> query = new LambdaQueryWrapper<UserDO>()
                .select(UserDO::getId, UserDO::getName, UserDO::getPhone)
                .in(UserDO::getId, distinct);
        Map<Long, UserBriefInfo> result = new HashMap<>(distinct.size());
        for (UserDO user : userMapper.selectList(query)) {
            result.put(user.getId(), new UserBriefInfo(user.getId(), user.getName(), user.getPhone()));
        }
        return result;
    }
}
