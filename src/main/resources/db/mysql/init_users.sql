-- MySQL 8.0.16+：在已创建的 frog 数据库中执行。
-- 用户业务示例的表结构和本地演示数据。
CREATE TABLE IF NOT EXISTS users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '用户ID',
    name VARCHAR(50) NOT NULL COMMENT '用户名',
    email VARCHAR(254) NOT NULL COMMENT '邮箱',
    phone VARCHAR(20) COMMENT '手机号',
    wechat VARCHAR(64) COMMENT '微信号',
    address VARCHAR(255) COMMENT '地址',
    created_time DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) COMMENT '创建时间',
    updated_time DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) COMMENT '更新时间，由聚合行为维护',
    CONSTRAINT uk_users_name UNIQUE (name),
    CONSTRAINT uk_users_email UNIQUE (email),
    CONSTRAINT ck_users_name CHECK (CHAR_LENGTH(TRIM(name)) > 0),
    CONSTRAINT ck_users_email CHECK (CHAR_LENGTH(TRIM(email)) > 0),
    INDEX idx_users_created (created_time DESC, id DESC)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户表';

-- 重复执行不新增样例用户；唯一键冲突时保留现有数据。
INSERT INTO users (name, email, phone, wechat, address) VALUES
('user1', 'user1@example.com', '13800138001', 'wechat_user1', '杭州'),
('user2', 'user2@example.com', '13800138002', NULL, '上海'),
('user3', 'user3@example.com', '13800138003', NULL, NULL)
ON DUPLICATE KEY UPDATE name = name;
