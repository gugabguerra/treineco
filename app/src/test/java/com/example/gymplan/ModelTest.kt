package com.example.gymplan

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Test

class ModelTest {
    private val ex = Exercise(1, "Leg press", 4, "10", 80.0, "Seat 5", "Back pad 2")

    @Test
    fun weightChangesAreLogged() {
        var d = Data().upsertPlan(Plan(10, "A"))
        d = d.upsertExercise(10, ex, now = 100)
        d = d.upsertExercise(10, ex.copy(sets = 3), now = 200)   // same weight: no log
        d = d.upsertExercise(10, ex.copy(weight = 90.0), now = 300)
        assertEquals(listOf(WeightEntry(100, 80.0), WeightEntry(300, 90.0)), d.history[1])
        assertEquals(1, d.plans[0].exercises.size)
        assertEquals(emptyMap<Long, List<WeightEntry>>(), d.deletePlan(10).history)
    }

    @Test
    fun ticksClearTogether24hAfterTheLastOne() {
        val a = ex.copy(id = 1); val b = ex.copy(id = 2)
        var d = Data().upsertPlan(Plan(10, "A", listOf(a, b))).toggleDone(1, now = 0)
        d = d.toggleDone(2, now = 10 * 3_600_000L)                     // ticked 10 h later
        assertEquals(true, d.isDone(1, now = DAY_MS + 1))               // 24 h counts from the last tick
        assertEquals(false, d.isDone(1, now = 34 * 3_600_000L))
        assertEquals(false, d.isDone(2, now = 34 * 3_600_000L))
        assertEquals(emptyMap<Long, Long>(), d.expireDone(now = 34 * 3_600_000L).done)
        assertSame(d, d.expireDone(now = DAY_MS))                       // nothing stale: same instance
        // ticking a stale plan starts over instead of reviving the old ticks
        val next = d.toggleDone(2, now = 50 * 3_600_000L)
        assertEquals(false, next.isDone(1, now = 50 * 3_600_000L))
        assertEquals(true, next.isDone(2, now = 50 * 3_600_000L))
        assertEquals(false, d.toggleDone(2, now = 11 * 3_600_000L).isDone(2, now = 11 * 3_600_000L))
        assertEquals(mapOf(2L to 10 * 3_600_000L), d.deleteExercise(10, 1).done)
        assertEquals(false, d.clearDone(listOf(1L)).isDone(1, now = 11 * 3_600_000L))
    }

    @Test
    fun legacyEpochDayTicksAreRead() {
        val json = """{"plans":[{"id":10,"name":"A","exercises":[]}],"done":{"1":20000}}"""
        assertEquals(mapOf(1L to 20000 * DAY_MS), parseData(json).done)
        assertEquals(ExerciseDefaults(), parseData(json).defaults)
    }

    @Test
    fun newStrengthExerciseSetsDefaults() {
        var d = Data().upsertPlan(Plan(10, "A")).upsertExercise(10, ex)
        assertEquals(ExerciseDefaults(4, "10", 80.0), d.defaults)
        d = d.upsertExercise(10, ex.copy(weight = 90.0))                // editing keeps them
        assertEquals(ExerciseDefaults(4, "10", 80.0), d.defaults)
        d = d.upsertExercise(10, Exercise(2, "Bike", 1, "", 0.0, "", "", cardio = true, minutes = 20))
        assertEquals(ExerciseDefaults(4, "10", 80.0), d.defaults)       // cardio keeps them
    }

    @Test
    fun cloneAndReorderPlans() {
        val a = ex.copy(id = 1); val b = ex.copy(id = 2)
        var next = 100L
        var d = Data().upsertPlan(Plan(10, "A", listOf(a, b))).upsertPlan(Plan(20, "B")).toggleDone(1, now = 0)
        d = d.clonePlan(10, "A copy", now = 5) { next++ }
        assertEquals(listOf(10L, 102L, 20L), d.plans.map { it.id })
        assertEquals(listOf(100L, 101L), d.plans[1].exercises.map { it.id })
        assertEquals(listOf("Leg press", "Leg press"), d.plans[1].exercises.map { it.name })
        assertEquals(listOf(WeightEntry(5, 80.0)), d.history[100])
        assertEquals(false, d.isDone(100, now = 1))
        d = d.reorderPlans(listOf(20, 10))
        assertEquals(listOf(20L, 10L, 102L), d.plans.map { it.id })
    }

    @Test
    fun cloneExerciseInsertsAfterOriginal() {
        val a = ex.copy(id = 1); val b = ex.copy(id = 2)
        val d = Data().upsertPlan(Plan(10, "A", listOf(a, b))).cloneExercise(10, 1, "Leg press (copy)", now = 7, id = 9)
        assertEquals(listOf(1L, 9L, 2L), d.plans[0].exercises.map { it.id })
        assertEquals(a.copy(id = 9, name = "Leg press (copy)"), d.plans[0].exercises[1])
        assertEquals(listOf(WeightEntry(7, 80.0)), d.history[9])
    }

    @Test
    fun cardioTracksMinutes() {
        val bike = Exercise(2, "Bike", 1, "", 0.0, "Seat 7", "", cardio = true, minutes = 20, intensity = "Level 8")
        var d = Data().upsertPlan(Plan(10, "A")).upsertExercise(10, bike, now = 100)
        d = d.upsertExercise(10, bike.copy(minutes = 30), now = 200)
        assertEquals(listOf(WeightEntry(100, 20.0), WeightEntry(200, 30.0)), d.history[2])
    }

    @Test
    fun reorderKeepsAllExercises() {
        val a = ex.copy(id = 1); val b = ex.copy(id = 2); val c = ex.copy(id = 3)
        var d = Data().upsertPlan(Plan(10, "A", listOf(a, b, c)))
        d = d.reorderExercises(10, listOf(3, 1, 2))
        assertEquals(listOf(3L, 1L, 2L), d.plans[0].exercises.map { it.id })
        d = d.reorderExercises(10, listOf(2))                       // unlisted ids keep relative order after
        assertEquals(listOf(2L, 3L, 1L), d.plans[0].exercises.map { it.id })
    }

    @Test
    fun perSetRepsAndWeightsRoundTrip() {
        val split = ex.copy(
            sets = 3, reps = "12", weight = 70.0,
            repsPerSet = listOf(12, 10, 8), weightsPerSet = listOf(70.0, 80.0, 75.0),
        )
        val d = Data().upsertPlan(Plan(10, "A")).upsertExercise(10, split, now = 100)
        assertEquals(split, parseData(d.toJson()).plans[0].exercises[0])
        assertEquals(80.0, split.tracked, 0.0)                     // heaviest set is tracked
        assertEquals(ExerciseDefaults(3, "12", 80.0), d.defaults)
    }

    @Test
    fun cardRepsHidesSingleSetCount() {
        assertEquals("× 10", ex.copy(sets = 1, reps = "10").cardReps())
        assertEquals("3 × 10", ex.copy(sets = 3, reps = "10", repsPerSet = listOf(10, 10, 10)).cardReps())
        assertEquals("12 / 10 / 8", ex.copy(sets = 3, reps = "12", repsPerSet = listOf(12, 10, 8)).cardReps())
    }

    @Test
    fun cardWeightsShowsVariationsAndTruncatesAboveThree() {
        assertEquals(null, ex.copy(sets = 3, weightsPerSet = listOf(70.0, 70.0, 70.0)).cardWeights())
        assertEquals("70 / 65 / 60", ex.copy(sets = 3, weightsPerSet = listOf(70.0, 65.0, 60.0)).cardWeights())
        assertEquals("70 / 65 / 60 / ...",
            ex.copy(sets = 4, weightsPerSet = listOf(70.0, 65.0, 60.0, 55.0)).cardWeights())
    }

    @Test
    fun cardRepsTruncatesAboveThree() {
        assertEquals("5 × 12", ex.copy(sets = 5, reps = "12", repsPerSet = listOf(12, 12, 12, 12, 12)).cardReps())
        assertEquals("12 / 10 / 8", ex.copy(sets = 3, reps = "12", repsPerSet = listOf(12, 10, 8)).cardReps())
        assertEquals("12 / 10 / 8 / ...", ex.copy(sets = 4, reps = "12", repsPerSet = listOf(12, 10, 8, 6)).cardReps())
    }

    @Test
    fun cloneExerciseToPlanAppendsFreshCopy() {
        val d = Data().upsertPlan(Plan(10, "A", listOf(ex))).upsertPlan(Plan(11, "B"))
        val out = d.cloneExerciseToPlan(10, ex.id, 11, now = 100, id = 99)
        val src = out.plans.first { it.id == 10L }.exercises
        val dst = out.plans.first { it.id == 11L }.exercises
        assertEquals(listOf(ex), src)                              // source untouched
        assertEquals(listOf(ex.copy(id = 99)), dst)                // appended, fresh id
        assertEquals(listOf(WeightEntry(100, ex.tracked)), out.history[99L])
    }

    @Test
    fun legacyExerciseWithoutBreakdownLoads() {
        val json = """{"plans":[{"id":10,"name":"A","exercises":[{"id":1,"name":"Leg press","sets":4,"reps":"8-12","weight":80,"seat":"Seat 5","notes":""}]}]}"""
        val e = parseData(json).plans[0].exercises[0]
        assertEquals(emptyList<Int>(), e.repsPerSet)
        assertEquals(emptyList<Double>(), e.weightsPerSet)
        assertEquals(80.0, e.tracked, 0.0)
    }

    @Test
    fun jsonRoundTrip() {
        val d = Data()
            .upsertPlan(Plan(10, "A"))
            .upsertExercise(10, ex, now = 100)
            .copy(body = listOf(WeightEntry(5, 82.5)))
            .toggleDone(1, now = 1_700_000_000_000L)
            .upsertExercise(10, Exercise(2, "Treadmill", 1, "", 0.0, "", "", true, 25, "8 km/h, 3%"), now = 100)
        assertEquals(d, parseData(d.toJson()))
    }

    @Test
    fun exportedBackupImportsBackUnchanged() {
        val d = Data()
            .upsertPlan(Plan(10, "A"))
            .upsertExercise(10, ex, now = 100)
            .copy(body = listOf(WeightEntry(5, 82.5)))
            .toggleDone(1, now = 1_700_000_000_000L)
        assertEquals(d, parseBackup(d.toJson()))
    }

    @Test
    fun backupMustBeAJsonObjectWithPlans() {
        assertNull(parseBackup(""))
        assertNull(parseBackup("not json"))
        assertNull(parseBackup("[]"))
        assertNull(parseBackup("{}"))
        assertNull(parseBackup("""{"foo":1}"""))
        assertNull(parseBackup("""{"plans":{}}"""))
        assertNull(parseBackup("""{"plans":[{"name":"missing id"}]}"""))
        assertEquals(Data(), parseBackup("""{"plans":[]}"""))     // an emptied app is a valid backup
        assertEquals(listOf("A"), parseBackup("""{"plans":[{"id":10,"name":"A"}]}""")?.plans?.map { it.name })
    }

    @Test
    fun oversizedImportIsRejected() {
        val fit = ByteArray(MAX_BACKUP_BYTES) { 'x'.code.toByte() }
        assertEquals(MAX_BACKUP_BYTES, fit.inputStream().readTextAtMost()?.length)
        val tooBig = fit + 'x'.code.toByte()
        assertNull(tooBig.inputStream().readTextAtMost())
    }
}
