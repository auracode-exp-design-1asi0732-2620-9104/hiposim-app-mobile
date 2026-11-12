package com.auracode.hiposim

import android.content.Context
import android.content.res.Configuration
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.auracode.hiposim.core.analytics.UsageEvents
import com.auracode.hiposim.core.analytics.UsageTracker
import com.auracode.hiposim.core.util.DEMO_LANGUAGE_TAG
import dagger.hilt.android.AndroidEntryPoint
import java.util.Locale
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @Inject
    lateinit var usageTracker: UsageTracker

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(newBase.withDemoLocale())
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Only a real launch: a rotation or a process restore brings a saved state.
        if (savedInstanceState == null) {
            usageTracker.track(
                UsageEvents.APP_OPENED,
                mapOf(
                    "appVersion" to BuildConfig.VERSION_NAME,
                    "language" to resources.configuration.locales[0].language,
                ),
            )
        }
        enableEdgeToEdge()
        setContent { HipoSimRoot() }
    }
}

private fun Context.withDemoLocale(): Context {
    if (DEMO_LANGUAGE_TAG.isEmpty()) return this
    val configuration = Configuration(resources.configuration)
    configuration.setLocale(Locale.forLanguageTag(DEMO_LANGUAGE_TAG))
    return createConfigurationContext(configuration)
}
