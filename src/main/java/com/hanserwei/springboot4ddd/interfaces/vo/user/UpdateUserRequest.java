package com.hanserwei.springboot4ddd.interfaces.vo.user;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 更新用户请求DTO
 *
 * @author Hanserwei
 * @since 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateUserRequest {

    @Email(message = "邮箱格式不正确")
    @Size(max = 254, message = "邮箱最多支持 254 个字符")
    private String email;

    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
    private String phone;

    @Size(max = 64, message = "微信号最多支持 64 个字符")
    private String wechat;

    @Size(max = 255, message = "地址最多支持 255 个字符")
    private String address;
}
