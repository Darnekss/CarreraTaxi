package com.example.carrerastaxi.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.carrerastaxi.utils.Prefs

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        val cfg = Prefs.load(context)
        if (cfg.journeyActive || cfg.autoStart) {
            context.startForegroundService(Intent(context, LocationService::class.java))
        }
    }
}
