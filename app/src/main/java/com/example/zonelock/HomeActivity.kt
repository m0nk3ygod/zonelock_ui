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

class HomeActivity : AppCompatActivity() {

    private lateinit var txtSsid: TextView
    private lateinit var txtLocation: TextView
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationRequest: LocationRequest
    private lateinit var locationCallback: LocationCallback

    private val LOCATION_PERMISSION_REQUEST_CODE = 1001

    // 관리자 진입 터치 감지
    private var secretTapCount = 0
    private val resetDelay: Long = 3000
    private val handler = Handler(Looper.getMainLooper())
    private val resetTapRunnable = Runnable {
        secretTapCount = 0
    }

    private val correctPassword = "1234"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            stopLockTask()
        }

        txtSsid = findViewById(R.id.txt_ssid_home)
        txtLocation = findViewById(R.id.txt_location_home)
        val btnToMap = findViewById<Button>(R.id.btn_to_map_home)

        // 버튼 클릭 시 지도 보기
        btnToMap.setOnClickListener {
            val intent = Intent(this, MapActivity::class.java)
            intent.putExtra("view_only", true)
            startActivity(intent)
        }

        // 현재 SSID 출력
        val wifiManager = applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
        val ssid = wifiManager.connectionInfo.ssid.replace("\"", "")
        txtSsid.text = "현재 연결된 와이파이: $ssid"

        // 위치 권한 확인
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        checkLocationPermission()
    }

    private fun checkLocationPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED
        ) {
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
            == PackageManager.PERMISSION_GRANTED
        ) {
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
        grantResults: IntArray,
    ) {
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE && grantResults.isNotEmpty()
            && grantResults[0] == PackageManager.PERMISSION_GRANTED
        ) {
            requestLocation()
        }
    }

    // 우측 상단 5번 터치 시 → 비밀번호 확인 후 AdminActivity 이동
    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (event.action == MotionEvent.ACTION_DOWN) {
            val screenWidth = resources.displayMetrics.widthPixels
            val screenHeight = resources.displayMetrics.heightPixels
            val x = event.x
            val y = event.y

            // 화면의 오른쪽 상단 1/4 영역 감지
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
