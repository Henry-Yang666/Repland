package com.swan1127.repland.domain.model

import java.time.LocalDate
import java.time.temporal.TemporalAdjusters

/** Offline calendar dates are explicit input; an unsupported expression must not silently become today. */
object AutonomousCaptureDate {
    data class Selection(val date: LocalDate, val explicit: Boolean)

    fun resolve(text: String, today: LocalDate): Selection {
        val dates = mutableSetOf<LocalDate>()
        if (Regex("今天|今晚|今早|今下午").containsMatchIn(text)) dates += today
        if (text.contains("明天")) dates += today.plusDays(1)
        if (text.contains("大后天")) dates += today.plusDays(3)
        else if (text.contains("后天")) dates += today.plusDays(2)
        require(!Regex("每天|每周|每月|昨天|前天").containsMatchIn(text)) {
            "请说明要安排的具体未来日期；重复任务可以先逐次记录。"
        }
        Regex("""(?<!\d)(\d{4})-(\d{1,2})-(\d{1,2})(?!\d)""").findAll(text).forEach {
            dates += LocalDate.of(it.groupValues[1].toInt(), it.groupValues[2].toInt(), it.groupValues[3].toInt())
        }
        Regex("""(?:(\d{4})年)?(\d{1,2})月(\d{1,2})[日号]?""").findAll(text).forEach {
            dates += LocalDate.of(it.groupValues[1].toIntOrNull() ?: today.year,
                it.groupValues[2].toInt(), it.groupValues[3].toInt())
        }
        Regex("""(下周|本周|这周|周|星期)([一二三四五六日天])""").findAll(text).forEach {
            val day = ("一二三四五六日".indexOf(it.groupValues[2].replace("天", "日")) + 1)
            val monday = today.with(TemporalAdjusters.previousOrSame(java.time.DayOfWeek.MONDAY))
            dates += if (it.groupValues[1] == "下周") monday.plusDays(7 + day - 1L)
                else if (it.groupValues[1] in setOf("本周", "这周")) monday.plusDays(day - 1L)
                else today.with(TemporalAdjusters.nextOrSame(java.time.DayOfWeek.of(day)))
        }
        require(dates.size <= 1) { "这句话涉及不同日期，请按日期分开发送，我会分别安排。" }
        if (dates.isEmpty()) require(!Regex("下周|下个月|下月|周末|每天|每周|每月|大后天|昨天|前天").containsMatchIn(text)) {
            "请写明具体日期（如明天、下周一或10月20日）；重复任务可以先逐次记录。"
        }
        val date = dates.singleOrNull() ?: today
        require(date >= today) { "这个日期已经过去，请说明要安排到哪一天。" }
        return Selection(date, dates.isNotEmpty() || Regex("上午|下午|晚上|早上|中午").containsMatchIn(text))
    }
}
