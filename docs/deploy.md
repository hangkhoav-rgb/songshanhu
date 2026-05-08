## 部署说明

### 1. 环境要求
- Java 17、Maven 3.9+
- Node 18+
- MySQL 8.0、Redis 7.2

### 2. 数据库初始化
- 执行后端资源目录下的 SQL 脚本：
  - `backend/songshanhu-blog-backend/src/main/resources/db/schema.sql`
- 根据需要创建管理员账号（可在启动后通过接口注册）

如你的数据库已存在且需要补充地理坐标字段，请执行：
```sql
ALTER TABLE article
  ADD COLUMN longitude DOUBLE NULL COMMENT '经度',
  ADD COLUMN latitude  DOUBLE NULL COMMENT '纬度';
```

### 3. 后端配置
编辑 `backend/songshanhu-blog-backend/src/main/resources/application.yaml`：
- 确认 `spring.datasource.url/username/password`
- `server.servlet.context-path=/api`
- `jwt.secret` 建议替换为生产安全值
- 配置头像签名密钥（生产建议通过环境变量注入）：
  - `AVATAR_SIGNING_SECRET=<random-strong-secret>`

启动后端：
```bash
mvn -f backend/songshanhu-blog-backend/pom.xml spring-boot:run
```

### 4. 前端配置
编辑环境变量：
- `frontend/.env.production`
  - `VITE_API_BASE_URL=https://<your-domain>/api`
  - `VITE_AMAP_KEY=<your-amap-key>`
  - `VITE_AMAP_SECURITY_JS_CODE=<your-security-js-code>`

构建与预览：
```bash
cd frontend
npm ci
npm run build
npm run preview
```

### 5. Nginx 部署建议
示例：
```
server {
  listen 80;
  server_name <your-domain>;

  root /var/www/songshanhu-frontend;
  index index.html;

  location / {
    try_files $uri $uri/ /index.html;
  }

  location /api/ {
    proxy_pass http://127.0.0.1:8080/api/;
    proxy_set_header Host $host;
    proxy_set_header X-Real-IP $remote_addr;
  }
}
```

### 6. 生产注意事项
- 关闭测试/演示相关接口（如数据种子）
- 配置 HTTPS（TLS）
- 开启日志轮转与监控
- 数据库定期备份（MySQL/Redis）
