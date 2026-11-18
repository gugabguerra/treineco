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
    val repsPerSet: List<Int> = emptyList(),      // empty: every set uses [reps]
    val weightsPerSet: List<Double> = emptyList(), // empty: every set uses [weight]
)

/** The number whose evolution we track: the heaviest set, or duration for cardio. */
val Exercise.tracked get() = if (cardio) minutes.toDouble() else weightsPerSet.maxOrNull() ?: weight
val Exercise.unit get() = if (cardio) "min" else "kg"

/** Training-list reps: "10" for one set, "× 10" when sets is 1, "3 × 10", or "12 / 10 / 8" when reps vary. */
fun Exercise.cardReps(): String = when {
    repsPerSet.distinct().size > 1 -> repsSummary()
    sets == 1 -> "× ${repsSummary()}"
    else -> "$sets × ${repsSummary()}"
}

/** Training-list weights when they vary: "70 / 65 / 60"; over four values, the first three and "...". */
fun Exercise.cardWeights(): String? {
    val values = weightsPerSet.distinct()
    if (values.size <= 1) return null
    val shown = values.map(::fmt)
    return if (shown.size > 4) shown.take(3).joinToString(" / ") + " / ..." else shown.joinToString(" / ")
}

data class Plan(val id: Long, val name: String, val exercises: List<Exercise> = emptyList())

data class WeightEntry(val date: Long, val weight: Double)

/** Sets, reps and weight of the last strength exercise added; pre-fills the next new one. */
data class ExerciseDefaults(val sets: Int = 3, val reps: String = "10", val weight: Double? = null)

data class Data(
    val plans: List<Plan> = emptyList(),
    val body: List<WeightEntry> = emptyList(),
    val history: Map<Long, List<WeightEntry>> = emptyMap(), // exercise id -> weight changes
    val done: Map<Long, Long> = emptyMap(), // exercise id -> epoch ms it was marked complete
    val defaults: ExerciseDefaults = ExerciseDefaults(),
)

fun newId() = System.nanoTime()

const val DAY_MS = 24 * 60 * 60 * 1000L

private fun Plan.lastDone(done: Map<Long, Long>) = exercises.mapNotNull { done[it.id] }.maxOrNull()

/**
 * A plan's ticks all clear together 24 h after its most recent tick, so a session that runs past
 * midnight stays checked and the next session starts clean.
 */
fun Data.isDone(exId: Long, now: Long = System.currentTimeMillis()): Boolean {
    if (exId !in done) return false
    val last = plans.find { p -> p.exercises.any { it.id == exId } }?.lastDone(done) ?: return false
    return now - last < DAY_MS
}

/** Drops the ticks of plans whose last tick is 24 h old or more; returns `this` when none are. */
fun Data.expireDone(now: Long = System.currentTimeMillis()): Data {
    val stale = plans.filter { p -> p.lastDone(done)?.let { now - it >= DAY_MS } == true }
        .flatMap { p -> p.exercises.map { it.id } }
    return if (stale.isEmpty()) this else copy(done = done - stale.toSet())
}

/** Expires first, so ticking a stale plan never revives its old ticks. */
fun Data.toggleDone(exId: Long, now: Long = System.currentTimeMillis()) = expireDone(now).run {
    copy(done = if (exId in done) done - exId else done + (exId to now))
}

/** Puts the plan's exercises in the order of [ids]; ids not listed keep their place at the end. */
fun Data.reorderExercises(planId: Long, ids: List<Long>): Data {
    val plan = plans.first { it.id == planId }
    val pos = ids.withIndex().associate { (i, id) -> id to i }
    return upsertPlan(plan.copy(exercises = plan.exercises.sortedBy { pos[it.id] ?: Int.MAX_VALUE }))
}

/** Puts the plans in the order of [ids]; ids not listed keep their place at the end. */
fun Data.reorderPlans(ids: List<Long>): Data {
    val pos = ids.withIndex().associate { (i, id) -> id to i }
    return copy(plans = plans.sortedBy { pos[it.id] ?: Int.MAX_VALUE })
}

/**
 * Inserts a copy of the plan right after it, under [name], with fresh exercise ids. Each copied
 * exercise starts its own history at its current value; ticks are not copied.
 */
fun Data.clonePlan(planId: Long, name: String, now: Long = System.currentTimeMillis(), id: () -> Long = ::newId): Data {
    val i = plans.indexOfFirst { it.id == planId }
    val src = plans[i]
    val exs = src.exercises.map { it.copy(id = id()) }
    val clone = Plan(id(), name, exs)
    return copy(
        plans = plans.toMutableList().apply { add(i + 1, clone) },
        history = history + exs.associate { it.id to listOf(WeightEntry(now, it.tracked)) },
    )
}

/** Inserts a copy of the exercise right after it, under [name]; its history starts fresh. */
fun Data.cloneExercise(planId: Long, exId: Long, name: String, now: Long = System.currentTimeMillis(), id: Long = newId()): Data {
    val plan = plans.first { it.id == planId }
    val i = plan.exercises.indexOfFirst { it.id == exId }
    val e = plan.exercises[i].copy(id = id, name = name)
    return upsertPlan(plan.copy(exercises = plan.exercises.toMutableList().apply { add(i + 1, e) }))
        .copy(history = history + (id to listOf(WeightEntry(now, e.tracked))))
}

/**
 * Copies the exercise to the end of another plan, under a fresh id; its history starts at its
 * current value and ticks are not copied.
 */
fun Data.cloneExerciseToPlan(
    fromPlanId: Long, exId: Long, toPlanId: Long, now: Long = System.currentTimeMillis(), id: Long = newId(),
): Data {
    val e = plans.first { it.id == fromPlanId }.exercises.first { it.id == exId }.copy(id = id)
    val target = plans.first { it.id == toPlanId }
    return upsertPlan(target.copy(exercises = target.exercises + e))
        .copy(history = history + (id to listOf(WeightEntry(now, e.tracked))))
}

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
    val defs = if (old == null && !e.cardio) ExerciseDefaults(e.sets, e.reps, e.tracked) else defaults
    return upsertPlan(plan.copy(exercises = exs)).copy(history = h, defaults = defs)
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
                .put("repsPerSet", JSONArray(e.repsPerSet))
                .put("weightsPerSet", JSONArray(e.weightsPerSet))
        }))
    }))
    .put("body", entriesJson(body))
    .put("history", JSONObject().apply { history.forEach { (k, v) -> put(k.toString(), entriesJson(v)) } })
    .put("done", JSONObject().apply { done.forEach { (k, v) -> put(k.toString(), v) } })
    .put("defaults", JSONObject().put("sets", defaults.sets).put("reps", defaults.reps)
        .apply { defaults.weight?.let { put("weight", it) } })
    .toString()

fun parseData(s: String): Data {
    val o = JSONObject(s)
    val h = o.optJSONObject("history")
    val dn = o.optJSONObject("done")
    val df = o.optJSONObject("defaults")
    return Data(
        plans = o.optJSONArray("plans")?.objs()?.map { p ->
            Plan(p.getLong("id"), p.getString("name"), p.optJSONArray("exercises")?.objs()?.map { e ->
                Exercise(
                    e.getLong("id"), e.getString("name"), e.optInt("sets"), e.optString("reps"),
                    e.optDouble("weight", 0.0), e.optString("seat"), e.optString("notes"),
                    e.optBoolean("cardio"), e.optInt("minutes"), e.optString("intensity"),
                    repsPerSet = e.optJSONArray("repsPerSet")?.let { a -> List(a.length()) { a.optInt(it) } }.orEmpty(),
                    weightsPerSet = e.optJSONArray("weightsPerSet")?.let { a -> List(a.length()) { a.optDouble(it, 0.0) } }.orEmpty(),
                )
            }.orEmpty())
        }.orEmpty(),
        body = entries(o.optJSONArray("body")),
        history = h?.keys()?.asSequence()?.associate { it.toLong() to entries(h.getJSONArray(it)) }.orEmpty(),
        // Before ticks expired after 24 h, "done" held epoch days; read those as that day's start.
        done = dn?.keys()?.asSequence()?.associate { k ->
            k.toLong() to dn.getLong(k).let { if (it < 100_000_000L) it * DAY_MS else it }
        }.orEmpty(),
        defaults = df?.let {
            ExerciseDefaults(it.optInt("sets", 3), it.optString("reps", "10"),
                if (it.has("weight")) it.getDouble("weight") else null)
        } ?: ExerciseDefaults(),
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
