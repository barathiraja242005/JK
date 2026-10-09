package com.barathiraja.jk.steps

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat

/** Whether JK may read the step counter (activity recognition; always allowed before Android 10). */
fun Context.canCountSteps(): Boolean = Build.VERSION.SDK_INT < 29 ||
    ContextCompat.checkSelfPermission(this, Manifest.permission.ACTIVITY_RECOGNITION) == PackageManager.PERMISSION_GRANTED

/** The step permission as screen state: [granted], and [ask] to show the system prompt. */
class StepPermission(val granted: Boolean, val ask: () -> Unit)

/** Remembers the step permission; [onGranted] runs when the person allows it (start counting then). */
@Composable
fun rememberStepPermission(onGranted: () -> Unit): StepPermission {
    val context = LocalContext.current
    var granted by remember { mutableStateOf(context.canCountSteps()) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { ok ->
        granted = ok
        if (ok) onGranted()
    }
    return StepPermission(granted) { launcher.launch(Manifest.permission.ACTIVITY_RECOGNITION) }
}
