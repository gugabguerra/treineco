package com.example.gymplan

import org.junit.Assert.assertEquals
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
    fun completionIsPerDay() {
        val d = Data().upsertPlan(Plan(10, "A")).upsertExercise(10, ex).toggleDone(1, day = 5)
        assertEquals(true, d.isDone(1, day = 5))
        assertEquals(false, d.isDone(1, day = 6))           // next day starts unchecked
        assertEquals(false, d.toggleDone(1, day = 5).isDone(1, day = 5))
        assertEquals(emptyMap<Long, Long>(), d.deleteExercise(10, 1).done)
        assertEquals(false, d.clearDone(listOf(1L)).isDone(1, day = 5))
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
    fun jsonRoundTrip() {
        val d = Data()
            .upsertPlan(Plan(10, "A"))
            .upsertExercise(10, ex, now = 100)
            .copy(body = listOf(WeightEntry(5, 82.5)))
            .toggleDone(1, day = 20000)
            .upsertExercise(10, Exercise(2, "Treadmill", 1, "", 0.0, "", "", true, 25, "8 km/h, 3%"), now = 100)
        assertEquals(d, parseData(d.toJson()))
    }
}
