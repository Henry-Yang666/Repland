package com.swan1127.repland

import com.swan1127.repland.domain.model.AutonomousCaptureDate
import java.time.LocalDate
import org.junit.Assert.*
import org.junit.Test

class AutonomousCaptureDateTest {
    private val today = LocalDate.of(2026, 10, 16)
    @Test fun resolves_unambiguous_dates_without_inventing_a_deadline() {
        assertEquals(today.plusDays(1), AutonomousCaptureDate.resolve("明天复习", today).date)
        assertEquals(LocalDate.of(2026, 10, 19), AutonomousCaptureDate.resolve("下周一复习", today).date)
        assertEquals(LocalDate.of(2026, 10, 20), AutonomousCaptureDate.resolve("10月20日复习", today).date)
        assertEquals(LocalDate.of(2026, 11, 1), AutonomousCaptureDate.resolve("2026-11-01复习", today).date)
        assertFalse(AutonomousCaptureDate.resolve("复习", today).explicit)
    }
    @Test fun mixed_or_unsupported_dates_are_not_silently_scheduled_today() {
        for (text in listOf("今天跑步，明天复习", "下周复习", "周末复习", "每天复习", "10月1日复习")) {
            try { AutonomousCaptureDate.resolve(text, today); fail("Should reject $text") }
            catch (_: IllegalArgumentException) { }
        }
    }
}
