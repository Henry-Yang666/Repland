package com.swan1127.repland.domain.model

/** One cached explanation per evidence revision, with bounded automatic refreshes. */
object UsageSummaryUpdatePolicy {
    const val RETRY_INTERVAL = 15 * 60_000L
    const val UPDATE_INTERVAL = 6 * 60 * 60_000L
    const val SIGNIFICANT_OPERATIONS = 3

    fun shouldRequest(snapshot: UsageSummarySnapshot, document: UsageSummaryDocument?, attempt: UsageSummaryAttempt?,
        configurationRevision: String, accessRevision: String, now: Long): Boolean {
        if (snapshot.facts.isEmpty()) return false
        val cached = document?.takeIf {
            it.contentVersion == UsageSummaryDocument.CURRENT_CONTENT_VERSION &&
                it.configurationRevision == configurationRevision && it.accessRevision == accessRevision
        }
        if (cached?.revision == snapshot.revision) return false
        if (attempt != null && attempt.contentVersion == UsageSummaryDocument.CURRENT_CONTENT_VERSION &&
            attempt.configurationRevision == configurationRevision && attempt.accessRevision == accessRevision &&
            now - attempt.attemptedAt < RETRY_INTERVAL) return false
        return cached == null || now - cached.generatedAt >= UPDATE_INTERVAL ||
            snapshot.operationCount - cached.operationCount >= SIGNIFICANT_OPERATIONS
    }
}
