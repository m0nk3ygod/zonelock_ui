package com.example.zonelock

import android.Manifest
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.net.wifi.WifiManager
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices

class AdminActivity : AppCompatActivity() {

    private lateinit var fusedLocationClient: FusedLocationProviderClient

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_admin)

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            stopLockTask()
        }

        // ✅ 현재 연결된 SSID 출력
        val ssidTextView = findViewById<TextView>(R.id.txt_ssid)
        val wifiManager = applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
        val wifiInfo = wifiManager.connectionInfo
        val ssid = wifiInfo?.ssid ?: "알 수 없음"
        ssidTextView.text = "현재 연결된 와이파이는 $ssid 입니다."

        // ✅ 위치 클라이언트 초기화
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        val locationTextView = findViewById<TextView>(R.id.txt_location)

        // ✅ 위치 권한 확인 및 요청
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(this, arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ), 100)
        } else {
            // ✅ 위치 가져오기
            fusedLocationClient.lastLocation
                .addOnSuccessListener { location: Location? ->
                    location?.let {
                        val lat = it.latitude
                        val lon = it.longitude
                        locationTextView.text = "현재 위치는 $lat, $lon 입니다."
                    } ?: run {
                        locationTextView.text = "위치 정보를 가져올 수 없습니다."
                    }
                }
        }

        // ✅ LockActivity로 이동
        findViewById<Button>(R.id.btn_to_lock).setOnClickListener {
            startActivity(Intent(this, LockActivity::class.java))
        }

        // ✅ NetworkMonitorService 시작
        findViewById<Button>(R.id.btn_start_network_service).setOnClickListener {
            val intent = Intent(this, NetworkMonitorService::class.java)
            startService(intent)
        }

        // ✅ MapActivity 열기
        findViewById<Button>(R.id.btn_open_map).setOnClickListener {
            val intent = Intent(this, MapActivity::class.java)
            intent.putExtra("view_only", true)
            startActivity(intent)
        }

        findViewById<Button>(R.id.btn_go_home).setOnClickListener {
            val intent = Intent(this, HomeActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
            startActivity(intent)
            finish()
        }

        // ✅ Device Admin 관련
        val devicePolicyManager = getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
        val compName = ComponentName(this, MyDeviceAdminReceiver::class.java)

        // 관리자 권한 활성화
        findViewById<Button>(R.id.btn_enable_admin).setOnClickListener {
            if (!devicePolicyManager.isAdminActive(compName)) {
                val intent = Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN).apply {
                    putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, compName)
                    putExtra(DevicePolicyManager.EXTRA_ADD_EXPLANATION, "앱 삭제 방지를 위해 관리자 권한이 필요합니다.")
                }
                startActivity(intent)
            }
        }

        // 관리자 권한 해제
        findViewById<Button>(R.id.btn_disable_admin).setOnClickListener {
            if (devicePolicyManager.isAdminActive(compName)) {
                devicePolicyManager.removeActiveAdmin(compName)
            }
        }
    }
}
