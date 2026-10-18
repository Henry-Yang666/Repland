package com.swan1127.repland.domain.model

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Protect the automatic summary's request budget while allowing fresh evidence to catch up. */
class UsageSummaryUpdatePolicyTest {
    private val now = 100L * 60 * 60 * 1000
    private val minute = 60_000L
    private val hour = 60 * minute
    private val configuration = "provider-a"
    private val access = "access-a"
    private val fact = UsageSummaryFact("tasks-created", UsageSummaryDimension.FOCUS,
        "最近30天新建了 10 项任务。", 10, (1..10).map { "task:$it:created" })

    private fun snapshot(revision: String = "current", operations: Int = 10) = UsageSummarySnapshot(
        revision, "2026-09-19", "2026-10-18", now, listOf(fact),
        "最近留下了使用记录。", emptyList(), operations, 2,
    )

    private fun document(revision: String = "previous", generatedAt: Long = now - hour,
        operations: Int = 10) = UsageSummaryDocument(
        revision, generatedAt, "AI · fixture", "此前的使用总结。", emptyList(), listOf(fact.id),
        operations, configuration, access, "2026-09-19", "2026-10-18", listOf(fact),
    )

    private fun request(snapshot: UsageSummarySnapshot, document: UsageSummaryDocument? = null,
        attempt: UsageSummaryAttempt? = null, provider: String = configuration,
        permission: String = access) = UsageSummaryUpdatePolicy.shouldRequest(
        snapshot, document, attempt, provider, permission, now,
    )

    @Test fun an_existing_summary_of_the_same_evidence_is_reused_even_after_six_hours() {
        assertFalse(request(snapshot(), document(revision = "current", generatedAt = now - 48 * hour)))
    }

    @Test fun a_first_summary_requests_automatically_when_there_are_real_facts() {
        assertTrue(request(snapshot()))
    }

    @Test fun a_persisted_failed_attempt_prevents_restart_retry_until_fifteen_minutes_pass() {
        val lastAttempt = UsageSummaryAttempt("current", now - 15 * minute + 1, configuration, access)
        assertFalse(request(snapshot(), attempt = lastAttempt))
        assertTrue(request(snapshot(), attempt = lastAttempt.copy(attemptedAt = now - 15 * minute)))
    }

    @Test fun one_or_two_new_operations_keep_a_recent_summary_and_a_third_updates_it() {
        val old = document()
        assertFalse(request(snapshot(operations = 11), old))
        assertFalse(request(snapshot(operations = 12), old))
        assertTrue(request(snapshot(operations = 13), old))
    }

    @Test fun changed_evidence_is_refreshed_after_six_hours_even_without_three_added_operations() {
        val old = document(generatedAt = now - 6 * hour + 1)
        assertFalse(request(snapshot(), old))
        assertTrue(request(snapshot(), old.copy(generatedAt = now - 6 * hour)))
    }

    @Test fun recent_failure_also_throttles_an_otherwise_due_update() {
        val old = document(generatedAt = now - 8 * hour)
        val pending = UsageSummaryAttempt("current", now - minute, configuration, access)
        assertFalse(request(snapshot(operations = 13), old, pending))
        assertTrue(request(snapshot(operations = 13), old, pending.copy(attemptedAt = now - 15 * minute)))
    }

    @Test fun an_empty_history_does_not_send_a_request_after_clear_or_provider_change() {
        val cleared = snapshot().copy(facts = emptyList(), operationCount = 0, activeDays = 0)
        assertFalse(request(cleared))
        assertFalse(request(cleared, document(), provider = "provider-b"))
    }

    @Test fun provider_or_access_changes_do_not_reuse_an_incompatible_summary_or_retry_marker() {
        val old = document(revision = "current")
        val attempt = UsageSummaryAttempt("current", now, configuration, access)
        assertTrue(request(snapshot(), old, attempt, provider = "provider-b"))
        assertTrue(request(snapshot(), old, attempt, permission = "access-b"))
    }
}
