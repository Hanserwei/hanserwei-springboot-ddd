# 定时任务与邮件示例

`OrderScanScheduledTask` 演示任务编排：查询超时未支付订单，通过 `EmailSender` 发送管理员提醒。任务位于应用层，数据库读取使用领域仓储接口，SMTP 实现在基础设施层。

## 配置

```yaml
notification:
  admin-email: admin@example.com
  order-timeout-hours: 2
  order-scan-cron: "0 0 * * * ?"

spring:
  mail:
    host: smtp.qq.com
    port: 587
    username: your-email@qq.com
    password: your-app-password
    properties:
      mail:
        smtp:
          auth: true
          starttls:
            enable: true
```

默认每小时整点运行，查找创建超过两小时、状态为 `PENDING` 的订单。SMTP password 使用邮件服务要求的授权凭据。

## 文件入口

| 文件 | 职责 |
|---|---|
| `Application` | 启用 Spring Scheduling |
| `application/scheduled/OrderScanScheduledTask` | 查询、渲染和发送任务 |
| `application/port/EmailSender` | 邮件能力端口 |
| `infrastructure/notification/EmailService` | SMTP 适配 |
| `templates/email/order-timeout.html` | 提醒邮件模板 |

仓储的 `findExpiredPendingOrders` 通过 MyBatis-Plus Lambda 条件查询状态和创建时间；PostgreSQL 示例表提供对应组合索引。

## 新项目中的使用

替换任务查询、收件人、主题和模板即可编排自己的通知。任务不需要修改聚合状态时，无需为发送邮件持有数据库写事务。

需要多实例调度、通知去重或失败重试时，按实际业务选择相应机制；当前任务每次扫描都可能通知同一批仍未支付的订单。

没有邮件任务需求时，裁剪任务 Bean、模板、邮件实现和 starter。开发自己的任务时，可先验证查询结果和模板内容，再配置实际 SMTP 做一次发送检查。
