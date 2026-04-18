package com.reelcount.app.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            // AccessibilityService is re-enabled by the system if it was enabled before reboot.
            // Nothing to do here explicitly — Android restores it.
            // This receiver exists as a hook for future foreground service restart logic.
        }
    }
}
