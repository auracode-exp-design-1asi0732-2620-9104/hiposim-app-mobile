package com.auracode.hiposim.core.analytics

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException
import java.math.BigDecimal

/** TS10b: anonymous usage events sent to POST /api/events. */
@OptIn(ExperimentalCoroutinesApi::class)
class ApiUsageTrackerTest {
    private val scope = TestScope(StandardTestDispatcher())

    @Test
    fun events_goWithTheMobileSourceAndOneSessionPerTracker() =
        scope.run {
            val service = FakeEventsService()
            val tracker = ApiUsageTracker(service, this)

            tracker.track(UsageEvents.APP_OPENED)
            tracker.track(
                UsageEvents.SIMULATION_COMPLETED,
                mapOf(
                    "termYears" to 20,
                    "goodPayerBonus" to true,
                    "graceType" to "partial",
                    "rate" to BigDecimal("0.0885"),
                ),
            )
            advanceUntilIdle()

            val (opened, completed) = service.received
            assertEquals("mobile", opened.source)
            assertEquals(UsageEvents.APP_OPENED, opened.name)
            assertNull(opened.properties)
            assertEquals(opened.sessionId, completed.sessionId)
            assertTrue(Regex("^[A-Za-z0-9_-]{8,64}$").matches(completed.sessionId))
            assertEquals(
                buildJsonObject {
                    put("termYears", 20)
                    put("goodPayerBonus", true)
                    put("graceType", "partial")
                    put("rate", 0.0885)
                },
                completed.properties,
            )
        }

    @Test
    fun aNewTracker_hasANewSession() {
        val first = ApiUsageTracker(FakeEventsService(), scope)
        val second = ApiUsageTracker(FakeEventsService(), scope)

        assertTrue(first.sessionId != second.sessionId)
    }

    @Test
    fun failures_areIgnored_andLaterEventsStillGo() =
        scope.run {
            val sent = mutableListOf<String>()
            var fail = true
            val service =
                object : EventsService {
                    override suspend fun sendEvent(request: UsageEventRequestDto): UsageEventResponseDto {
                        if (fail) throw IOException("offline")
                        sent += request.name
                        return UsageEventResponseDto("1", "2026-11-12T00:00:00Z")
                    }
                }
            val tracker = ApiUsageTracker(service, this)

            tracker.track(UsageEvents.APP_OPENED)
            advanceUntilIdle()
            fail = false
            tracker.track(UsageEvents.SCHEDULE_OPENED)
            advanceUntilIdle()

            assertEquals(listOf(UsageEvents.SCHEDULE_OPENED), sent)
        }

    @Test
    fun onlyTextNumbersAndFlagsTravel_andTextIsCut() {
        val json =
            with(ApiUsageTracker.Companion) {
                mapOf("note" to "a".repeat(150), "list" to listOf(1, 2), "months" to 3).toJson()
            }

        assertEquals(setOf("note", "months"), json?.keys)
        assertEquals(JsonPrimitive("a".repeat(100)), json?.get("note"))
    }
}
