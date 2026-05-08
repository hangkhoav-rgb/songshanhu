# Frontend（Dashboard & 我的点赞/收藏）

## 本地启动

```bash
cd frontend
npm install
npm run dev -- --host
```

默认前端地址：`http://localhost:3000`  
接口默认通过 Vite 代理转发到后端（`/api`）。

## 环境变量

### API
- `VITE_API_BASE_URL`：默认 `/api`

### 高德地图（可选）
不配置会导致看板地图模块降级为“加载失败占位”，不影响其它功能。
- `VITE_AMAP_KEY`
- `VITE_AMAP_SECURITY_JS_CODE`

## 单元测试（Vitest）

```bash
npm run test:unit
npm run test:unit:coverage
```

覆盖率报告输出到 `frontend/coverage/`。

## E2E（Playwright）

首次运行需要安装浏览器：

```bash
npx playwright install
```

运行：

```bash
npm run test:e2e
```

### E2E 环境变量
- `E2E_BASE_URL`：默认 `http://localhost:3000`
- `E2E_USER` / `E2E_PASS`：可选。未提供时测试会自动注册随机账号并写入本地登录态
- `E2E_NEW_PASS`：首次重置密码场景使用的密码（默认 `123456`）

### 录屏/报告
Playwright 配置已开启视频录制，产物位于 `frontend/test-results/`（失败用例也会包含截图与错误上下文）。

## 接口 Mock 说明

当前 E2E 用例默认直连本地后端（通过 Vite 代理 `/api`）。如需 Mock：
- 可在 Playwright 用例中使用 `page.route('**/api/**', ...)` 对请求进行拦截与响应替换

