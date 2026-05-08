## 用户个性化资料系统 API 文档

### 概述
- 模块目标：头像上传（裁剪/压缩/多分辨率）、昵称修改（唯一+敏感词）、性别（含保密）、扩展信息
- 安全基线：JWT 认证、服务端签名头像 URL、防重复提交（幂等）、XSS 防护（前端表单与后端校验）

### 数据结构（User 扩展字段）
- `gender`：`male/female/secret`
- `bio`：个人简介（≤200）
- `extra`：JSON 字符串（≤2000）
- `avatar`：头像 key（服务器侧保存）

### 1) 获取个人资料
- 接口：`GET /api/user/profile`
- 认证：需要
- 响应：
```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "id": 1,
    "username": "asd",
    "nickname": "昵称",
    "email": "xx@xx.com",
    "role": "ROLE_USER",
    "gender": "secret",
    "bio": "个人简介",
    "extra": "{\"city\":\"东莞\"}",
    "avatarUrls": {
      "sm": "/api/user/avatar/<file>?exp=...&sig=...",
      "md": "...",
      "lg": "...",
      "orig": "..."
    }
  }
}
```

### 2) 更新个人资料
- 接口：`PUT /api/user/profile`
- 认证：需要
- 幂等：请求头 `Idempotency-Key: <uuid>`（10s 内相同 key 仅一次生效）
- 入参：
```json
{
  "nickname": "新昵称(2-20)",
  "gender": "male|female|secret",
  "bio": "最多200字",
  "extra": "{\"city\":\"东莞\"}"
}
```
- 校验：
  - 昵称：长度 2-20、正则 `^[\\p{L}\\p{N}_\\-·\\s]+$`、敏感词过滤、唯一性校验
  - 性别：枚举
  - 简介/扩展：长度限制
- 响应：`200` 成功，冲突/非法返回 `400/500`

### 3) 上传头像
- 接口：`POST /api/user/avatar`
- 认证：需要
- 表单：`multipart/form-data` 字段 `file`
- 限制：
  - 类型：`image/jpeg|image/png|image/webp`
  - 大小：≤ 2MB
- 后端处理：
  - 居中裁剪正方形，生成多分辨率 `64/128/256/orig`（JPEG）
  - 返回签名 URL（默认 3600s），服务端二次校验 `sig/exp`
- 响应：
```json
{
  "code": 200,
  "data": {
    "sm": "/api/user/avatar/xxx_64.jpg?exp=...&sig=...",
    "md": "...",
    "lg": "...",
    "orig": "..."
  }
}
```

### 4) 访问签名头像
- 接口：`GET /api/user/avatar/{filename}?exp=...&sig=...`
- 认证：匿名可访问（仅签名有效时）
- 说明：到期失效。服务端进行 HMAC-SHA256 校验与到期校验

### 前端组件
- 页面：`/profile`（[Profile.vue](file:///d:/songshanhu-blog-platform/frontend/src/views/Profile.vue)）
  - 客户端压缩与居中裁剪（Canvas）
  - 实时预览与错误提示
  - 保存时携带 `Idempotency-Key` 防重复

### 单元测试/接口测试
- 后端：`UserControllerTest` 覆盖基本场景（[UserControllerTest.java](file:///d:/songshanhu-blog-platform/backend/songshanhu-blog-backend/src/test/java/com/songshanhu/blog/UserControllerTest.java)）
- 建议补充：
  - 并发更新昵称唯一性
  - 头像签名 URL 到期校验
  - 敏感词命中

### 性能基准（目标）
- 个人资料页面首屏（已登录）：≤ 300ms（不含网络）
- 头像上传：≤ 2s（2MB 以内，含服务端处理）

