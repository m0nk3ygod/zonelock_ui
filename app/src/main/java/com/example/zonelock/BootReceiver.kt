package com.example.zonelock

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.content.ContextCompat

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            Log.d("BootReceiver", "기기 부팅됨 - LockActivity 실행")
            val startIntent = Intent(context, LockActivity::class.java)
            startIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(startIntent)
            val serviceIntent = Intent(context, NetworkMonitorService::class.java)
            ContextCompat.startForegroundService(context, serviceIntent)

            val gpsIntent = Intent(context, LocationMonitorService::class.java)
            ContextCompat.startForegroundService(context, gpsIntent)

            val netIntent = Intent(context, NetworkMonitorService::class.java)
            ContextCompat.startForegroundService(context, netIntent)
        }
    }
}