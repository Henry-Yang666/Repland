package com.swan1127.repland.data.ai

import com.swan1127.repland.domain.model.*
import com.swan1127.repland.domain.ports.AiProviderConfigRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.first
import org.json.JSONArray
import org.json.JSONObject

/** Finite read-only snapshot contract. No task prose, raw logs, writes or arbitrary model prose. */
class CompatibleNumericProfileAdvisor(private val config: AiProviderConfigRepository): NumericProfileAdvisor {
    override suspend fun describe(request: NumericNarrationRequest): NumericNarration? = try {
        val metadata=config.observe().first()
        val provider=config.readSecret()
        if(provider==null) null else {
            val payload=payload(request)
            val text=CompatibleChatTransport().complete(provider,
                "Return exactly one JSON object, no Markdown or prose, with all four fields: requestId, profileVersion, claims [{parameterId,value}], nextStep. Keep the supplied requestId and profileVersion. Return 1 to 6 distinct claims, only for supplied parameter IDs and their exact values. No user ratings, new numbers, or write commands. nextStep is one of NONE, REVIEW_ESTIMATE, CONSIDER_SPLITTING and is optional planning advice, never a fact. Choose NONE unless the following conditions hold: REVIEW_ESTIMATE requires a claimed estimate parameter with availability USABLE; CONSIDER_SPLITTING requires a claimed rounds parameter with availability USABLE. REFERENCE parameters have insufficient samples for either advice. Do not infer advice from them.",payload)
            if(config.observe().first()!=metadata) null else decode(text)
        }
    } catch(c: CancellationException) { throw c } catch(_: Exception) { null }
    companion object {
        fun payload(request: NumericNarrationRequest): JSONObject = JSONObject().put("contract","numeric-description/v1").put("requestId",request.requestId)
            .put("profileVersion",request.profileVersion).put("rule",request.rule)
            .put("windowStart",request.windowStart).put("windowEnd",request.windowEnd)
            .put("parameters",JSONArray(request.parameters.filter { it.value!=null && it.availability!=NumericAvailability.DISABLED }.map { p -> JSONObject().put("id",p.id).put("value",p.value)
                .put("unit",p.unit).put("count",p.count).put("availability",p.availability.name) }))
        fun decode(text: String): NumericNarration {
            // Strip only the code fence also accepted by the task-advice adapter.
            // The finite facts-only schema and subsequent value validation remain strict.
            val json=text.trim().removePrefix("```json").removePrefix("```").removeSuffix("```").trim()
            val j=JSONObject(json)
            require(j.keys().asSequence().toSet()==setOf("requestId","profileVersion","claims","nextStep"))
            val a=j.getJSONArray("claims"); require(a.length() in 1..6)
            return NumericNarration(j.getString("requestId"),j.getString("profileVersion"),(0 until a.length()).map { i ->
                val c=a.getJSONObject(i); require(c.keys().asSequence().toSet()==setOf("parameterId","value"))
                NumericClaim(c.getString("parameterId"),c.getDouble("value"))
            },NumericNextStep.valueOf(j.getString("nextStep")))
        }
    }
}
