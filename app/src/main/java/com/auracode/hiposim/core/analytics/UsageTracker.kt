package com.auracode.hiposim.core.analytics

/**
 * Anonymous usage events for the experiments of the report (TS10b). The names are the closed list that
 * `POST /api/events` accepts; the API rejects any other.
 */
object UsageEvents {
    const val APP_OPENED = "app_opened"
    const val SIMULATION_STARTED = "simulation_started"
    const val SIMULATION_COMPLETED = "simulation_completed"
    const val SCHEDULE_OPENED = "schedule_opened"
    const val SCENARIO_SAVED = "scenario_saved"
    const val SCENARIOS_COMPARED = "scenarios_compared"
    const val LEAD_SENT = "lead_sent"
}

/**
 * Records that something happened in the app. It never blocks and never fails: an event that cannot be sent is
 * dropped, because measuring must not get in the way of the buyer.
 *
 * Properties carry only figures, flags and short codes (term in years, grace type...). Never a name, an email, a
 * phone or an identifier of the person: the API refuses those keys, and the session is a random id per launch.
 */
interface UsageTracker {
    fun track(
        name: String,
        properties: Map<String, Any> = emptyMap(),
    )

    companion object {
        /** Tracks nothing. The default where a class is built by hand, as in the unit tests. */
        val None: UsageTracker =
            object : UsageTracker {
                override fun track(
                    name: String,
                    properties: Map<String, Any>,
                ) = Unit
            }
    }
}
