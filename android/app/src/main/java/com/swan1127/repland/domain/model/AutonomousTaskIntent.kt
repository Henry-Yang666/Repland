package com.swan1127.repland.domain.model

import java.text.Normalizer
import java.util.Locale

/** These operations describe what the user actually said, not what a timer or model inferred. */
sealed interface AutonomousTaskIntent {
    data class Capture(val text: String) : AutonomousTaskIntent
    data class StatusUpdates(val commands: List<AutonomousStatusCommand>) : AutonomousTaskIntent
    data class Query(val scope: AutonomousQueryScope, val day: AutonomousDay? = null) : AutonomousTaskIntent
    data class Arrange(val day: AutonomousDay) : AutonomousTaskIntent
    data class NeedsClarification(val reason: AutonomousIntentAmbiguity, val sourceText: String) : AutonomousTaskIntent
}

enum class AutonomousDay { TODAY, TOMORROW, TONIGHT }
enum class AutonomousQueryScope { ACTIVE, SCHEDULED, INBOX, OVERDUE }
enum class AutonomousIntentAmbiguity {
    EMPTY_INPUT, QUOTED_OR_HYPOTHETICAL, QUESTION, NEGATED_STATUS,
    INVALID_PROGRESS, MULTIPLE_TARGETS, CONFLICTING_COMMANDS, MIXED_OPERATIONS,
}

sealed interface AutonomousTaskTarget {
    data class Named(val name: String) : AutonomousTaskTarget
    /** Resolve only against an explicit page/conversation context, never the first or closest task. */
    data class Contextual(val reference: String) : AutonomousTaskTarget
}

sealed interface AutonomousTaskChange {
    data object Complete : AutonomousTaskChange
    data object Cancel : AutonomousTaskChange
    /** An absolute completion percentage. It supplies no evidence about actual minutes worked. */
    data class Progress(val percent: Int) : AutonomousTaskChange {
        init { require(percent in 0..100) }
    }
}

enum class AutonomousFactSource { USER_EXPLICIT }

data class AutonomousStatusCommand(
    val target: AutonomousTaskTarget,
    val change: AutonomousTaskChange,
    val sourceText: String,
    val factSource: AutonomousFactSource = AutonomousFactSource.USER_EXPLICIT,
)

sealed interface AutonomousTargetResolution {
    data class Found(val task: Task) : AutonomousTargetResolution
    data class Missing(val target: AutonomousTaskTarget) : AutonomousTargetResolution
    data class Ambiguous(val candidates: List<Task>) : AutonomousTargetResolution
    data object ContextRequired : AutonomousTargetResolution
}

/** Small offline grammar. Unknown descriptions remain capture input; named references never use fuzzy matching. */
object AutonomousTaskIntentParser {
    private val requestPrefix = Regex("""^(?:请(?:帮我)?|帮我|麻烦(?:你)?(?:帮我)?)\s*""")
    private val capturePrefix = Regex("""^(?:新增|新建|添加)(?:一个|一项)?(?:任务|事项)?|^(?:只记着|只记下|只记录|仅记录|只新增|不要安排|暂不安排|先不安排|记下|待办[:：]|记录(?:任务|事项))""")
    private val statusWords = Regex("""完成|做完|写完|搞定|取消|不做了|不去了|一半|\d+\s*[%％]""")
    private val nonLiteral = Regex("""比如|例如|举个例子|示例|假设|如果|要是|他说|她说|有人说|这句话|输入[“"「『]|预计|估计|可能|也许""")
    private val questions = Regex("""[?？]|(?:了|完|完成|取消)(?:吗|么)|是否|有没有|能否|能不能|什么意思|怎么|如何""")
    private val negated = Regex("""(?:还?没(?:有)?|未|不要|不用|不会|不是|不想|别).*(?:完成|做完|写完|搞定|取消|一半|\d+\s*[%％])""")
    private val clauseSeparator = Regex("""[，,。；;\n]+|\s*(?:然后|并且|同时|顺便)\s*""")
    private val progress = Regex("""^(.+?)(?:我)?(?:已经|目前|现在|已)?(?:做了|完成了|完成|做到|进度(?:是|为|到)?|写了)\s*([+-]?\d+(?:\.\d+)?)\s*[%％](?:了)?$""")
    private val setProgress = Regex("""^(?:把|将)?(.+?)(?:的)?进度(?:设为|改为|更新为|标记为)\s*([+-]?\d+(?:\.\d+)?)\s*[%％](?:了)?$""")
    private val halfProgress = Regex("""^(.+?)(?:我)?(?:已经|目前|现在|已)?(?:做了|完成了|完成|写了|写完了|做完了)一半(?:了)?$""")
    private val halfRemaining = Regex("""^(.+?)(?:现在|目前)?(?:还剩|剩余|剩下)一半(?:没做|未完成|没完成|了)?$""")
    private val percentRemaining = Regex("""^(.+?)(?:现在|目前)?(?:还剩|剩余|剩下)\s*([+-]?\d+(?:\.\d+)?)\s*[%％](?:没做|未完成|没完成|了)?$""")
    private val completed = Regex("""^(.+?)(?:我)?(?:已经|刚刚|刚才|终于|也|已)?(?:做完了|写完了|完成了|搞定了|已完成|已做完|已写完|已搞定)$""")
    private val completedFirst = Regex("""^(?:我)?(?:已经|刚刚|刚才|刚|已)?(?:完成了|做完了|写完了|搞定了)(.+)$""")
    private val markCompleted = Regex("""^(?:把|将)?(.+?)(?:标记|设置?)(?:为|成)?(?:已完成|完成)(?:了)?$""")
    private val markCompletedFirst = Regex("""^(?:标记|设置?)(.+?)(?:为|成)(?:已完成|完成)(?:了)?$""")
    private val cancelled = Regex("""^(.+?)(?:已经|已)?(?:取消了|取消掉了|不做了|不去了)$""")
    private val cancelledFirst = Regex("""^(?:我)?(?:已经|刚刚|刚才|已)?取消(?:了)?(?:任务[:：]?\s*)?(.+?)(?:吧)?$""")
    private val cancelTarget = Regex("""^(?:把|将)(.+?)取消(?:了|掉|吧)?$""")
    private val multipleTargets = Regex("""[、]|(?:和|与|及|以及).*(?:都|全部|全都)$""")
    private val contextual = setOf("这个", "那个", "它", "这项", "那项", "这个任务", "那个任务", "这件事", "那件事", "当前任务", "刚才那个", "上一个任务")

    fun parse(text: String): AutonomousTaskIntent {
        val words = text.trim().trimEnd('。', '！', '!').trim()
        fun ambiguous(reason: AutonomousIntentAmbiguity) = AutonomousTaskIntent.NeedsClarification(reason, text)
        if (words.isBlank()) return ambiguous(AutonomousIntentAmbiguity.EMPTY_INPUT)
        // An explicit capture wrapper makes its contents task text, even when the title resembles a report.
        if (capturePrefix.containsMatchIn(words)) return AutonomousTaskIntent.Capture(text)
        if (statusWords.containsMatchIn(words) && (isWholeQuote(words) || nonLiteral.containsMatchIn(words))) {
            return ambiguous(AutonomousIntentAmbiguity.QUOTED_OR_HYPOTHETICAL)
        }
        parseQuery(words)?.let { return it }
        parseArrange(words)?.let { return it }
        if (statusWords.containsMatchIn(words) && questions.containsMatchIn(words)) return ambiguous(AutonomousIntentAmbiguity.QUESTION)

        val clauses = words.split(clauseSeparator).map(String::trim).filter(String::isNotEmpty)
        val commands = mutableListOf<AutonomousStatusCommand>()
        for (clause in clauses) {
            val line = clause.replaceFirst(requestPrefix, "").trim()
            val remaining = halfRemaining.matchEntire(line) ?: percentRemaining.matchEntire(line)
            // A remaining-work report may legitimately end in “没完成”. Other negation is not a completion command.
            if (negated.containsMatchIn(line) && (remaining == null || negated.containsMatchIn(remaining.groupValues[1] + "完成"))) {
                return ambiguous(AutonomousIntentAmbiguity.NEGATED_STATUS)
            }
            val parsed = parseStatus(line, clause)
            if (parsed is AutonomousTaskIntent.NeedsClarification) return parsed.copy(sourceText = text)
            val command = (parsed as? AutonomousTaskIntent.StatusUpdates)?.commands?.single()
            if (command == null) {
                if (commands.isNotEmpty() || clauses.any { parseStatus(it.replaceFirst(requestPrefix, ""), it) is AutonomousTaskIntent.StatusUpdates }) {
                    return ambiguous(AutonomousIntentAmbiguity.MIXED_OPERATIONS)
                }
                val timedAction = Regex("\\d+\\s*(?:分钟|分|小时|h|min)", RegexOption.IGNORE_CASE).containsMatchIn(line)
                val unknownRead = Regex("^(?:查询|查看|列出|看看|显示)").containsMatchIn(line) && !timedAction
                if (unknownRead || Regex("^(?:为什么|怎么|如何)|[?？]|(?:改到|移到|挪到|推迟到)").containsMatchIn(line)) {
                    return ambiguous(AutonomousIntentAmbiguity.QUESTION)
                }
                return AutonomousTaskIntent.Capture(text)
            }
            commands += command
        }
        if (commands.isEmpty()) return AutonomousTaskIntent.Capture(text)
        val grouped = commands.groupBy { targetKey(it.target) }
        if (grouped.values.any { sameTarget -> sameTarget.map { it.change }.distinct().size > 1 }) {
            return ambiguous(AutonomousIntentAmbiguity.CONFLICTING_COMMANDS)
        }
        return AutonomousTaskIntent.StatusUpdates(grouped.values.map { it.first() })
    }

    fun resolveTarget(target: AutonomousTaskTarget, tasks: List<Task>, contextTaskId: String? = null): AutonomousTargetResolution {
        val candidates = when (target) {
            is AutonomousTaskTarget.Contextual -> {
                if (contextTaskId == null) return AutonomousTargetResolution.ContextRequired
                tasks.filter { it.id == contextTaskId }
            }
            is AutonomousTaskTarget.Named -> tasks.filter {
                normalizedName(it.displayName) == normalizedName(target.name)
            }
        }.distinctBy { it.id }
        return when (candidates.size) {
            0 -> AutonomousTargetResolution.Missing(target)
            1 -> AutonomousTargetResolution.Found(candidates.single())
            else -> AutonomousTargetResolution.Ambiguous(candidates)
        }
    }

    private fun parseStatus(line: String, source: String): AutonomousTaskIntent? {
        fun command(name: String, change: AutonomousTaskChange): AutonomousTaskIntent {
            val cleaned = name.trim()
            if (multipleTargets.containsMatchIn(name)) return AutonomousTaskIntent.NeedsClarification(AutonomousIntentAmbiguity.MULTIPLE_TARGETS, source)
            val unquoted = unquote(cleaned)
            if (unquoted.isBlank()) return AutonomousTaskIntent.NeedsClarification(AutonomousIntentAmbiguity.MULTIPLE_TARGETS, source)
            val target = if (unquoted in contextual) AutonomousTaskTarget.Contextual(unquoted) else AutonomousTaskTarget.Named(unquoted)
            return AutonomousTaskIntent.StatusUpdates(listOf(AutonomousStatusCommand(target, change, source)))
        }
        fun percentage(match: MatchResult, remaining: Boolean = false): AutonomousTaskIntent {
            val percent = match.groupValues[2].toIntOrNull()
            if (percent == null || percent !in 0..100) return AutonomousTaskIntent.NeedsClarification(AutonomousIntentAmbiguity.INVALID_PROGRESS, source)
            return command(match.groupValues[1], AutonomousTaskChange.Progress(if (remaining) 100 - percent else percent))
        }
        percentRemaining.matchEntire(line)?.let { return percentage(it, remaining = true) }
        halfRemaining.matchEntire(line)?.let { return command(it.groupValues[1], AutonomousTaskChange.Progress(50)) }
        setProgress.matchEntire(line)?.let { return percentage(it) }
        progress.matchEntire(line)?.let { return percentage(it) }
        halfProgress.matchEntire(line)?.let { return command(it.groupValues[1], AutonomousTaskChange.Progress(50)) }
        listOf(markCompletedFirst, markCompleted, completedFirst, completed).forEach { pattern ->
            pattern.matchEntire(line)?.let { return command(it.groupValues[1], AutonomousTaskChange.Complete) }
        }
        if (line.startsWith("完成") && line.removePrefix("完成").trim() in contextual) {
            return command(line.removePrefix("完成"), AutonomousTaskChange.Complete)
        }
        listOf(cancelTarget, cancelledFirst, cancelled).forEach { pattern ->
            pattern.matchEntire(line)?.let { return command(it.groupValues[1], AutonomousTaskChange.Cancel) }
        }
        return null
    }

    private fun parseQuery(words: String): AutonomousTaskIntent.Query? {
        val line = words.replaceFirst(requestPrefix, "")
        val noun = "(?:任务|待办(?:事项)?|待处理事项|安排|日程|计划)"
        val query = Regex("""^(?:查询|查看|列出|看看|显示)(?:一下)?(?:今天|明天|今晚)?(?:的)?(?:所有|全部|待安排|逾期)?$noun(?:有哪些|是什么)?(?:[?？])?$""").matches(line) ||
            Regex("""^(?:今天|明天|今晚)(?:有(?:什么|哪些)(?:$noun)?|要做什么|该做什么|做什么|怎么安排|的$noun)(?:[?？])?$""").matches(line)
        if (!query) {
            val legacy = ArrangementReadIntent.queryScope(words) ?: return null
            // The legacy grammar is intentionally broad. Restrict writes to other branches, but keep its English reads.
            if (Regex("""今天|明天|今晚""").containsMatchIn(line)) return null
            return AutonomousTaskIntent.Query(when (legacy) {
                TaskQueryScope.ALL_ACTIVE -> AutonomousQueryScope.ACTIVE
                TaskQueryScope.TODAY -> AutonomousQueryScope.SCHEDULED
                TaskQueryScope.INBOX -> AutonomousQueryScope.INBOX
                TaskQueryScope.OVERDUE -> AutonomousQueryScope.OVERDUE
            }, if (legacy == TaskQueryScope.TODAY) AutonomousDay.TODAY else null)
        }
        val day = dayIn(line)
        val scope = when {
            line.contains("逾期") -> AutonomousQueryScope.OVERDUE
            line.contains("待安排") -> AutonomousQueryScope.INBOX
            day != null -> AutonomousQueryScope.SCHEDULED
            else -> AutonomousQueryScope.ACTIVE
        }
        return AutonomousTaskIntent.Query(scope, day)
    }

    private fun parseArrange(words: String): AutonomousTaskIntent.Arrange? {
        val line = words.replaceFirst(requestPrefix, "")
        val isRequest = Regex("""^(?:(?:重新)?(?:安排|规划|排)|重排)(?:一下)?(?:今天|明天|今晚)(?:的)?(?:任务|日程|计划|安排)?(?:一下|吧)?$""").matches(line) ||
            Regex("""^(?:今天|明天|今晚)(?:的)?(?:任务|日程|计划)?(?:(?:重新)?(?:安排|规划)|重排)(?:一下|吧)?$""").matches(line) ||
            Regex("""^(?:重新安排|重排|安排一下任务|安排任务|安排今天|规划今天)(?:一下|吧)?$""").matches(line)
        return if (isRequest) AutonomousTaskIntent.Arrange(dayIn(line) ?: AutonomousDay.TODAY) else null
    }

    private fun dayIn(text: String): AutonomousDay? = when {
        text.contains("明天") -> AutonomousDay.TOMORROW
        text.contains("今晚") -> AutonomousDay.TONIGHT
        text.contains("今天") -> AutonomousDay.TODAY
        else -> null
    }

    private fun targetKey(target: AutonomousTaskTarget): String = when (target) {
        is AutonomousTaskTarget.Named -> "name:${normalizedName(target.name)}"
        is AutonomousTaskTarget.Contextual -> "context"
    }

    private fun normalizedName(text: String): String = Normalizer.normalize(unquote(text.trim()), Normalizer.Form.NFKC)
        .lowercase(Locale.ROOT).replace(Regex("""\s+"""), " ")

    private fun isWholeQuote(text: String): Boolean = text.length >= 2 &&
        listOf('“' to '”', '「' to '」', '『' to '』', '"' to '"', '\'' to '\'').any { (left, right) -> text.first() == left && text.last() == right }

    private fun unquote(text: String): String = if (isWholeQuote(text)) text.substring(1, text.lastIndex).trim() else text
}
