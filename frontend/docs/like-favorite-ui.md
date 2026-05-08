# 点赞/收藏 UI 组件设计与交互说明

## 目标
- 提升视觉体验与交互流畅度
- 业务逻辑保持不变：计数、状态、回调、持久化、无障碍
- 适配 320–414px 与深色模式，无截断、无错位

## 组件范围
- 文章详情页点赞/收藏操作区
- 组件文件：[LikeCollectBar.vue](file:///d:/songshanhu-blog-platform/frontend/src/components/Interaction/LikeCollectBar.vue)

## 视觉规范（Design Tokens）
### 颜色
- 主色：`--color-primary: #0052D9`
- 辅助色：`--color-secondary: #69C0FF`
- 强调色：`--color-accent: #FF6B6B`
- 文本：`var(--text)`
- 次级文本：`var(--muted)`

### 字体与字号
- 字体栈：`PingFang SC, Helvetica Neue, Arial`
- 标签（“点赞/收藏”）：13px / 600
- 数字（count）：13px / 400

### 圆角与边框
- 按钮圆角：12px
- 图标底座圆角：10px
- 边框：`1px solid var(--border-soft)`

### 动效与交互时长
- hover/状态切换：180ms
- 按压反馈（scale）：≤180ms
- 可逆切换保护窗口（防连点）：300ms

### 按压态与禁用态
- 按压态：`transform: scale(0.98)`
- 禁用态：`opacity: 0.6` + `cursor: not-allowed`

## 交互说明（四种状态）
### 正常（Normal）
- `aria-pressed` 反映当前状态
- hover 显示轻微底色与边框强调

### 加载（Loading）
- 点击后按钮右侧出现 spinner
- 进入 disabled，避免重复触发

### 成功（Success）
- 状态由父层业务逻辑更新（计数与 pressed 同步）
- 轻触反馈：支持设备触发 `navigator.vibrate(10)`

### 失败（Fail）
- 状态不翻转
- 错误提示由父层业务逻辑继续负责（保持原逻辑不变）

## 无障碍
- `role="group"` + `aria-label`
- `button` 原生可聚焦
- `aria-pressed` 表达切换态
- `:focus-visible` 提供清晰焦点环

## 响应式
- 320–414px：按钮纵向堆叠，避免截断
- 深色模式：背景/描边/文字对比度提升，count 与 active 色彩区分

## 原型与截图
- 可直接查看截图（由脚本生成）：
  - 正常（浅色）：`docs/assets/like-collect/state_normal_light.png`
  - 成功（点赞，浅色）：`docs/assets/like-collect/state_success_like_light.png`
  - 加载（收藏，浅色）：`docs/assets/like-collect/state_loading_collect_light.png`
  - 失败（点赞，浅色）：`docs/assets/like-collect/state_fail_like_light.png`
  - 正常（深色）：`docs/assets/like-collect/state_normal_dark.png`

## 自动化验证
- 单元测试：`npm run test:unit`
- E2E：`npm run test:like-collect`
- 原型截图生成：`npm run proto:like-collect`
- 性能报告生成：`npm run perf:like-collect`

## 上线检查清单
- 点赞/收藏成功后 `aria-pressed` 与计数同步
- 失败时状态不改变且仍显示错误提示
- 320/375/414 宽度下无横向滚动与截断
- 深色模式下文字与边框对比度正常
- 无控制台错误（图片 ORB 已在封面规整后缓解）
