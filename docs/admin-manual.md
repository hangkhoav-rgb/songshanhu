## 管理员操作手册

### 1. 系统概览

平台采用前后端分离架构：
- 前端：Vue 3 + Vite + Element Plus + Pinia
- 后端：Spring Boot + Spring Security + MyBatis-Plus
- 可视化：ECharts
- 地图：高德地图 JS API（需要配置 Key 与安全密钥）

### 2. 数据看板

#### 2.1 数据来源

数据看板接口：
- `GET /api/stats/dashboard`（需登录）

数据来源与计算逻辑：
- 总文章数：按当前用户 `author_id` 统计已发布文章数量（`status = 1`）
- 总阅读量/点赞/评论：对文章表中 `views/likes/comments` 字段聚合求和
- 分类分布：对文章表 `category` 进行分组计数
- 近 7 天趋势：按文章 `create_time` 统计每日发布数，并补全缺失日期
- 地图点位：取文章 `longitude/latitude` 非空的记录，输出为点位列表

#### 2.2 更新机制

- 前端在进入数据看板页面时拉取一次数据（页面刷新/重新进入会重新拉取）
- 如需“实时刷新”，建议在前端增加定时轮询（例如 30s）或后端 WebSocket（当前未实现）

#### 2.3 常见问题

- 看板接口返回“请先登录”：说明请求未携带 JWT Token 或 Token 过期，请重新登录
- 地图不显示：请检查 `VITE_AMAP_KEY` 与 `VITE_AMAP_SECURITY_JS_CODE` 是否已配置，且网络可访问高德资源

### 3. 内容管理（文章/评论）

#### 3.1 文章列表与搜索

公开接口（无需登录）：
- `GET /api/article/list?current=1&size=12&category=tech&keyword=xxx`

说明：
- `category` 支持：`all/tech/life/visual/other`
- `keyword` 会对标题与摘要做模糊匹配

#### 3.2 文章发布

需登录接口：
- `POST /api/article/publish`

请求字段（示例）：
- `title`：标题
- `content`：富文本 HTML
- `summary`：摘要（可选；为空时后端会从正文自动截取）
- `coverImage`：封面图 URL（可选）
- `category`：分类（可选）
- `longitude/latitude`：文章关联坐标（可选）

内容审核：
- 后端发布流程会进行关键词合规检查（基础版），命中违禁词会被拒绝发布
- 违禁词配置位于后端 `ContentModerator` 中，可按学校/平台规范补充

#### 3.3 评论管理

公开接口（无需登录）：
- `GET /api/comment/list/{articleId}`

需登录接口：
- `POST /api/comment/add`

建议的管理扩展（当前未实现）：
- 评论分页、删除、屏蔽
- 文章审核状态（待审/通过/驳回）
- 敏感词库持久化（数据库/Redis）与审计日志

### 4. 权限管理与配置方法

#### 4.1 当前权限策略

后端基于 Spring Security + JWT：
- 登录注册相关：匿名可访问
- 文章列表/详情、评论列表、公开展示：匿名可访问
- 数据看板：需要登录
- 发布文章、发表评论：需要登录

#### 4.2 角色与扩展建议

用户表包含 `role` 字段（示例：`ROLE_USER/ROLE_ADMIN`），但当前未对不同角色做强约束。

如需启用管理员权限控制，建议：
- 在 `SecurityConfig` 中对管理接口加 `hasRole("ADMIN")` 或 `hasAuthority("ROLE_ADMIN")`
- 为管理端新增专用 controller 路径（例如 `/admin/**`）
- 管理端前端路由守卫：基于 `userStore.role` 控制入口与菜单

### 5. 独立管理员后台（songshanhu-admin）

本项目引入独立的 RuoYi 管理后台（目录：`songshanhu-admin`），管理员仅通过该后台登录操作，复用 RuoYi 的 RBAC、菜单与操作日志。

对接方式：RuoYi 后端直连业务库 `songshanhu_blog`，直接管理业务表 `user`、`article`，并新增内容治理相关表。

需要初始化 SQL（按顺序执行）：
- `songshanhu-admin/sql/ry_20260417.sql`（RuoYi 系统表/权限/菜单基础数据）
- `songshanhu-admin/sql/quartz.sql`（定时任务相关表）
- `songshanhu-admin/sql/biz_content_governance.sql`（内容治理：用户封禁/文章审核/敏感词 + 菜单/角色/perms）

后台新增业务模块：
- 用户管理：`/biz/user/*`（perms：`biz:user:list/query/ban/unban`）
- 文章管理/审核：`/biz/article/*`（perms：`biz:article:list/query/review/offline`）
- 敏感词管理：`/biz/sensitive/*`（perms：`biz:sensitive:list/query/add/edit/remove`）

说明：
- 用户封禁通过 `biz_user_punish_log` 生效；业务端登录/鉴权会校验封禁状态，被封禁账号无法登录且携带旧 Token 会被拒绝。
- 文章发布时会读取 `biz_sensitive_word` 做敏感词检测：命中 L3 直接拦截；命中 L2 会自动写入 `biz_article_review` 为 `PENDING`，供后台“文章审核”处理。

### 6. 演示数据导入（仅开发/演示环境）

接口：
- `POST /api/seed/articles`

说明：
- 用于快速生成演示文章与封面、阅读量等数据
- 生产环境建议关闭该接口或改为仅管理员可访问
