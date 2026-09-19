package com.auracode.hiposim

import android.app.Application
import androidx.compose.runtime.Composable
import androidx.navigation.compose.rememberNavController
import com.auracode.hiposim.core.designsystem.theme.HipoSimTheme
import com.auracode.hiposim.core.navigation.HipoSimNavHost
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class HipoSimApp : Application()

/** Root composable: theme plus navigation. */
@Composable
fun HipoSimRoot() {
    HipoSimTheme {
        HipoSimNavHost(navController = rememberNavController())
    }
}
