package com.example.zonelock

import android.Manifest
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.net.wifi.WifiManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.MotionEvent
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.location.*
import okhttp3.*
import org.json.JSONArray
import java.io.IOException

class HomeActivity : AppCompatActivity() {

    private lateinit var txtSsid: TextView
    private lateinit var txtLocation: TextView
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationRequest: LocationRequest
    private lateinit var locationCallback: LocationCallback

    private val LOCATION_PERMISSION_REQUEST_CODE = 1001
    private val apiUrl = "https://c96c-203-241-183-12.ngrok-free.app"

    private var secretTapCount = 0
    private val resetDelay: Long = 3000
    private val handler = Handler(Looper.getMainLooper())
    private val resetTapRunnable = Runnable { secretTapCount = 0 }

    private val correctPassword = "1234"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)



        txtSsid = findViewById(R.id.txt_ssid_home)
        txtLocation = findViewById(R.id.txt_location_home)
        val btnToMap = findViewById<Button>(R.id.btn_to_map_home)

        val networkIntent = Intent(this, NetworkMonitorService::class.java)
        ContextCompat.startForegroundService(this, networkIntent)

        val locationIntent = Intent(this, LocationMonitorService::class.java)
        ContextCompat.startForegroundService(this, locationIntent)

        val gpsIntent = Intent(this, LocationMonitorService::class.java)
        startService(gpsIntent)

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            stopLockTask()
        }

        btnToMap.setOnClickListener {
            val intent = Intent(this, MapActivity::class.java)
            intent.putExtra("view_only", true)
            startActivity(intent)
        }

        val wifiManager = applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
        val ssid = wifiManager.connectionInfo.ssid.replace("\"", "")
        txtSsid.text = "현재 연결된 와이파이: $ssid"

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        checkLocationPermission()

        // → 여기서도 강제로 Lock 조건 검사 시작
        checkSSIDAndLocation()
    }

    private fun checkSSIDAndLocation() {
        // 1. SSID 확인
        val wifiManager = applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
        val currentSSID = wifiManager.connectionInfo.ssid.replace("\"", "")

        val ssidRequest = Request.Builder().url("$apiUrl/ssid").build()
        OkHttpClient().newCall(ssidRequest).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                e.printStackTrace()
            }

            override fun onResponse(call: Call, response: Response) {
                if (response.isSuccessful) {
                    val json = response.body?.string() ?: return
                    val array = JSONArray(json)
                    val ssidList = List(array.length()) { array.getString(it) }
                    if (currentSSID !in ssidList) {
                        goToLock()
                    }
                }
            }
        })

        // 2. 위치 확인
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            == PackageManager.PERMISSION_GRANTED) {
            fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                if (location != null) {
                    val lat = location.latitude
                    val lng = location.longitude

                    val locationRequest = Request.Builder()
                        .url("$apiUrl/coords")
                        .build()

                    OkHttpClient().newCall(locationRequest).enqueue(object : Callback {
                        override fun onFailure(call: Call, e: IOException) {}
                        override fun onResponse(call: Call, response: Response) {
                            if (response.isSuccessful) {
                                val responseBody = response.body?.string() ?: return
                                val jsonArray = JSONArray(responseBody)
                                val polygon = Array(jsonArray.length()) {
                                    val obj = jsonArray.getJSONObject(it)
                                    Pair(obj.getDouble("lat"), obj.getDouble("lng"))
                                }

                                if (!pointInPolygon(lat, lng, polygon)) {
                                    goToLock()
                                }
                            }
                        }
                    })
                }
            }
        }
    }

    private fun goToLock() {
        val intent = Intent(this, LockActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        startActivity(intent)
    }

    private fun pointInPolygon(lat: Double, lng: Double, polygon: Array<Pair<Double, Double>>): Boolean {
        var inside = false
        var j = polygon.size - 1
        for (i in polygon.indices) {
            val xi = polygon[i].first
            val yi = polygon[i].second
            val xj = polygon[j].first
            val yj = polygon[j].second

            val intersect = (yi > lng) != (yj > lng) &&
                    lat < (xj - xi) * (lng - yi) / (yj - yi + 0.00000001) + xi
            if (intersect) inside = !inside
            j = i
        }
        return inside
    }

    private fun checkLocationPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                LOCATION_PERMISSION_REQUEST_CODE
            )
        } else {
            requestLocation()
        }
    }

    private fun requestLocation() {
        locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 3000).build()
        locationCallback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                val location: Location? = result.lastLocation
                if (location != null) {
                    val lat = location.latitude
                    val lng = location.longitude
                    txtLocation.text = "현재 위치: $lat, $lng"
                }
            }
        }

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            == PackageManager.PERMISSION_GRANTED) {
            fusedLocationClient.requestLocationUpdates(
                locationRequest,
                locationCallback,
                Looper.getMainLooper()
            )
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray ,
    ) {
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE && grantResults.isNotEmpty()
            && grantResults[0] == PackageManager.PERMISSION_GRANTED
        ) {
            requestLocation()
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (event.action == MotionEvent.ACTION_DOWN) {
            val screenWidth = resources.displayMetrics.widthPixels
            val screenHeight = resources.displayMetrics.heightPixels
            val x = event.x
            val y = event.y

            if (x > screenWidth * 0.75 && y < screenHeight * 0.25) {
                secretTapCount++
                handler.removeCallbacks(resetTapRunnable)
                handler.postDelayed(resetTapRunnable, resetDelay)

                if (secretTapCount >= 5) {
                    secretTapCount = 0
                    showPasswordDialog()
                }
            } else {
                secretTapCount = 0
            }
        }
        return super.onTouchEvent(event)
    }

    private fun showPasswordDialog() {
        val editText = EditText(this)
        editText.hint = "비밀번호 입력"

        AlertDialog.Builder(this)
            .setTitle("관리자 비밀번호")
            .setView(editText)
            .setPositiveButton("확인") { _, _ ->
                if (editText.text.toString() == correctPassword) {
                    val intent = Intent(this@HomeActivity, AdminActivity::class.java)
                    startActivity(intent)
                } else {
                    Toast.makeText(this, "비밀번호가 틀렸습니다.", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("취소", null)
            .show()
    }
}
