package com.swan1127.repland.domain.model

/** Provider wording and estimates may help, but cannot replace explicit local time/duration input. */
object AutonomousCaptureFacts {
    fun recordOnly(text: String?): Boolean = text != null &&
        Regex("只记着|只记下|只记录|仅记录|只新增|不要安排|暂不安排|先不安排").containsMatchIn(text)
    fun reconcile(local: List<ArrangementCandidate>, proposed: List<ArrangementCandidate>): List<ArrangementCandidate> {
        if (local.isEmpty()) return proposed
        fun key(title: String) = title.trim().lowercase().replace(Regex("\\s+"), "")
        val pairs = if (local.size == 1 && proposed.size == 1) listOf(local.single() to proposed.single())
            else {
                if (local.size != proposed.size || local.map { key(it.title) }.distinct().size != local.size ||
                    proposed.map { key(it.title) }.distinct().size != proposed.size) return local
                local.map { original -> original to (proposed.singleOrNull { key(it.title) == key(original.title) } ?: return local) }
            }
        return pairs.map { (original, suggestion) -> suggestion.copy(
            title = original.title,
            durationMinutes = original.durationMinutes ?: suggestion.durationMinutes,
            timeHint = original.timeHint,
            placementSource = original.placementSource,
            existingTaskId = null,
        ) }
    }
}
