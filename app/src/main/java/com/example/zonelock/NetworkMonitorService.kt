package com.example.zonelock

import android.app.Service
import android.content.Context
import android.content.Intent
import android.net.wifi.WifiManager
import android.os.Handler
import android.os.IBinder
import android.util.Log
import android.widget.Toast
import okhttp3.*
import org.json.JSONArray
import java.io.IOException

class NetworkMonitorService : Service() {

    private val TAG = "NetworkMonitorService"
    private val apiUrl = "https://c96c-203-241-183-12.ngrok-free.app/ssid" // ← ngrok 주소 바뀌면 갱신할 것

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Thread {
            try {
                val wifiManager = applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
                val wifiInfo = wifiManager.connectionInfo
                val currentSSID = wifiInfo.ssid.replace("\"", "")
                Log.d(TAG, "현재 SSID: $currentSSID")

                val client = OkHttpClient()
                val request = Request.Builder()
                    .url(apiUrl)
                    .build()

                client.newCall(request).enqueue(object : Callback {
                    override fun onFailure(call: Call, e: IOException) {
                        Log.e(TAG, "API 요청 실패: ${e.message}")
                        showToast("API 요청 실패: ${e.message}")
                    }

                    override fun onResponse(call: Call, response: Response) {
                        if (response.isSuccessful) {
                            val responseBody = response.body?.string()
                            val jsonArray = JSONArray(responseBody)
                            val ssidList = mutableListOf<String>()
                            for (i in 0 until jsonArray.length()) {
                                ssidList.add(jsonArray.getString(i))
                            }

                            Log.d(TAG, "DB SSID 목록: $ssidList")

                            val isMatched = currentSSID in ssidList
                            val message = if (isMatched) {
                                "✔ SSID 일치: $currentSSID"
                            } else {
                                "❌ SSID 불일치: $currentSSID"
                            }

                            Log.d(TAG, message)
                            showToast(message)

                            // 🔒 SSID가 불일치하면 LockActivity로 이동
                            if (!isMatched) {
                                val intent = Intent(applicationContext, LockActivity::class.java).apply {
                                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                }
                                startActivity(intent)
                            }

                        } else {
                            Log.e(TAG, "API 응답 실패: ${response.code}")
                            showToast("API 응답 실패: ${response.code}")
                        }
                    }
                })

            } catch (e: Exception) {
                Log.e(TAG, "오류 발생: ${e.message}", e)
                showToast("오류 발생: ${e.message}")
            }
        }.start()

        return START_NOT_STICKY
    }

    private fun showToast(message: String) {
        Handler(mainLooper).post {
            Toast.makeText(applicationContext, message, Toast.LENGTH_LONG).show()
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
