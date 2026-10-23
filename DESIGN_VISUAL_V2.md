# Repland 第二轮视觉优化

这轮修改仅位于设计 worktree 和独立应用 `com.swan1127.repland.design`。沿用既有任务、草稿与规划逻辑，整理原生 Compose 界面的视觉层级。

## 使用的 skill 与取舍

从 [GitHub UI/UX Pro Max](https://github.com/nextlevelbuilder/ui-ux-pro-max-skill) 安装了 `.claude/skills/ui-ux-pro-max`，固定提交 `7246805992a42b7bba37d4cac83b01416f0c6c44`。实际阅读并应用该提交中的 `SKILL.md`、`references/pro-rules.md` 与 `references/quick-reference.md` 的关键原生界面规范。

通过 skill 的 `scripts/search.py` 执行并筛选了这些查询：

- `"productivity planner calm minimal" --design-system -p Repland -f markdown`：结果偏向健康服务、拟物阴影和营销页，未采用；按 skill 要求调整查询一次。
- `"task management app" --design-system -p Repland -f markdown`：采用适合任务工具的 Flat Design 方向；其中视频、营销段落和西文装饰字体不适合本项目，未采用。
- `"flat minimalist productivity" --domain style -n 3`：核对扁平与简洁界面规则。
- `"material typography spacing" --stack jetpack-compose -n 5`：采用 MaterialTheme 语义字体及 Arrangement/Alignment 布局规则。
- 图标搜索 `"icon stroke consistency"` 无结果；重试 `"icon-style-consistent"` 仍不相关。笔画与图标规则取自已阅读的原生 `pro-rules.md`，不把无关搜索项当作依据。

以下设计是按本应用场景调整的实现，并非直接套用 skill 生成的营销模板。

## 实现

- 使用森林绿主色 `#285E4A`、浅灰绿背景 `#F5F7F5` 和白色信息卡；首页焦点区域用深绿 `#173E33`，主操作用浅黄绿 `#D8ECB9`。
- 深色模式有独立表面与文字 token。11 组已测量的普通文字色对均达到 4.5:1，最低为 5.68:1；此测量不代表每个叠加态和禁用态都已覆盖。
- 继续使用 Android 原生 SansSerif。统一标题、正文、说明和标签的 Material 字体角色，不下载字体。
- 按钮圆角 12dp、信息卡圆角 14dp、主要容器 20dp。减少胶囊形和无信息量的装饰，使用细边框区分层次。
- 导航和主要入口使用同组 Compose 矢量图标，24dp 视口、1.8 笔画；没有新增位图、字体包或依赖。
- Button、OutlinedButton、FilterChip 包装继续调用 Material 原生控件，保留 ripple、选中与禁用语义，控件最小高度 48dp。
- 今日页突出焦点与下一步行动；任务页减少标签，筛选项可换行；详情按信息卡分组；助手与 AI 设置沿用同一图标、边框和按钮规则。
- 主页面内容宽度上限 720dp，宽屏居中留白；大字体详情字段可纵向展开，月格可以随内容增高。

## 预览与验证

当前构建、测试、安装包哈希、原版校验及资源状态见 [DESIGN_VERIFICATION.md](DESIGN_VERIFICATION.md)。

- [首页](../../work/design-copy-preview-2026-10-14/visual-v2-home.png)
- [任务](../../work/design-copy-preview-2026-10-14/visual-v2-tasks.png)
- [详情](../../work/design-copy-preview-2026-10-14/visual-v2-detail.png)
- [助手](../../work/design-copy-preview-2026-10-14/visual-v2-assistant.png)
- [设置](../../work/design-copy-preview-2026-10-14/visual-v2-settings.png)

仍使用原有一个模拟器和共享开发环境；没有增加常驻预览服务。入口仍为桌面“Repland 设计副本”快捷方式或本目录 [Open-Design.cmd](Open-Design.cmd)。
