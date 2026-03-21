# CIDI 错误码映射

本文件定义后端统一错误码与 HTTP 状态码的对应关系。

## 错误响应格式

```json
{
  "timestamp": "2026-03-21T10:00:00",
  "status": 400,
  "code": "REQ-400",
  "message": "请求参数错误",
  "path": "/api/example"
}
```

字段说明：

- `timestamp`: 错误产生时间
- `status`: HTTP 状态码
- `code`: 业务错误码（稳定字段，供前端与日志系统识别）
- `message`: 错误描述（可包含具体字段校验信息）
- `path`: 请求路径

## 错误码清单

| 错误码 | 名称 | HTTP 状态码 | 说明 |
| --- | --- | --- | --- |
| SYS-500 | INTERNAL_SERVER_ERROR | 500 | 未捕获的服务器内部错误 |
| REQ-400 | BAD_REQUEST | 400 | 通用请求参数错误、请求体不可解析 |
| REQ-422 | VALIDATION_FAILED | 422 | Bean Validation 校验失败 |
| REQ-401 | UNAUTHORIZED | 401 | 未授权访问 |
| REQ-403 | FORBIDDEN | 403 | 权限不足 |
| REQ-404 | RESOURCE_NOT_FOUND | 404 | 资源不存在 |
| REQ-405 | METHOD_NOT_ALLOWED | 405 | HTTP 方法不被允许 |
| REQ-415 | UNSUPPORTED_MEDIA_TYPE | 415 | 不支持的请求内容类型 |
| WX-502 | WECHAT_API_ERROR | 502 | 微信接口调用失败 |
| WX-503 | WECHAT_IDENTIFIER_MISSING | 502 | 微信接口未返回 unionid/openid |

## 使用约定

- 业务层建议抛出 `CidiException` 并指定 `ErrorCode`，由全局异常处理器统一封装返回。
- 非业务可预期异常默认归类为 `SYS-500`。
- 前端请优先根据 `code` 做错误分流，不要仅依赖 `message`。
