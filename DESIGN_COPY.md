# Repland 设计副本

这是可以独立修改的原生 Android 前端副本，位于 `outputs/Repland-Design/`，Git 分支为 `codex/frontend-design-copy`，基于 `0a508090567f`。原目录 `outputs/Repland/` 与原版应用保持独立。

## 使用

- 双击 `Open-Design.cmd`，复用本机唯一模拟器打开“Repland 设计副本”。应用包名 `com.swan1127.repland.design`，与 `com.swan1127.repland.current` 分开存储数据。
- 编辑本目录 `android/app/src/main/java/com/swan1127/repland/ui/` 下的源码。
- 修改后，在 PowerShell 运行 `./Build-Design.ps1`，然后再次运行 `./Start-Design.ps1`。
- APK 位于 `android/app/build/outputs/apk/designPreview/app-designPreview.apk`。
- 副本首次启动为空数据，不复制原版任务、密钥或通知配置。本次验证在副本中创建了演示事项 `Read 20 pages`，可自行修改或取消。

## 本轮设计

“我的”顶部已有可更换的圆形头像和可编辑用户名，保存于本地，重启保留；可恢复默认头像。说明见 [个人资料](DESIGN_PERSONAL_PROFILE.md)。

当前按参考图采用轻浅纸面、白色大圆角卡片与日期标题，系统加入浅蓝、薄荷、浅紫、米黄和淡粉。任务颜色按 ID 稳定分配；普通文字维持深色。取舍与实现见 [参考图日程改版](DESIGN_PRIORITY_SCHEDULE.md)；[第二轮视觉优化](DESIGN_VISUAL_V2.md) 为历史记录。

当前有运行时动效：按压缩放、松开轻弹、原生涟漪、总计/详细与页面切换、折叠旋转和录入步骤过渡；支持系统关闭动画。没有新增动画库、常驻服务或装饰循环。此前的 skill 来源与动效记录见 [动效说明](DESIGN_MOTION.md)。

- 首页默认日程，新增卡片可录入真实任务；右侧加号可安排已有事项。
- 原合轨改为“总计”：按真实重叠时间切段，同一行按优先级依次减弱视觉强调。原分轨改为“详细”：按优先级分组，保留原始时间和备注。
- 已完成、已取消与时间已结束依据真实状态分别显示；时间经过不自动完成任务。
- 保留原有时间与备注编辑、任务查看、安排移除、课程和轨道调整入口。
- 新任务以保存为主操作，日期和时长按需补充，草稿恢复仍保留。
- 任务列表减少未知字段标签和零进度装饰；详情突出“开始这件事”。
- 助手明确区分已有任务规划与输入新安排，数据发送范围可展开查看。
- 周视图纵向展示一周，月视图显示实际已安排时长，移除固定每天 8 小时的暗示。
- 设置页移除面向旧版本迁移的常驻说明。
- 任务详情分组为有边框的信息卡，助手与 AI 设置使用相同视觉规则。
- 任务筛选在窄屏和大字体下可换行，主页面宽度上限为 720dp；保留深色模式与原生控件反馈。

## 资源控制

源码采用 Git worktree，共享历史；不复制 SDK、AVD、JDK、依赖缓存及原项目构建目录。Gradle 使用单次构建进程、两个工作线程、1.5 GiB 最大 Java 堆及进程内 Kotlin 编译；构建退出后不会常驻编译守护进程。只运行一个模拟器，新启动时配置 1536 MiB 来宾 RAM；这不等于 Windows 总工作集硬上限。

安装版与原版是两个图标。按 Home 后可以切换对照，平时只运行其中一个以节约内存。关闭模拟器窗口即可释放模拟器资源。

当前安装包、录屏与验证结果见 [交付验证记录](DESIGN_CURRENT.md)；[上一轮记录](DESIGN_VERIFICATION.md) 保留用于对照。

设计构建中的 `SchedulePreviewActivity` 仅供只读演示，明确标注演示内容，不写入真实计划。它与主应用使用同一套界面组件；日常使用仍双击上述入口。
可双击 [Open-Schedule-Preview.cmd](Open-Schedule-Preview.cmd) 打开演示；它复用同一个模拟器。
