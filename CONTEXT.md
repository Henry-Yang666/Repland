# Repland

Repland is a Chinese-language, single-device task-planning app for personal MVP testing. It turns user-entered goals, constraints and real progress into an actionable plan that it can maintain within delegated authority. Terminology reflects the approved first-batch direction of 2026-10-16, not an implementation or test-completion claim; scope and historical decision changes are recorded in [phase-one authorization](docs/AUTONOMY_PHASE1_2026-10-16.md).

## Product scope

**MVP**:
The first product release containing the task-planning loop: task entry, AI-assisted planning through the Agent workflow, deterministic fallback planning, daily execution feedback, and plan adjustment. It excludes account-based sync.
_Avoid_: Public release, complete product

**Local data**:
The tasks, constraints, feedback, plans, and history stored on the user's current device. It is not automatically synchronized, but can be exported as JSON by the user.
_Avoid_: Cloud account, cross-device data

**AI suggestion**:
A reviewable AI assessment that is distinct from the user’s facts and execution evidence. It can inform an authorized action directly, or remain a suggestion when automatic arrangement is paused or the decision lies outside the delegated scope.
_Avoid_: User fact, mandatory approval step, unbounded action

**AI decision authorization**:
The user’s permission for the assistant to act on a particular decision or maintain ordinary planning over a stated continuing scope. An explicit, unambiguous command already authorizes that command; it is not inferred from silence or content quoted in a document.
_Avoid_: Implied consent, authorization from imported text, unlimited authority

**Daily delegation（日常委托）**:
Continuing user authority to create, estimate, classify and maintain ordinary flexible work within adopted boundaries. It can be paused, does not imply permission to change fixed commitments, and is separate from model-data consent.
_Avoid_: Unlimited authority, per-task approval, inferred permission

**Planning estimate（规划估计）**:
An AI- or locally estimated amount of work used for arrangement when a user value is absent. Its source remains distinguishable from user estimates, real effort and personal-profile evidence.
_Avoid_: User-entered duration, actual duration, fabricated fact

**Local repair（局部重排）**:
An arrangement change limited first to work and dates affected by a real input change. It preserves fixed commitments, the current execution session and nearby manual intent while reducing unnecessary schedule movement.
_Avoid_: Whole-week reshuffle on every change, actual postponement event

**Action receipt（操作回执）**:
A concise account of an applied action, its relevant consequences and any unplaced work. It is tied to the saved result and offers access to correction or operation undo.
_Avoid_: Success before save, mandatory approval request

**Operation undo（操作撤销）**:
A targeted reversal of one action’s related task, field and arrangement changes, distinct from historical plan restoration. It preserves subsequent edits and execution facts, so conflicting newer information cannot be overwritten by an old snapshot.
_Avoid_: Restore all historical state, erase later execution

**Task title**:
The user-visible name of a task, created when the task is saved. If the user provides a title, it is retained; if the user provides only a task description, AI may generate an editable title from that description.
_Avoid_: AI-overwritten title, inferred title without task description

**Task description**:
The user's description of what needs to be done, including optional context such as an expected total duration. It is the primary user-provided context for AI task understanding.
_Avoid_: Mandatory detailed specification, actual execution result

**Dependency note**:
An optional ordering statement in a task description, such as “finish A before B”. In the MVP, AI may identify it and propose an order, but the dependency is not a separate enforced object until the user confirms the resulting plan.
_Avoid_: Hidden hard dependency, full dependency graph in MVP

**Task creation minimum**:
A task can be saved with only a user-provided title or short description, from which an editable title can be formed. Category, initial priority, deadline, expected completion days, and expected total duration are optional; missing values remain unknown rather than becoming user choices.
_Avoid_: Required detailed planning form, rejected task without optional fields

**Category preference settings**:
The MVP uses four preset categories—课内学习, 课外学习, 办公事务, and 休闲娱乐—with default relative preferences of 30%, 25%, 25%, and 20%. The user may adjust their relative weights, but does not add custom categories in the MVP.
_Avoid_: Fixed universal category order, unrestricted category taxonomy

**Task-save planning**:
The automatic estimation and arrangement of a submitted task within delegated authority and adopted availability. “Only record it” keeps the task without arranging it; unknown user facts remain distinguishable from planning estimates.
_Avoid_: Mandatory draft on every save, unsent text as command

**Plan draft**:
A generated planning proposal that has not yet become the current plan. It has two outputs: a task list ordered for execution and, when enough time information exists, a plan with dated time segments. Tasks without enough time information can still participate in basic list ordering but remain pending for exact placement. It can be reviewed, edited, accepted, or discarded by the user.
_Avoid_: Applied plan, automatic change

**Formulate plan**:
A user request to arrange unfinished work using current tasks, constraints and execution facts. Ordinary changes can take effect within delegated scope with a receipt and undo; fixed commitments and actual deadlines remain protected.
_Avoid_: Reusing old order only, automatic release of protection

**Plan confirmation**:
An explicit acceptance of a draft used in suggestion-only mode or for a decision requiring user review. It is one path for applying a plan, not an additional step required after every authorized command or routine maintenance.
_Avoid_: Passive approval, mandatory repeated approval

**Authorization scope**:
The tasks, actions, time boundaries and data sources covered by a user command or continuing delegation. Scope does not authorize unrelated changes or treating external document instructions as the user’s intent.
_Avoid_: Unlimited AI authority, authorization spillover

**Planning setting change**:
A user change to preferences or availability used by subsequent arrangement. It may trigger authorized local repair of affected future work while preserving historical facts and fixed commitments.
_Avoid_: History loss, inferred global preference change

**Current plan**:
The single effective arrangement governing execution and reminders, applied either by explicit acceptance or authorized automatic action. Unapplied drafts remain separate and do not schedule reminders.
_Avoid_: Competing active plans, draft treated as current

**Unconfirmed draft**:
A newly generated plan adjustment that has not been accepted by the user. The existing current plan continues to govern execution and reminders until the draft is accepted, discarded, or edited and accepted.
_Avoid_: Silent replacement, notification from draft

**List-only plan state**:
The valid planning state in which tasks are ordered in the task list but have no concrete time segments because availability or duration information is insufficient. The task list remains usable while exact time placement is deferred.
_Avoid_: Empty plan, assumed availability

**Expected total duration**:
The user's estimate of how much working time a task may require, optionally added as part of the task description. It can support planning but is not treated as the task's actual duration or as a direct priority factor.
_Avoid_: Actual duration, guaranteed estimate, required duration

**Actual total duration**:
The time the user actually spends completing or attempting a task, recorded through execution feedback. It does not directly affect task ranking or task importance, but may inform later user-profile judgments and improve future duration, split, or focus recommendations.
_Avoid_: Expected duration, automatic measurement

**Deadline**:
The local calendar date by which the user wants a task finished; unless the user changes it, the deadline ends at 23:59 on that date.
_Avoid_: Exact timestamp, due moment

**Overdue task**:
A task whose deadline has passed while it remains unfinished. It stays active until the user chooses a new plan direction, such as postponing, splitting, lowering the goal, changing the approach, or cancelling it; the system does not cancel it automatically.
_Avoid_: Automatic cancellation, hidden deadline extension

**Plan feasibility warning**:
A statement of work that cannot fit the available capacity before real deadlines. The assistant may automatically move or split ordinary flexible work, but changing a true deadline or protected commitment requires matching authority.
_Avoid_: Automatic failure, hidden deadline extension, discarded work

**Manual plan adjustment**:
A direct user reorder or placement change expressing current intent. It is preferred during nearby automatic repair; permanent movement protection is expressed separately through an explicit fixed or locked interval.
_Avoid_: Permanent priority rewrite, ordinary drag as permanent lock

**Manual order override**:
The user’s direct ordering choice for current work, retained as a traceable planning decision and given stability preference during local repair. It does not silently rewrite task importance or grant permission to violate fixed time.
_Avoid_: Unrecorded manual intent, permanent protection inferred from one drag

**Manual-order behavior evidence**:
The user's repeated direct ordering choices may inform later AI suggestions about planning preferences. They do not silently rewrite initial priorities, category weights, or the user's task data.
_Avoid_: Hidden preference mutation, one-time drag treated as permanent rule

**Manual-order feasibility warning**:
A non-blocking warning shown when the user's direct ordering may cause a deadline or capacity problem. It informs the user but cannot prevent or reverse the manual order.
_Avoid_: Blocking user control, silent reorder

**Manual parallel override**:
A user-confirmed overlap between ordinary, unprotected task placements, not evidence of simultaneous execution. It cannot cover courses, fixed commitments, hard-protected rest, locked intervals, or confirmed unavoidable-task intervals without first changing or releasing the relevant protection.
_Avoid_: AI-generated concurrency, automatic conflict correction

**Initial-priority change**:
A user edit of the task’s stated importance, separate from dynamic planning urgency and interval protection. It may trigger authorized repair without replacing the user’s value with an AI score.
_Avoid_: Dynamic value written as user choice, importance equals fixed time

**Locked time block**:
A scheduled task interval protected from movement or conflicting placement by AI, local planning, and fallback planning. The user must explicitly release or change its protection before making a conflicting adjustment.
_Avoid_: Locked task, immutable task

**Fixed task**:
A task marked “不可避免”, considered preferentially before placement and whose confirmed intervals are protected from automatic movement or conflicting placement across AI, local, and fallback planning. It is not a separate fixed-event object; the user can explicitly change its placement with confirmation.
_Avoid_: Separate event model, AI-generated overlap

**Execution log**:
A record of a user-confirmed task status or feedback event. It remains historically true even when a plan version is later rolled back. If the user corrects a past entry, the original event and the correction are both retained.
_Avoid_: Rewritten history, inferred completion

**Plan version**:
A retained snapshot of effective task order and scheduled intervals, whether applied manually or through authorized automation. Historical plan restoration creates a new version without reverting outcomes, reviving finished tasks or restarting past work.
_Avoid_: Overwritten plan, operation undo, temporary draft

**Task status**:
The execution state supported by an explicit user action or clear natural-language statement: not started, in progress, completed, postponed, cancelled or replaced. Clock time and elapsed timer targets do not establish a result; later corrections retain their provenance.
_Avoid_: AI-invented result, automatically completed from time

**Scheduled-start reminder**:
A reminder that a confirmed placement's planned start has arrived, without changing task status or recording actual work. It is distinct from a user-confirmed start or retrospective execution record.
_Avoid_: Automatic-start event, inferred execution

**Task-segment status distinction**:
A daily work segment's completed or postponed result is recorded independently from the overall task status. Completing one segment does not automatically complete the task; the user controls the task-level outcome and total progress.
_Avoid_: Segment completion equals task completion, inferred total progress

**Cancelled task**:
A user-confirmed task outcome that removes a task from future planning without deleting its history. A cancelled task remains available in plan versions, execution logs, and relevant statistics and may be restored by the user.
_Avoid_: Hard deletion, AI cancellation

**Task restoration**:
An explicit user action or clear command returning a completed or cancelled task to active planning while preserving the earlier result. Its new arrangement may be applied within delegated scope without erasing history.
_Avoid_: Erased outcome, automatic resurrection

**Planning quantum**:
A 30-minute unit used by the MVP to place work into available time. A task's supplied duration is rounded up to whole quanta for automatic scheduling.
_Avoid_: Exact productivity measure, fixed appointment length

**Recurring time template**:
A weekly repeating course or rest interval that blocks planning on matching days. A date-specific override may change that day's availability.
_Avoid_: Imported calendar, one-off event

**Recurring task scope**:
The MVP does not provide a dedicated recurring-task template. Repeated work is represented by user-created tasks while the need is being validated; recurring course and rest availability remain separate supported templates.
_Avoid_: Habit automation in MVP, recurring-task confusion

**Pending task**:
A retained task that is not currently placed, for example because the user asked to record only, availability is absent or capacity is insufficient. Missing user duration alone need not block placement when a source-labelled planning estimate is available.
_Avoid_: Invalid task, ignored task, missing duration always blocks

**Deterministic fallback priority**:
The MVP's explainable priority calculation used when no AI is available. AI-specific factors receive neutral scores while user priority, category preference, deadline or overdue state, and postponements drive ordering with stable tie-breaking. Execution-time proximity is presented by the timed execution view and is not an independent fallback-ranking factor.
_Avoid_: Machine-learned ranking, random order

**Plan regeneration trigger**:
A relevant change such as a submitted task, real constraint update or explicit progress report that calls for arrangement maintenance. It can trigger authorized local repair or a reviewable draft, while opening a page alone is not a new instruction.
_Avoid_: Model call on every page open, unrelated global replan

**Future-constraint change**:
A change to an upcoming planning input that can trigger repair of affected future flexible work. It preserves elapsed arrangements, real execution and commitments not covered by the change.
_Avoid_: Rewriting history, retroactive schedule mutation

**Dynamic priority**:
The explainable, changing priority used to consider candidate tasks for planning, based on user priority, deadlines, planned dates, execution evidence and category preferences. It never rewrites initial priority or overrides protected time; its versioned scoring rules belong to the product specification.
_Avoid_: Overwritten user priority, fixed lifetime score

**Execution-time proximity**:
The fact that a task's scheduled time segment is about to begin. In the current product direction it does not independently raise dynamic priority; the time-segment execution view presents tasks by their scheduled rows instead.
_Avoid_: Extra ranking factor, urgency score from clock proximity

**Priority presentation**:
The user's explicit “高 / 中 / 低 / 不可避免” choice remains distinct from dynamic urgency; an absent choice is shown as “未设置”. Planning urgency may change without rewriting that choice or removing unavoidable-placement protection.
_Avoid_: Confusing dynamic urgency with initial priority, hidden rewrite

**Structured AI assessment**:
A non-binding, structured recommendation about a task's estimated duration, split, work pattern or planning trade-offs, informed by relevant task facts and available personal planning parameters. It does not add a separate profile-based priority factor, rewrite user facts or authorize a task-status change.
_Avoid_: Opaque personality score, extra profile priority weight, autonomous status change

**User correction of AI assessment**:
Any user edit to an AI-suggested task description, duration, split, execution cycle, or goal context. The correction is treated as user-provided planning evidence and takes precedence over the earlier AI suggestion in later analysis.
_Avoid_: AI suggestion treated as fact, uneditable judgment

**Repeated postponement**:
Each user-confirmed postponement raises a task's dynamic urgency. After two cumulative postponements, Repland should prompt the user to reassess the task's goal or approach; the prompt is advice and does not change the task automatically.
_Avoid_: Infinite priority escalation, automatic goal change

**Local reminder**:
A device-scheduled notification for a confirmed plan, limited in the MVP to a task's start time when it has a confirmed concrete time segment, a near-deadline warning when a deadline exists, and the end-of-day review. A task without a deadline does not receive a deadline reminder, and a task without a concrete time segment does not receive a start reminder. It is optional, one-time per trigger, and uses the device's local time.
_Avoid_: Cloud push, mandatory alarm

**First-use setup**:
The user may create a first task and receive a basic plan without first configuring a timetable or rest template. Those availability settings are optional inputs that improve exact time placement.
Without configured availability, the MVP provides the task list but does not assume the user is free or generate a reliable exact time-slot plan.
_Avoid_: Setup wall, unusable empty state, assumed free time

**Completion cleanup**:
The removal of future unexecuted placements after an explicit completion action or command, with prior arrangements retained in history. Freed capacity may be reused through authorized local repair.
_Avoid_: Deleted history, inferred completion

**Daily review**:
An optional end-of-day summary covering completed and unfinished work, actual time, progress, learning results when relevant, AI interpretation, and next-day adjustment advice. Skipping it does not allow the system to infer task status.
_Avoid_: Mandatory diary, automatic status inference

**End-of-day final-plan record**:
The record produced at the end of the day from the user's final manually adjusted plan table. It preserves the final order and relevant plan state for subsequent AI analysis and historical review, without changing the user's task statuses by inference.
_Avoid_: AI-generated final order, unrecorded manual arrangement

**End-of-day snapshot**:
A retained account of the day’s final arrangement for historical review. It is not evidence that scheduled work actually happened and is distinct from any newly maintained future plan.
_Avoid_: Inferred execution, rewritten historical outcome

**Profile aggregation**:
A traceable summary of effective execution evidence and explicit preferences within a stated scope and time range. The summary does not by itself authorize deleting source history or replacing uncertain evidence with a permanent personal label.
_Avoid_: Automatic history pruning, discarded source evidence, permanent label

**Planning horizon**:
The rolling 30-day window in which the MVP expands tasks into daily work. Tasks beyond that window remain stored without being rendered as daily items.
_Avoid_: Project end date, permanent schedule

**Daily work segment**:
A dated portion of one task’s planned work, possibly based on a source-labelled estimate. Multiple segments retain one task identity and may be adjusted within delegated authority without implying execution or completion.
_Avoid_: Duplicated task, planned time as actual effort

**Timed execution view**:
The view for a confirmed plan with concrete time segments. It displays scheduled tasks as equal-width rows, with a time axis on the left for reference; each task segment is represented by its start and end time points. After a segment ends, that row leaves this view and becomes available in the past-segment review view.
_Avoid_: Permanent task list, AI-marked completion

**Task time label**:
The concrete time representation shown for a scheduled task segment, using two points such as “09:00–10:00” for start and end rather than using task-card width to encode duration.
_Avoid_: Duration encoded by width, continuous chart required

**Past-segment review view**:
An optional view of elapsed planned segments where the user may supplement actual progress or correct a result. Unknown old results do not require review before remaining work can be arranged.
_Avoid_: Mandatory review debt, automatic completion

**Pending past segment**:
An elapsed planned segment whose actual result is unknown. It is not automatically completed, failed or postponed, and does not block authorized arrangement of remaining work.
_Avoid_: Silent failure, inferred postponement, mandatory approval debt

**Task detail feedback**:
Optional actual time, progress, results or corrections supplied from task details or a clear language command. Missing fields stay unknown, and a clear status statement does not need a second form submission.
_Avoid_: Mandatory long form, invented actual duration

**Batch status edit**:
A user action that selects multiple tasks and applies the same status change together. Each selected task receives its own execution record and any resulting planning analysis; batch editing does not allow AI or the system to infer the statuses.
_Avoid_: Bulk inferred status, merged task identity

**Task replacement**:
A user choice from task details to turn the current task into a new task. The original task receives the “已替换” status and moves to today's past-task record with its history and feedback, while the replacement task receives a new task identity, may reuse selected description, goal context, or feedback, and enters a new planning analysis.
_Avoid_: Overwriting task identity, erased original task

**Execution feedback**:
The user’s report of actual progress, effort or result. A clear instruction can directly update the corresponding status and remaining arrangement, with the original statement retained as its source.
_Avoid_: Inferred outcome, every report requires another confirmation

**Postponement feedback**:
An explicit user statement that work should be postponed, distinct from a missed scheduled slot. It can update the recorded decision and trigger authorized repair without creating a fictitious actual-duration record.
_Avoid_: Automatic rollover counted as failure, unrecorded source

**Postponement reason**:
An optional explanation attached to a user-confirmed postponement, such as insufficient time, unexpected difficulty, or an interruption. It can inform later AI analysis and profile updates but is never required to record the postponement.
_Avoid_: Mandatory justification, moral judgment

**Partial completion adjustment**:
The repair of remaining work after an explicit partial-progress report. It can take effect within delegated scope without a full feedback form or a repeated approval, and does not imply full completion.
_Avoid_: Lost remaining work, inferred completion

**User planning cycle**:
The user's expected number of days for completing a task. It is the primary planning cycle when supplied; AI may suggest a different cycle, but the suggestion does not replace the user's value unless the user changes it.
_Avoid_: AI-imposed deadline, fixed universal cycle

**Profile update**:
The revision of personal planning parameters from effective execution evidence and explicit user preferences, with their uncertainty and provenance preserved. An updated profile does not change confirmed tasks or plans, and any description based on an older profile remains distinguishable from the current profile.
_Avoid_: Hidden permanent label, AI-only identity, silent plan mutation

**User usage summary（用户画像 / 近期使用总结）**:
An automatically maintained explanation of the user's recent recorded task activity, progress, execution timing and explicit arrangement changes. It describes observable use without requiring profile forms or confirmations, and is not evidence for a new personal claim or authority to modify a task.
_Avoid_: Personality assessment, self-discipline score, AI text used as fresh evidence

**Explicit usage action（主动使用操作）**:
A committed user action such as editing a task, moving an arrangement or changing its lock. System-generated schedule repairs are separate events and do not establish the user's habit or preference.
_Avoid_: Every plan version as a user adjustment, cancelled drafts as completed actions

**Numeric planning profile（数值规划参数）**:
A user-inspectable set of planning parameters, primarily computed from traceable records or taken from explicit user preferences, that reduces repeated AI interpretation of the user's history. It describes planning context rather than personality, ability, self-discipline or health.
_Avoid_: Personality assessment, productivity grade, AI-invented identity

**Planning parameter（画像参数）**:
A value with a defined meaning, unit, scope and evidence basis, describing an observed planning pattern or an explicit preference. A missing or insufficiently supported parameter is unknown, not a neutral score.
_Avoid_: Universal 0–100 score, guessed default, unqualified personal trait

**Profile snapshot（画像快照）**:
A particular version of the user's planning parameters with their applicable evidence and time range. Recommendations and descriptions tied to that snapshot are not automatically assertions about a later snapshot.
_Avoid_: Timeless identity, description treated as current after evidence changes

**Observed execution pattern（执行统计特征）**:
A summary of recorded work, such as execution lengths or time-of-day distribution, limited to the available evidence. It is distinct from the user's preferred schedule and does not establish efficiency or concentration ability.
_Avoid_: Observed behavior equals preference, inferred capability

**Explicit planning preference（明确规划偏好）**:
A planning choice stated or confirmed by the user, such as category preference, preferred time range or grouping style. An inferred pattern does not silently replace this preference or override manual decisions and protected time.
_Avoid_: Hidden preference mutation, one drag as permanent preference

**Duration calibration factor（估时修正系数）**:
A comparison between sufficiently recorded actual total effort and a comparable task's traceable original estimate. It supports a new duration suggestion without rewriting that estimate or compounding the same correction during repeated replanning.
_Avoid_: Actual duration guessed from progress, cumulative correction, task-importance factor

**Execution segment distribution（执行段时长分布）**:
The distribution of recorded active working durations for user-started execution sessions, distinct from planned durations and paused time. It is evidence for segment-length suggestions, not a measurement of concentration capacity.
_Avoid_: Planned interval as actual work, preferred length inferred as fact

**Replanning and interruption evidence（重排与中断证据）**:
Records distinguishing requested replanning, accepted schedule changes and explicitly reported interruptions, with their observation scope and known reasons. Cancelled drafts and missing reports do not establish actual disruption or procrastination.
_Avoid_: Every draft as a replan event, missing feedback as failure, moral judgment

**Profile evidence eligibility（画像证据可用性）**:
Whether a record supplies the traceable information required for a particular planning parameter. Eligibility is parameter-specific: valid task history may still be insufficient for estimating total effort or identifying a preference.
_Avoid_: Every log counts for every dimension, missing information treated as zero

**Profile current-state description（画像现状描述）**:
An AI-authored, user-visible explanation of a specified profile snapshot's recent observations, evidence limits and optional planning suggestions. The description is not numeric evidence, a diagnosis, a permanent label or authority to change tasks, parameters or plans.
_Avoid_: AI text as new profile evidence, invented numbers, autonomous profile mutation

**Profile parameter opt-out（画像参数停用）**:
The user's choice to stop a parameter from informing new planning suggestions. It does not erase source execution history or disable basic local planning.
_Avoid_: Source-log deletion, forced AI dependence

**Goal reassessment**:
The user-facing review prompted after repeated postponement. It offers choices such as maintaining the goal, splitting the task, lowering the goal, moving the deadline, changing the approach, or cancelling the task; AI may recommend but does not change the goal without authorization.
_Avoid_: Automatic goal reduction, shame-oriented reminder

**Long-term goal context**:
Optional goal background attached to a task in the MVP, usually supplied in the task description or identified by AI. It helps the AI judge context relevance without creating a standalone long-term planning module; that module is reserved for a later version.
_Avoid_: Full goal hierarchy in MVP, hidden goal inference

**Global planning pass**:
The planning stage that considers all unfinished tasks together before assigning daily dates and time segments. It uses constraints, available time, task splitting, and dynamic priority so that local daily ordering does not undermine later deadlines.
_Avoid_: Independent per-day sorting, local optimum only

**New-task conflict draft**:
An optional reviewable arrangement for a new task when suggestion-only mode, an important ambiguity or a boundary decision calls for review. Routine flexible conflicts can instead be repaired directly within authority, retaining all unplaced work.
_Avoid_: Draft required for every conflict, discarded new task

**AI degraded mode**:
The local fallback mode used when the AI API is unavailable or returns unusable data. It continues basic explainable ordering and plan generation, treats AI-specific judgments as neutral, and tells the user that advanced AI assessment is unavailable.
_Avoid_: Broken core loop, fabricated AI judgment

**Date-specific override**:
A user-entered change to one occurrence of a weekly course or rest template. It changes that date's availability without rewriting the recurring template.
_Avoid_: Permanent template edit, imported calendar event

**One-day availability override**:
A user-specified availability change for one date that takes precedence over its recurring template. It may trigger authorized local repair for that date without rewriting the recurring rule or past execution.
_Avoid_: Permanent template rewrite, retroactive history change

**Local timetable import**:
The MVP capability to read and load a user-provided timetable on the device and turn courses into fixed planning constraints. The user confirms or corrects the imported result; external academic-system synchronization is outside the MVP.
_Avoid_: Unverified course schedule, external sync dependency

**Habit-formation flow**:
A later feature flow that uses execution feedback and profile data to provide active habit-building guidance. The MVP collects the supporting evidence through planning, execution, and review but does not include the complete habit-coaching mechanism.
_Avoid_: Mandatory coaching, MVP behavior intervention

**MVP validation**:
The evaluation of whether users can repeatedly complete the low-cost loop of adding tasks, receiving a plan, executing, giving feedback, and adjusting the plan. Core measures include plan confirmation, task completion, user-initiated adjustment, and continued use before adding long-term planning or full habit formation.
_Avoid_: Feature-count completion, AI quality judged in isolation

**Local data reset**:
An explicit user action that clears the app's locally stored tasks, plans, logs, and settings after confirmation. Previously exported JSON files are outside the reset scope.
_Avoid_: Account deletion, remote wipe

**AI contract**:
The boundary through which structured AI interpretations and proposed actions enter the planning domain. A response is not permission to bypass user authority, real facts or fixed commitments; authorized actions are distinguishable from model suggestions.
_Avoid_: Model output as unlimited authority, direct facts from guesses

**Model-provider abstraction**:
The product boundary that hides the specific AI model and service provider from users. The Agent workflow and AI contract remain stable while the implementation may change providers or models.
_Avoid_: Provider-dependent user experience, model choice as core workflow

**AI data boundary**:
The rule that only the current task's user-provided content and necessary, relevant structured history may leave the device after explicit user consent. Unrelated local data and raw history are not sent or retained by default.
_Avoid_: Full-device upload, silent profiling

**AI consent**:
The user’s permission to send the selected relevant data to a configured model service. It can cover continuing requests within a disclosed scope, remains revocable and is distinct from authority to change local plans.
_Avoid_: Consent to all data and providers, mandatory per-request approval

**Participation mode**:
A historical three-mode preference retained for compatibility and export, not a current user-type switch. Automatic arrangement enabled, paused or suggestion-only describes delegation behavior rather than those old participation modes.
_Avoid_: Current three-mode switch, inferred user type

**Usage event**:
An observation of a specific user interaction, such as switching participation mode or opening a schedule item. It is evidence of that interaction only, not evidence that scheduled work was performed.
_Avoid_: Productivity score, inferred task completion

**Schedule-time state**:
The relationship between the current clock time and a planned interval: upcoming, within the interval, or past its end. It describes the plan's clock, not the user's actual work duration or outcome.
_Avoid_: Actual execution state, automatic overtime conclusion

**Voice transcript**:
Editable text returned by a device-selected speech-recognition service and reviewed by the user before it becomes task input. It is not a confirmed task, schedule change, or stored audio record.
_Avoid_: Auto-saved task, guaranteed offline recognition

**Event object**:
The reusable user-owned content object that describes what should be done, such as a task. It can exist independently of a day and can be placed into one or more schedule instances without duplicating its identity.
_Avoid_: A task being permanently bound to its first time slot, duplicated task records

**Schedule Agent**:
The assistant surface that interprets submitted task and progress language and carries out authorized multi-step planning actions. It can ask about material ambiguity or boundary decisions, and returns a reviewable receipt rather than requiring a draft for every routine action.
_Avoid_: Unbounded general agent, mandatory input-and-confirm workflow

**Agent proposal**:
A reviewable set of changes retained for suggestion-only operation or a decision needing user review, separate from official tasks and the current plan until applied. Authorized routine actions may use an internal action plan without making this proposal a required user step.
_Avoid_: Preview already applied, every action requires proposal approval

**Schedule instance**:
One dated placement of an event object, with a start, end, and track. Moving or reusing an event object creates or changes a placement while preserving the event identity and execution history.
_Avoid_: Treating the event object as the timeline rectangle, overwriting history when rescheduling

**Schedule track**:
A named parallel rhythm lane in the day view. Tracks borrow the visual grammar of music arrangement software: events remain independent objects, while their temporal placement and parallel context are shown on the track.
_Avoid_: An automatically inferred overlap lane, a permanent task category

**Horizon view**:
The three planning resolutions exposed by Repland: day for execution and track placement, week for rhythm and load distribution, and month for density and milestones. Each view is a projection of the same schedule instances rather than a separate source of truth.
_Avoid_: Three duplicated calendars, month view as an editable task list
