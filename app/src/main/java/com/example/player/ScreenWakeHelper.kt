package com.example.player

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.view.WindowManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalContext

/**
 * Reference-counted anti-sleep window flag coordinator so overlapping inline preview
 * and full-screen player transitions never prematurely clear FLAG_KEEP_SCREEN_ON.
 */
object ScreenWakeCoordinator {
    private val activeTokens = mutableSetOf<String>()

    fun setKeepScreenOn(activity: Activity?, token: String, enabled: Boolean) {
        val window = activity?.window ?: return
        synchronized(activeTokens) {
            if (enabled) {
                activeTokens.add(token)
            } else {
                activeTokens.remove(token)
            }
            if (activeTokens.isNotEmpty()) {
                window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            } else {
                window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            }
        }
    }
}

@Composable
fun KeepScreenOnEffect(token: String, enabled: Boolean) {
    val context = LocalContext.current
    val activity = context.findActivity()

    DisposableEffect(activity, token, enabled) {
        ScreenWakeCoordinator.setKeepScreenOn(activity, token, enabled)
        onDispose {
            ScreenWakeCoordinator.setKeepScreenOn(activity, token, false)
        }
    }
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
