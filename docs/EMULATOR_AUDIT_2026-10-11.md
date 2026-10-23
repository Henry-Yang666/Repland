# 本地源码与模拟器检查记录

日期：2026-10-11，Asia/Shanghai。基于本地拉取提交 `0a508090567fad108503929558f39bb7d80c58f2`，修复保留在分支 `codex/emulator-audit-2026-10-11` 的工作区，未提交或推送。候选版本升为 **0.1.36 / 1036**；APK 内构建来源是上述基础提交，实际包含本轮未提交修复，不代表干净提交构建。

本文保存第一轮结果；用户重新配置模型后的真实 API 验证及后续修复见 [继续检查记录](EMULATOR_FOLLOWUP_2026-10-11.md)。证据目录中的最新汇总可能包含后续验证，请按原始日志区分各轮。

## 安装与环境

- 设备：`emulator-5554`，AVD `Pixel_6`，Android API 36，1080×2400。
- 按用户明确要求卸载 `.repland`、`.repland.internal`、`.repland.current`、`.repland.qa` 四个旧安装包，均返回 Success；旧应用数据随卸载清除。
- 最新实际使用包：`com.swan1127.repland.current`，显示名“Repland 当前版”，0.1.36-internal / 1036。
- 收尾已卸载 QA 与 QA 测试包，`pm list packages repland` 只返回 current。安装包与最终构建 APK 的 SHA-256 相同，记录在 `output/audit-2026-10-11/final-installation.json`；应用停留在首页。
- 本轮旧源码回归在 GMT 时区；修复后检查使用 Asia/Shanghai。不构成完整跨时区验收。
- 使用既有 JDK 与 Gradle 9.1.0、SDK 36；首次 lint 离线缺依赖，联网补齐后重新检查。

## 发现与修复

| 项目 | 证据与影响 | 修复 |
| --- | --- | --- |
| 清除本地数据始终失败 | 0.1.35 实际 current 包点击“确认清除”显示“清除失败”。新增真实 Activity 用例也失败；ViewModel 在 Main 调用阻塞的 Room `clearAllTables()`，旧仓储测试使用 `allowMainThreadQueries()` 掩盖了生产限制。 | 在仓储中通过 `withContext(Dispatchers.IO)` 执行阻塞删除。实际 current 0.1.36 成功提示，新用例验证取消不清除，确认后任务、日志、计划、约束、画像来源和数值清空。 |
| 通知权限异常边界缺失 | lint 报 1 个 MissingPermission 错误；原代码在另一方法检查权限，发送处没有捕获权限在检查后被撤销的异常。没有声称已在设备上制造此竞态崩溃。 | 在发送方法直接检查 POST_NOTIFICATIONS，捕获发送产生的 SecurityException，保持任务状态不变。 |
| 提醒重排广播未核对 action | lint 的 UnsafeProtectedBroadcastReceiver 警告；接收器 exported，收到其他 action 也会读取数据并重排提醒。 | 只接受启动、系统时间和时区变化三个声明的 action，其他输入立即返回。 |
| 完成结果显示内部代码 | 实际 current 完成专注后，任务详情显示 `COMPLETED`，截图 `task-completed.png`。 | 任务详情、日志和每日回顾将四种保留结果码显示为中文，储存内容不变，其他文本原样显示。最终 APK 的历史任务显示“整个任务已完成”，截图 `task-completed-localized.png`。 |
| 保护时段测试定位不稳定 | 原 310 项全套 309 通过、1 失败：保护任务移动用例找不到时间块。保持原数据单独重跑通过。 | 在测试中等待 Room 更新后的时间块进入语义树，再滚动定位；业务断言保持。此项属于测试同步修复，不计作已复现的产品缺陷。 |
| README 版本信息过时 | 首页把 0.1.28 / 0.1.29 描述为当前版本，与源码 0.1.35 不一致。 | 更新为本轮 0.1.36，并链接本记录。旧版验收资料保留。 |

清除后的活动画像观察器会自动建立一个空的 `numeric-snapshot` 缓存。新用例最初将“没有任何缓存行”作为断言，因而在操作已成功后失败；随后改为验证缓存无来源、无样本、无数值，且没有用户输入或描述记录。保留这次失败记录，不将其计作第二个产品 bug。中止过一轮只完成 18 项的回归以纠正该断言，日志中的 Process crashed 来自主动停止 QA 包，不是应用自然崩溃。

## 本轮验证

150 个 JVM 用例已在最终文案修复后重新通过；开发网关 1 个 Node 用例通过。debug lint 已通过，0 errors / 72 warnings，不能称为“无警告”。debug、测试 APK、internal APK 均已构建，离线主代码边界检查通过。

设备行为回归累计 **311 个不同方法通过，0 个剩余失败**，是分段验证：全套运行完成 148 项时模拟器连接消失，其中原生返回用例超时，其余 147 项通过；重启模拟器后将未通过和未完成方法合并续跑，164 / 164 通过，包括原生返回和保护任务移动。未查明模拟器退出原因，不能将连接中断归因于应用崩溃，也不能称为单次完整全套绿跑。原生返回流程随后在实际 current 包再次手动通过。

这 311 项覆盖清除、提醒及测试同步修复，发生在最后的结果码中文显示改动之前。文案改动后重新构建，并运行 ExecutionWorkflowUiTest、TaskFeedbackWorkflowUiTest、DataManagementWorkflowUiTest **9 / 9 通过**，另在最终 current 包验证旧任务的中文结果显示。最新源码未再连续重跑全部 311 项；时间顺序和历史失败均保留在证据中。

新增 `DataManagementWorkflowUiTest` 使用应用真实数据库配置与页面入口，专项已通过。仪器化测试只运行 QA 隔离包，实际 current 包操作单独记录。

| 用户操作族 | 本轮自动化与实际检查依据 |
| --- | --- |
| 四页导航、返回、页面重建、读取失败与重试 | NativePageNavigationUiTest、ReadFailureWorkflowUiTest、RecoverableReadViewModelTest |
| 任务分步收集、草稿恢复、估时、截止日期、非法数字、键盘返回 | CoreWorkflowUiTest、TaskCaptureSheetUiTest、CaptureNumericWorkflowUiTest、DraftSaveWorkflowUiTest |
| 分组筛选、已安排日期、排序、撤销排序 | TaskPlanMembershipUiTest、TaskPageAppBarUiTest、PlanningOperationServiceTest、PlanningWorkspaceRepositoryTest |
| 编辑、开始、整项完成、部分完成、延期、取消、恢复、替换、更正日志 | TaskEditorWorkflowUiTest、TaskFeedbackWorkflowUiTest、TaskLifecycleRepositoryTest、ExecutionWorkflowUiTest |
| 制定计划、缺时间引导、草案编辑、容量不足、确认与过期保护 | QuickAvailabilityTest、PlanDraftPreviewUiTest、PlanningIsolationWorkflowUiTest、PlanningProtectionWorkflowUiTest |
| 日周月日程、轨道、放入事项、时间编辑、重叠确认与锁定保护 | TimelineDashboardUiTest、PlacementNumericWorkflowUiTest、TimelineWriteReceiptWorkflowTest、ProtectedOverlapWorkflowUiTest |
| 计划历史、恢复、清空当前计划、锁定与解锁 | HistoryRestoreWorkflowUiTest、PlanRestoreRepositoryTest、ManualPlanChangeRepositoryTest |
| 专注开始、暂停、继续、后台恢复、四种结束结果 | ExecutionSessionUiTest、ExecutionSessionRepositoryTest、SessionInputWorkflowUiTest、SessionReadIntegrityTest |
| 助手离线解析、查询、解释、已有/新增事项、确认、连续修改、迟到响应失效 | AgentCenterUiTest、AssistantReadOperationUiTest、AssistantSaveReceiptTest、ArrangementAssistantAccessTest |
| 每周与单日约束、学期设置、编辑、删除与失败回执 | TimeSettingsWorkflowUiTest、TimeSettingsIntegrityTest、TimeConstraintRevisionRepositoryTest |
| PDF 读取、识别预览、修改、取消、确认、去重与钟点核对 | PdfTimetableImporterTest、TimetableConfirmationUiTest、TimetableConfirmationTest；生成的合成 PDF，不代表所有真实学校课表格式。 |
| 提醒开关、通知广播、禁用后不投递、状态不自动改变 | ReminderSettingsRepositoryTest、ReminderDeliveryIntegrityTest、LocalReminderPlannerTest |
| 模型配置、安全存储、无授权不发送、HTTP 错误与无效输出 | AiProviderSettingsUiTest、AiSettingsRepositoryTest、CompatibleTransportTest、DebugAgnesArrangementAdvisorParserTest；网络契约使用隔离夹具。 |
| 数值画像、未知值、输入来源、排除与停用、建议采纳、只读说明 | NumericProfileWorkflowUiTest、NumericProfileRepositoryTest、NumericProfileAiWorkflowTest、NumericNarrationContractTest |
| 导出、清除、防草稿回写、迁移与持久化 | LocalDataJsonExporterTest、ProfileEvidenceAndDataManagementRepositoryTest、DataManagementWorkflowUiTest、TaskCaptureViewModelTest、Migration20To21Test、Migration5To6Test |
| 深色、字号、窄屏与横竖屏相关交互 | EditorSheetUiTest、TaskPageAppBarUiTest、NativeSheetTestInput 相关 Activity 用例；不是全部布局或 TalkBack 验收。 |

关联类通过只证明该类中的断言，不代表同一操作所有组合已被穷尽。当前 85 篇功能清单仍是入口地图，原有 0.1.35 结果不替代本轮结果。

## 实际 current 包操作

在 `com.swan1127.repland.current` 上完成以下真实页面操作；截图均在 `output/audit-2026-10-11/`：

1. 启动并检查版本；新增 ReplandAuditTask，键盘返回保留录入弹层，语音不可用时显示可继续文字输入的提示，取消后文字草稿保留。
2. 补充 30 分钟估时并保存；补充今天 21:00–23:00 的可用时间，生成尚未写入的计划预览，再确认采用。证据：`task-created.png`、`plan-preview.png`。
3. 提前开始专注、暂停；强制停止 current 后重新启动，恢复暂停会话，再继续计时并确认整项完成。任务保存实际耗时 1 分钟、100% 与可回溯日志。证据：`focus-paused.png`、`cold-session-recovery.png`、`task-completed-localized.png`。
4. 通过 Android 系统文件选择器导入合成 Repland-AuditMath.pdf，核对默认节次时间后确认，回执显示新增 1 门课程，列表出现周一 08:00–08:45。证据：`pdf-preview.png`、`pdf-imported.png`；仅代表此合成格式。
5. 固定时间编辑中按系统返回键：收起键盘但保留弹层 → 关闭弹层但保留时间页 → 返回助手；未保存的 AuditCancel 没有写入。证据：`keyboard-back-keeps-sheet.png`、`sheet-back-keeps-time-page.png`、`native-back-assistant.png`。
6. 通过系统保存选择器导出 JSON，页面成功提示，拉取后解析成功，包含 1 个任务、2 条执行日志、1 个会话、1 个课程和计划历史。证据：`json-exported.png`、`exported-snapshot.json`。
7. 对上述非空业务数据执行清除，显示成功；回到首页待安排 0、轨道 0 项。证据：`nonempty-clear-success.png`、`final-home.png`。本轮生成数据已清理，导出证据仍保留。

文案修复使用 ui-ux-pro-max 技能；检索没有直接匹配结果码文案的规则，按通用清晰反馈原则处理，范围仅限结果显示。没有声称完成 TalkBack 或全部屏幕尺寸验收。

## 证据与未验证范围

本地证据目录：`output/audit-2026-10-11/`；原始本轮日志位于仓库根的 `.codex-current-audit-*.log`，不提交到 Git。`device-results.json` 保留各轮完成、失败、中止与方法清单；`source-and-build.json` 保存源码文件及 APK SHA-256，并明确未提交修复。

`latest-device-coverage.json` 保存 311 个方法的最后通过结果与分段标志；`feature-coverage.md` 将既有 85 篇功能文档与本轮关联类对应。最终构建日志为 `.codex-current-audit-label-build.log`，最后 9 项设备专项为 `.codex-current-audit-label-device.log`。

- 实际远程模型成功请求：卸载旧包后没有 API Key，本轮不发送真实业务数据；契约、错误路径、配置和本地回退测试与真实服务成功分开。
- 模拟器没有可处理 `android.speech.action.RECOGNIZE_SPEECH` 的应用，无法验证识别成功和真实麦克风输入；检查无服务时的回退提示。
- 真机、其他 Android API、OEM 后台限制、实际闹钟准时性、真实学生 PDF、TalkBack 与大数据性能未验证。
- JSON 导出是当前本地业务快照，产品尚没有完整备份恢复与云同步功能。
