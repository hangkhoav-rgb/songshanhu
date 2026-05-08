## 高德地图配置说明

请在前端根目录的 `.env.development` / `.env.production` 中填写如下变量（不要提交真实值到仓库，可参考 `.env.example`）：

- `VITE_AMAP_KEY`：高德 JS API Key（浏览器端）
- `VITE_AMAP_SECURITY_JS_CODE`：安全密钥（jscode，用于 JS API 安全）

说明：
- 本项目通过运行时注入的方式加载 SDK（见 `src/utils/amap.ts`），不会在仓库中写死 Key 与安全密钥
- 若你需要在 CI 注入，请在构建前导出这两个变量，Vite 会将其编译进产物

校验：
- 启动前端后访问 `/dashboard`，地图底图加载且可缩放拖拽
- DevTools Network 中 `https://webapi.amap.com/maps` 返回 200

常见错误：
- USERKEY_PLAT_NOMATCH：Key 未配置允许的域名白名单或安全密钥不匹配
- 401/403：Key 无效或域名未授权

