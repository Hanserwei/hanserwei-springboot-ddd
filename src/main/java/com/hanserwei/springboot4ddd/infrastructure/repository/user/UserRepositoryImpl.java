package com.hanserwei.springboot4ddd.infrastructure.repository.user;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.hanserwei.springboot4ddd.domain.model.user.User;
import com.hanserwei.springboot4ddd.domain.page.PageRequest;
import com.hanserwei.springboot4ddd.domain.page.PageResult;
import com.hanserwei.springboot4ddd.domain.page.SortOrder;
import com.hanserwei.springboot4ddd.domain.repository.user.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 用户仓储实现（MyBatis-Plus）- MySQL 数据源
 *
 * <p>对外实现领域定义的 {@link UserRepository}（操作 {@code User} + 领域分页类型），
 * 对内通过 {@link UserMybatisPlusMapper} 读写 {@link UserDO}，
 * 两种模型的边界转换由 {@link UserConverter} 显式完成。
 *
 * <p>注意：Repository 层不管理事务，事务由 Service 层控制。
 *
 * @author Hanserwei
 * @since 1.0.0
 */
@Slf4j
@Repository
public class UserRepositoryImpl implements UserRepository {

    /**
     * 可排序字段白名单：领域属性名 → users 表列名。
     * 排序字段来自 HTTP 入参，必须经白名单映射，防止 SQL 注入。
     */
    private static final Map<String, String> SORTABLE_COLUMNS = Map.of(
            "id", "id",
            "name", "name",
            "email", "email",
            "phone", "phone",
            "address", "address",
            "createdTime", "created_time",
            "updatedTime", "updated_time"
    );

    private final UserMybatisPlusMapper userMapper;

    public UserRepositoryImpl(UserMybatisPlusMapper userMapper) {
        this.userMapper = userMapper;
    }

    @Override
    public User save(User user) {
        UserDO userDO = UserConverter.toDO(user);
        LocalDateTime now = LocalDateTime.now();
        if (userDO.getCreatedTime() == null) {
            userDO.setCreatedTime(now);
        }
        userDO.setUpdatedTime(now);
        // IdType.AUTO：insert 后主键自动回填到 userDO.id
        userMapper.insert(userDO);
        user.markPersisted(userDO.getId());
        log.info("User saved with id: {}", user.getId());
        return user;
    }

    @Override
    public Optional<User> findById(Long id) {
        return Optional.ofNullable(userMapper.selectById(id))
                .map(UserConverter::toModel);
    }

    @Override
    public Optional<User> findByName(String name) {
        return Optional.ofNullable(userMapper.selectOne(
                        new LambdaQueryWrapper<UserDO>().eq(UserDO::getName, name)))
                .map(UserConverter::toModel);
    }

    @Override
    public Optional<User> findByEmail(String email) {
        return Optional.ofNullable(userMapper.selectOne(
                        new LambdaQueryWrapper<UserDO>().eq(UserDO::getEmail, email)))
                .map(UserConverter::toModel);
    }

    @Override
    public List<User> findAll() {
        return UserConverter.toModelList(userMapper.selectList(
                new LambdaQueryWrapper<UserDO>().orderByDesc(UserDO::getCreatedTime)));
    }

    @Override
    public PageResult<User> findAll(PageRequest pageRequest) {
        // 排序列来自 HTTP 入参，必须经白名单映射为列名；
        // 无显式排序时按创建时间倒序，保证分页结果稳定。
        // 注：动态列名只能用 QueryWrapper（String 列名），LambdaQueryWrapper 仅支持编译期字段引用。
        QueryWrapper<UserDO> wrapper = new QueryWrapper<>();
        if (pageRequest.hasSort()) {
            applySorts(wrapper, pageRequest.getSorts());
        } else {
            wrapper.orderByDesc("created_time");
        }

        Page<UserDO> page = userMapper.selectPage(
                new Page<>(pageRequest.getPageNumber(), pageRequest.getPageSize()), wrapper);

        return new PageResult<>(
                UserConverter.toModelList(page.getRecords()),
                page.getTotal(),
                pageRequest.getPageNumber(),
                pageRequest.getPageSize()
        );
    }

    @Override
    public User update(User user) {
        UserDO userDO = UserConverter.toDO(user);
        userDO.setUpdatedTime(LocalDateTime.now());

        int updated = userMapper.updateById(userDO);
        if (updated == 0) {
            throw new IllegalArgumentException("User not found with id: " + userDO.getId());
        }
        log.info("User updated with id: {}", user.getId());
        return user;
    }

    @Override
    public void deleteById(Long id) {
        int deleted = userMapper.deleteById(id);
        if (deleted == 0) {
            throw new IllegalArgumentException("User not found with id: " + id);
        }
        log.info("User deleted with id: {}", id);
    }

    @Override
    public boolean existsByName(String name) {
        return userMapper.selectCount(
                new LambdaQueryWrapper<UserDO>().eq(UserDO::getName, name)) > 0;
    }

    @Override
    public boolean existsByEmail(String email) {
        return userMapper.selectCount(
                new LambdaQueryWrapper<UserDO>().eq(UserDO::getEmail, email)) > 0;
    }

    /**
     * 将领域排序条件映射为 SQL 排序（经白名单过滤，未知属性直接忽略）
     */
    private void applySorts(QueryWrapper<UserDO> wrapper, List<SortOrder> sorts) {
        for (SortOrder sort : sorts) {
            String column = SORTABLE_COLUMNS.get(sort.getProperty());
            if (column == null) {
                log.debug("忽略未知排序字段: {}", sort.getProperty());
                continue;
            }
            wrapper.orderBy(true, sort.getDirection() == SortOrder.Direction.ASC, column);
        }
    }
}
