# Repland 约束总清单：供用户重新定义

整理日期：2026-10-16（Asia/Shanghai）。本文件是规则盘点与编辑工作表，**没有修改任何现行规则、App 代码或用户数据**。

覆盖范围：本次仓库根目录、docs、exploration 中的 230 份 Markdown；关键生产代码、构建配置与本会话要求。整理出 **283 条可逐项决策的记录**，另附 **330 个代码常量/校验位置**。原文快照包含历史资料，收录不等于同时有效；本表同样不等于逐条实现/验收证明。

## 如何阅读和修改

- “规格”表示文档要求；“代码现状”表示当前实现限制；“设计约定”是可更改的选型；“会话要求”只适用于本阶段/个人数据；“后续方案”不应成为当前前置条件。
- 原文中的“必须”“不可违反”属于过去的产品决定，**你可以重新定义**。原文和代码存在差异时看 X 类；不能仅改文档便声称 App 已改变。
- 来源为项目文件及本会话。没有把通用设计 skill 的建议、竞品能力、工具审批限制擅自提升为你的产品规则。
- 每条末列填：`保留`、`修改：新规则`、`删除`、`延后` 或 `待决定`。编号保持不变，方便后续同步代码、测试、文档。
- 核心规格入口：REPLAND_IDEA、CONTEXT、产品交付计划。C01–C07 是文档明确记录已由用户逐项确认的历史决定；其他项目文档不一概声称来自用户原话。
- 本轮请求只是盘点。你给出新规则后，才按新决定同步权威文档、实现和验收，不自动放宽或收紧任何规则。

## 分类导航

| 类别 | 范围 | 条数 |
| --- | --- | ---: |
| A | 产品定位与首版范围 | 12 |
| B | 任务创建、字段和身份 | 18 |
| C | 时间约束、课表与轨道 | 17 |
| D | 排序、容量与排程算法 | 17 |
| E | 草案、确认与 AI 授权范围 | 17 |
| F | 执行、生命周期与反馈 | 17 |
| G | 个人规划画像与证据 | 33 |
| H | AI、网络、模型与权限 | 22 |
| I | 提醒、复盘与系统服务 | 10 |
| J | 数据、迁移、历史和导出 | 14 |
| K | UI、导航与交互规范 | 20 |
| L | 用户名与头像 | 8 |
| M | 当前代码中的数值、格式与容量限制 | 29 |
| N | 开发、构建、测试和发布 | 18 |
| O | 延后能力的附带准入条件 | 7 |
| P | 本会话明确要求与个人课表数据 | 6 |
| X | 冲突、历史口径及必须重新决定的差异 | 18 |

## 先纠正前两次答复中容易混淆的口径

1. “正式 release 离线”是当前构建现状和旧检查，并非你永久禁止正式联网；目标已经是正式 BYOK。
2. “30 分钟粒度”要拆成自动排程、手动调整、课程钟点、草案校验；文档与代码并不完全一致。
3. 90 天/至少 3 样本/24 小时上限等是可重定的工程默认，不是科学或平台不可变规则。
4. 当前大卡片、颜色、圆角、首页密度不是 Compose 的技术限制；紧凑首页与后来的参考图存在两套设计目标。
5. 旧三模式、7 天试用、开发网关、早期森林绿、旧画像权重均不能因仍写在历史文件中而恢复为现行要求。

## A．产品定位与首版范围

性质：规格。

| 编号 | 现有规则/限制/差异 | 来源 | 你的新决定 |
| --- | --- | --- | --- |
| A01 | 产品为 Android 个人任务规划与执行辅助 App，首阶段单设备、本地优先。 | [IDEA:14](/D:/Repland/REPLAND_IDEA.md:14) | 待决定 |
| A02 | 首批核心用户是大学大一新生；长期面向容易任务失控、拖延或难以规划的用户，不局限学生。 | [IDEA:35](/D:/Repland/REPLAND_IDEA.md:35) | 待决定 |
| A03 | 首阶段不包含账号、云同步、多端协作。 | [IDEA:14](/D:/Repland/REPLAND_IDEA.md:14) | 待决定 |
| A04 | 采用单一操作模式，不提供重度参与、适度引导、轻量执行三模式。 | [IDEA:18](/D:/Repland/REPLAND_IDEA.md:18) | 待决定 |
| A05 | AI 是可选协作者；本地任务、约束、规划、提醒与执行记录可独立使用。 | [IDEA:22](/D:/Repland/REPLAND_IDEA.md:22) | 待决定 |
| A06 | 当前目标采用用户自配第三方 HTTPS 模型与 API Key（BYOK）；网关不是前置条件。 | [IDEA:20](/D:/Repland/REPLAND_IDEA.md:20) | 待决定 |
| A07 | Agent 是结构化任务/规划入口，不是任意自由聊天或任意代码执行入口。 | [CTX:390](/D:/Repland/CONTEXT.md:390) | 待决定 |
| A08 | 同时只有一个当前生效计划；草案和历史计划不能成为并列生效计划。 | [CTX:68](/D:/Repland/CONTEXT.md:68) | 待决定 |
| A09 | 当前 MVP 不提供专门周期任务模板；周期课程/休息模板另行支持。 | [CTX:169](/D:/Repland/CONTEXT.md:169) | 待决定 |
| A10 | 描述中的前后依赖可成为排序建议；首版不等于具备完整强制依赖图。 | [CTX:31](/D:/Repland/CONTEXT.md:31) | 待决定 |
| A11 | 长期目标、习惯系统、记忆曲线等复杂能力延后；有限数值规划画像另列当前范围。 | [IDEA:132](/D:/Repland/REPLAND_IDEA.md:132) | 待决定 |
| A12 | 理想功能蓝图是长期愿景，项目/里程碑/关键路径/协作等不是当前每项都必须实现的前置要求。 | [VISION:1](/D:/Repland/IDEAL_PLAN_FEATURES.md:1) | 待决定 |

## B．任务创建、字段和身份

性质：规格。

| 编号 | 现有规则/限制/差异 | 来源 | 你的新决定 |
| --- | --- | --- | --- |
| B01 | 默认仅保存到收集箱/待安排，不默认立即排程。 | [IDEA:45](/D:/Repland/REPLAND_IDEA.md:45) | 待决定 |
| B02 | 标题或一句简短事项即可保存，允许从描述形成可编辑标题。 | [IDEA:48](/D:/Repland/REPLAND_IDEA.md:48) | 待决定 |
| B03 | 分类、优先级、截止日期、计划日期、总预计时长和预计天数均可选。 | [IDEA:48](/D:/Repland/REPLAND_IDEA.md:48) | 待决定 |
| B04 | 缺项保留未分类/未设置/未知，不悄悄补课程、中优先级或 1 天。 | [IDEA:48](/D:/Repland/REPLAND_IDEA.md:48) | 待决定 |
| B05 | 未设置优先级采用可解释中性排序，但不得记录成用户选择了“中”。 | [IDEA:49](/D:/Repland/REPLAND_IDEA.md:49) | 待决定 |
| B06 | 只有用户主动选择保存并安排，才进入安排流程并追问最少必要信息。 | [IDEA:50](/D:/Repland/REPLAND_IDEA.md:50) | 待决定 |
| B07 | AI 可提取任务、日期、时间、时长、类别并建议拆分/优先级；确认前仍是建议。 | [IDEA:51](/D:/Repland/REPLAND_IDEA.md:51) | 待决定 |
| B08 | 没有总时长的任务可存在、可排序，目标规则不允许其被自动编造具体排程。 | [IDEA:81](/D:/Repland/REPLAND_IDEA.md:81) | 待决定 |
| B09 | 仅有预计天数时为软完成窗口；仅有总时长时可在主动安排后作为灵活任务处理。 | [IDEA:52](/D:/Repland/REPLAND_IDEA.md:52) | 待决定 |
| B10 | 预置课程、课外、办公、休闲四类；未分类另表示未知，不等于第五种明确偏好。 | [IDEA:53](/D:/Repland/REPLAND_IDEA.md:53) | 待决定 |
| B11 | 首版不开放自定义类别；默认相对偏好为 30/25/25/20，可由用户调整。 | [CTX:39](/D:/Repland/CONTEXT.md:39) | 待决定 |
| B12 | 截止日期按本地日历日表示，默认该日 23:59 结束，不等同具体预约时间。 | [CTX:89](/D:/Repland/CONTEXT.md:89) | 待决定 |
| B13 | 计划日期是强偏好，截止日期是最晚完成目标，具体锁定时段是保护约束，三者分开。 | [IDEA:80](/D:/Repland/REPLAND_IDEA.md:80) | 待决定 |
| B14 | 预计天数用于跨日分配已知总时长，不直接提高任务优先级。 | [IDEA:80](/D:/Repland/REPLAND_IDEA.md:80) | 待决定 |
| B15 | 预计时长与实际投入分开，预计时长不是实际工作证据，也不是直接重要性因子。 | [CTX:81](/D:/Repland/CONTEXT.md:81) | 待决定 |
| B16 | 一个任务是可复用事项身份，可有多个日期安排，不因拖动或再次放置而复制为新任务。 | [CTX:422](/D:/Repland/CONTEXT.md:422) | 待决定 |
| B17 | 替换创建新任务身份；原任务标记已替换并保留历史，不覆盖原记录。 | [CTX:274](/D:/Repland/CONTEXT.md:274) | 待决定 |
| B18 | 语音先变成可编辑识别文字，再由用户确认；识别结果不是自动保存任务或音频档案。 | [CTX:418](/D:/Repland/CONTEXT.md:418) | 待决定 |

## C．时间约束、课表与轨道

性质：规格。

| 编号 | 现有规则/限制/差异 | 来源 | 你的新决定 |
| --- | --- | --- | --- |
| C01 | 任务/事项、计划实例、轨道三层分离；实例保存日期、起止时间与轨道。 | [IDEA:57](/D:/Repland/REPLAND_IDEA.md:57) | 待决定 |
| C02 | 目标默认轨道顺序为课程/固定事项、专注、休息；自定义轨道默认排在后面。 | [IDEA:58](/D:/Repland/REPLAND_IDEA.md:58) | 待决定 |
| C03 | 目标支持自定义轨道持久化、新增、排序、隐藏和锁定；是否全部实现需另验收。 | [IDEA:58](/D:/Repland/REPLAND_IDEA.md:58) | 待决定 |
| C04 | 课程由本地 PDF 或手动录入管理；教务系统联网同步不属于当前首版。 | [IDEA:59](/D:/Repland/REPLAND_IDEA.md:59) | 待决定 |
| C05 | 解析先产生候选，用户核对/修正并确认后才写入课程。 | [CTX:374](/D:/Repland/CONTEXT.md:374) | 待决定 |
| C06 | 支持固定事项、休息、可安排时段、单日例外、学期周和重复规则。 | [IDEA:55](/D:/Repland/REPLAND_IDEA.md:55) | 待决定 |
| C07 | 课程、固定事项、硬保护休息不可直接覆盖；目标允许用户把休息设为软偏好。 | [IDEA:31](/D:/Repland/REPLAND_IDEA.md:31) | 待决定 |
| C08 | 没有可用时间时只能给待安排/补充信息入口，不假定全天空闲。 | [IDEA:61](/D:/Repland/REPLAND_IDEA.md:61) | 待决定 |
| C09 | AI 不生成任务重叠；普通未保护任务只有用户明确确认后才能手动重叠。 | [IDEA:62](/D:/Repland/REPLAND_IDEA.md:62) | 待决定 |
| C10 | 课程、固定事项、硬休息、锁定或已确认不可避免时段，须先修改约束/解除保护才可冲突调整。 | [IDEA:62](/D:/Repland/REPLAND_IDEA.md:62) | 待决定 |
| C11 | 冲突检查放在领域层，不能只靠按钮禁用或界面判断。 | [IDEA:62](/D:/Repland/REPLAND_IDEA.md:62) | 待决定 |
| C12 | 跨轨重叠不代表用户同时执行，也不能成为真实投入证据。 | [CTX:117](/D:/Repland/CONTEXT.md:117) | 待决定 |
| C13 | 单日例外只改变选定日期，不默默修改整套每周模板或过去记录。 | [CTX:366](/D:/Repland/CONTEXT.md:366) | 待决定 |
| C14 | 单日可用区间用于该日新草案，未来计划调整仍需确认。 | [CTX:370](/D:/Repland/CONTEXT.md:370) | 待决定 |
| C15 | 日/周/月视图读取同一批实例，不能维护三套互相矛盾的日历数据。 | [CTX:442](/D:/Repland/CONTEXT.md:442) | 待决定 |
| C16 | 用户可调整任务日期、时间、轨道和分配；单项手动调整保留计划历史。 | [IDEA:83](/D:/Repland/REPLAND_IDEA.md:83) | 待决定 |
| C17 | 首次使用不能强制完整配置课表/休息；可先收集任务和排序，缺时间只影响精确排程。 | [CTX:217](/D:/Repland/CONTEXT.md:217) | 待决定 |

## D．排序、容量与排程算法

性质：规格。

| 编号 | 现有规则/限制/差异 | 来源 | 你的新决定 |
| --- | --- | --- | --- |
| D01 | 任务排序决定先考虑谁，排程决定放入哪个可用时段，两者分离。 | [IDEA:66](/D:/Repland/REPLAND_IDEA.md:66) | 待决定 |
| D02 | 优先级名称统一高/中/低/不可避免，不混用必须/必做。 | [IDEA:67](/D:/Repland/REPLAND_IDEA.md:67) | 待决定 |
| D03 | 不可避免未安排时优先考虑，但不能凭空制造固定时间；确认后的时段跨 AI/本地/降级路径保护。 | [IDEA:67](/D:/Repland/REPLAND_IDEA.md:67) | 待决定 |
| D04 | 约束、锁定和用户手动顺序先于算法分数；算法不能静默推翻手动意图。 | [IDEA:68](/D:/Repland/REPLAND_IDEA.md:68) | 待决定 |
| D05 | 用户明确请求“制定计划”可提出新排序，但要展示与手动顺序的变化并确认；不等于授权解锁。 | [PLAN:17](/D:/Repland/docs/PRODUCT_DELIVERY_PLAN_2026-10-03.md:17) | 待决定 |
| D06 | V1 权重：优先级 35%、截止紧急度 25%、计划日期 15%、执行/延期证据 10%、类别偏好 10%。 | [IDEA:69](/D:/Repland/REPLAND_IDEA.md:69) | 待决定 |
| D07 | 目标 AI 修正最多 ±5 分，需理由和置信度；关闭/失败/未配置时为 0，不擅自归一化基础 95%。 | [IDEA:69](/D:/Repland/REPLAND_IDEA.md:69) | 待决定 |
| D08 | 个人画像不另加优先级权重，类别偏好不重复加权。 | [IDEA:74](/D:/Repland/REPLAND_IDEA.md:74) | 待决定 |
| D09 | 新任务加入应保持旧排序稳定，明显变化需解释。 | [IDEA:71](/D:/Repland/REPLAND_IDEA.md:71) | 待决定 |
| D10 | 手动顺序造成容量/截止风险时给非阻断警告，不自动撤销用户排序。 | [CTX:113](/D:/Repland/CONTEXT.md:113) | 待决定 |
| D11 | 目标每次明确延期提高动态紧急度；累计两次后提示重审目标/方法，不自动改任务。 | [CTX:209](/D:/Repland/CONTEXT.md:209) | 待决定 |
| D12 | 容量不足必须列未排入任务、缺口、风险和可选处理动作，不能静默挤掉任务。 | [IDEA:67](/D:/Repland/REPLAND_IDEA.md:67) | 待决定 |
| D13 | 逾期任务保持活动，用户选择延期/降低目标/拆分/取消，不自动取消或暗改截止。 | [CTX:93](/D:/Repland/CONTEXT.md:93) | 待决定 |
| D14 | 默认滚动规划窗口 30 天；更远任务仍保存，不要求全部展开为每日时段。 | [CTX:242](/D:/Repland/CONTEXT.md:242) | 待决定 |
| D15 | 目标自动排程用 30 分钟粒度，手动调整可精确到 15 分钟；实现差异见 X02。 | [IDEA:79](/D:/Repland/REPLAND_IDEA.md:79) | 待决定 |
| D16 | 自动排程把已知时长向上取整为完整量子，不能把量子当实际效率或强制预约长度。 | [CTX:161](/D:/Repland/CONTEXT.md:161) | 待决定 |
| D17 | 估时修正仅形成建议，不改原估计，不在连续重排时累乘系数。 | [IDEA:74](/D:/Repland/REPLAND_IDEA.md:74) | 待决定 |

## E．草案、确认与 AI 授权范围

性质：规格。

| 编号 | 现有规则/限制/差异 | 来源 | 你的新决定 |
| --- | --- | --- | --- |
| E01 | Agent/AI/本地规则的批量安排先生成可持久化 Proposal，再提交正式数据。 | [IDEA:87](/D:/Repland/REPLAND_IDEA.md:87) | 待决定 |
| E02 | 草案应呈现任务、实例、受影响事项、冲突、风险、未排原因、理由和逐项决定。 | [IDEA:88](/D:/Repland/REPLAND_IDEA.md:88) | 待决定 |
| E03 | 目标支持草案内编辑任务字段与时段/轨道，以及删除、忽略、暂不安排。 | [IDEA:89](/D:/Repland/REPLAND_IDEA.md:89) | 待决定 |
| E04 | 一次确认将任务、安排和决策原子提交，任一失败不能留下部分结果。 | [IDEA:90](/D:/Repland/REPLAND_IDEA.md:90) | 待决定 |
| E05 | 独立制定计划可不输入对话；排序和时段确认后一起生效。 | [CTX:51](/D:/Repland/CONTEXT.md:51) | 待决定 |
| E06 | 用户可接受整份或编辑后确认；明确选择“由 AI 决定”只授权当前决定范围。 | [CTX:55](/D:/Repland/CONTEXT.md:55) | 待决定 |
| E07 | 当前冲突的 AI 授权不能扩展到无关任务、状态、全局设置或永久自治。 | [CTX:60](/D:/Repland/CONTEXT.md:60) | 待决定 |
| E08 | 未确认草案本地自动保存，切页/重启可恢复，并可明确丢弃。 | [IDEA:91](/D:/Repland/REPLAND_IDEA.md:91) | 待决定 |
| E09 | 草案明确未确认，不替换当前计划，不产生正式时段提醒。 | [IDEA:91](/D:/Repland/REPLAND_IDEA.md:91) | 待决定 |
| E10 | 确认前重检任务/约束/版本，变化时刷新或调整，拒绝陈旧预览覆盖新事实。 | [IDEA:91](/D:/Repland/REPLAND_IDEA.md:91) | 待决定 |
| E11 | 重复确认不得重复新增；保存中禁止重复提交，成功以真实事务回执为准。 | [PLAN:129](/D:/Repland/docs/PRODUCT_DELIVERY_PLAN_2026-10-03.md:129) | 待决定 |
| E12 | 重排读取已有任务、计划、约束、执行记录，不能重建重复任务代替调整。 | [IDEA:92](/D:/Repland/REPLAND_IDEA.md:92) | 待决定 |
| E13 | 安排今天默认只影响选定当天未保护部分，跨日影响必须另行展示。 | [PLAN:29](/D:/Repland/docs/PRODUCT_DELIVERY_PLAN_2026-10-03.md:29) | 待决定 |
| E14 | 更改全局偏好/可用时间成为新草案输入，不立即批量重写当前计划。 | [CTX:64](/D:/Repland/CONTEXT.md:64) | 待决定 |
| E15 | 单项手动调整可即时生效并记录版本；批量调整走预览与确认。 | [IDEA:93](/D:/Repland/REPLAND_IDEA.md:93) | 待决定 |
| E16 | 草案补充指令围绕当前草案；失败保留输入和原草案，不上传完整对话历史。 | [PRIV:29](/D:/Repland/docs/PRIVACY.md:29) | 待决定 |
| E17 | 实际新增/调整/写入数量以业务回执为准，不把成功提示虚构为长期聊天历史。 | [PRIV:29](/D:/Repland/docs/PRIVACY.md:29) | 待决定 |

## F．执行、生命周期与反馈

性质：规格。

| 编号 | 现有规则/限制/差异 | 来源 | 你的新决定 |
| --- | --- | --- | --- |
| F01 | 到点只提醒；主动开始或明确补录才算执行，补录标记来源。 | [C07:11](/D:/Repland/docs/CONSISTENCY_REVIEW_2026-10-06.md:11) | 待决定 |
| F02 | 一个任务可有多个安排和执行会话；一轮结束不等于整项完成。 | [IDEA:98](/D:/Repland/REPLAND_IDEA.md:98) | 待决定 |
| F03 | 执行入口支持主动开始、暂停、恢复、结束和修正实际时长。 | [IDEA:97](/D:/Repland/REPLAND_IDEA.md:97) | 待决定 |
| F04 | 执行会话后台/重启可恢复，保留开始、暂停、恢复、结束和修正记录。 | [IDEA:101](/D:/Repland/REPLAND_IDEA.md:101) | 待决定 |
| F05 | 目标一轮结束可选择继续、部分完成、完成任务、暂不处理；结论由用户确认。 | [IDEA:99](/D:/Repland/REPLAND_IDEA.md:99) | 待决定 |
| F06 | 已结束但未确认的计划实例进入待确认，不自动算失败、延期或完成。 | [IDEA:100](/D:/Repland/REPLAND_IDEA.md:100) | 待决定 |
| F07 | 时段结果和任务整体状态独立，完成一段不自动完成整项。 | [CTX:149](/D:/Repland/CONTEXT.md:149) | 待决定 |
| F08 | 记录反馈本身是证据，不自动改变状态；实际耗时可未知，不拿预计时长冒充。 | [CTX:278](/D:/Repland/CONTEXT.md:278) | 待决定 |
| F09 | 延期原因可选，不强制用户解释或作道德评价。 | [CTX:286](/D:/Repland/CONTEXT.md:286) | 待决定 |
| F10 | 部分完成后按剩余工作生成调整草案，不能直接滚动到下一天。 | [CTX:290](/D:/Repland/CONTEXT.md:290) | 待决定 |
| F11 | 每日复盘可选，跳过不能推断任务状态；AI 复盘不能修改计划或状态。 | [CTX:226](/D:/Repland/CONTEXT.md:226) | 待决定 |
| F12 | 用户完成整项后移除未来未执行安排，历史保留；空出时间的重分配仍是草案。 | [CTX:222](/D:/Repland/CONTEXT.md:222) | 待决定 |
| F13 | 恢复已完成/取消任务是独立显式操作，保留原结果，重新规划而不抹历史。 | [CTX:157](/D:/Repland/CONTEXT.md:157) | 待决定 |
| F14 | 目标批量改状态仍逐任务生成记录，不合并身份、不允许 AI 推断。 | [CTX:270](/D:/Repland/CONTEXT.md:270) | 待决定 |
| F15 | 反馈更正追加新记录，派生计算使用有效版本，不能覆盖原始历史。 | [CTX:133](/D:/Repland/CONTEXT.md:133) | 待决定 |
| F16 | 目标日终可留存用户最终计划表，但不自动生成生效的未来计划。 | [CTX:234](/D:/Repland/CONTEXT.md:234) | 待决定 |
| F17 | 用户预计完成天数优先，AI 可建议不同周期，但不直接替换用户值。 | [CTX:294](/D:/Repland/CONTEXT.md:294) | 待决定 |

## G．个人规划画像与证据

性质：规格/工程口径。

| 编号 | 现有规则/限制/差异 | 来源 | 你的新决定 |
| --- | --- | --- | --- |
| G01 | 画像用于规划参数，不评价人格、能力、自律、心理或健康，不设统一总分。 | [IDEA:105](/D:/Repland/REPLAND_IDEA.md:105) | 待决定 |
| G02 | 程序计算与持久化，AI 主要消费相关摘要、表达现状，不生成/改写画像分数。 | [IDEA:107](/D:/Repland/REPLAND_IDEA.md:107) | 待决定 |
| G03 | 用户同时看到数值、单位、范围、样本量、时间窗、更新时间、来源和可用状态。 | [PLAN:73](/D:/Repland/docs/PRODUCT_DELIVERY_PLAN_2026-10-03.md:73) | 待决定 |
| G04 | 缺数据为未知/待积累，不填默认中间分，不把缺记录解释为没执行。 | [IDEA:108](/D:/Repland/REPLAND_IDEA.md:108) | 待决定 |
| G05 | 六个候选维度：估时修正、执行段分布、重排中断、类别偏好、时段偏好、安排方式偏好。 | [IDEA:109](/D:/Repland/REPLAND_IDEA.md:109) | 待决定 |
| G06 | 首批只实现估时/执行段/既有类别偏好，其余三维度未知，不从延期或拖动猜测。 | [NUM:3](/D:/Repland/docs/features/H01_NUMERIC_CONTRACT.md:3) | 待决定 |
| G07 | 统计现象和明确偏好分开；常在晚间做事不证明喜欢或高效，重排不等于拖延。 | [IDEA:110](/D:/Repland/REPLAND_IDEA.md:110) | 待决定 |
| G08 | numeric-v1 统计计算日及之前 89 个本地自然日，共 90 天，不是科学必然窗口。 | [NUM:5](/D:/Repland/docs/features/H01_NUMERIC_CONTRACT.md:5) | 待决定 |
| G09 | 0 样本未知，1–2 个仅供参考，至少 3 个才可用于建议；门槛不等于统计显著性。 | [NUM:6](/D:/Repland/docs/features/H01_NUMERIC_CONTRACT.md:6) | 待决定 |
| G10 | 排除未来记录，旧完成任务不能靠重新确认变成近期样本，日期/时区/到时资格参与版本。 | [NUM:5](/D:/Repland/docs/features/H01_NUMERIC_CONTRACT.md:5) | 待决定 |
| G11 | 首次主动开始时同事务冻结可信原估计及类别；之后改字段/替换/恢复不改历史快照。 | [NUM:5](/D:/Repland/docs/features/H01_NUMERIC_CONTRACT.md:5) | 待决定 |
| G12 | 旧任务已经开始但无可信快照，原估计保持未知，不拿现在字段补造过去。 | [NUM:7](/D:/Repland/docs/features/H01_NUMERIC_CONTRACT.md:7) | 待决定 |
| G13 | 估时仅用当前明确完成、可信原估计及明确确认累计实际投入的可比任务。 | [NUM:7](/D:/Repland/docs/features/H01_NUMERIC_CONTRACT.md:7) | 待决定 |
| G14 | 更正/恢复/新反馈使旧总投入确认失效，取最后有效确认，不把手填与会话重复相加。 | [NUM:7](/D:/Repland/docs/features/H01_NUMERIC_CONTRACT.md:7) | 待决定 |
| G15 | 每任务一个 actualTotal/originalEstimate 样本，两者大于 0，按冻结类别取中位数。 | [NUM:9](/D:/Repland/docs/features/H01_NUMERIC_CONTRACT.md:9) | 待决定 |
| G16 | 未分类可有全局摘要，但未知类别不能借全局系数获得类别建议；异常不暗中剔除。 | [NUM:9](/D:/Repland/docs/features/H01_NUMERIC_CONTRACT.md:9) | 待决定 |
| G17 | 执行段只统计主动开始且已结束会话的运行分钟，暂停不计，会话 ID 去重。 | [NUM:10](/D:/Repland/docs/features/H01_NUMERIC_CONTRACT.md:10) | 待决定 |
| G18 | 关联耗时更正后保守排除会话；不把累计手填当单轮，不用进度反推总耗时。 | [NUM:10](/D:/Repland/docs/features/H01_NUMERIC_CONTRACT.md:10) | 待决定 |
| G19 | 分布显示中位数和 Q1–Q3，用 R7 插值，最多两位小数；正且小于 0.01 显示 <0.01，不显示零。 | [NUM:10](/D:/Repland/docs/features/H01_NUMERIC_CONTRACT.md:10) | 待决定 |
| G20 | 没有执行段样本不默认 25/30 分钟。 | [NUM:10](/D:/Repland/docs/features/H01_NUMERIC_CONTRACT.md:10) | 待决定 |
| G21 | 复用类别原权重，默认与用户明确设置分开，不新增或重复加权。 | [NUM:11](/D:/Repland/docs/features/H01_NUMERIC_CONTRACT.md:11) | 待决定 |
| G22 | 版本覆盖规则、窗口、来源、排除和设置；读取/解码/持久化失败保留最后可信快照。 | [NUM:12](/D:/Repland/docs/features/H01_NUMERIC_CONTRACT.md:12) | 待决定 |
| G23 | 用户可改明确偏好、纠正原事实、排除样本、停用参数；用户声明不伪装成程序统计。 | [IDEA:112](/D:/Repland/REPLAND_IDEA.md:112) | 待决定 |
| G24 | AI 描述绑定参数快照，只表达近期现象/依据/可选建议，不变成永久标签。 | [IDEA:113](/D:/Repland/REPLAND_IDEA.md:113) | 待决定 |
| G25 | AI 描述不回写参数，也不成为下一轮画像证据，防止自我强化。 | [IDEA:113](/D:/Repland/REPLAND_IDEA.md:113) | 待决定 |
| G26 | AI 描述由用户主动生成/刷新，按版本缓存；打开页面不自动联网。 | [NUM:12](/D:/Repland/docs/features/H01_NUMERIC_CONTRACT.md:12) | 待决定 |
| G27 | 取消、关开关、换配置或新证据后的迟到响应拒收；失败标明本地摘要。 | [NUM:14](/D:/Repland/docs/features/H01_NUMERIC_CONTRACT.md:14) | 待决定 |
| G28 | 画像请求仅参数值/单位/样本量/时间窗/版本/匿名参数 ID，无任务文本、原日志和密钥。 | [NUM:14](/D:/Repland/docs/features/H01_NUMERIC_CONTRACT.md:14) | 待决定 |
| G29 | AI 仅引用参数 ID 及精确值，选预设规划建议；事实由前端据引用渲染，拒绝捏造数字。 | [NUM:14](/D:/Repland/docs/features/H01_NUMERIC_CONTRACT.md:14) | 待决定 |
| G30 | 采纳估时建议需可信已知原预计和同类别可用系数，单独确认后才改目标字段。 | [NUM:13](/D:/Repland/docs/features/H01_NUMERIC_CONTRACT.md:13) | 待决定 |
| G31 | 保存未修正基准及引用版本，重复采纳/重排不累乘；任务变化后旧建议拒写。 | [NUM:13](/D:/Repland/docs/features/H01_NUMERIC_CONTRACT.md:13) | 待决定 |
| G32 | 参数只影响新建议/草案，不改状态、截止、原优先级、类别权重、手动顺序或保护段。 | [IDEA:115](/D:/Repland/REPLAND_IDEA.md:115) | 待决定 |
| G33 | 停用保留来源和数值，但禁止该参数建议/AI 发送；基础本地规划仍可用。 | [NUM:6](/D:/Repland/docs/features/H01_NUMERIC_CONTRACT.md:6) | 待决定 |

## H．AI、网络、模型与权限

性质：规格/接口约束。

| 编号 | 现有规则/限制/差异 | 来源 | 你的新决定 |
| --- | --- | --- | --- |
| H01 | 目标 AI 总开关及理解/分析/安排/复盘/个性化子开关默认开启，但配置、授权、请求条件仍需满足。 | [IDEA:22](/D:/Repland/REPLAND_IDEA.md:22) | 待决定 |
| H02 | 未配置、未完成发送确认或关闭 AI 时，不发送业务模型请求。 | [IDEA:22](/D:/Repland/REPLAND_IDEA.md:22) | 待决定 |
| H03 | 连接测试是独立显式操作，发送固定测试文字而不发个人任务，不等同业务 AI 请求。 | [PRIV:21](/D:/Repland/docs/PRIVACY.md:21) | 待决定 |
| H04 | 助手只允许白名单业务：任务查询、计划预览、排序解释、可编辑新增/调整建议。 | [PRIV:17](/D:/Repland/docs/PRIVACY.md:17) | 待决定 |
| H05 | 模型不执行任意代码、SQL 或设置修改，返回值不能当权威写入命令。 | [PRIV:17](/D:/Repland/docs/PRIVACY.md:17) | 待决定 |
| H06 | 查询事实与排序理由来自本地统一操作层，不能把模型臆造内容当真实任务。 | [PRIV:17](/D:/Repland/docs/PRIVACY.md:17) | 待决定 |
| H07 | 关闭 AI 可对明确查询句式本地只读回退，不承诺任意自然语言都可离线理解。 | [PRIV:17](/D:/Repland/docs/PRIVACY.md:17) | 待决定 |
| H08 | 联网只发该功能必要输入/相关上下文，不默认发全设备或全历史。 | [PRIV:21](/D:/Repland/docs/PRIVACY.md:21) | 待决定 |
| H09 | 安排上下文最多 50 个未完成任务、当前最多 8 项草案。 | [PRIV:27](/D:/Repland/docs/PRIVACY.md:27) | 待决定 |
| H10 | 仅明确提及/草案引用的最多 5 项任务，各附最近 3 条有效用户反馈，每段反馈最多 200 字。 | [PRIV:27](/D:/Repland/docs/PRIVACY.md:27) | 待决定 |
| H11 | 安排助手不发任务备注、全库历史或原始执行会话，首轮不发临时解析草案身份。 | [PRIV:27](/D:/Repland/docs/PRIVACY.md:27) | 待决定 |
| H12 | 补充修改只加本次指令和草案内部引用，不发完整聊天历史；失败保留输入。 | [PRIV:29](/D:/Repland/docs/PRIVACY.md:29) | 待决定 |
| H13 | 现有任务调整只引用所选日期安排，不让模型修改任务状态、优先级或总时长。 | [PRIV:27](/D:/Repland/docs/PRIVACY.md:27) | 待决定 |
| H14 | 用户可取消请求；输入、来源、工作流、配置变化取消旧请求并拒收旧响应。 | [PRIV:25](/D:/Repland/docs/PRIVACY.md:25) | 待决定 |
| H15 | 取消只能断开本地传输，不能保证供应商未接收/未计费；已取消请求不自动重试。 | [PRIV:25](/D:/Repland/docs/PRIVACY.md:25) | 待决定 |
| H16 | 供应商留存/训练按其条款，不能由 App 承诺第三方绝不留存或训练。 | [PRIV:21](/D:/Repland/docs/PRIVACY.md:21) | 待决定 |
| H17 | 网络、鉴权、超时、格式错误等不丢输入/草案/当前计划，退回本地能力。 | [IDEA:198](/D:/Repland/REPLAND_IDEA.md:198) | 待决定 |
| H18 | 变更服务地址且原来已有密钥时必须重新输入新服务密钥，避免向新服务转发旧密钥。 | [AIKEY:34](/D:/Repland/android/app/src/main/java/com/swan1127/repland/data/security/SecureAiProviderConfigRepository.kt:34) | 待决定 |
| H19 | 当前 Provider 地址限定 HTTPS、有效主机、无 userInfo/查询串/片段，模型名非空。 | [AIKEY:33](/D:/Repland/android/app/src/main/java/com/swan1127/repland/data/security/SecureAiProviderConfigRepository.kt:33) | 待决定 |
| H20 | 密钥由 Android Keystore 支持的加密存储管理，不进 Room、导出、日志或错误报告，不硬编码 APK。 | [IDEA:20](/D:/Repland/REPLAND_IDEA.md:20) | 待决定 |
| H21 | AI 使用有版本的类型化契约，校验请求身份、任务引用、时间范围和保护约束，不接受越权输出。 | [CONTRACT:11](/D:/Repland/android/app/src/main/java/com/swan1127/repland/domain/model/AiAdvisorContract.kt:11) | 待决定 |
| H22 | 后续账号/同步授权不能借用 AI 同意；账号、网关不应成为读取本地数据的条件。 | [EXT:33](/D:/Repland/docs/EXTENSION_READINESS.md:33) | 待决定 |

## I．提醒、复盘与系统服务

性质：规格/代码现状。

| 编号 | 现有规则/限制/差异 | 来源 | 你的新决定 |
| --- | --- | --- | --- |
| I01 | 提醒本地调度，使用设备本地时间，不是云推送或强制闹钟。 | [CTX:213](/D:/Repland/CONTEXT.md:213) | 待决定 |
| I02 | 开启提醒并获得 Android 通知权限后才能发通知，用户可以关闭。 | [PRIV:33](/D:/Repland/docs/PRIVACY.md:33) | 待决定 |
| I03 | 草案不创建正式提醒，只依据已确认计划和有效任务条件安排。 | [PRIV:33](/D:/Repland/docs/PRIVACY.md:33) | 待决定 |
| I04 | 没有截止日期不发截止提醒，没有具体时段不发开始提醒。 | [CTX:213](/D:/Repland/CONTEXT.md:213) | 待决定 |
| I05 | 目标包含开始、截止、待确认实例及复盘提醒；当前代码实际仅有三种 ReminderKind，待确认提醒不能冒称已有。 | [IDEA:130](/D:/Repland/REPLAND_IDEA.md:130) | 待决定 |
| I06 | 当前提醒默认关闭。 | [REMIND:10](/D:/Repland/android/app/src/main/java/com/swan1127/repland/domain/model/Reminders.kt:10) | 待决定 |
| I07 | 当前截止提醒固定在截止日前一天 20:00，且只调度未来触发点。 | [REMIND:34](/D:/Repland/android/app/src/main/java/com/swan1127/repland/domain/model/Reminders.kt:34) | 待决定 |
| I08 | 当前每日复盘提醒固定 20:30，本地下一次时间，没有独立自定义时间功能。 | [REMIND:32](/D:/Repland/android/app/src/main/java/com/swan1127/repland/domain/model/Reminders.kt:32) | 待决定 |
| I09 | 当前提醒规划函数无当前计划时返回空，包含截止提醒；这是代码限制，可重设。 | [REMIND:41](/D:/Repland/android/app/src/main/java/com/swan1127/repland/domain/model/Reminders.kt:41) | 待决定 |
| I10 | 提醒需处理权限拒绝/授予/撤回、设备重启、时区变化和后台限制；不得据模拟器宣称所有真机均验证。 | [TEST:21](/D:/Repland/docs/TESTING.md:21) | 待决定 |

## J．数据、迁移、历史和导出

性质：规格/工程要求。

| 编号 | 现有规则/限制/差异 | 来源 | 你的新决定 |
| --- | --- | --- | --- |
| J01 | 任务、约束、计划、历史、执行、设置和画像存于设备私有存储。 | [PRIV:7](/D:/Repland/docs/PRIVACY.md:7) | 待决定 |
| J02 | 当前无广告/分析/远程崩溃 SDK，关闭 Android 自动备份；这是当前隐私/架构选择。 | [PRIV:7](/D:/Repland/docs/PRIVACY.md:7) | 待决定 |
| J03 | 核心任务、生命周期日志、约束、轨道、实例、计划历史、草案/决定、会话、AI 开关及安全配置需持久化。 | [IDEA:120](/D:/Repland/REPLAND_IDEA.md:120) | 待决定 |
| J04 | 确认的计划改动保留历史，记录理由、AI 提议和用户改动；恢复本身也创建新版本。 | [IDEA:121](/D:/Repland/REPLAND_IDEA.md:121) | 待决定 |
| J05 | 恢复只恢复排序/安排，不回退任务状态/执行事实，不复活结束任务、不重启过去时段。 | [C07:13](/D:/Repland/docs/CONSISTENCY_REVIEW_2026-10-06.md:13) | 待决定 |
| J06 | 执行误操作通过独立撤销/更正，不能借恢复计划静默撤销。 | [IDEA:122](/D:/Repland/REPLAND_IDEA.md:122) | 待决定 |
| J07 | 迁移不能破坏任务、课表、安排、日志、历史；旧默认值不能冒充用户主动输入。 | [IDEA:123](/D:/Repland/REPLAND_IDEA.md:123) | 待决定 |
| J08 | 保存任务和消费创建草稿在同一事务完成，失败不能丢草稿或生成重复任务。 | [PRIV:9](/D:/Repland/docs/PRIVACY.md:9) | 待决定 |
| J09 | 创建草稿本地保留，关抽屉不创建任务，明确放弃只清该草稿。 | [PRIV:9](/D:/Repland/docs/PRIVACY.md:9) | 待决定 |
| J10 | 导出由用户选位置，含当前业务及暂存草稿；不是云同步，不含 API Key。 | [PRIV:9](/D:/Repland/docs/PRIVACY.md:9) | 待决定 |
| J11 | 当前业务 JSON 不包含用户名/头像；全量本地快照和业务 JSON 是不同备份形式。 | [PROFILE:37](/D:/Repland/DESIGN_PERSONAL_PROFILE.md:37) | 待决定 |
| J12 | 清除本地数据需明确确认，含任务/计划/日志/设置/资料；不能删除已经导出的外部文件。 | [CTX:386](/D:/Repland/CONTEXT.md:386) | 待决定 |
| J13 | 读取失败保留最后可信快照与用户输入，不能把失败当空数据或报保存成功。 | [PLAN:33](/D:/Repland/docs/PRODUCT_DELIVERY_PLAN_2026-10-03.md:33) | 待决定 |
| J14 | 并发编辑、配置变化、来源过期时旧预览/响应拒写，提供刷新/重试。 | [PLAN:218](/D:/Repland/docs/PRODUCT_DELIVERY_PLAN_2026-10-03.md:218) | 待决定 |

## K．UI、导航与交互规范

性质：设计约定/目标。

| 编号 | 现有规则/限制/差异 | 来源 | 你的新决定 |
| --- | --- | --- | --- |
| K01 | 原目标主导航为今日/任务/Agent/我的，时间工作台二级；当前设计改为日程/任务/助手/我的，需重新定名。 | [IDEA:127](/D:/Repland/REPLAND_IDEA.md:127) | 待决定 |
| K02 | 原目标首页是紧凑焦点工作台；当前参考图设计突出大新增卡片，两种方向需用户重选。 | [IDEA:128](/D:/Repland/REPLAND_IDEA.md:128) | 待决定 |
| K03 | 目标焦点卡片内上下切换，手势仅接管卡内起始操作，避免阻断页面滚动；不视为当前全部实现。 | [IDEA:129](/D:/Repland/REPLAND_IDEA.md:129) | 待决定 |
| K04 | 当前视觉约定：#F7F8FC 背景、白卡、#242831 正文、#606572 辅助、#326B75 行动色。 | [VIS:7](/D:/Repland/DESIGN_PRIORITY_SCHEDULE.md:7) | 待决定 |
| K05 | 事项采用浅蓝/薄荷/浅紫/米黄/淡粉，按 ID 稳定分配，跨视图一致，不逐帧随机。 | [VIS:9](/D:/Repland/DESIGN_PRIORITY_SCHEDULE.md:9) | 待决定 |
| K06 | 系统中文 SansSerif、语义字体角色、时间等宽数字，不额外下载字体。 | [VIS:10](/D:/Repland/DESIGN_PRIORITY_SCHEDULE.md:10) | 待决定 |
| K07 | 主要卡片 24dp 圆角，左右 20dp，组间 16–24dp；属于可改设计数值。 | [VIS:11](/D:/Repland/DESIGN_PRIORITY_SCHEDULE.md:11) | 待决定 |
| K08 | 主页面最大宽度 720dp，宽屏居中；窄屏/大字体允许换行和展开。 | [COPY:31](/D:/Repland/DESIGN_COPY.md:31) | 待决定 |
| K09 | 主要按钮触控高度至少 48dp，保留原生 ripple、禁用、选中和可访问语义。 | [VIS:11](/D:/Repland/DESIGN_PRIORITY_SCHEDULE.md:11) | 待决定 |
| K10 | 总计按真实时间边界拆重叠区间，组内按优先级排列；只改变投影，不改安排。 | [VIS:32](/D:/Repland/DESIGN_PRIORITY_SCHEDULE.md:32) | 待决定 |
| K11 | 详细按优先级分组并保留原始起止时间/备注；固定安排单列，未设优先级最后。 | [VIS:32](/D:/Repland/DESIGN_PRIORITY_SCHEDULE.md:32) | 待决定 |
| K12 | 总计的同时事项窄屏允许横向滚动；不能泛化成全 App 禁止任何横向滚动。 | [VIS:32](/D:/Repland/DESIGN_PRIORITY_SCHEDULE.md:32) | 待决定 |
| K13 | 深色模式使用对应背景和文字，正文不随优先级背景一起变淡，需保证可读性。 | [VIS:40](/D:/Repland/DESIGN_PRIORITY_SCHEDULE.md:40) | 待决定 |
| K14 | 视觉改版保留任务查看、时间/备注编辑、移除、执行、课程和轨道入口。 | [VIS:34](/D:/Repland/DESIGN_PRIORITY_SCHEDULE.md:34) | 待决定 |
| K15 | 动效遵循系统关闭动画设置，能中断/反向，动作不等待动画完成。 | [MOTION:30](/D:/Repland/DESIGN_MOTION.md:30) | 待决定 |
| K16 | 动画仅表现 UI 状态，不触发/延迟保存、不复制业务数据、不按帧重算规划。 | [MOTION:21](/D:/Repland/DESIGN_MOTION.md:21) | 待决定 |
| K17 | 切换只保留当前内容处理操作，导航稳定，离开主题树注销设置监听。 | [MOTION:26](/D:/Repland/DESIGN_MOTION.md:26) | 待决定 |
| K18 | 当前按压缩放按钮 96.5%、卡片 97.5%、主加号 93%，松开有限弹簧；数值可改。 | [VIS:42](/D:/Repland/DESIGN_PRIORITY_SCHEDULE.md:42) | 待决定 |
| K19 | 当前节奏为按下 90ms、释放 140ms、页面 240ms、展开 260ms、收起 180ms；工程选择而非平台限制。 | [MOTIONCODE:32](/D:/Repland/android/app/src/main/java/com/swan1127/repland/ui/theme/Motion.kt:32) | 待决定 |
| K20 | 演示页明确标只读/不写真实计划，复用真实组件，演示数据不充当用户事实。 | [VIS:44](/D:/Repland/DESIGN_PRIORITY_SCHEDULE.md:44) | 待决定 |

## L．用户名与头像

性质：规格/代码现状。

| 编号 | 现有规则/限制/差异 | 来源 | 你的新决定 |
| --- | --- | --- | --- |
| L01 | 个人资料是本地资料，不增加账号/登录/云同步。 | [PROFILE:37](/D:/Repland/DESIGN_PERSONAL_PROFILE.md:37) | 待决定 |
| L02 | 默认用户名“Repland 用户”，不擅自假定用户姓名。 | [PROFILE:25](/D:/Repland/DESIGN_PERSONAL_PROFILE.md:25) | 待决定 |
| L03 | 用户名去首尾空白后 1–24 个 Unicode 码点，不可空白、换行或控制字符。 | [PNAME:5](/D:/Repland/android/app/src/main/java/com/swan1127/repland/domain/model/PersonalProfileName.kt:5) | 待决定 |
| L04 | 编辑有标签、保存/取消和原位错误；非法或超长输入保留供修改。 | [PROFILE:25](/D:/Repland/DESIGN_PERSONAL_PROFILE.md:25) | 待决定 |
| L05 | 头像用系统照片选择器只选图片，取消不变，不申请整库读取权限。 | [PROFILE:26](/D:/Repland/DESIGN_PERSONAL_PROFILE.md:26) | 待决定 |
| L06 | 头像后台按方向解码、中心正方裁剪、最多 384×384，私有 JPEG，不保留原始大图/临时 URI 依赖。 | [PROFILE:27](/D:/Repland/DESIGN_PERSONAL_PROFILE.md:27) | 待决定 |
| L07 | 头像原子替换，失败保留旧头像，可恢复默认。 | [PROFILE:27](/D:/Repland/DESIGN_PERSONAL_PROFILE.md:27) | 待决定 |
| L08 | 姓名/头像跨切页、重启、覆盖更新保留；清除全部本地数据时一并清理。 | [PROFILE:28](/D:/Repland/DESIGN_PERSONAL_PROFILE.md:28) | 待决定 |

## M．当前代码中的数值、格式与容量限制

性质：代码现状。

| 编号 | 现有规则/限制/差异 | 来源 | 你的新决定 |
| --- | --- | --- | --- |
| M01 | 预计天数字段当前仅允许未知或 1–30 天。 | [TASK:88](/D:/Repland/android/app/src/main/java/com/swan1127/repland/domain/model/Task.kt:88) | 待决定 |
| M02 | 预计总时长当前仅允许未知或 1–1440 分钟；这是总时长上限，长项目需改模型/校验。 | [TASK:89](/D:/Repland/android/app/src/main/java/com/swan1127/repland/domain/model/Task.kt:89) | 待决定 |
| M03 | 从描述自动形成的名称取首行最多 24 个 UTF-16 字符后加省略号；这不是用户手填标题最大长度。 | [TASK:77](/D:/Repland/android/app/src/main/java/com/swan1127/repland/domain/model/Task.kt:77) | 待决定 |
| M04 | 单条反馈实际耗时可未知或 1–1440 分钟。 | [LIFE:42](/D:/Repland/android/app/src/main/java/com/swan1127/repland/domain/model/TaskExecution.kt:42) | 待决定 |
| M05 | 普通反馈进度 0–100；完成反馈进度为空或 100。 | [LIFE:43](/D:/Repland/android/app/src/main/java/com/swan1127/repland/domain/model/TaskExecution.kt:43) | 待决定 |
| M06 | 部分完成当前要求 1–99 进度并且完成内容非空。 | [LIFE:90](/D:/Repland/android/app/src/main/java/com/swan1127/repland/domain/model/TaskExecution.kt:90) | 待决定 |
| M07 | 当前状态机：未开始/进行中/延期是活动态；完成/取消可显式恢复；已替换不能直接恢复。 | [LIFE:81](/D:/Repland/android/app/src/main/java/com/swan1127/repland/domain/model/TaskExecution.kt:81) | 待决定 |
| M08 | 起点 00:00–23:59，终点可 24:00，结束须晚于开始；单个区间不能直接跨午夜。 | [TIME:92](/D:/Repland/android/app/src/main/java/com/swan1127/repland/domain/model/TimeArrangement.kt:92) | 待决定 |
| M09 | 原始数字输入只认 ASCII 0–9 整数，非法值保留报错，不以截断/进位变成另一个事实。 | [POLICY:5](/D:/Repland/android/app/src/main/java/com/swan1127/repland/domain/model/UserNumericInput.kt:5) | 待决定 |
| M10 | 手动放入任务的安排段：未知总时长暂用 30 分钟，已知合法总时长夹到 15–240 分钟，不改任务总时长。 | [POLICY:18](/D:/Repland/android/app/src/main/java/com/swan1127/repland/domain/model/UserNumericInput.kt:18) | 待决定 |
| M11 | 自动/草案编辑与部分确认校验仍要求 30 分钟起止对齐；不能据按钮“15 分”认定整条链路支持 15 分钟。 | [PLANNER:60](/D:/Repland/android/app/src/main/java/com/swan1127/repland/domain/model/Plan.kt:60) | 待决定 |
| M12 | 未分类中性分 50；截止未知为 50，逾期 100、当天 98、隔天 92、2 天 84、3–5 天 74、6–10 天 64、其余 50。 | [RANK:100](/D:/Repland/android/app/src/main/java/com/swan1127/repland/domain/model/PlanningPriority.kt:100) | 待决定 |
| M13 | 当前延期因子为 min(100, 50+15×延期次数)，总分四舍五入限制 0–100，AI 本地贡献为 0。 | [RANK:87](/D:/Repland/android/app/src/main/java/com/swan1127/repland/domain/model/PlanningPriority.kt:87) | 待决定 |
| M14 | 同分/排序链：手动位置、不可避免、总分、初始优先级、截止、延期次数、创建时间、ID。 | [RANK:70](/D:/Repland/android/app/src/main/java/com/swan1127/repland/domain/model/PlanningPriority.kt:70) | 待决定 |
| M15 | 类别权重各 0–100 且总和大于 0，不强制总和恰好 100；代码相对最大权重归一化类别得分。 | [RANK:15](/D:/Repland/android/app/src/main/java/com/swan1127/repland/domain/model/PlanningPriority.kt:15) | 待决定 |
| M16 | 初始优先级代码分：未知 50、不可避免 100、高 80、中 50、低 20；未知分与中相同不代表来源相同。 | [TASK:56](/D:/Repland/android/app/src/main/java/com/swan1127/repland/domain/model/Task.kt:56) | 待决定 |
| M17 | PDF 导入当前最多 4 页。 | [PDF:46](/D:/Repland/android/app/src/main/java/com/swan1127/repland/data/importer/PdfTimetableImporter.kt:46) | 待决定 |
| M18 | PDF 解析针对有文字的星期列/节次网格排版及固定坐标/字号阈值，没有通用 OCR 或任意学校识别保证。 | [PDF:18](/D:/Repland/android/app/src/main/java/com/swan1127/repland/data/importer/PdfTimetableImporter.kt:18) | 待决定 |
| M19 | 默认节次只映射 1–12 节，使用内置钟点；导入项可改，不会自动知道学校实际钟点。 | [TTCLOCK:20](/D:/Repland/android/app/src/main/java/com/swan1127/repland/domain/model/TimetableImport.kt:20) | 待决定 |
| M20 | 同时仅一个活动专注会话；先结束当前轮次才能开始另一个。 | [SESSIONDB:22](/D:/Repland/android/app/src/main/java/com/swan1127/repland/data/room/RoomExecutionSessionRepository.kt:22) | 待决定 |
| M21 | 单轮累计显示运行时长当前限制最多 24 小时，负时间差不计。 | [SESSION:19](/D:/Repland/android/app/src/main/java/com/swan1127/repland/domain/model/ExecutionSession.kt:19) | 待决定 |
| M22 | 模型单次请求最多 131072 字节（128 KiB），响应最多 262144 字节（256 KiB）。 | [NET:29](/D:/Repland/android/app/src/remote/java/com/swan1127/repland/data/ai/CompatibleChatTransport.kt:29) | 待决定 |
| M23 | 当前模型连接超时 12 秒，读取超时 45 秒。 | [NET:36](/D:/Repland/android/app/src/remote/java/com/swan1127/repland/data/ai/CompatibleChatTransport.kt:36) | 待决定 |
| M24 | 当前传输不自动跟随重定向、不自动重试；发送到 baseURL/chat/completions。 | [NET:34](/D:/Repland/android/app/src/remote/java/com/swan1127/repland/data/ai/CompatibleChatTransport.kt:34) | 待决定 |
| M25 | 当前兼容接口请求固定 temperature=0.1，采用 system+user 文本消息结构，非通用多模态/工具调用客户端。 | [NET:25](/D:/Repland/android/app/src/remote/java/com/swan1127/repland/data/ai/CompatibleChatTransport.kt:25) | 待决定 |
| M26 | 传统 Advisor 文本校验 1–2000 字符、难度 1–5、建议时长 1–1440 分钟；具体请求类型受结构化契约约束。 | [CONTRACT:317](/D:/Repland/android/app/src/main/java/com/swan1127/repland/domain/model/AiAdvisorContract.kt:317) | 待决定 |
| M27 | 传统规划请求最多 50 个影响任务、20 条约束摘要、240 个硬约束，单任务最近反馈最多 5 条；与安排助手的 3 条规则不是同一路接口。 | [CONTRACT:380](/D:/Repland/android/app/src/main/java/com/swan1127/repland/domain/model/AiAdvisorContract.kt:380) | 待决定 |
| M28 | 画像模型返回限定 requestId/profileVersion/claims/nextStep，claims 1–6，单项仅 parameterId/value，不能自由输出额外字段。 | [NUMAI:34](/D:/Repland/android/app/src/remote/java/com/swan1127/repland/data/ai/CompatibleNumericProfileAdvisor.kt:34) | 待决定 |
| M29 | 头像中间解码目标 768，最终 384；属于内存/存储工程选择。 | [PSTORE:143](/D:/Repland/android/app/src/main/java/com/swan1127/repland/data/profile/LocalPersonalProfileStore.kt:143) | 待决定 |

## N．开发、构建、测试和发布

性质：工程/交付要求。

| 编号 | 现有规则/限制/差异 | 来源 | 你的新决定 |
| --- | --- | --- | --- |
| N01 | 当前原生 Kotlin/Compose/Material 技术基础，minSdk 24，compile/target 36；换平台/栈需要重构而非改文案。 | [BUILD:63](/D:/Repland/android/app/build.gradle.kts:63) | 待决定 |
| N02 | Java 源/目标兼容 11，当前构建工具使用 JDK 17；不是同一个版本维度。 | [BUILD:120](/D:/Repland/android/app/build.gradle.kts:120) | 待决定 |
| N03 | 本机 Gradle 堆最大 1536 MiB、2 worker、关闭常驻 daemon、Kotlin 进程内编译；不是 App 内存硬上限。 | [GRADLE:9](/D:/Repland/android/gradle.properties:9) | 待决定 |
| N04 | 串行构建共享输出，不并行重建；只处理确认属于本轮的锁/子进程，不全局终止 Java 或删除用户工作区。 | [TEST:16](/D:/Repland/docs/TESTING.md:16) | 待决定 |
| N05 | .qa 测试、.design 设计、.current 内测当前版独立包与数据；安装和测试须明确目标。 | [BUILD:87](/D:/Repland/android/app/build.gradle.kts:87) | 待决定 |
| N06 | 原资源约定日常只运行一个模拟器、复用 SDK/JDK/缓存；本轮曾为隔离验收临时增加 QA AVD，非永久产品限制。 | [COPY:35](/D:/Repland/DESIGN_COPY.md:35) | 待决定 |
| N07 | 当前 debug/internal/designPreview 接 remote HTTPS 实现；release 无 remote sourceSet，仍本地 Advisor。 | [BUILD:125](/D:/Repland/android/app/build.gradle.kts:125) | 待决定 |
| N08 | 当前检查 src/main 不得出现 INTERNET/HttpURLConnection/OkHttpClient/Retrofit.Builder；限制是现有构建检查，和正式 BYOK 目标待协调。 | [BUILD:180](/D:/Repland/android/app/build.gradle.kts:180) | 待决定 |
| N09 | 正式 release 必须配置签名；internal 可用本地 debug 签名作为封闭测试后备，不能混为正式发布。 | [BUILD:168](/D:/Repland/android/app/build.gradle.kts:168) | 待决定 |
| N10 | versionName 要语义化版本、versionCode 正整数；记录源码、APK 哈希和版本，不移动旧标签冒充同版本。 | [BUILD:10](/D:/Repland/android/app/build.gradle.kts:10) | 待决定 |
| N11 | 设备自动化只在专用测试设备/隔离包，不清用户真实任务、密钥、计划。 | [TEST:27](/D:/Repland/docs/TESTING.md:27) | 待决定 |
| N12 | 升级验收保留旧数据并检验 Room 迁移；不以卸载/破坏性迁移替代升级成功。 | [TEST:20](/D:/Repland/docs/TESTING.md:20) | 待决定 |
| N13 | CI 使用 API 26，候选另测 API 36；API 26 并非 minSdk 24，API24/真机矩阵不能冒称已覆盖。 | [TEST:14](/D:/Repland/docs/TESTING.md:14) | 待决定 |
| N14 | 发布前验证真实设备升级、通知权限/重启/时区/后台、PDF、断网/AI失败、日期和深色大字体。 | [TEST:18](/D:/Repland/docs/TESTING.md:18) | 待决定 |
| N15 | 规格、实现、自动化、指定包端到端与结论分层记录，完成要有证据；文档通过不等于代码通过。 | [PLAN:252](/D:/Repland/docs/PRODUCT_DELIVERY_PLAN_2026-10-03.md:252) | 待决定 |
| N16 | 用户已移除招募、7 天试用及类似用户研究，不再作为本轮完成条件。 | [PROGRESS:21](/D:/Repland/docs/DELIVERY_PROGRESS.md:21) | 待决定 |
| N17 | 源码提交不得包含 API Key、签名密码/私钥、local.properties 或个人业务快照；版本证据须脱敏。 | [RELEASE:27](/D:/Repland/docs/RELEASE.md:27) | 待决定 |
| N18 | 当前优先使用已有 Compose/API，不新增动画/图片库、常驻后台服务或装饰循环；可重新评估依赖需求。 | [CURRENT:3](/D:/Repland/DESIGN_CURRENT.md:3) | 待决定 |

## O．延后能力的附带准入条件

性质：后续方案，非当前前置。

| 编号 | 现有规则/限制/差异 | 来源 | 你的新决定 |
| --- | --- | --- | --- |
| O01 | 若建设同步，必须仍离线可用、对象稳定 ID/修订号，冲突保留双方并让用户选择，不静默覆盖。 | [EXT:33](/D:/Repland/docs/EXTENSION_READINESS.md:33) | 待决定 |
| O02 | 若建设外部同步，逐来源授权/关闭、标来源和影响范围，支持导出及删除远端副本。 | [EXT:43](/D:/Repland/docs/EXTENSION_READINESS.md:43) | 待决定 |
| O03 | 旧网关方案含鉴权/限流/预算/审计/脱敏等条件；当前 BYOK 不要求先建设网关，不能复活为当前前置。 | [EXT:9](/D:/Repland/docs/EXTENSION_READINESS.md:9) | 待决定 |
| O04 | 未来目标/习惯模块可选，不用它们也能完成基本任务闭环，不强制打卡/惩罚提示。 | [EXT:47](/D:/Repland/docs/EXTENSION_READINESS.md:47) | 待决定 |
| O05 | 未来团队共享只暴露授权的任务/里程碑/可用时间，不默认公开完整个人日程。 | [VISION:197](/D:/Repland/IDEAL_PLAN_FEATURES.md:197) | 待决定 |
| O06 | 外部日历/邮件/消息提取先产生待确认候选，不直接改个人任务状态/核心计划。 | [VISION:207](/D:/Repland/IDEAL_PLAN_FEATURES.md:207) | 待决定 |
| O07 | 自由聊天、AI 自动写入/自动完成/自动延期不在当前规划助手权限范围；要启用需重新定义授权和回退。 | [IDEA:132](/D:/Repland/REPLAND_IDEA.md:132) | 待决定 |

## P．本会话明确要求与个人课表数据

性质：会话要求，不自动全局化。

| 编号 | 现有规则/限制/差异 | 来源 | 你的新决定 |
| --- | --- | --- | --- |
| P01 | 本阶段不要大幅改动 UI，修复剩余 Bug；这是当前阶段偏好，可由用户撤销或替换。 | 本会话用户原话 | 待决定 |
| P02 | 检查/合并 Henry PR 前须能够恢复到原版本；此前备份仍保留，不把一次合并授权视作以后所有合并/发布授权。 | 本会话用户原话 | 待决定 |
| P03 | 本课表第一教学周周一为 2026-08-31；这是一份课表的设置，不是所有用户的学期日期。 | 本会话用户原话 | 待决定 |
| P04 | 本课表节次：1 08:20–09:05；2 09:10–09:55；3 10:20–11:05；4 11:10–11:55；5 14:00–14:45；6 14:50–15:35；7 16:00–16:45；8 16:50–17:35；9 18:30–19:15；10 19:20–20:05。仅适用本课表。 | 本会话用户原话 | 待决定 |
| P05 | 按普通用户路径测试功能，提供计划、UI 效果和 Bug 证据；模拟任务/进度应明确是 QA 场景，不能作为真实学习成绩。 | 本会话用户原话 | 待决定 |
| P06 | 本次仅要求给出全部约束供重新修改；尚未授权把任意规则删掉或更改 App 行为。 | 本会话用户原话 | 待决定 |

## X．冲突、历史口径及必须重新决定的差异

性质：待用户重定，不自动取舍。

| 编号 | 现有规则/限制/差异 | 来源 | 你的新决定 |
| --- | --- | --- | --- |
| X01 | X01：目标已要求用“无配置/关闭零请求、正式 BYOK”替代“正式包绝对离线”；当前 release/source 检查和旧 ADR 仍限制联网。 | [IDEA:147](/D:/Repland/REPLAND_IDEA.md:147) | 待决定 |
| X02 | X02：目标手动 15 分钟，草案及部分确认链路实际要求 30 分钟；不能只改按钮。 | [IDEA:79](/D:/Repland/REPLAND_IDEA.md:79) | 待决定 |
| X03 | X03：主思路紧凑焦点工作台与当前参考图大新增卡片不一致；需重新确定首页主操作/密度。 | [IDEA:128](/D:/Repland/REPLAND_IDEA.md:128) | 待决定 |
| X04 | X04：目标“今日/Agent”与当前“日程/助手”命名、时间入口职责存在差异；旧四页“时间”口径也仍在历史报告中。 | [IDEA:127](/D:/Repland/REPLAND_IDEA.md:127) | 待决定 |
| X05 | X05：目标允许休息软偏好，当前 PlacementValidator 将非 AVAILABLE 的 REST 作为硬冲突；需要模型/入口/校验同步决定。 | [IDEA:60](/D:/Repland/REPLAND_IDEA.md:60) | 待决定 |
| X06 | X06：目标无时长不自动精确排程，但手动放入策略以未知=30 分钟，已知夹到 15–240；需决定是否显式确认本次时长。 | [IDEA:81](/D:/Repland/REPLAND_IDEA.md:81) | 待决定 |
| X07 | X07：主思路说密钥“只存 Keystore”，实现是 Keystore 加密密钥+私有偏好存密文；应明确术语指明文保护而非所有字节仅在 Keystore。 | [IDEA:20](/D:/Repland/REPLAND_IDEA.md:20) | 待决定 |
| X08 | X08：主思路要求“不可避免”，设计记录出现“必须”；需统一展示名称，不能擅自变更 REQUIRED 保护语义。 | [IDEA:67](/D:/Repland/REPLAND_IDEA.md:67) | 待决定 |
| X09 | X09：目标有待确认提醒，当前只开始/截止/复盘三种；提醒固定时刻与无当前计划全关闭也需重审。 | [CTX:213](/D:/Repland/CONTEXT.md:213) | 待决定 |
| X10 | X10：业务导出未含个人资料，不能称“包含所有本地数据”；是否升级格式/增加恢复能力待决定。 | [PROFILE:37](/D:/Repland/DESIGN_PERSONAL_PROFILE.md:37) | 待决定 |
| X11 | X11：旧术语强调对用户隐藏模型供应商，当前 BYOK 需要用户配置供应商；应区分隐藏业务差异与隐藏服务身份。 | [CTX:394](/D:/Repland/CONTEXT.md:394) | 待决定 |
| X12 | X12：旧 MVP 定义包含连续使用/留存，后续已移除招募和 7 天试用；技术验收不应重新依赖这些活动。 | [CTX:382](/D:/Repland/CONTEXT.md:382) | 待决定 |
| X13 | X13：旧 ADR2 写仅 debug+开发网关、internal本地；现状 internal/design 已 HTTPS BYOK，网关及服务端持钥非当前前置。 | [ADR2:7](/D:/Repland/docs/adr/0002-debug-ai-gateway-and-execution-evidence.md:7) | 待决定 |
| X14 | X14：旧森林绿视觉、只用 tween 不弹跳与当前柔和彩色/有限弹簧不同；旧数值不是当前强制规范。 | [MOTION:15](/D:/Repland/DESIGN_MOTION.md:15) | 待决定 |
| X15 | X15：旧 ADR3 将轨道名仅留 UI、全局轨道库待后续；后续已有持久化轨道能力，旧探索限制不应删除现成功能。 | [ADR3:15](/D:/Repland/docs/adr/0003-event-object-track-instance-model.md:15) | 待决定 |
| X16 | X16：旧画像额外 15%、旧 45% 中性 AI 公式已被替代，不能当当前权重约束恢复。 | [C07:42](/D:/Repland/docs/CONSISTENCY_REVIEW_2026-10-06.md:42) | 待决定 |
| X17 | X17：90 天、3 样本、R7 等为工程默认，并非不可更改科学阈值；目标仍保留更大画像愿景，不能把未实现三维度当永久禁令。 | [NUM:3](/D:/Repland/docs/features/H01_NUMERIC_CONTRACT.md:3) | 待决定 |
| X18 | X18：README/发布/隐私/交付记录中的版本数与“当前”称谓分属不同历史阶段，需按实际构建和最新证据核对，不一概沿用。 | [PRIV:19](/D:/Repland/docs/PRIVACY.md:19) | 待决定 |

## 修改记录模板

可直接编辑上表最后一列，或按编号回复，例如：

```text
A03：修改为允许可选账号和双设备同步，本地功能不依赖登录。
D15：修改为自动排程 15 分钟，手动精确到 1 分钟。
H06：保留。
X01：统一正式 BYOK，并同步修改旧 ADR 和构建检查。
```

以上仅示范填写方式，不代表你已作出这些决定。涉及 AI 权限时建议把可操作对象、触发条件、授权有效期、保护例外、撤销/日志同时写清，避免一句“AI 自动处理”产生多种解释。

## 原文、代码与覆盖证据

- [全部文档原文快照（含历史/探索，带原始行号）](/D:/Repland/output/constraints-review-2026-10-16/source-bundle.md)
- [代码常量及校验补充索引](/D:/Repland/output/constraints-review-2026-10-16/code-constraints.md)
- [来源清单、分类与 SHA-256](/D:/Repland/output/constraints-review-2026-10-16/source-manifest.json)
- [同一清单的结构化版本](/D:/Repland/output/constraints-review-2026-10-16/constraints.json)

这份盘点不是把所有历史提案都升级为现行规则，也不是穷举每个 if 表达式或证明所有约束都已正确实施。X 类集中呈现此次已确认的主要文档/实现冲突；其他未逐项运行的规则保留为规格或静态现状。若重新定义长期愿景，可同时修改 IDEAL_PLAN_FEATURES，而不必继续受本阶段延期范围约束。
