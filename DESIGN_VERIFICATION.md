# 设计副本历史验证（2026-10-14）

当前版本见 [DESIGN_CURRENT.md](DESIGN_CURRENT.md)。下文是上一轮历史记录，其安装包哈希和测试数量不代表当前构建。

日期：2026-10-14（Asia/Shanghai）。当前为第二轮视觉优化后的动效版；副本源代码基线为 `0a508090567f`，前端优化保留为 `codex/frontend-design-copy` 分支上的本地修改，便于继续编辑与对照。设计取舍见 [DESIGN_VISUAL_V2.md](DESIGN_VISUAL_V2.md)，图片中 skill 的应用、动效与录屏见 [DESIGN_MOTION.md](DESIGN_MOTION.md)。

## 可使用的结果

- 应用名：Repland 设计副本。
- 包名：`com.swan1127.repland.design`。
- 版本：`0.1.35-design / 1035`。
- 打开入口：[Open-Design.cmd](Open-Design.cmd)。
- 源码和构建说明：[DESIGN_COPY.md](DESIGN_COPY.md)。
- 安装包：[app-designPreview.apk](android/app/build/outputs/apk/designPreview/app-designPreview.apk)，21,833,738 字节，约 20.8 MiB。
- 最终 APK SHA-256：`3c6dfa43f1255bba5331c25c1ed636b304f758d8cac43a0852c13f54b63ff471`。

## 原版保护

原目录 `../Repland/` 的 Android 源码无本次改动。模拟器中原版仍为 `com.swan1127.repland.current / 0.1.35-internal / 1035`，安装更新时间为 `2026-10-11 12:42:53`。

原版已安装 APK 的 SHA-256 为 `9c5a54385aad72ca6e0d8ac2e4477862c83bb93ac4f23acccfd12722267341b2`，与之前保存的 `work/current-installed-audit.apk` 完全一致。副本使用独立数据目录，未复制原版任务、密钥和通知设置；验证只对副本操作。

## 验证结果

- 最终构建 `:app:assembleDesignPreview` 通过。
- 150 项单元测试全部通过，31 个测试套件，0 失败、0 错误、0 跳过。
- `:app:verifyOfflineMvpBoundary` 通过。
- 两个 PowerShell 入口均通过语法解析；一键启动脚本已实际完成启动、安装和打开副本。
- 第一轮已验证：首页记录入口、任务保存、关闭后恢复草稿、任务详情、首页安排入口跳转、日程显示设置展开/收起、周/月切换、设置页。
- 第二轮重新构建、安装并查看浅色首页、任务列表、详情、助手和设置；同一模拟器分别覆盖约 375dp 窄屏、横屏、约 985dp 平板宽度，以及深色模式、200% 字体下的首页、任务筛选、月视图和详情。
- 第二轮检查时关闭系统三项动画倍率，仍可导航和打开详情。动效版新增有限的操作过渡，并监听系统动画设置；检查结束恢复原来的分辨率、密度、字体、主题和动画设置。
- 动效版另有 10 项设备测试通过，验证快速切页、展开反向、系统设置即时生效与任务录入回归。已在同一模拟器实际录制切页、详情、抽屉及设置展开/收起，并补查深色大字体、关闭动画的首页。
- 11 组主要普通文字色对均达到 4.5:1，最低 5.68:1。没有穷举所有叠加、焦点或禁用状态；本次也未做完整 TalkBack 流程验收。
- 模拟器崩溃日志未发现本次运行的崩溃记录。

本次没有执行整套 Android 仪器测试，也没有测试真实模型请求。副本没有原版密钥，助手使用本地能力；这些结果不代表全部业务路径均已复测。

## 资源和停止状态

源码约 9.2 MiB；含 APK、设备测试包、测试结果和编译中间产物的副本目录约 154 MiB。SDK、JDK、Gradle 依赖缓存、AVD 镜像与 Git 历史均复用既有资源。运行录屏约 2.3 MiB；新安装的四项 skill 合计约 0.26 MiB。

构建最多两个工作线程，Java 堆上限 1536 MiB，Kotlin 编译在同一进程执行，Gradle 为单次进程。只启动一个模拟器，来宾 RAM 设置为 1536 MiB；Windows 模拟器进程总内存仍可能明显更高，不能把来宾设置当作主机内存硬上限。

验证结束后关闭了本次模拟器和 ADB 服务；编译进程已自行退出。交付文件不会常驻占用这些进程的内存。需要试用时双击打开入口，结束后关闭模拟器窗口。

## 证据

预览与原始验证结果位于工作区 `work/design-copy-preview-2026-10-14/`：

- [第二轮最终首页](../../work/design-copy-preview-2026-10-14/visual-v2-home.png)
- [第二轮任务](../../work/design-copy-preview-2026-10-14/visual-v2-tasks.png)
- [第二轮详情](../../work/design-copy-preview-2026-10-14/visual-v2-detail.png)
- [草稿恢复](../../work/design-copy-preview-2026-10-14/draft-restored.png)
- [第二轮助手](../../work/design-copy-preview-2026-10-14/visual-v2-assistant.png)
- [第二轮设置](../../work/design-copy-preview-2026-10-14/visual-v2-settings.png)
- [周视图](../../work/design-copy-preview-2026-10-14/week.png)
- [月视图](../../work/design-copy-preview-2026-10-14/month.png)
- [窄屏任务](../../work/design-copy-preview-2026-10-14/visual-v2-small-tasks.png)
- [横屏任务](../../work/design-copy-preview-2026-10-14/visual-v2-landscape-tasks.png)
- [平板宽度首页](../../work/design-copy-preview-2026-10-14/visual-v2-tablet-home.png)
- [第二轮深色、大字号首页](../../work/design-copy-preview-2026-10-14/visual-v2-large-home.png)
- [第二轮深色、大字号月视图](../../work/design-copy-preview-2026-10-14/visual-v2-large-month.png)
- [第二轮验证元数据](../../work/design-copy-preview-2026-10-14/visual-v2-verification.json)
- [当前动效版验证元数据](../../work/design-copy-preview-2026-10-14/motion-verification.json)
- [实际运行录屏](../../work/design-motion-preview/repland-motion-preview.mp4)
- [动效设备测试日志](../../work/design-motion-device-tests.log)
- [第一轮历史验证元数据](../../work/design-copy-preview-2026-10-14/verification.json)

当前构建日志：`work/design-motion-final-build.log`；启动日志：`work/design-motion-final-launch.log`；配色验证：`work/design-visual-v2-contrast.json`；设备设置恢复基准：`work/visual-v2-device-settings.json`。副本保留演示任务 `Read 20 pages`，可自行修改或取消。
