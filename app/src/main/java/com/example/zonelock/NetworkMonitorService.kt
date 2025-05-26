package com.example.zonelock

import android.app.Service
import android.content.Context
import android.content.Intent
import android.net.wifi.WifiManager
import android.os.IBinder
import android.util.Log

class NetworkMonitorService : Service() {
    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        checkCurrentSSID()
        return START_STICKY
    }

    private fun checkCurrentSSID() {
        val wifiManager = applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
        val info = wifiManager.connectionInfo
        val ssid = info.ssid

        Log.d("NetworkMonitorService", "현재 연결된 SSID: $ssid")
        // 등록된 SSID와 비교하는 로직 추가 예정
    }
}