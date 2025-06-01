package com.example.zonelock

import android.Manifest
import android.app.Service
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.os.IBinder
import android.os.Looper
import android.util.Log
import androidx.core.app.ActivityCompat
import com.google.android.gms.location.*
import com.google.android.gms.maps.model.LatLng

class LocationMonitorService : Service() {

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationRequest: LocationRequest
    private lateinit var locationCallback: LocationCallback
    private var isLocked = false

    private val polygonCoords = listOf(
        LatLng(35.146421, 129.006428),
        LatLng(35.145915, 129.006488),
        LatLng(35.145510, 129.006725),
        LatLng(35.145694, 129.007742),
        LatLng(35.146064, 129.007747),
        LatLng(35.146178, 129.008721),
        LatLng(35.146597, 129.008568)
    )

    override fun onCreate() {
        super.onCreate()
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 3000).build()

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                val location = locationResult.lastLocation ?: return
                val current = LatLng(location.latitude, location.longitude)
                Log.d("LocationMonitor", "현재 위치: $current")

                if (!isLocked && !isInsidePolygon(current, polygonCoords)) {
                    isLocked = true
                    Log.d("LocationMonitor", "구역 이탈! LockActivity 실행")
                    val intent = Intent(applicationContext, LockActivity::class.java).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                    }
                    startActivity(intent)
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
        } else {
            Log.e("LocationMonitor", "위치 권한 없음")
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        fusedLocationClient.removeLocationUpdates(locationCallback)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d("LocationMonitor", "서비스 시작됨")
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun isInsidePolygon(point: LatLng, polygon: List<LatLng>): Boolean {
        var intersectCount = 0
        for (i in polygon.indices) {
            val j = (i + 1) % polygon.size
            val p1 = polygon[i]
            val p2 = polygon[j]
            if (rayCastIntersect(point, p1, p2)) intersectCount++
        }
        return intersectCount % 2 == 1
    }

    private fun rayCastIntersect(point: LatLng, p1: LatLng, p2: LatLng): Boolean {
        val aY = p1.latitude
        val bY = p2.latitude
        val aX = p1.longitude
        val bX = p2.longitude
        val pY = point.latitude
        val pX = point.longitude

        if ((pY > minOf(aY, bY)) && (pY <= maxOf(aY, bY)) &&
            (pX <= maxOf(aX, bX)) && aY != bY) {
            val xinters = (pY - aY) * (bX - aX) / (bY - aY) + aX
            return pX <= xinters
        }
        return false
    }
}
