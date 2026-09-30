package com.hanserwei.springboot4ddd.infrastructure.repository.user;

import com.baomidou.mybatisplus.annotation.FieldStrategy;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 用户数据对象（Data Object）
 *
 * <p>仅承担"与 users 表字段一对一映射"的职责，MyBatis-Plus 的
 * ORM 注解集中在这里，领域模型 {@code User} 不背负持久化细节，
 * 转换由 {@link UserConverter} 显式完成。
 *
 * @author Hanserwei
 * @since 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("users")
public class UserDO {

    /**
     * 主键由数据库自增生成，insert 后自动回填
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    private String name;

    private String email;

    // 可选资料允许显式清空；MP 默认 NOT_NULL 策略会跳过这些更新。
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String phone;

    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String wechat;

    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String address;

    @TableField(updateStrategy = FieldStrategy.NEVER)
    private LocalDateTime createdTime;

    private LocalDateTime updatedTime;
}
