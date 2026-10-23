# 当前副本交付记录

日期：2026-10-15，Asia/Shanghai。基于 `codex/frontend-design-copy` 的本地修改，原生 Android / Compose；未增加前端动画依赖。

## 使用

- 正常使用：双击桌面“Repland 设计副本”，或 [Open-Design.cmd](Open-Design.cmd)。
- 查看重叠事项与优先级：双击 [Open-Schedule-Preview.cmd](Open-Schedule-Preview.cmd)。这是明确标注的只读演示，按系统返回键退出，不写入真实计划。
- 修改源码：`android/app/src/main/java/com/swan1127/repland/ui/`；修改后运行 [Build-Design.ps1](Build-Design.ps1)，再重新打开。
- 独立包名：`com.swan1127.repland.design`。安装包：[app-designPreview.apk](android/app/build/outputs/apk/designPreview/app-designPreview.apk)，22,188,549 字节。
- 当前 APK SHA-256：`e4ee1fe8e2c025811401575fe77d6d4d6d7fff69faec550d72778763af917947`。

## 变化

- “我的”最上端新增头像与用户名；可从照片选择器更换头像、恢复默认头像，编辑并保存用户名。个人资料仅在副本本地保存，重启和覆盖更新后保留，原有清除本地数据同时清理资料。实现见 [DESIGN_PERSONAL_PROFILE.md](DESIGN_PERSONAL_PROFILE.md)。
- 首页按参考图改为简洁日期、白色大圆角卡片、柔和渐变的新增日程入口和时间侧栏。
- 原合轨改名“总计”：按真实时间边界拆分重叠区间，同一行按优先级排列并逐步减弱视觉强调，窄屏可横向查看。
- 原分轨改名“详细”：按实际优先级分组，保留每项完整起止时间和备注。固定安排单独标注，未设优先级放最后。
- 浅蓝、薄荷、浅紫、米黄和淡粉用于事项和选中状态；同一事项跨视图保持同色。文字不随背景一起降低透明度。
- 加号、按钮、筛选、导航和日程项有按压缩放、松开轻弹与原生涟漪；分组有展开/收起和箭头旋转；切换只保留当前内容，支持快速反向操作。
- 系统关闭动画时停用这些过渡；触控布局和动作触发不等待动画完成。
- 时间经过不自动完成任务；已完成、已取消、仅时间已结束分别展示。原有查看任务、时间编辑、备注、课程、轨道调整和移除安排流程保留。

设计与 skill 应用见 [DESIGN_PRIORITY_SCHEDULE.md](DESIGN_PRIORITY_SCHEDULE.md)。

## 验证

- 最终 `assembleDesignPreview`、`testDesignPreviewUnitTest`、`verifyOfflineMvpBoundary` 通过。
- 163 项单元测试、33 个套件，0 失败。
- 37 项设备测试通过：5 项个人资料存储、5 项个人资料交互、8 项优先级日程、9 项既有日程入口、4 项动效与 6 项录入回归。新增覆盖头像尺寸、私有副本在源图移除后仍可读取、相机方向、无效图片不覆盖旧头像、姓名保存/取消/失败/长度与减少动画、大字体、清除本地数据的集成。
- 上传前将仪器测试构建改为可配置：默认保留 debug CI，`-PreplandTestBuildType=designPreview` 选择隔离设计版。9 项既有日程测试已按总计/详细与设置入口更新，保留课程、时间、冲突保护及轨道检查；默认 debug 任务图 dry-run 通过，未冒称在本机跑过 API 26 CI。
- 实际打开 Android 照片选择器，选入自建测试图片，重启应用仍显示；恢复默认头像后移除测试图片。另检查窄屏、横屏、平板、200% 字体暗色模式；窄屏和大字体下弹出系统键盘后仍可保存、取消。
- 同一模拟器检查普通手机、约 375dp 窄屏、横屏、200% 字体与暗色模式；首页新增入口能打开真实录入抽屉，关闭保留草稿，任务仍显示原来的进行中状态。
- 25 组正文/选中态/标签色对检查，最低 5.09:1。没有穷举禁用、叠加或焦点状态；没有执行完整 TalkBack 流程或整套仪器测试，也未测试真实模型调用。
- 演示视频使用真实组件、原生帧时钟和只读固定数据；不是你的实际计划，也不用于判断模型效果。

## 原版与资源

原版 Android 源码无本轮差异。原安装包 `com.swan1127.repland.current` 的 SHA-256 仍为 `9c5a54385aad72ca6e0d8ac2e4477862c83bb93ac4f23acccfd12722267341b2`，与此前审计一致；安装更新时间仍为 `2026-10-11 12:42:53`。副本独立存储数据，没有复制原版密钥或任务。

复用 SDK、JDK、AVD、依赖缓存和 Git 历史；构建最多 2 个工作线程、1536 MiB Java 堆、无常驻 Gradle 守护进程。本轮复用任务开始时已运行的模拟器，不新增实例；恢复系统设置后保留原窗口供查看。头像最多 384×384，不保存所选原始大图，未引入图片库或后台服务。

## 证据

可随 GitHub 分支查看的当前证据见 [docs/design-preview](docs/design-preview/README.md)。下面的 `../../work/` 链接保留为原电脑本地验证记录。

- [当前“我的”页面](../../work/personal-profile-preview-2026-10-15/profile-final.png)、[编辑用户名](../../work/personal-profile-preview-2026-10-15/edit-name.png)
- [新头像重启后保留](../../work/personal-profile-preview-2026-10-15/profile-restarted.png)
- [本轮 APK、单元测试与原版审计](../../work/personal-profile-preview-2026-10-15/verification.json)、[设备测试](../../work/personal-profile-device-tests.log)、[构建](../../work/personal-profile-final-build.log)
- [原生动态演示（只读演示日程）](../../work/priority-schedule-preview-2026-10-15/priority-schedule-preview.mp4)
- [真实副本首页](../../work/priority-schedule-preview-2026-10-15/home.png)
- [总计](../../work/priority-schedule-preview-2026-10-15/final-summary-overlap.png)、[详细](../../work/priority-schedule-preview-2026-10-15/final-detailed.png)
- [任务](../../work/priority-schedule-preview-2026-10-15/final-tasks.png)、[我的](../../work/priority-schedule-preview-2026-10-15/final-profile.png)
- [上一轮日程构建审计](../../work/priority-schedule-preview-2026-10-15/verification.json)
- [设备测试日志](../../work/priority-schedule-device-tests.log)
- [配色检查](../../work/priority-schedule-contrast.json)
- [构建日志](../../work/priority-schedule-final-build.log)
- [此前原版录屏](../../work/original-motion-preview/repland-original-preview.mp4)

原版录屏与本次演示的数据和配置不同，不能将示例任务差异当成设计变化。
