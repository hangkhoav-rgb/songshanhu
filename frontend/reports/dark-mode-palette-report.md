# 暗色模式配色评估与优化方案

生成时间：2026/4/11 21:41:07

## 调整要点
- 降低偏蓝背景梯度的存在感，改为更中性的深灰蓝基底，减少长时间观看疲劳
- 将文本分为 primary/secondary/tertiary 三层，控制对比度梯度，避免“过亮刺眼/过灰读不清”
- 交互主色在暗背景下上调明度（primary: #4C9AFF），保证可见且不过饱和

## 色值表（Hex / RGB / HSL）

### 背景/基底

| Token | Hex | RGB | HSL |
|---|---:|---:|---:|
| dm-bg-0 | #0F1115 | rgb(15, 17, 21) | hsl(220, 17%, 7%) |
| dm-bg-1 | #11141B | rgb(17, 20, 27) | hsl(222, 23%, 9%) |
| article-bg | #111318 | rgb(17, 19, 24) | hsl(223, 17%, 8%) |
| nav-bg | #141821 | rgb(20, 24, 33) | hsl(222, 25%, 10%) |

### 表面层

| Token | Hex | RGB | HSL |
|---|---:|---:|---:|
| surface-1 | #151922 | rgb(21, 25, 34) | hsl(222, 24%, 11%) |
| surface-2 | #1B2130 | rgb(27, 33, 48) | hsl(223, 28%, 15%) |
| surface-3 | #20273A | rgb(32, 39, 58) | hsl(224, 29%, 18%) |

### 文本层

| Token | Hex | RGB | HSL |
|---|---:|---:|---:|
| text-primary | #E6E8EE | rgb(230, 232, 238) | hsl(225, 19%, 92%) |
| text-secondary | #A0A7B4 | rgb(160, 167, 180) | hsl(219, 12%, 67%) |
| text-tertiary | #7C8596 | rgb(124, 133, 150) | hsl(219, 11%, 54%) |

### 交互色

| Token | Hex | RGB | HSL |
|---|---:|---:|---:|
| primary | #4C9AFF | rgb(76, 154, 255) | hsl(214, 100%, 65%) |
| primary-700 | #1E6FDB | rgb(30, 111, 219) | hsl(214, 76%, 49%) |
| secondary | #7AD1FF | rgb(122, 209, 255) | hsl(201, 100%, 74%) |
| accent | #FF6B6B | rgb(255, 107, 107) | hsl(0, 100%, 71%) |
| collect | #4ECDC4 | rgb(78, 205, 196) | hsl(176, 56%, 55%) |

### 分隔线

| Token | Hex | RGB | HSL |
|---|---:|---:|---:|
| divider-10% | #E6E8EE | rgb(230, 232, 238) | hsl(225, 19%, 92%) |

## WCAG 对比度校验（AA 参考）

| 场景 | 前景 | 背景 | 对比度 | 结论 |
|---|---:|---:|---:|---:|
| 正文 text-primary on article-bg | #E6E8EE | #111318 | 15.17 | AA 通过（普通文本） |
| 正文 text-secondary on article-bg | #A0A7B4 | #111318 | 7.68 | AA 通过（普通文本） |
| 正文 text-tertiary on article-bg | #7C8596 | #111318 | 5.00 | AA 通过（普通文本） |
| 导航 text-primary on nav-bg | #E6E8EE | #141821 | 14.50 | AA 通过（普通文本） |
| 导航 active on nav-bg | #FFFFFF | #141821 | 17.76 | AA 通过（普通文本） |
| 链接/主色 primary on article-bg | #4C9AFF | #111318 | 6.52 | AA 通过（普通文本） |

## 设备/亮度测试说明
- 已在 375px/768px/1366px 视口生成暗色截图用于层级检查
- 建议你用手机（iOS/Android）在 25%/50%/100% 亮度各看一次：重点关注 secondary/tertiary 文本与分隔线可读性