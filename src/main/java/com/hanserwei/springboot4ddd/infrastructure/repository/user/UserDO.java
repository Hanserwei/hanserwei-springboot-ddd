package com.hanserwei.springboot4ddd.infrastructure.repository.user;

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
 * <p>当前数据库未包含 {@code wechat} 列，DO 保留该字段供未来扩展，
 * 通过 {@code @TableField(exist = false)} 告知 MyBatis-Plus 不参与 SQL。
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

    private String phone;

    /**
     * 表中暂无此列，不参与生成的 SQL
     */
    @TableField(exist = false)
    private String wechat;

    private String address;

    private LocalDateTime createdTime;

    private LocalDateTime updatedTime;
}
