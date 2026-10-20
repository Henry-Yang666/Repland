package com.swan1127.repland.data.room

import com.swan1127.repland.domain.model.*
import org.json.JSONArray
import org.json.JSONObject

/** Cache payloads contain aggregate facts and source references, never credentials or raw replies. */
object UsageSummaryCodec {
    private fun row() = JSONObject().put("version", 1)
    private fun parse(payload: String) = JSONObject(payload).also { require(it.getInt("version") == 1) }
    private fun JSONObject.nullable(key: String): String? = if (isNull(key)) null else getString(key)
    private fun JSONArray.strings() = (0 until length()).map(::getString)
    private fun fact(value: UsageSummaryFact) = JSONObject().put("id", value.id)
        .put("dimension", value.dimension.name).put("text", value.text).put("count", value.count)
        .put("sources", JSONArray(value.sourceIds))
    private fun facts(values: List<UsageSummaryFact>) = JSONArray(values.map(::fact))
    private fun JSONObject.facts() = getJSONArray("facts").let { array ->
        (0 until array.length()).map { index -> array.getJSONObject(index).let {
            UsageSummaryFact(it.getString("id"), UsageSummaryDimension.valueOf(it.getString("dimension")),
                it.getString("text"), it.getInt("count"), it.getJSONArray("sources").strings())
        } }
    }

    fun encode(value: UsageSummarySnapshot): String = row().put("revision", value.revision)
        .put("windowStart", value.windowStart).put("windowEnd", value.windowEnd).put("updatedAt", value.updatedAt)
        .put("facts", facts(value.facts)).put("localSummary", value.localSummary)
        .put("localHighlights", JSONArray(value.localHighlights)).put("operationCount", value.operationCount)
        .put("activeDays", value.activeDays).toString()

    fun snapshot(payload: String): UsageSummarySnapshot = parse(payload).let {
        UsageSummarySnapshot(it.getString("revision"), it.getString("windowStart"), it.getString("windowEnd"),
            it.getLong("updatedAt"), it.facts(), it.getString("localSummary"), it.getJSONArray("localHighlights").strings(),
            it.getInt("operationCount"), it.getInt("activeDays"))
    }

    fun encode(value: UsageSummaryDocument): String = row().put("revision", value.revision)
        .put("generatedAt", value.generatedAt).put("origin", value.origin).put("text", value.text)
        .put("highlights", JSONArray(value.highlights)).put("sourceFactIds", JSONArray(value.sourceFactIds))
        .put("operationCount", value.operationCount).put("configurationRevision", value.configurationRevision ?: JSONObject.NULL)
        .put("accessRevision", value.accessRevision ?: JSONObject.NULL).put("windowStart", value.windowStart)
        .put("windowEnd", value.windowEnd).put("facts", facts(value.facts))
        .put("contentVersion", value.contentVersion).toString()

    fun document(payload: String): UsageSummaryDocument = parse(payload).let {
        UsageSummaryDocument(it.getString("revision"), it.getLong("generatedAt"), it.getString("origin"),
            it.getString("text"), it.getJSONArray("highlights").strings(), it.getJSONArray("sourceFactIds").strings(),
            it.getInt("operationCount"), it.nullable("configurationRevision"), it.nullable("accessRevision"),
            it.getString("windowStart"), it.getString("windowEnd"), it.facts(), it.optInt("contentVersion", 1))
    }

    fun encode(value: UsageSummaryAttempt): String = row().put("revision", value.revision)
        .put("attemptedAt", value.attemptedAt).put("configurationRevision", value.configurationRevision)
        .put("accessRevision", value.accessRevision).put("contentVersion", value.contentVersion).toString()

    fun attempt(payload: String): UsageSummaryAttempt = parse(payload).let {
        UsageSummaryAttempt(it.getString("revision"), it.getLong("attemptedAt"), it.getString("configurationRevision"),
            it.getString("accessRevision"), it.optInt("contentVersion", 1))
    }

    fun encode(value: UsageSummaryAction): String = row().put("id", value.id).put("kind", value.kind.name)
        .put("taskId", value.taskId ?: JSONObject.NULL).put("sourceId", value.sourceId ?: JSONObject.NULL)
        .put("occurredAt", value.occurredAt).toString()

    fun action(payload: String): UsageSummaryAction = parse(payload).let {
        UsageSummaryAction(it.getString("id"), UsageSummaryActionKind.valueOf(it.getString("kind")),
            it.nullable("taskId"), it.nullable("sourceId"), it.getLong("occurredAt"))
    }
}
