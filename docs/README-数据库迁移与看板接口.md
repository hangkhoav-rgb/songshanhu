# 数据库迁移与看板接口（本地/测试/生产）

## 1. 解决 `article_like/article_collect` 表不存在

项目已接入 Flyway，后端启动时会自动执行迁移脚本，确保 `songshanhu_blog` 库中存在：
- `article_like`
- `article_collect`

迁移脚本位置：`backend/songshanhu-blog-backend/src/main/resources/db/migration/`
- `V1__baseline.sql`
- `V2__article_actions.sql`
- `V3__password_policy.sql`

执行顺序按版本号自动排序，无需手工指定。

## 2. 启动与迁移（本地/生产）

确保 MySQL 中已存在 `songshanhu_blog` 数据库，并在 `application.yaml` 中配置正确的账号密码。

在 `backend/songshanhu-blog-backend` 目录启动：

```powershell
mvn -DskipTests spring-boot:run "-Dspring-boot.run.arguments=--server.port=8082"
```

首次启动时 Flyway 会自动建表；后续升级只需提交新的 `V{n}__*.sql` 即可。

## 3. 测试环境

单元测试使用 `application-test.yaml`（H2 内存库），并关闭 Flyway；需要的表由测试用例 `@Sql` 自建。

运行测试：

```powershell
cd backend/songshanhu-blog-backend
mvn test
```

## 4. 看板接口

已新增接口：
- `GET /api/dashboard/summary`：返回当前用户 `likeCount/collectCount/lastActionTime`（summary 缓存 5min，Cache-Aside）
- `GET /api/dashboard/actions`：分页返回最近 N 天的点赞/收藏记录（默认 30 天），可用 `actionType=liked|collected` 筛选

## 5. 密码策略与历史账号兼容

密码策略调整为：6–32 位。

历史弱密码账号（数据库中存储为明文且长度 < 6）登录时不会发放正常 token，接口会返回：
- `mustResetPassword=true`
- `resetToken=<first_reset token>`

前端会强制跳转到 `/first-reset`，调用 `POST /api/auth/first-reset` 完成首次重置后再登录。

## 6. Postman

Postman 集合：`docs/postman/songshanhu-blog.postman_collection.json`

环境变量：
- `baseUrl`：例如 `http://localhost:8082`
- `token`：登录后自动写入
- `firstResetToken`：首次重置场景自动写入

