package com.auracode.hiposim.core.analytics

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import java.math.BigDecimal
import java.util.UUID

/**
 * Sends each event to `POST /api/events` in the background, with source `mobile` and a random session id that
 * lives as long as the process. Failures (offline, server down, a rejected event) are ignored on purpose.
 */
class ApiUsageTracker(
    private val service: EventsService,
    private val scope: CoroutineScope,
    val sessionId: String = newSessionId(),
) : UsageTracker {
    override fun track(
        name: String,
        properties: Map<String, Any>,
    ) {
        val request =
            UsageEventRequestDto(
                sessionId = sessionId,
                source = SOURCE,
                name = name,
                properties = properties.toJson(),
            )
        scope.launch {
            try {
                service.sendEvent(request)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                // Measuring must never get in the way of the buyer: the event is dropped.
            }
        }
    }

    companion object {
        const val SOURCE = "mobile"

        /** 32 hex characters, inside the `^[A-Za-z0-9_-]{8,64}$` the API asks for. */
        fun newSessionId(): String = UUID.randomUUID().toString().replace("-", "")

        /** Only text, numbers and flags travel; anything else is left out. */
        internal fun Map<String, Any>.toJson(): JsonObject? {
            if (isEmpty()) return null
            val values =
                mapNotNull { (key, value) ->
                    val primitive =
                        when (value) {
                            is Boolean -> JsonPrimitive(value)
                            is BigDecimal -> JsonPrimitive(value.toDouble())
                            is Number -> JsonPrimitive(value)
                            is String -> JsonPrimitive(value.take(MAX_TEXT_LENGTH))
                            is Enum<*> -> JsonPrimitive(value.name.lowercase())
                            else -> null
                        }
                    primitive?.let { key to it }
                }.toMap()
            return if (values.isEmpty()) null else JsonObject(values)
        }

        private const val MAX_TEXT_LENGTH = 100
    }
}
