package com.example.gymplan

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

data class Exercise(
    val id: Long,
    val name: String,
    val sets: Int,
    val reps: String,   // text so "8-12" or "to failure" work
    val weight: Double, // kg
    val seat: String,
    val notes: String,  // other adjustments
    val cardio: Boolean = false,
    val minutes: Int = 0,       // cardio duration
    val intensity: String = "", // cardio: speed / incline / level, free text
)

/** The number whose evolution we track: load for strength, duration for cardio. */
val Exercise.tracked get() = if (cardio) minutes.toDouble() else weight
val Exercise.unit get() = if (cardio) "min" else "kg"

data class Plan(val id: Long, val name: String, val exercises: List<Exercise> = emptyList())

data class WeightEntry(val date: Long, val weight: Double)

data class Data(
    val plans: List<Plan> = emptyList(),
    val body: List<WeightEntry> = emptyList(),
    val history: Map<Long, List<WeightEntry>> = emptyMap(), // exercise id -> weight changes
    val done: Map<Long, Long> = emptyMap(), // exercise id -> epoch day it was last marked complete
)

fun newId() = System.nanoTime()

fun today() = java.time.LocalDate.now().toEpochDay()

fun Data.isDone(exId: Long, day: Long = today()) = done[exId] == day

/** Completion is per day, so every exercise starts unchecked on the next training day. */
fun Data.toggleDone(exId: Long, day: Long = today()) =
    copy(done = if (isDone(exId, day)) done - exId else done + (exId to day))

fun Data.clearDone(exIds: Collection<Long>) = copy(done = done - exIds.toSet())

fun Data.upsertPlan(p: Plan) = copy(
    plans = if (plans.any { it.id == p.id }) plans.map { if (it.id == p.id) p else it } else plans + p
)

fun Data.deletePlan(id: Long): Data {
    val exIds = plans.find { it.id == id }?.exercises?.map { it.id }.orEmpty().toSet()
    return copy(plans = plans.filter { it.id != id }, history = history - exIds, done = done - exIds)
}

/** Saves the exercise and logs a history point whenever its weight is new or changed. */
fun Data.upsertExercise(planId: Long, e: Exercise, now: Long = System.currentTimeMillis()): Data {
    val plan = plans.first { it.id == planId }
    val old = plan.exercises.find { it.id == e.id }
    val exs = if (old != null) plan.exercises.map { if (it.id == e.id) e else it } else plan.exercises + e
    val h = if (old?.tracked == e.tracked && old.cardio == e.cardio) history
    else history + (e.id to history[e.id].orEmpty() + WeightEntry(now, e.tracked))
    return upsertPlan(plan.copy(exercises = exs)).copy(history = h)
}

fun Data.deleteExercise(planId: Long, exId: Long): Data {
    val plan = plans.first { it.id == planId }
    return upsertPlan(plan.copy(exercises = plan.exercises.filter { it.id != exId })).copy(history = history - exId, done = done - exId)
}

// --- JSON ---

private fun entriesJson(l: List<WeightEntry>) =
    JSONArray(l.map { JSONObject().put("date", it.date).put("weight", it.weight) })

private fun JSONArray.objs() = (0 until length()).map { getJSONObject(it) }

private fun entries(a: JSONArray?) =
    a?.objs()?.map { WeightEntry(it.getLong("date"), it.getDouble("weight")) }.orEmpty()

fun Data.toJson(): String = JSONObject()
    .put("plans", JSONArray(plans.map { p ->
        JSONObject().put("id", p.id).put("name", p.name).put("exercises", JSONArray(p.exercises.map { e ->
            JSONObject().put("id", e.id).put("name", e.name).put("sets", e.sets).put("reps", e.reps)
                .put("weight", e.weight).put("seat", e.seat).put("notes", e.notes)
                .put("cardio", e.cardio).put("minutes", e.minutes).put("intensity", e.intensity)
        }))
    }))
    .put("body", entriesJson(body))
    .put("history", JSONObject().apply { history.forEach { (k, v) -> put(k.toString(), entriesJson(v)) } })
    .put("done", JSONObject().apply { done.forEach { (k, v) -> put(k.toString(), v) } })
    .toString()

fun parseData(s: String): Data {
    val o = JSONObject(s)
    val h = o.optJSONObject("history")
    val dn = o.optJSONObject("done")
    return Data(
        plans = o.optJSONArray("plans")?.objs()?.map { p ->
            Plan(p.getLong("id"), p.getString("name"), p.optJSONArray("exercises")?.objs()?.map { e ->
                Exercise(
                    e.getLong("id"), e.getString("name"), e.optInt("sets"), e.optString("reps"),
                    e.optDouble("weight", 0.0), e.optString("seat"), e.optString("notes"),
                    e.optBoolean("cardio"), e.optInt("minutes"), e.optString("intensity"),
                )
            }.orEmpty())
        }.orEmpty(),
        body = entries(o.optJSONArray("body")),
        history = h?.keys()?.asSequence()?.associate { it.toLong() to entries(h.getJSONArray(it)) }.orEmpty(),
        done = dn?.keys()?.asSequence()?.associate { it.toLong() to dn.getLong(it) }.orEmpty(),
    )
}

class Store(private val file: File) {
    var data by mutableStateOf(load())
        private set

    private fun load(): Data {
        if (!file.exists()) return Data()
        return try {
            parseData(file.readText())
        } catch (e: Exception) {
            // keep the unreadable file instead of overwriting it on the next save
            file.renameTo(File(file.path + ".bad-" + System.currentTimeMillis()))
            Data()
        }
    }

    fun update(f: (Data) -> Data) {
        data = f(data)
        val tmp = File(file.path + ".tmp")
        tmp.writeText(data.toJson())
        tmp.renameTo(file) // atomic replace: a crash mid-write can't corrupt the saved data
    }
}
