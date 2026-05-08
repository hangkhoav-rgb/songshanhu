## 文章图片加载可靠性方案

### 一、监控与上报
- 前端接入监控（已实现）：
  - 封面图与正文 `<img>` 绑定 `error` 事件，降级到占位图并上报
  - 上报接口：`POST /api/monitor/image-error`
  - 字段：`url/page/userAgent/referrer/message`
- 后端落日志（WARN），便于 ELK/Promtail 收集并做看板

### 二、常见失败原因与修复方案
1) 静态资源域名错误
   - 表现：DNS 失败、404
   - 方案：统一资源域名配置；将白名单域名集中在 CDN/反代层；替换错误域名
2) CDN 回源失败（5xx）
   - 表现：间歇性 5xx
   - 方案：增加回源重试/超时，设置合适缓存策略；源站流量限速时开启分发缓存
3) 权限 403（Referer 限制/Token 失效）
   - 表现：跨域加载被拒
   - 方案：将前端域名加入 Referer 白名单；对需要鉴权的图片改走签名 URL（同头像方案）
4) 格式不支持（仅 avif/webp）
   - 表现：Safari/旧浏览器不显示
   - 方案：提供后备格式（`<picture>` 或后端转码成 jpeg）；服务端检测 UA 降级
5) 懒加载脚本/阈值计算异常
   - 表现：首屏滚动后仍未加载
   - 方案：使用原生 `loading="lazy"` 或 IntersectionObserver；避免容器高度为 0
6) 高像素大图过多导致超时
   - 表现：弱网下超时
   - 方案：引入多分辨率缩略图，首屏加载小图；启用 HTTP/2 多路复用

### 三、回归测试（建议）
- 数据集：≥ 200 张不同尺寸/格式/域名图片（包含 webp/jpg/png、CDN/源站）
- 条件：正常网络/限速 3G/断网重试
- 验收：整体加载成功率 ≥ 99.5%
- 建议脚本（Playwright 或 Puppeteer）：
```ts
// 伪代码：遍历图片 URL 列表，验证 naturalWidth>0
for (const url of urls) {
  await page.evaluate((u) => new Promise((resolve) => {
    const img = new Image()
    img.onload = () => resolve(true)
    img.onerror = () => resolve(false)
    img.src = u
  }), url)
}
```

### 四、运维看板
- 指标：图片加载失败率、按域名/状态码分布、TopN 失败 URL
- 来源：后端日志聚合（Promtail/Loki 或 ELK），前端埋点（可接入 Grafana）

