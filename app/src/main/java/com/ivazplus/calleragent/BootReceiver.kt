package com.ivazplus.calleragent

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat

/**
 * Restarts the listening service after the phone reboots, so the operator
 * doesn't have to remember to reopen the app every time. Starting a
 * foreground service from a BOOT_COMPLETED receiver is one of the
 * explicitly allowed exemptions to Android's background-start
 * restrictions.
 */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED && Prefs.isConfigured(context)) {
            val serviceIntent = Intent(context, CallRelayService::class.java)
            ContextCompat.startForegroundService(context, serviceIntent)
        }
    }
}
