# API 签名示例

接口通过 `@RequireSign` 启用签名检查。调用方和权限定义在 `apiauth-config.yaml`，密钥通过环境变量提供。

订单示例的支付、取消、完成和删除需要签名。user/order 的其他接口是否启用签名，由新项目按调用场景配置。

## 算法与路径

不含参数时：

```text
source = appCode + secretKey + path + timestamp
sign = SHA256(source)
```

含参数时，先过滤空值和复杂类型，再将标量参数按名称排序并拼接：

```text
source = key1=value1&key2=value2 + appCode + secretKey + path + timestamp
sign = SHA256(source)
```

路径使用 Spring MVC 匹配到的模板，例如 `/api/orders/{id}/pay`。实际 URL 可以是 `/api/orders/1/pay`，签名时仍使用模板路径。服务端自行解析路径，不依赖 `Sign-path` 请求头。

## 请求头

| Header | 含义 |
|---|---|
| `Sign-appCode` | 配置中的调用方标识 |
| `Sign-time` | 当前毫秒时间戳 |
| `Sign-sign` | SHA-256 十六进制签名 |

时间窗口使用 `sign.signature.ttl`，默认 10 分钟。

## 调用支付示例

以下 Python 示例使用已配置的 ios1 调用方，将订单 ID 替换为自己的待支付订单：

```python
import hashlib
import os
import time
import urllib.request

app_code = "ios1"
secret = os.environ["API_AUTH_IOS_SECRET"]
path = "/api/orders/{id}/pay"
timestamp = str(int(time.time() * 1000))
sign = hashlib.sha256((app_code + secret + path + timestamp).encode()).hexdigest()
request = urllib.request.Request(
    "http://localhost:8080/api/orders/1/pay",
    method="POST",
    headers={"Sign-appCode": app_code, "Sign-time": timestamp, "Sign-sign": sign},
)
with urllib.request.urlopen(request) as response:
    print(response.read().decode())
```

## 注解与配置

```java
@PostMapping("/{id}/pay")
@RequireSign(withParams = WithParams.FALSE)
public ApiResponse<OrderDTO> payOrder(@PathVariable Long id) {
    return ApiResponse.success(orderService.payOrder(id));
}
```

注解可以放在方法或类上；方法配置优先。`@IgnoreSignHeader` 可跳过类级别签名要求。`WithParams.DEFAULT` 使用 `sign.signature.default-with-params`，默认 false。

```yaml
sign:
  signature:
    ttl: 600000
    default-with-params: false
  allow-cached-body: true
  cached-body-path-patterns:
    - /api/orders/**

apiauth:
  apps:
    - appCode: ios1
      appName: local-example
      secretKey: ${API_AUTH_IOS_SECRET}
      permissions:
        - "/api/orders/{id}/pay"
```

权限路径应与 Controller 模板一致。需要把请求体纳入签名时，配置相应请求体缓存路径。

新项目应按自身客户端协议使用签名示例，并实现自己的用户认证、授权和请求重放处理。异常信息包括缺少请求头、时间戳错误、签名过期、未知调用方、无接口权限和签名不匹配。
