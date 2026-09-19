package com.auracode.hiposim

import android.content.Context
import android.content.res.Configuration
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.auracode.hiposim.core.util.DEMO_LANGUAGE_TAG
import dagger.hilt.android.AndroidEntryPoint
import java.util.Locale

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(newBase.withDemoLocale())
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
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
