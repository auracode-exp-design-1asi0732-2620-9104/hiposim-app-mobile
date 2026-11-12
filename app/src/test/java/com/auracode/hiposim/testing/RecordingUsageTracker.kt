package com.auracode.hiposim.testing

import com.auracode.hiposim.core.analytics.UsageTracker

/** Keeps the events it is given, in order, so a test can check what was measured. */
class RecordingUsageTracker : UsageTracker {
    val events = mutableListOf<Pair<String, Map<String, Any>>>()

    val names: List<String> get() = events.map { it.first }

    override fun track(
        name: String,
        properties: Map<String, Any>,
    ) {
        events += name to properties
    }
}
