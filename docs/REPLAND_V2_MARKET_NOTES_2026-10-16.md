# Repland 新方案市场参考记录

核验日期：2026-10-16（Asia/Shanghai）。仅采用官方产品、帮助中心和开发者资料；未购买、注册或实测相关服务。参考约十种代表路线，不声称穷尽市场。

下面的“事实”仅限页面明确内容，“采用建议”为 Repland 的产品判断。没有据官网描述推断中文识别准确率、完整离线、跨平台一致性或所有动作均无确认。

| 产品/主题 | 核实事实及边界 | 官方来源 |
| --- | --- | --- |
| Motion 自动排程 | 根据任务信息与可用时间安排，变更触发重排；排不下会显示问题。 | [排程机制](https://www.usemotion.com/help/time-management/auto-scheduling/reference-auto-scheduling/how-auto-scheduling-works-behind-the-scenes) |
| Motion 配置 | 支持工作/个人等时段与周期任务窗口；Hard Deadline 可能突破普通工作时段，这不是 Repland 应照搬的默认。 | [配置说明](https://www.usemotion.com/help/time-management/auto-scheduling/auto-scheduling-how-to-guide) |
| Motion 手动干预 | 拖动可形成锁定，但未处理且时间已过去的锁定任务可能再次被排程；不能将锁定理解为永远不动。 | [任务状态与类型](https://www.usemotion.com/help/project-management/task/reference-tasks/task-states-and-task-types) |
| Reclaim 2.0 | 后台 Agents 可自动维护个人弹性时间，聊天/MCP 修改需预览后应用；Smart Meetings 不自动改期。不要混用旧 1.0 帮助内容。 | [2.0 FAQ](https://help.reclaim.ai/en/articles/15280604-reclaim-2-0-faq) |
| Reclaim 记忆与摘要 | Chat 可使用记忆、规划摘要，但偏好记忆本身不等于自动执行授权。 | [Chat Overview](https://help.reclaim.ai/en/articles/17124965-reclaim-chat-overview-ai-assistant-for-your-calendar) |
| SkedPal 时间偏好 | Time Maps 提供分级偏好，系统在允许范围内选择时间。 | [Time Maps](https://docs.skedpal.com/time-maps/introduction-to-time-maps) |
| SkedPal 自动范围 | Instant Daily Schedule Update 覆盖当天；其他日期仍需更新日程动作。 | [设置](https://docs.skedpal.com/status-tracker/getting-started/essential-settings) |
| SkedPal 固定块 | 拖动/调整可 Pin，外部日历与忙闲状态另有行为；不代表任意导入事件可由 AI 修改。 | [日历说明](https://docs.skedpal.com/calendar/using-the-skedpal-calendar) |
| Sunsama Sunny | 可操作任务、子任务、估时、计时、周期、日历、复盘及部分设置；文档没有列清每种动作确认粒度。 | [Sunny](https://help.sunsama.com/docs/usage-guides/sunny/) |
| Sunsama 日常维护 | 有冲突/提前结束/工作顺延等自动设置；不能将每日规划向导自动显示等同自动应用全天方案。 | [设置](https://help.sunsama.com/docs/settings/user-settings/) |
| Todoist Ramble | 自然语音捕获多任务和日期/优先级等信息，支持口头修正，定位是捕获工具。 | [Ramble](https://www.todoist.com/help/todoist/todoist-and-ai/dictate-to-add-tasks-with-ramble-P1Raq7vVF) |
| Todoist 文档捕获 | 可从文本/图片/文档提取任务，但需要预览确认；并非自动排程证据。 | [Task Capture Assist](https://www.todoist.com/help/todoist/todoist-and-ai/capture-tasks-from-text-images-and-documents-cAflh0WKe) |
| TickTick | 官方更新说明助手可创建、拆解项目、整理清单与复盘，另有语音捕获；后台/写入确认细节未确认。 | [官方更新](https://help.ticktick.com/articles/7082552170989486080) |
| Structured | AI 可给批量修改/移动/删除建议，仍需用户应用；外部导入事件限制不同。 | [AI 编辑说明](https://help.structured.app/en/articles/3320002) |
| Tiimo | 文字/语音拆步骤、估时、组织视觉日程，强调降低开始与规划负担。 | [AI 规划](https://www.tiimoapp.com/product/ai-planning) |
| Morgen | AI Planner 推荐规划并由用户预览后保存，支持缺时长默认值和拆分；它代表较多人工审阅的另一条路线。 | [AI Planner](https://www.morgen.so/guides/plan-your-day-using-the-ai-planner) |
| Notion Agent | 按需助手执行多步操作，受用户及工具权限范围限制；部分外部发送另有确认。 | [Agent](https://www.notion.com/help/notion-agent) |
| Notion Custom Agents | 可由事件/时间触发后台运行，限定数据与工具，记录活动。配置版本恢复不代表所有外部动作能撤销。 | [Custom Agents](https://www.notion.com/help/custom-agents) |
| Android 后台 | 周期 WorkManager 最小间隔15分钟且可能延迟；不能据此承诺每分钟远程重排。 | [PeriodicWorkRequest](https://developer.android.com/reference/androidx/work/PeriodicWorkRequest) |
| Android 提醒 | 闹钟在进程外调度，但精度、后台及权限受平台约束。 | [Schedule alarms](https://developer.android.com/develop/background-work/services/alarms) |

## 对 Repland 的设计推导

1. 使用持续授权，常见个人操作自动执行；明确语言指令本身是一次执行授权。
2. 保留少数有实际影响的取舍，避免把所有步骤改成隐形无限权限，也避免所有步骤都问一次。
3. 用内部动作计划、规则执行、事务、来源、回执和撤销提高可靠性，外部不强制经过草案页。
4. 估计直接参与安排，真实结果仍须有来源；不靠追问全部字段制造正确性。
5. 减少无意义重排和提醒；小范围修复优先，用户暂停和纠偏始终容易。

这些是我们的推荐设计，不能写成上述产品都已做到，也不能声称借此已验证 Repland 的实际自动化效果。
