package com.example.zonelock

import android.Manifest
import android.app.*
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.wifi.WifiManager
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.Log
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import okhttp3.*
import org.json.JSONArray
import java.io.IOException
import android.content.pm.ServiceInfo

class NetworkMonitorService : Service() {

    private val TAG = "NetworkMonitorService"
    private val apiUrl = "https://4ea1-203-241-183-12.ngrok-free.app/ssid"
    private val handler = Handler(Looper.getMainLooper())
    private val checkInterval: Long = 5000L

    private val checkRunnable = object : Runnable {
        override fun run() {
            checkSSID()
            handler.postDelayed(this, checkInterval)
        }
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        requestPermissionsIfNeeded()
        startForegroundServiceWithDefaultText()
        handler.post(checkRunnable)
        return START_STICKY
    }

    private fun requestPermissionsIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED
            ) {
                Toast.makeText(this, "⚠️ 알림 권한이 필요합니다. 설정에서 허용해주세요.", Toast.LENGTH_LONG).show()
            }
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED
        ) {
            Toast.makeText(this, "⚠️ 위치 권한이 필요합니다. 설정에서 허용해주세요.", Toast.LENGTH_LONG).show()
        }
    }

    private fun startForegroundServiceWithDefaultText() {
        val defaultIntent = Intent(this, HomeActivity::class.java)
        val defaultPendingIntent = PendingIntent.getActivity(
            this, 0, defaultIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notification = NotificationCompat.Builder(this, "ssid_channel")
            .setContentTitle("ZoneLock")
            .setContentText("네트워크 상태 감지 중...")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentIntent(defaultPendingIntent)
            .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(1, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
        } else {
            startForeground(1, notification)
        }
    }

    private fun updateNotificationForMismatch() {
        val lockIntent = Intent(this, LockActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }

        val pendingIntent = PendingIntent.getActivity(
            this, 0, lockIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notification = NotificationCompat.Builder(this, "ssid_channel")
            .setContentTitle("ZoneLock")
            .setContentText("❌ 등록되지 않은 네트워크입니다. 터치 시 잠금됩니다.")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(1, notification)

        // 강제 LockActivity 실행 시도
        try {
            val am = getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
            am.appTasks.firstOrNull()?.moveToFront()
            startActivity(lockIntent)
            Log.d(TAG, "🔐 LockActivity 강제 실행됨")
        } catch (e: Exception) {
            Log.e(TAG, "LockActivity 실행 실패: ${e.message}")
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "ssid_channel",
                "SSID 감시 서비스",
                NotificationManager.IMPORTANCE_HIGH
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    private fun checkSSID() {
        try {
            val wifiManager = applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager

            // 1️⃣ 와이파이 꺼져있으면 바로 잠금
            if (!wifiManager.isWifiEnabled) {
                Log.e(TAG, "📴 Wi-Fi 꺼져 있음 - LockActivity 이동")
                updateNotificationForMismatch()
                return
            }

            val wifiInfo = wifiManager.connectionInfo
            val currentSSID = wifiInfo.ssid.replace("\"", "")
            Log.d(TAG, "현재 SSID: $currentSSID")

            // 2️⃣ SSID가 아예 인식 안 되는 경우도 바로 잠금
            if (currentSSID == "<unknown ssid>") {
                Log.e(TAG, "📛 SSID 인식 실패 - LockActivity 이동")
                updateNotificationForMismatch()
                return
            }

            // 3️⃣ API 요청 → SSID 리스트 확인
            val client = OkHttpClient()
            val request = Request.Builder().url(apiUrl).build()

            client.newCall(request).enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    Log.e(TAG, "🌐 API 요청 실패 - LockActivity 이동: ${e.message}")
                    updateNotificationForMismatch()
                }

                override fun onResponse(call: Call, response: Response) {
                    if (response.isSuccessful) {
                        val responseBody = response.body?.string() ?: return
                        val jsonArray = JSONArray(responseBody)
                        val ssidList = List(jsonArray.length()) { jsonArray.getString(it) }

                        val isMatched = currentSSID in ssidList
                        Log.d(TAG, if (isMatched) "✔ SSID 일치: $currentSSID" else "❌ SSID 불일치: $currentSSID")

                        if (!isMatched) {
                            updateNotificationForMismatch()
                        }
                    } else {
                        Log.e(TAG, "❗ API 응답 실패 (${response.code}) - LockActivity 이동")
                        updateNotificationForMismatch()
                    }
                }
            })
        } catch (e: Exception) {
            Log.e(TAG, "예외 발생 - LockActivity 이동: ${e.message}", e)
            updateNotificationForMismatch()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacks(checkRunnable)
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
