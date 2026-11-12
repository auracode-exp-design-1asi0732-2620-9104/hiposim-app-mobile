package com.auracode.hiposim.core.analytics

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject
import retrofit2.http.Body
import retrofit2.http.POST

/** Body of `POST /api/events` (TS09). */
@Serializable
data class UsageEventRequestDto(
    val sessionId: String,
    val source: String,
    val name: String,
    val properties: JsonObject? = null,
)

@Serializable
data class UsageEventResponseDto(
    val id: String,
    val receivedAt: String,
)

/** Anonymous endpoint: it needs no session, so a guest's simulation is measured too. */
interface EventsService {
    @POST("api/events")
    suspend fun sendEvent(
        @Body request: UsageEventRequestDto,
    ): UsageEventResponseDto
}

/** In-memory stand-in used with `USE_FAKE_API`. It keeps what it receives so a test can look at it. */
class FakeEventsService : EventsService {
    private val _received = mutableListOf<UsageEventRequestDto>()
    val received: List<UsageEventRequestDto> get() = synchronized(_received) { _received.toList() }

    override suspend fun sendEvent(request: UsageEventRequestDto): UsageEventResponseDto {
        synchronized(_received) { _received += request }
        return UsageEventResponseDto(id = "fake-${received.size}", receivedAt = "2026-11-12T00:00:00Z")
    }
}
