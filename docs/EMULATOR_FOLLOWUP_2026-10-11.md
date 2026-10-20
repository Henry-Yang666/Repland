# 继续检查与真实 API 验证

本记录接续 [第一轮检查](EMULATOR_AUDIT_2026-10-11.md)。仍基于本地提交 `0a508090567fad108503929558f39bb7d80c58f2` 与本地未提交修复；当前候选为 0.1.36 / 1036。

## API 的实际结果

本轮开始时 current 显示“尚未添加密钥”。用户随后在模拟器保存配置，应用显示“密钥已加密保存在本设备”。当前实际配置：`https://api.deepseek.com`，`deepseek-flash`；没有将密钥抄入报告、导出或测试代码。

- 实际点击“测试连接”，经过“正在验证连接…”后显示“连接成功：deepseek-flash 已可用于安排助手”。此测试使用真实适配器和固定无个人数据请求，不是模拟成功提示。证据 `api-connection-success.png`。
- 打开规划助手授权，发送测试输入 `Read a book for 10 minutes and practice English for 15 minutes today.`，实际收到两个可编辑候选，时长分别为 10、15 分钟。没有可用时间时保持待决定，未自动落位。手动确认后回执新增 2 项、0 个时段，任务页显示两项。证据 `api-assistant-response.png`、`api-two-candidates.png`、`api-create-receipt.png`、`api-created-tasks.png`。
- 任务详情 → 打开对话 → 帮我理解这项任务 → 发送 → 查看数据预览 → 确认请求，收到有效的真实任务理解建议。随后估时、拆分请求也收到有效建议，任务原始时长保持 10 分钟，执行日志未被 AI 新增。证据 `api-task-understanding.png`、`api-task-duration.png`、`api-task-breakdown.png`。
- 连续补充草案：先请求拉伸与喝水各 5 分钟，再要求拉伸改为 7 分钟，实际返回更新后的 7 / 5 分钟候选。没有确认创建这些候选。证据 `api-followup-response.png`、`api-followup-durations.png`。
- 为合成测试任务明确完成并补录 10 分钟累计投入后，数值画像第一次请求显示本地事实摘要（非 AI）。首次回退的具体原因没有确定，不能将回退计为远程成功。修复解析与请求说明后，在最终安装包中重测，收到有效远程结果，状态为 `AI · deepseek-flash · 基于当前指定快照`，画像版本 `0b7b0b65abed`。证据 `api-numeric-description.png`、`api-numeric-final-response.png`。

这证明当前地址、模型、鉴权及以上业务契约可用，不代表所有模型输出或服务持续可用。覆盖安装和冷启动后仍保留用户配置，重新测试连接成功；未保存模型编辑不会被误判为成功，关闭重开仍为原模型。证据 `api-retained-after-update.png`、`api-final-connection-success.png`、`api-unsaved-guard.png`。未清除 current 数据或用户密钥。

## 新发现与修复

配置弹窗修改模型、地址或输入替换密钥后，原“测试连接”仍可点击，回调实际使用旧的已保存配置。旧连接成功提示也继续显示，容易误认为新配置已验证。

新增两个 Compose 回归用例在修复前均失败。修复为：存在未保存改动时隐藏旧连接结果、提示先加密保存、禁止连接测试；仓储保存成功并更新配置版本后清空替换密钥输入，保存失败保留输入以便重试。最终没有替换或清除用户的真实密钥。

修复后首次专项 10 项中 9 通过，1 项断言误把输入框标签与占位文字当成密钥内容；实际 EditableText 已空。改为直接核对可编辑值，而非忽略失败。原始日志 `.codex-followup-audit-provider-red.log` 与 `.codex-followup-audit-provider-green.log` 保留。

另外补强取消任务用例：先取消确认并证明未写日志，再明确取消，恢复为未开始，验证追加记录但保留原始历史，页面重建后状态仍持久化。

数值画像解析另有可独立复现的缺陷：合法 JSON 包在 Markdown 代码块内会被拒绝。新增断言修复前以 JSONException 失败，修复为只去掉外围代码块，再执行原有严格字段与值校验；仍拒绝带额外字段或前置说明的响应。请求说明也补全建议枚举与样本可用性的校验条件，明确 REFERENCE 样本只能给出 NONE。上述两项修复有独立证据，但不能据此确定第一次真实回退的原因。

请求预览原先固定称“任务理解请求”，估时和拆分也显示这句话。已统一为“AI 建议请求”，最终 current 实际估时预览确认文案正确，取消后未发出请求。证据 `api-final-request-preview.png`。

## 完整回归与操作覆盖

最终生产代码重新构建成功，150 个 JVM 测试通过，lint 0 errors / 72 warnings，离线代码边界检查通过。

本轮完整设备回归共 313 项，312 通过、1 失败，耗时 665.691 秒。唯一失败为原生返回用例：没有明确键盘状态，单次返回后断言编辑 sheet 已关闭超时。保留失败日志，随后改为先明确显示键盘，再验证第一次返回只关键盘、第二次关 sheet、第三次返回助手，并在 finally 恢复原系统设置。该用例连续 3 次通过。累计 313 个不同方法已有通过结果，但不是一次全绿的完整运行，也没有仅凭此确定旧失败的根因。

上述完整运行发生在最后的请求预览文案和数值画像适配器改动之前。最终代码另外运行 18 项相关专项（核心流程、API 配置、原生导航、数值画像契约与工作流）。第一次专项在完成前 8 项后被 `INSTRUMENTATION_ABORTED: System has crashed.` 中断；没有将中断计为通过。日志 `.codex-followup-audit-final-special.log` 和 `.codex-followup-audit-system-crash.log` 保留。系统重启未恢复，随后关闭并冷启动同一 AVD，没有清除数据；重跑 18 项全部通过，耗时 21.8 秒，日志 `.codex-followup-audit-final-special-retry.log`。最终代码还在 current 中完成真实画像请求和预览文案验证。

85 篇功能文档是操作清单，第一轮矩阵按关联测试类整理，不能将关联类通过直接等同于每个操作的完整人工验收，更不能等同于所有输入组合通过。本轮继续补查实际远程 API、配置修改后的连接提示、取消确认与恢复历史。

## 收尾状态

当前模拟器只保留 `com.swan1127.repland.current`，临时 QA 与测试包再次卸载。实际版本 0.1.36-internal / 1036，已拉取安装包核对 SHA-256：`5D4090CE3420FF87BDB7B46D2EC1F7F322FFED5A9CA6ADE08A8AA74AD8CD70DE`，与最终 internal 构建相同。覆盖安装与模拟器冷启动均保留用户密钥及应用数据；冷启动后重新点击测试连接仍成功，证据 `api-final-config-after-cold-start.png`、`api-final-cold-start-connection.png`。冷启动后系统时区回到 GMT，最后 18 项专项在该时区执行；随后恢复为 Asia/Shanghai，不构成完整跨时区验收。

保留两项合成验证任务：Read a book 已完成且明确补录累计投入 10 分钟，Practice English 未开始、15 分钟。两者来自本轮真实助手请求；已开启助手授权与画像的个性化发送授权。连续修改草案没有确认创建为任务。没有清除用户数据或密钥。

仍需明确区分：

- 模拟器没有语音识别服务：只验证了无服务回退，识别成功路径未完成。
- 课表 PDF 只验证合成格式；其他学校的真实格式未全部验证。
- 实际闹钟准点性、OEM 后台限制、真机、其他 Android API、TalkBack 与大数据性能未完成。
- 自动化包含真实 Activity、组件、仓储及契约测试；这些证据层级不同，不能互相替代。

证据目录 `output/audit-2026-10-11/`；本轮日志前缀 `.codex-followup-audit-`。所有源码修复尚未提交或推送。
