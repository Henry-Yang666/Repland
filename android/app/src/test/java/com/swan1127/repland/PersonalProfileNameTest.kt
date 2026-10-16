package com.swan1127.repland

import com.swan1127.repland.domain.model.PersonalProfileName
import org.junit.Assert.*
import org.junit.Test

class PersonalProfileNameTest {
    @Test fun trims_outer_spaces_and_keeps_chinese_and_internal_spaces() {
        assertEquals("小林 的日程", PersonalProfileName.normalized("  小林 的日程  "))
    }
    @Test fun rejects_empty_or_whitespace_only_names() {
        listOf("", " ", "　").forEach { assertNotNull(PersonalProfileName.error(it)) }
    }
    @Test fun rejects_controls_and_line_separators_instead_of_silently_modifying_them() {
        listOf("小\n林", "小\r林", "小\t林", "a\u0000b", "a\u2028b", "a\u2029b").forEach { assertNotNull(PersonalProfileName.error(it)) }
    }
    @Test fun accepts_24_code_points_and_rejects_25_without_truncating() {
        assertNull(PersonalProfileName.error("林".repeat(24)))
        assertNotNull(PersonalProfileName.error("林".repeat(25)))
        assertEquals(25, PersonalProfileName.length("林".repeat(25)))
    }
    @Test fun counts_supplementary_emoji_as_one_code_point() {
        val emoji = "\uD83C\uDF31"
        assertEquals(24, PersonalProfileName.length(emoji.repeat(24)))
        assertEquals(emoji.repeat(24), PersonalProfileName.normalized(emoji.repeat(24)))
        assertNotNull(PersonalProfileName.error(emoji.repeat(25)))
    }
}
