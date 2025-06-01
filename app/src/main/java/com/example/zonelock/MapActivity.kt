package com.example.zonelock

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Bundle
import android.os.Looper
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.MapView
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.PolygonOptions


class MapActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var mapView: MapView
    private lateinit var  googleMap: GoogleMap
    private val LOCATION_PERMISSION_REQUEST_CODE = 1000

    //추가
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationRequest: com.google.android.gms.location.LocationRequest
    private lateinit var locationCallback: LocationCallback

    private val polygonCoords = listOf(
        LatLng(35.146421, 129.006428),
        LatLng(35.145915, 129.006488),
        LatLng(35.145510, 129.006725),
        LatLng(35.145694, 129.007742),
        LatLng(35.146064, 129.007747),
        LatLng(35.146178, 129.008721),
        LatLng(35.146597, 129.008568)
    )


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_map)

//      MapView 연결 및 초기화
        mapView = findViewById(R.id.mapView)
        mapView.onCreate(savedInstanceState)
        mapView.getMapAsync(this)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        val btnBack = findViewById<Button>(R.id.btn_back_to_admin)
        btnBack.setOnClickListener {
            finish() // 이전 액티비티(AdminActivity)로 돌아감
        }

    }
    //      마커 추가랑 이동 기능
    override fun onMapReady(googleMap: GoogleMap) {
        this.googleMap = googleMap
        checkLocationPermission()
        googleMap.mapType = GoogleMap.MAP_TYPE_NORMAL //지도 스타일은 노멀


        val polygonOptions = PolygonOptions()
            .addAll(polygonCoords)
            .strokeColor(Color.RED) //선색깔
            .strokeWidth(6f) //선두께
            .fillColor(Color.TRANSPARENT) //내부 투명
        googleMap.addPolygon(polygonOptions)

        checkLocationPermission() //이게 isMy 머시기 앞에 보장되어 있지 않으면 오류남
    }

    //일단 여기서 부터 수정
    private fun checkLocationPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            !=PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                LOCATION_PERMISSION_REQUEST_CODE
            )
        } else {
            startLocationUpdates()
        }
    }

    private var isLocked = false //이거도 감지 기능할 때 추가

    private fun startLocationUpdates() {

        locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 3000).build()

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(p0: LocationResult) {
                super.onLocationResult(p0)
                val location = p0.lastLocation
                if (location != null) {
                    val userLatLng = LatLng(location.latitude, location.longitude)
                    googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(userLatLng, 18f))

                    if (!isLocked && !isInsidePolygon(userLatLng, polygonCoords)) {
                        isLocked = true
                        val intent = Intent(this@MapActivity, LockActivity::class.java)
                        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                        startActivity(intent)
                    } //사용자가 폴리곤을 벗어났을 때 무조건 잠금 화면으로 이동하고, MapActivity를 포함한 이전 액티비티로 절대 돌아가지 못하게 하려는 목적
                }
            }
        }

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            == PackageManager.PERMISSION_GRANTED) {

            googleMap.isMyLocationEnabled = true

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
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE &&
            grantResults.isNotEmpty() &&
            grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            startLocationUpdates()
        }
    }
    //추가요
    private fun isInsidePolygon(point: LatLng, polygon: List<LatLng>): Boolean {
        var intersectCount = 0
        for (i in polygon.indices) {
            val j = (i + 1) % polygon.size
            val p1 = polygon[i]
            val p2 = polygon[j]
            if (rayCastIntersect(point, p1, p2)) {
                intersectCount++
            }
        }
        return (intersectCount % 2 == 1)
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

    //생명주기? 연결이래요
    override fun onResume() {
        super.onResume()
        mapView.onResume()
    }

    override fun onPause() {
        super.onPause()
        mapView.onPause()
    }

    override fun onStart() {
        super.onStart()
        mapView.onStart()
    }

    override fun onStop() {
        super.onStop()
        mapView.onStop()
    }

    override fun onDestroy() {
        super.onDestroy()
        mapView.onDestroy()
    }

    override fun onLowMemory() {
        super.onLowMemory()
        mapView.onLowMemory()
    }

}