package com.example.zonelock

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.util.Log

class LocationMonitorService : Service() {
    override fun onCreate() {
        super.onCreate()
        Log.d("LocationMonitorService", "GPS 감지 서비스 시작됨")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // 추후 위치 감지 로직 작성
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d("LocationMonitorService", "GPS 감지 서비스 종료됨")
    }

    override fun onBind(intent: Intent?): IBinder? = null
}