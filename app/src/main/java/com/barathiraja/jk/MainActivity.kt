package com.barathiraja.jk

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import android.graphics.Color
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.DisposableEffect
import com.barathiraja.jk.data.ThemeMode
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.barathiraja.jk.data.ExerciseRepo
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.barathiraja.jk.ui.JkRoot
import com.barathiraja.jk.ui.JkViewModel
import com.barathiraja.jk.ui.TrainingViewModel
import com.barathiraja.jk.ui.theme.JkTheme

class MainActivity : ComponentActivity() {
    private val container get() = (application as JkApp).container
    private val vm: JkViewModel by viewModels { JkViewModel.factory(container, application) }
    private val tvm: TrainingViewModel by viewModels { TrainingViewModel.factory(container) }

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen().setKeepOnScreenCondition { !ExerciseRepo.ready }
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val settings by vm.settings.collectAsStateWithLifecycle()
            // Status/nav bar icons follow JK's theme, not the phone's, so they stay visible.
            val dark = when (settings.theme) {
                ThemeMode.SYSTEM -> isSystemInDarkTheme()
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
            }
            DisposableEffect(dark) {
                val bars = if (dark) SystemBarStyle.dark(Color.TRANSPARENT)
                else SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT)
                enableEdgeToEdge(statusBarStyle = bars, navigationBarStyle = bars)
                onDispose {}
            }
            JkTheme(settings.theme) {
                Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) { JkRoot(vm, tvm) }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        val granted = Build.VERSION.SDK_INT < 29 ||
            ContextCompat.checkSelfPermission(this, Manifest.permission.ACTIVITY_RECOGNITION) == PackageManager.PERMISSION_GRANTED
        if (granted) container.steps.start()
        tvm.refreshToday() // a new day may have started while the app was in the background
    }

    override fun onStop() {
        container.steps.stop()
        super.onStop()
    }
}
