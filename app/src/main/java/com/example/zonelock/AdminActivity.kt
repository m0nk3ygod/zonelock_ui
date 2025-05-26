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
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices

class AdminActivity : AppCompatActivity() {

    private lateinit var fusedLocationClient: FusedLocationProviderClient

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_admin)

        // 현재 연결된 SSID 출력
        val ssidTextView = findViewById<TextView>(R.id.txt_ssid)
        val wifiManager = applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
        val wifiInfo = wifiManager.connectionInfo
        val ssid = wifiInfo?.ssid ?: "알 수 없음"
        ssidTextView.text = "현재 연결된 와이파이는 $ssid 입니다."

        // 위치 서비스 초기화
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        // 위치 출력용 TextView
        val locationTextView = findViewById<TextView>(R.id.txt_location)

        // 위치 권한 확인
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
            ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ), 100)
            return
        }

        //위치 가져오기
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

        //버튼 : LockActivity로 이동
        val toLockBtn = findViewById<Button>(R.id.btn_to_lock)
        toLockBtn.setOnClickListener {
            val intent = Intent(this, LockActivity::class.java)
            startActivity(intent)
        }

        //버튼 : NetworkMonitorService 시작
        val btn = findViewById<Button>(R.id.btn_start_network_service)
        btn.setOnClickListener {
            val intent = Intent(this, NetworkMonitorService::class.java)
            startService(intent)
        }

        // 버튼 : MapActivity 열기
        val btnOpenMap = findViewById<Button>(R.id.btn_open_map)
        btnOpenMap.setOnClickListener {
            val intent = Intent(this, MapActivity::class.java)
            startActivity(intent)
        }

        // Device Admin 관련 기능 추가
        val devicePolicyManager = getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
        val compName = ComponentName(this, MyDeviceAdminReceiver::class.java)

        // 버튼: 관리자 권한 활성화
        val btnEnable = findViewById<Button>(R.id.btn_enable_admin)
        btnEnable.setOnClickListener {
            if (!devicePolicyManager.isAdminActive(compName)) {
                val intent = Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN)
                intent.putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, compName)
                intent.putExtra(DevicePolicyManager.EXTRA_ADD_EXPLANATION, "앱 삭제 방지를 위해 관리자 권한이 필요합니다.")
                startActivity(intent)
            }
        }

        // 버튼: 관리자 권한 해제
        val btnDisable = findViewById<Button>(R.id.btn_disable_admin)
        btnDisable.setOnClickListener {
            if (devicePolicyManager.isAdminActive(compName)) {
                devicePolicyManager.removeActiveAdmin(compName)
            }
        }
    }
}