# 本机重要资料同步 · 2026-10-23

本次把本机尚未完整归档的三份项目工作区、用户功能实测和前端迁移说明同步到仓库。
源码快照保留各自版本及本地修改，解压后可继续开发；没有把不同阶段的业务代码自动合并。

## 文件入口

| 内容 | 位置 |
| --- | --- |
| 当前仓库源码 | [仓库根目录](../../)；已有前端、多角色规划团队和记忆模型集成提交 |
| 10 月 22 日交付包、APK、源码 ZIP、验证资料 | [已有交付目录](../handover-2026-10-22/Repland-Frontend-2026-10-21/) |
| 视觉参考、动效拆解、提示词、环境说明 | [已有交接资料](../handover-2026-10-22/) |
| 原版工作区源码与本地实测更新 | [Repland 快照](Repland-source-2026-10-23.zip) |
| 设计副本源码 | [Repland-Design 快照](Repland-Design-source-2026-10-23.zip) |
| 前端更新工作区与迁移说明 | [Repland-Frontend 快照](Repland-Frontend-source-2026-10-23.zip) |
| 用户功能与工作流实测 | [实测文档](Repland/docs/USER_FEATURE_WORKFLOWS_2026-10-11.md) · [原始证据](Repland/docs/verification/user-audit-2026-10-11/README.md) |
| 前端迁移提示词 | [迁移说明](Repland-Frontend/docs/REPLAND_FRONTEND_MIGRATION_PROMPT_2026-10-20.md) |
| 快照来源、逐文件 SHA-256 | [manifest.json](manifest.json) · [压缩包校验值](SHA256SUMS.txt) |

## 范围与核对

快照使用 Git 跟踪文件和未被忽略的本地新增文件生成。排除了开发环境、模拟器、构建产物、缓存、IDE 配置、本机路径配置、数据库、凭据和签名材料。
已有的 APK 和完整交付 ZIP 保留原位置，本次按 SHA-256 核对后复用。
所有新增文本及压缩包成员均检查凭据特征；所有快照逐文件校验并检查 ZIP 完整性。
本次只同步与归档，没有修改 Android 产品代码，也没有重新执行历史文档所列的应用测试。
历史记录中的测试结论只适用于各自记录的版本和条件。
