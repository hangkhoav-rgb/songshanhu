# 点赞/收藏 UI 上线检查清单

## 功能一致性（必须 100%）
- 点赞/收藏点击后仍调用原接口：`/api/article/:id/like`、`/api/article/:id/collect`
- 点赞计数与收藏计数更新规则不变
- 未登录点击：仍按原逻辑跳转登录页并带 `redirect`
- 状态持久化：刷新后 `like/status`、`collect/status` 能恢复 UI

## UI/交互
- 小屏（320/375/414）无截断、无错位、无横向滚动条
- 深色模式下对比度正常，`active` 与 `normal` 可区分
- 动效时长 ≤300ms，无明显重排/卡顿
- 加载中按钮禁用且显示 spinner
- 失败时状态不翻转

## 无障碍
- Tab 可聚焦到两个按钮
- `aria-pressed` 正确反映状态
- 焦点环可见且不被遮挡

## 测试与报告
- 单元测试通过：`npm run test:unit`
- E2E 通过：`npm run test:like-collect`
- 性能报告已生成：`reports/like-collect-perf.md`

## 风险点
- 外链封面在部分环境可能触发 ORB；当前已在前端规整封面 URL 避免请求

